package com.pcloudai.backend.auth

import com.pcloudai.backend.core.domain.Role
import io.dropwizard.auth.Authenticator
import org.slf4j.LoggerFactory
import java.util.Optional
import javax.inject.Inject
import kotlin.math.min

/**
 * JWT authenticator that validates a token and constructs a UserPrincipal.
 */
class JwtAuthenticator @Inject constructor(
    private val jwtHelper: JwtHelper
) : Authenticator<String, UserPrincipal> {
    private val logger = LoggerFactory.getLogger(JwtAuthenticator::class.java)

    companion object {
        private const val MAX_TOKEN_LOG_LENGTH = 10
    }

    /**
     * Authenticates a JWT token by parsing it and extracting all claims.
     * Returns a UserPrincipal if the token is valid and contains all required claims.
     */
    @Suppress("ReturnCount", "TooGenericExceptionCaught")
    override fun authenticate(token: String): Optional<UserPrincipal> {
        logger.debug("Authenticating JWT token: {}...", token.substring(0, min(MAX_TOKEN_LOG_LENGTH, token.length)))

        val claims = jwtHelper.validateTokenAndGetClaims(token)
            ?: return Optional.empty<UserPrincipal>().also {
                logger.debug("JWT validation failed (invalid signature, expired, or malformed).")
            }

        return try {
            val username = claims.subject
            val userId = claims.get(JwtHelper.CLAIM_USER_ID, Long::class.javaObjectType)
            val roleName = claims.get(JwtHelper.CLAIM_ROLE, String::class.java)

            if (username == null || userId == null || roleName == null) {
                logger.warn("JWT token is missing required claims (subject, userId, or role).")
                return Optional.empty()
            }

            val role = Role.valueOf(roleName)

            logger.info("JWT token authenticated successfully for user: {}", username)
            val principal = UserPrincipal(username, userId, role.name)
            Optional.of(principal)
        } catch (e: Exception) {
            logger.warn("Failed to construct UserPrincipal from token claims: {}", e.message)
            Optional.empty()
        }
    }
}
