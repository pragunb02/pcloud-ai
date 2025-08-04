package com.pcloudai.backend.dto.response

data class FileTextContentResponse(val textContent: String)
data class DeleteFileResponse(val success: Boolean, val message: String)
data class ErrorResponse(val error: String)


