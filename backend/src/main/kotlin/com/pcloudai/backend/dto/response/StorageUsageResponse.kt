package com.pcloudai.backend.dto.response

data class StorageUsageResponse(
    val usedBytes: Long,
    val totalBytes: Long,
    val usedGB: Double,
    val totalGB: Double,
    val percentage: Double
)
