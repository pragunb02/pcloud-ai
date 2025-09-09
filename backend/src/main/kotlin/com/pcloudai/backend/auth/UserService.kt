package com.pcloudai.backend.auth

import com.pcloudai.backend.core.domain.Role
import com.pcloudai.backend.core.domain.User
import com.pcloudai.backend.core.repository.UserRepository
import com.pcloudai.backend.util.PasswordUtils
import io.dropwizard.hibernate.UnitOfWork
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserService @Inject constructor(
    private val passwordUtils: PasswordUtils,
    private val userRepository: UserRepository
) {
    private val logger = LoggerFactory.getLogger(UserService::class.java)

    @UnitOfWork(readOnly = true)
    fun findByUsername(username: String): User? {
        return userRepository.findByUsername(username)
    }

    @UnitOfWork(readOnly = true)
    fun authenticate(username: String, password: String): User? {
        logger.debug("Authenticating user: {}", username)
        val user = userRepository.findByUsername(username) ?: return null

        return if (passwordUtils.checkPassword(password, user.password)) {
            logger.debug("Authentication successful for user: {}", username)
            user
        } else {
            logger.debug("Authentication failed for user: {}", username)
            null
        }
    }

    @UnitOfWork
    @Suppress("LongParameterList", "TooGenericExceptionThrown")
    fun createUser(
        username: String,
        password: String,
        role: Role = Role.USER,
        firstName: String,
        lastName: String,
        email: String
    ): User {
        logger.info("Attempting to create new user: {}", username)

        if (userRepository.findByUsername(username) != null) {
            throw RuntimeException(
                "User with username " +
                    "'$username' already exists."
            )
        }

        val hashedPassword = passwordUtils.hashPassword(password)

        val newUser = User(
            username = username,
            password = hashedPassword,
            firstName = firstName,
            lastName = lastName,
            email = email,
            role = role
        )

        val savedUser = userRepository.save(newUser)
        logger.info("Successfully created new user: {} with ID: {}", savedUser.username, savedUser.id)
        return savedUser
    }
}
