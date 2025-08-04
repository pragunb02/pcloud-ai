package com.pcloudai.backend.dto.request

import com.fasterxml.jackson.annotation.JsonProperty
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

data class UpdateUserProfileRequest(
    @field:NotBlank(message = "First name is required")
    @field:Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    @JsonProperty("firstName")
    val firstName: String,

    @field:NotBlank(message = "Last name is required")
    @field:Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    @JsonProperty("lastName")
    val lastName: String,

    @field:Email(message = "Invalid email format")
    @field:NotBlank(message = "Email is required")
    @JsonProperty("email")
    val email: String,

    @field:Size(max = 500, message = "Bio cannot exceed 500 characters")
    @JsonProperty("bio")
    val bio: String? = null
)
