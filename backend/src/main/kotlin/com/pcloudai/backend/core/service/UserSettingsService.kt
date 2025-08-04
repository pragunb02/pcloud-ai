package com.pcloudai.backend.core.service

import com.pcloudai.backend.core.domain.User
import com.pcloudai.backend.core.domain.UserSettings
import com.pcloudai.backend.core.repository.UserRepository
import com.pcloudai.backend.core.repository.UserSettingsRepository
import com.pcloudai.backend.dto.request.UpdateUserSettingsRequest
import com.pcloudai.backend.dto.response.UserSettingsResponse
import com.pcloudai.backend.exception.ResourceNotFoundException
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

interface IUserSettingsService {
    fun getUserSettings(userId: Long): UserSettingsResponse
    fun updateUserSettings(userId: Long, request: UpdateUserSettingsRequest): UserSettingsResponse
}

@Singleton
class UserSettingsService @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository,
    private val userRepository: UserRepository
) : IUserSettingsService {

    private val logger = LoggerFactory.getLogger(UserSettingsService::class.java)

    override fun getUserSettings(userId: Long): UserSettingsResponse {
        logger.info("Getting settings for user: $userId")

        val user = getUserById(userId)

        // Find existing settings or create default ones
        val settings = userSettingsRepository.findByUser(user) ?: createDefaultSettings(user)

        return UserSettingsResponse.fromUserSettings(settings)
    }

    // TODO CHECK LOGIC for this end to end
    override fun updateUserSettings(userId: Long, request: UpdateUserSettingsRequest): UserSettingsResponse {
        logger.info("Updating settings for user: $userId")

        val user = getUserById(userId)

        // Find existing settings or create default ones
        val settings = userSettingsRepository.findByUser(user) ?: createDefaultSettings(user)

        request.theme?.let { settings.theme = it }

        request.notifications?.let { notificationSettings ->
            notificationSettings.email?.let { settings.emailNotifications = it }
            notificationSettings.push?.let { settings.pushNotifications = it }
            notificationSettings.weeklyReport?.let { settings.weeklyReportNotifications = it }
        }

        val updatedSettings = if (settings.id == 0L) {
            userSettingsRepository.save(settings)
        } else {
            userSettingsRepository.update(settings)
        }

        return UserSettingsResponse.fromUserSettings(updatedSettings)
    }

    private fun createDefaultSettings(user: User): UserSettings {
        logger.info("Creating default settings for user: ${user.id}")
        return UserSettings(
            user = user,
            theme = "light",
            emailNotifications = true,
            pushNotifications = true,
            weeklyReportNotifications = true
        )
    }

    private fun getUserById(userId: Long): User {
        return userRepository.findById(userId)
            ?: throw ResourceNotFoundException("User not found with ID: $userId")
    }
}
