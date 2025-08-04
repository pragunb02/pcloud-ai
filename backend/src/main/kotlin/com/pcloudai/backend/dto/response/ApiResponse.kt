package com.pcloudai.backend.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

data class ApiResponse(
    @JsonProperty("success")
    val success: Boolean,
    @JsonProperty("message")
    val message: String
)
