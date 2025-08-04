package com.pcloudai.backend.core.service

import com.pcloudai.backend.core.domain.User
import com.pcloudai.backend.core.repository.UserRepository
import com.pcloudai.backend.dto.request.ChangePasswordRequest
import com.pcloudai.backend.dto.request.UpdateUserProfileRequest
import com.pcloudai.backend.dto.response.UserProfileResponse
import com.pcloudai.backend.exception.BadRequestException
import com.pcloudai.backend.exception.ResourceNotFoundException
import com.pcloudai.backend.util.PasswordUtils
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service interface for user profile
 */
interface IUserProfileService {
    fun getUserProfile(userId: Long): UserProfileResponse
    fun updateUserProfile(userId: Long, request: UpdateUserProfileRequest): UserProfileResponse
    fun changePassword(userId: Long, request: ChangePasswordRequest): Boolean
}

@Singleton
class UserProfileService @Inject constructor(
    private val userRepository: UserRepository,
    private val passwordUtils: PasswordUtils
) : IUserProfileService {

    private val logger = LoggerFactory.getLogger(UserProfileService::class.java)

    override fun getUserProfile(userId: Long): UserProfileResponse {
        logger.info("Getting profile for user: $userId")

        val user = getUserById(userId)
        return UserProfileResponse.fromUser(user)
    }

    override fun updateUserProfile(userId: Long, request: UpdateUserProfileRequest): UserProfileResponse {
        logger.info("Updating profile for user: $userId")

        val user = getUserById(userId)

        // Check if email is already taken by another user
        val existingUserWithEmail = userRepository.findByUsername(request.email)
        if (existingUserWithEmail != null && existingUserWithEmail.id != userId) {
            throw BadRequestException("Email is already in use by another account")
        }

        // Update user fields
        val updated = user.apply {
            firstName = request.firstName
            lastName = request.lastName
            email = request.email
            bio = request.bio
        }

        val updatedUser = userRepository.update(updated)

        return UserProfileResponse.fromUser(updatedUser)
    }

    override fun changePassword(userId: Long, request: ChangePasswordRequest): Boolean {
        logger.info("Changing password for user: $userId")

        val user = getUserById(userId)

        // Verify current password
        if (!passwordUtils.checkPassword(request.currentPassword, user.password)) {
            throw BadRequestException("Current password is incorrect")
        }

        // Prevent no-op
        if (passwordUtils.checkPassword(request.newPassword, user.password)) {
            throw BadRequestException("New password must differ from the old password")
        }

        // Update password
        user.password = passwordUtils.hashPassword(request.newPassword)
        userRepository.update(user)

        return true
    }

    private fun getUserById(userId: Long): User {
        return userRepository.findById(userId)
            ?: throw ResourceNotFoundException("User not found with ID: $userId")
    }
}
