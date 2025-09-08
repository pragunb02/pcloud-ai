package com.pcloudai.backend.auth

import com.pcloudai.backend.core.domain.Role
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
import kotlin.math.min

/**
 * Helper class for JWT token generation and validation.
 */
// TODO check for private val and magic numbers
class JwtHelper(private val jwtSecret: String, private val expirationMinutes: Long) {
    private val logger = LoggerFactory.getLogger(JwtHelper::class.java)
    private val signingKey: Key = Keys.hmacShaKeyFor(jwtSecret.toByteArray(StandardCharsets.UTF_8))

    /**
     * Generate a JWT token for a user
     */
    fun generateToken(user: User): String {
        logger.debug("Generating token for user: ${user.username}")

        val now = Date()
        val expiration = Date(now.time + expirationMinutes * 60 * 1000)

        return Jwts.builder()
            .setSubject(user.username)
            .claim("userId", user.id)
            .claim("role", user.role.name)
            .setIssuedAt(now)
            .setExpiration(expiration)
            .signWith(signingKey, SignatureAlgorithm.HS256)
            .compact()
    }

    /**
     * Validate a JWT token and extract the claims
     */
    @Suppress("TooGenericExceptionCaught")
    fun validateTokenAndGetClaims(token: String): Claims? {
        logger.debug("Validating JWT token: ${token.substring(0, min(10, token.length))}...")
        logger.debug("Using signing key with algorithm: ${signingKey.algorithm}, format: ${signingKey.format}")

        return try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .body

            logger.info(
                "JWT token validated successfully. Claims: ${claims.subject}, " +
                    "userId: ${claims["userId"]}, role: ${claims["role"]}"
            )
            claims
        } catch (e: JwtException) {
            logger.warn("Invalid JWT token: ${e.message}")
            null
        } catch (e: IllegalArgumentException) {
            logger.warn("JWT token is empty or invalid: ${e.message}")
            null
        } catch (e: Exception) {
            logger.error("Unexpected error validating JWT token: ${e.message}", e)
            null
        }
    }

    /**
     * Extract username from JWT token
     */
    fun getUsernameFromToken(token: String): String? {
        val claims = validateTokenAndGetClaims(token) ?: return null
        return claims.subject
    }

    /**
     * Extract user ID from a JWT token
     */
    // REVISED: Use a safer and more idiomatic approach to get the claim
    fun getUserIdFromToken(token: String): Long? {
        val claims = validateTokenAndGetClaims(token) ?: return null
        return claims.get("userId", Long::class.java)
    }

    /**
     * Extract a user role from a JWT token
     */
    @Suppress("SwallowedException", "ReturnCount")
    fun getRoleFromToken(token: String): Role? {
        val claims = validateTokenAndGetClaims(token) ?: return null
        val roleName = claims.get("role", String::class.java) ?: return null
        return try {
            Role.valueOf(roleName)
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid role in token: $roleName")
            null
        }
    }

    /**
     * Check if a token is valid
     */
    fun isTokenValid(token: String): Boolean {
        val claims = validateTokenAndGetClaims(token)
        val isValid = claims != null
        logger.debug("Token validity check: $isValid")
        return isValid
    }
}
