package com.pcloudai.backend.api

import com.pcloudai.backend.auth.JwtHelper
import com.pcloudai.backend.auth.UserPrincipal
import com.pcloudai.backend.auth.UserService
import com.pcloudai.backend.dto.LoginRequest
import com.pcloudai.backend.dto.LoginResponse
import com.pcloudai.backend.dto.UserRegisterRequest
import com.pcloudai.backend.dto.UserResponse
import io.dropwizard.auth.Auth
import io.dropwizard.hibernate.UnitOfWork
import org.slf4j.LoggerFactory
import javax.annotation.security.PermitAll
import javax.inject.Inject
import javax.inject.Singleton
import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * REST resource for authentication operations.
 */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Singleton
class AuthResource @Inject constructor(
    private val userService: UserService,
    private val jwtHelper: JwtHelper
) {
    private val logger = LoggerFactory.getLogger(AuthResource::class.java)

    /**
     * Login endpoint
     */
    @POST
    @Path("/login")
    @UnitOfWork
    fun login(
        @NotNull @Valid loginRequest: LoginRequest
    ): Response {
        logger.debug("Login attempt for user: ${loginRequest.username}")

        val user = userService.authenticate(loginRequest.username, loginRequest.password)

        if (user == null) {
            logger.debug("Authentication failed for user: ${loginRequest.username}")
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(mapOf("message" to "Invalid username or password"))
                .build()
        }

        logger.debug("Authentication successful for user: ${loginRequest.username}")
        val token = jwtHelper.generateToken(user)

        return Response.ok(
            LoginResponse(
                token = token,
                user = UserResponse(
                    id = user.id,
                    username = user.username,
                    role = user.role.name
                )
            )
        ).build()
    }

    /**
     * Registration endpoint
     */
    @POST
    @Path("/register")
    @UnitOfWork
    @Suppress("TooGenericExceptionCaught", "ReturnCount")
    fun register(
        @NotNull @Valid userRegisterRequest: UserRegisterRequest
    ): Response {
        logger.debug("Registration attempt for user: ${userRegisterRequest.username}")

        // Check if an existing user with the same username already exists
        val existingUser = userService.findByUsername(userRegisterRequest.username)
        if (existingUser != null) {
            logger.debug("Registration failed: username already exists: ${userRegisterRequest.username}")
            return Response.status(Response.Status.CONFLICT)
                .entity(mapOf("message" to "Username already exists"))
                .build()
        }

        // Create a new user
        try {
            val user = userService.createUser(
                username = userRegisterRequest.username,
                password = userRegisterRequest.password,
                firstName = userRegisterRequest.firstName,
                lastName = userRegisterRequest.lastName,
                email = userRegisterRequest.email
            )

            logger.debug("Registration successful for user: ${userRegisterRequest.username}")

            // Generate token for automatic login
            val token = jwtHelper.generateToken(user)

            return Response.status(Response.Status.CREATED)
                .entity(
                    LoginResponse(
                        token = token,
                        user = UserResponse(
                            id = user.id,
                            username = user.username,
                            role = user.role.name
                        )
                    )
                )
                .build()
        } catch (e: Exception) {
            logger.error("Error during user registration", e)
            return Response.serverError()
                .entity(mapOf("message" to "Registration failed due to server error"))
                .build()
        }
    }

    /**
     * Get current user info
     */
    // TODO: do i really need this?
    @GET
    @Path("/me")
    @PermitAll
    fun getCurrentUser(
        @Auth principal: UserPrincipal
    ): Response {
        logger.debug("Getting current user info for: ${principal.name}")

        return Response.ok(
            UserResponse(
                id = principal.getUserId(),
                username = principal.name,
                role = principal.getRole()
            )
        ).build()
    }
}
