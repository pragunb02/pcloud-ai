package com.pcloudai.backend.dto.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.pcloudai.backend.core.domain.User
import java.time.LocalDateTime

data class UserProfileResponse(
    @JsonProperty("id")
    val id: Long,

    @JsonProperty("username")
    val username: String,

    @JsonProperty("firstName")
    val firstName: String,

    @JsonProperty("lastName")
    val lastName: String,

    @JsonProperty("email")
    val email: String,

    @JsonProperty("bio")
    val bio: String?,

    @JsonProperty("createdAt")
    val createdAt: LocalDateTime,

    @JsonProperty("updatedAt")
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun fromUser(user: User): UserProfileResponse {
            return UserProfileResponse(
                id = user.id,
                username = user.username,
                firstName = user.firstName,
                lastName = user.lastName,
                email = user.email,
                bio = user.bio,
                createdAt = user.createdAt,
                updatedAt = user.updatedAt
            )
        }
    }
}
