package com.pcloudai.backend.dto.request

import com.fasterxml.jackson.annotation.JsonProperty
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

data class ChangePasswordRequest(
    @field:NotBlank(message = "Current password is required")
    @JsonProperty("currentPassword")
    val currentPassword: String,

    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, message = "New password must be at least 8 characters")
    @JsonProperty("newPassword")
    val newPassword: String
)
