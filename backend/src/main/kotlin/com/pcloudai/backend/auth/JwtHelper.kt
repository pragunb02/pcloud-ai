package com.pcloudai.backend.auth

import com.pcloudai.backend.core.domain.User
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.security.Key
import java.util.Date

/**
 * Helper class for JWT token generation and validation.
 * It encapsulates all low-level JWT logic.
 */
class JwtHelper(
    private val jwtSecret: String,
    private val expirationMinutes: Long
) {
    private val logger = LoggerFactory.getLogger(JwtHelper::class.java)
    private val signingKey: Key = Keys.hmacShaKeyFor(jwtSecret.toByteArray(StandardCharsets.UTF_8))

    companion object {
        const val CLAIM_USER_ID = "userId"
        const val CLAIM_ROLE = "role"
        private const val MILLISECONDS_IN_MINUTE = 60 * 1000L
    }

    /**
     * Generates a JWT token for a given user.
     */
    fun generateToken(user: User): String {
        logger.debug("Generating token for user: {}", user.username)

        val now = Date()
        val expiration = Date(now.time + expirationMinutes * MILLISECONDS_IN_MINUTE)

        return Jwts.builder()
            .setSubject(user.username)
            .claim(CLAIM_USER_ID, user.id)
            .claim(CLAIM_ROLE, user.role.name)
            .setIssuedAt(now)
            .setExpiration(expiration)
            .signWith(signingKey, SignatureAlgorithm.HS256)
            .compact()
    }

    /**
     * Validates a JWT token and returns the claims if successful.
     * This is the single, efficient method for parsing a token.
     * It returns null if the token is invalid for any reason.
     */
    @Suppress("TooGenericExceptionCaught")
    fun validateTokenAndGetClaims(token: String): Claims? {
        return try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .body

            logger.debug("JWT token validated successfully for subject: {}", claims.subject)
            claims
        } catch (e: JwtException) {
            logger.warn("Invalid JWT token: {}", e.message)
            null
        } catch (e: IllegalArgumentException) {
            logger.warn("JWT token is empty or invalid: {}", e.message)
            null
        } catch (e: Exception) {
            logger.error("Unexpected error validating JWT token: {}", e.message, e)
            null
        }
    }
}
