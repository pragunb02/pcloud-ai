package com.pcloudai.backend.dto

import com.fasterxml.jackson.annotation.JsonProperty
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Size

data class UserRegisterRequest(
    @field:NotEmpty(message = "Username is required")
    @field:Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @JsonProperty("username")
    val username: String,

    @field:NotEmpty(message = "First Name is required")
    @field:Size(min = 3, max = 50, message = "First Name must be between 3 and 50 characters")
    @JsonProperty("first_name")
    val firstName: String,

    @field:NotEmpty(message = "Last Name is required")
    @field:Size(min = 3, max = 50, message = "Last Name must be between 3 and 50 characters")
    @JsonProperty("last_name")
    val lastName: String,

    @field:NotEmpty(message = "Email is required")
    @field:Size(min = 3, max = 50, message = "Email must be between 3 and 50 characters")
    @JsonProperty("email")
    val email: String,

    @field:NotEmpty(message = "Password is required")
    @field:Size(min = 6, message = "Password must be at least 6 characters")
    @JsonProperty("password")
    val password: String,
)
