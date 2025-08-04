package com.pcloudai.backend.auth

import javax.ws.rs.NameBinding

/**
 * Annotation to mark endpoints that require authentication
 */
@NameBinding
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Secured
