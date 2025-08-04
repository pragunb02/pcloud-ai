package com.pcloudai.backend.dto

import com.fasterxml.jackson.annotation.JsonProperty
import javax.validation.constraints.NotEmpty

// TODO add email and need major fixes
data class LoginRequest(
    @NotEmpty
    @JsonProperty("username")
    val username: String,

    @NotEmpty
    @JsonProperty("password")
    val password: String
)

data class LoginResponse(
    @JsonProperty("token")
    val token: String,

    @JsonProperty("user")
    val user: UserResponse
)

/**
 * Response DTO for user information
 */
data class UserResponse(
    @JsonProperty("id")
    val id: Long,

    @JsonProperty("username")
    val username: String,

    @JsonProperty("role")
    val role: String
)
