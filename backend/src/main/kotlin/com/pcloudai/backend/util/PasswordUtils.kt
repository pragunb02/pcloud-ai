package com.pcloudai.backend.util

import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasswordUtils @Inject constructor(
    private val logRounds: Int,
    private val pepper: String? = null
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun hashPassword(password: String): String {
        val input = pepper?.let { password + it } ?: password
        return BCrypt.hashpw(input, BCrypt.gensalt(logRounds))
    }

    fun checkPassword(plain: String, hashed: String): Boolean {
        return try {
            val input = pepper?.let { plain + it } ?: plain
            BCrypt.checkpw(input, hashed)
        } catch (e: IllegalArgumentException) {
            logger.debug("Invalid BCrypt hash: {}", e.message)
            false
        }
    }
}
