package com.pcloudai.backend.exception

/**
 * Exception thrown when a requested resource is not found
 */
class ResourceNotFoundException(message: String) : RuntimeException(message)
