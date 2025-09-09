package com.pcloudai.backend.auth

import io.dropwizard.auth.AuthFilter
import io.dropwizard.auth.Authenticator
import java.security.Principal
import javax.annotation.Priority
import javax.inject.Inject
import javax.inject.Named
import javax.ws.rs.Priorities
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.SecurityContext
import kotlin.math.min

/**
 * Principal class representing an authenticated user.
 */
class UserPrincipal(
    private val username: String,
    private val userId: Long,
    private val role: String
) : Principal {
    override fun getName(): String = username
    fun getUserId(): Long = userId
    fun getRole(): String = role
    fun isAdmin(): Boolean = role == "ADMIN"
}

/**
 * JWT authentication filter for securing endpoints.
 */
@Priority(Priorities.AUTHENTICATION)
class JwtAuthFilter @Inject constructor(
    authenticator: Authenticator<String, UserPrincipal>,
    @Named("jwtRealm") realm: String
) : AuthFilter<String, UserPrincipal>() {

//    private val logger = LoggerFactory.getLogger(JwtAuthFilter::class.java)

    init {
        // Configure the superclass properties using the injected dependencies.
        logger.debug("JwtAuthFilter.init() - Initializing JwtAuthFilter with realm: $realm")
        this.authenticator = authenticator
        this.realm = realm
        this.prefix = "Bearer"
        logger.info("JWT Auth Filter initialized: $this")
    }

    /**
     * The main filter method called by Jersey for each request.
     */
    override fun filter(requestContext: ContainerRequestContext) {
        logger.debug("JwtAuthFilter.filter() - Starting authentication for path: ${requestContext.uriInfo.path}")
        val token = getTokenFromHeader(requestContext)

        if (token == null) {
            logger.debug("No Authorization header found or no token provided.")
            unauthorizedHandler.buildResponse(prefix, realm)
            return
        }

        val principal = authenticator.authenticate(token)

        if (principal.isPresent) {
            logger.debug(
                "Authentication successful for user: ${principal.get().name}, userId: ${principal.get().getUserId()}"
            )
            requestContext.securityContext = createSecurityContext(principal.get(), requestContext)
            logger.debug("Security context set with principal: ${principal.get().name}")
        } else {
            logger.debug("Authentication failed - invalid token.")
            unauthorizedHandler.buildResponse(prefix, realm)
        }
    }

    /**
     * Extracts the token from the Authorization header.
     */
    private fun getTokenFromHeader(requestContext: ContainerRequestContext): String? {
        val authHeader = requestContext.headers.getFirst(HttpHeaders.AUTHORIZATION)
        val authHeaderLength = authHeader?.length ?: 0
        logger.debug("Authorization header: ${authHeader?.substring(0, min(20, authHeaderLength))}...")

        if (authHeader == null || !authHeader.startsWith(prefix)) {
            logger.debug("Invalid Authorization header format. Expected prefix: '$prefix'")
            return null
        }

        val token = authHeader.substring(prefix.length).trim()
        val tokenLength = token.length
        logger.debug("Extracted token: ${token.substring(0, min(10, tokenLength))}...")
        return token
    }

    /**
     * Creates a security context with the authenticated principal.
     */
    private fun createSecurityContext(
        principal: UserPrincipal,
        requestContext: ContainerRequestContext
    ): SecurityContext {
        return object : SecurityContext {
            override fun getUserPrincipal(): Principal = principal
            override fun isUserInRole(role: String): Boolean = principal.getRole() == role
            override fun isSecure(): Boolean = requestContext.securityContext.isSecure
            override fun getAuthenticationScheme(): String = SecurityContext.BASIC_AUTH
        }
    }
}
