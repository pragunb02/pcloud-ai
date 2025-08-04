package com.pcloudai.backend.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Response DTO for file information
 */
data class FileResponse(
    @JsonProperty("id")
    val id: Long,

    @JsonProperty("name")
    val name: String,

    @JsonProperty("contentType")
    val contentType: String,

    @JsonProperty("size")
    val size: Long,

    @JsonProperty("uploadDate")
    val uploadDate: String,

    @JsonProperty("thumbnailUrl")
    val thumbnailUrl: String? = null,

    @JsonProperty("previewUrl")
    val previewUrl: String? = null,

    @JsonProperty("hasTextContent")
    val hasTextContent: Boolean = false
)

/**
 * Response DTO for paginated file list
 */
data class FileListResponse(
    @JsonProperty("files")
    val files: List<FileResponse>,

    @JsonProperty("total")
    val total: Long,

    @JsonProperty("page")
    val page: Int,

    @JsonProperty("pageSize")
    val pageSize: Int,

    @JsonProperty("totalPages")
    val totalPages: Int,

    @JsonProperty("hasNext")
    val hasNext: Boolean,

    @JsonProperty("hasPrevious")
    val hasPrevious: Boolean
)
