package com.pcloudai.backend.api

import com.pcloudai.backend.auth.Secured
import com.pcloudai.backend.auth.UserPrincipal
import com.pcloudai.backend.core.service.FileService
import com.pcloudai.backend.core.service.IUserProfileService
import com.pcloudai.backend.core.service.IUserSettingsService
import com.pcloudai.backend.dto.request.ChangePasswordRequest
import com.pcloudai.backend.dto.request.UpdateUserProfileRequest
import com.pcloudai.backend.dto.request.UpdateUserSettingsRequest
import com.pcloudai.backend.dto.response.ApiResponse
import io.dropwizard.auth.Auth
import io.dropwizard.hibernate.UnitOfWork
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.validation.Valid
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("TooGenericExceptionCaught")
class UserResource @Inject constructor(
    private val userProfileService: IUserProfileService,
    private val userSettingsService: IUserSettingsService,
    private val fileService: FileService,
) {
    private val logger = LoggerFactory.getLogger(UserResource::class.java)

    @GET
    @Path("/me")
    @Secured
    @UnitOfWork
    fun getCurrentUser(@Auth principal: UserPrincipal): Response {
        logger.info("Fetching profile for userId={}", principal.getUserId())
        return try {
            val profile = userProfileService.getUserProfile(principal.getUserId())
            Response.ok(profile).build()
        } catch (e: Exception) {
            logger.error("Error fetching profile for userId={}: {}", principal.getUserId(), e.message, e)
            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ApiResponse(success = false, message = "Could not load profile"))
                .build()
        }
    }

    @PUT
    @Path("/me")
    @Secured
    @UnitOfWork
    fun updateCurrentUser(
        @Auth principal: UserPrincipal,
        @Valid request: UpdateUserProfileRequest
    ): Response {
        logger.info("Updating profile for userId={}", principal.getUserId())
        val updated = userProfileService.updateUserProfile(principal.getUserId(), request)
        return Response.ok(updated).build()
    }

    @POST
    @Path("/change-password")
    @Secured
    @UnitOfWork
    fun changePassword(
        @Auth principal: UserPrincipal,
        @Valid request: ChangePasswordRequest
    ): Response {
        logger.info("Changing password for userId={}", principal.getUserId())
        return if (userProfileService.changePassword(principal.getUserId(), request)) {
            Response.ok(ApiResponse(success = true, message = "Password changed")).build()
        } else {
            logger.warn("Failed password change for userId={}", principal.getUserId())
            Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponse(success = false, message = "Password change failed"))
                .build()
        }
    }

    @GET
    @Path("/settings")
    @Secured
    @UnitOfWork
    fun getUserSettings(@Auth principal: UserPrincipal): Response {
        logger.info("Fetching settings for userId={}", principal.getUserId())
        val settings = userSettingsService.getUserSettings(principal.getUserId())
        return Response.ok(settings).build()
    }

    @PUT
    @Path("/settings")
    @Secured
    @UnitOfWork
    fun updateUserSettings(
        @Auth principal: UserPrincipal,
        @Valid request: UpdateUserSettingsRequest
    ): Response {
        logger.info("Updating settings for userId={}", principal.getUserId())
        val updated = userSettingsService.updateUserSettings(principal.getUserId(), request)
        return Response.ok(updated).build()
    }

    @GET
    @Path("/storage")
    @Secured
    @UnitOfWork
    fun getStorageUsage(@Auth principal: UserPrincipal): Response {
        logger.info("Fetching storage usage for userId={}", principal.getUserId())
        return try {
            val usage = fileService.calculateStorageUsage(principal.getUserId())
            Response.ok(usage).build()
        } catch (e: Exception) {
            logger.error("Error calculating storage for userId={}: {}", principal.getUserId(), e.message, e)
            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ApiResponse(success = false, message = "Could not calculate storage usage"))
                .build()
        }
    }
}
