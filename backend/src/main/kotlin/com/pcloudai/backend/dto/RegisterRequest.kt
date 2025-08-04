package com.pcloudai.backend.dto

import com.fasterxml.jackson.annotation.JsonProperty
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Size

// TODO add email and need major fixes
data class RegisterRequest(
    @field:NotEmpty(message = "Username is required")
    @field:Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @JsonProperty("username")
    val username: String,

    @field:NotEmpty(message = "Password is required")
    @field:Size(min = 6, message = "Password must be at least 6 characters")
    @JsonProperty("password")
    val password: String
)
