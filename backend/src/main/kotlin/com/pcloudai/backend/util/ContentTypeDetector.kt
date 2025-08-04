package com.pcloudai.backend.util

import org.slf4j.LoggerFactory
import java.util.Locale

/**
 * Utility class for detecting content types based on file extensions
 * Provides fallback content type detection when multipart form data detection fails
 */
@Suppress("ForbiddenComment")
object ContentTypeDetector {
    private val logger = LoggerFactory.getLogger(ContentTypeDetector::class.java)

    // TODO: more types can be added
    // Map of file extensions to MIME types
    private val extensionToMimeType = mapOf(
        // Images
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "png" to "image/png",
        "gif" to "image/gif",
        "bmp" to "image/bmp",
        "webp" to "image/webp",
        "svg" to "image/svg+xml",
        "ico" to "image/x-icon",

        // Audio
        "mp3" to "audio/mpeg",
        "wav" to "audio/wav",
        "ogg" to "audio/ogg",
        "m4a" to "audio/mp4",
        "aac" to "audio/aac",
        "flac" to "audio/flac",
        "wma" to "audio/x-ms-wma",

        // Video
        "mp4" to "video/mp4",
        "avi" to "video/x-msvideo",
        "mov" to "video/quicktime",
        "wmv" to "video/x-ms-wmv",
        "flv" to "video/x-flv",
        "webm" to "video/webm",
        "mkv" to "video/x-matroska",
        "3gp" to "video/3gpp",

        // Documents
        "pdf" to "application/pdf",
        "doc" to "application/msword",
        "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "xls" to "application/vnd.ms-excel",
        "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "ppt" to "application/vnd.ms-powerpoint",
        "pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",

        // Text
        "txt" to "text/plain",
        "md" to "text/markdown",
        "html" to "text/html",
        "htm" to "text/html",
        "css" to "text/css",
        "js" to "application/javascript",
        "json" to "application/json",
        "xml" to "application/xml",
        "csv" to "text/csv",

        // Archives
        // TODO: Check if used
        "zip" to "application/zip",
        "rar" to "application/x-rar-compressed",
        "7z" to "application/x-7z-compressed",
        "tar" to "application/x-tar",
        "gz" to "application/gzip"
    )

    /**
     * Detects content type based on filename and provided content type
     * Uses the provided content type if it's specific, otherwise falls back to extension-based detection
     *
     * @param filename The original filename
     * @param providedContentType The content type provided by the multipart form data
     * @return The detected content type
     */
    @Suppress("ReturnCount")
    fun detectContentType(filename: String, providedContentType: String): String {
        logger.debug("Detecting content type for file: $filename, provided: $providedContentType")

        // If the provided content type is specific (not generic), use it
        if (providedContentType != "application/octet-stream" &&
            providedContentType != "application/unknown" &&
            providedContentType.isNotBlank()
        ) {
            logger.debug("Using provided content type: $providedContentType")
            return providedContentType
        }

        // Extract file extension
        val extension = getFileExtension(filename)
        if (extension.isBlank()) {
            logger.debug("No file extension found, using provided content type: $providedContentType")
            return providedContentType
        }

        // Look up MIME type by extension
        val detectedType = extensionToMimeType[extension.lowercase(Locale.ROOT)]

        return if (detectedType != null) {
            logger.debug("Detected content type by extension: $detectedType for extension: $extension")
            detectedType
        } else {
            logger.debug("Unknown extension: $extension, using provided content type: $providedContentType")
            providedContentType
        }
    }

    /**
     * Extracts the file extension from a filename
     *
     * @param filename The filename to extract extension from
     * @return The file extension without the dot, or empty string if no extension
     */
    private fun getFileExtension(filename: String): String {
        // TODO: Need more refine refactoring like for cases like "archive.tar.gz" ".env"
        val lastDotIndex = filename.lastIndexOf('.')
        return if (lastDotIndex > 0 && lastDotIndex < filename.length - 1) {
            filename.substring(lastDotIndex + 1)
        } else {
            ""
        }
    }

    fun isImage(contentType: String): Boolean {
        return contentType.startsWith("image/")
    }

    fun isAudio(contentType: String): Boolean {
        return contentType.startsWith("audio/")
    }

    fun isVideo(contentType: String): Boolean {
        return contentType.startsWith("video/")
    }

    fun isText(contentType: String): Boolean {
        return contentType.startsWith("text/") ||
            contentType == "application/json" ||
            contentType == "application/xml" ||
            contentType == "application/javascript"
    }

    fun isPdf(contentType: String): Boolean {
        return contentType == "application/pdf"
    }
}
