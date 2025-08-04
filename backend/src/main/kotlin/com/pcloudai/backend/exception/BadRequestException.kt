package com.pcloudai.backend.exception

/**
 * Exception thrown when a request is invalid
 */
class BadRequestException(message: String) : RuntimeException(message)
