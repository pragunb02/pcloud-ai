package com.pcloudai.backend.api

import com.pcloudai.backend.PCloudConfiguration
import com.pcloudai.backend.auth.Secured
import com.pcloudai.backend.auth.UserPrincipal
import com.pcloudai.backend.core.service.FileService
import com.pcloudai.backend.dto.FileListResponse
import com.pcloudai.backend.dto.FileResponse
import com.pcloudai.backend.dto.response.DeleteFileResponse
import com.pcloudai.backend.dto.response.ErrorResponse
import com.pcloudai.backend.dto.response.FileTextContentResponse
import com.pcloudai.backend.util.ContentTypeDetector
import io.dropwizard.auth.Auth
import io.dropwizard.hibernate.UnitOfWork
import org.glassfish.jersey.media.multipart.FormDataBodyPart
import org.glassfish.jersey.media.multipart.FormDataParam
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.DefaultValue
import javax.ws.rs.GET
import javax.ws.rs.OPTIONS
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput

@Suppress("TooManyFunctions", "TooGenericExceptionCaught", "ReturnCount", "MagicNumber", "LongMethod", "MaxLineLength")
@Path("/files")
@Produces(MediaType.APPLICATION_JSON)
@Singleton
class FileResource @Inject constructor(
    private val fileService: FileService,
    private val configuration: PCloudConfiguration
) {
    private val logger = LoggerFactory.getLogger(FileResource::class.java)

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured
    @UnitOfWork
    @Suppress("TooGenericExceptionCaught", "LongMethod", "ReturnCount")
    fun uploadFile(
        @Auth principal: UserPrincipal,
        @FormDataParam("file") filePart: FormDataBodyPart?
    ): Response {
        logger.debug("File upload request from user: {}", principal.name)

        if (filePart == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf("error" to "Missing form field 'file'"))
                .build()
        }

        val cd = filePart.contentDisposition
        val originalFilename = cd.fileName ?: return Response.status(Response.Status.BAD_REQUEST)
            .entity(mapOf("error" to "Filename is required"))
            .build()

        val providedCt = filePart.mediaType?.toString().orEmpty()
        val detectedCt = ContentTypeDetector
            .detectContentType(originalFilename, providedCt)

        logger.debug(
            "Determined content type: {} for file: {} (provided: {})",
            detectedCt,
            originalFilename,
            providedCt
        )

        // Stream the upload, ensuring we close the InputStream
        val fileSize = cd.size
        try {
            filePart.getEntityAs(InputStream::class.java).use { inputStream ->
                logger.info(
                    "File upload details: filename='{}', contentType='{}', size={}",
                    originalFilename,
                    detectedCt,
                    fileSize
                )

                val file = fileService.uploadFile(
                    userId = principal.getUserId(),
                    originalFilename = originalFilename,
                    contentType = detectedCt,
                    fileSize = fileSize,
                    fileContent = inputStream
                )

                return Response.ok(
                    FileResponse(
                        id = file.id,
                        name = file.originalName,
                        contentType = file.contentType,
                        size = file.size,
                        uploadDate = file.createdAt.toString(),
                        thumbnailUrl = file.thumbnailUrl,
                        previewUrl = file.previewUrl,
                        hasTextContent = file.hasTextContent
                    )
                ).build()
            }
        } catch (e: IOException) {
            logger.error("I/O error uploading file", e)
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(mapOf("error" to "I/O error: ${e.message}"))
                .build()
        } catch (e: Exception) {
            logger.error("Unexpected error uploading file", e)
            return Response.serverError()
                .entity(mapOf("error" to "Failed to upload file: ${e.message}"))
                .build()
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Secured
    @UnitOfWork(readOnly = true)
    fun getFiles(
        @Auth principal: UserPrincipal,
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("pageSize") @DefaultValue("10") pageSize: Int,
        @QueryParam("search") search: String?
    ): Response {
            logger.warn("Invalid paging parameters: page=$page, pageSize=$pageSize")
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf("error" to "Invalid paging parameters"))
                .build()
        }

        val userId = principal.getUserId()
        logger.info("Fetching files for userId=$userId (page=$page, size=$pageSize)")

        val searchQuery = search?.takeIf { it.isNotBlank() }

        val files = fileService.getFilesForUser(userId, page, pageSize, searchQuery)
        logger.debug("Retrieved ${files.size} files from service")

        val totalFiles = fileService.getTotalFileCount(userId, searchQuery)
        logger.debug("Total matching files count: $totalFiles")

        val totalPages = if (totalFiles > 0) (((totalFiles - 1) / pageSize) + 1).toInt() else 0
        val hasNext = page < totalPages
        val hasPrevious = page > 1

        val fileResponses = files.map { f ->
            FileResponse(
                id = f.id,
                name = f.originalName,
                contentType = f.contentType,
                size = f.size,
                uploadDate = f.createdAt.toString(),
                thumbnailUrl = f.thumbnailUrl,
                previewUrl = f.previewUrl,
                hasTextContent = f.hasTextContent
            )
        }

        val payload = FileListResponse(
            files = fileResponses,
            total = totalFiles,
            page = page,
            pageSize = pageSize,
            totalPages = totalPages,
            hasNext = hasNext,
            hasPrevious = hasPrevious
        )

        logger.info("Returning page $page of $totalPages for userId=$userId")
        return Response.ok(payload).build()
    }

    /**
     * Handle OPTIONS preflight request for file download
     */
    @OPTIONS
    @Path("/{fileId}/download")
    @Secured
    fun optionsForFileDownload(
        @PathParam("fileId") fileId: Long
    ): Response {
        logger.debug("OPTIONS request for file download endpoint, file ID: $fileId")
        return Response.ok().build()
    }

    @GET
    @Path("/{fileId}/download")
    @Secured
    @UnitOfWork(readOnly = true)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun downloadFile(
        @Auth principal: UserPrincipal,
        @PathParam("fileId") fileId: Long
    ): Response {
        logger.info("Download requested: fileId=$fileId, user=${principal.name}")

        val file = fileService.getFile(fileId)
            ?: return Response.status(Response.Status.NOT_FOUND)
                .entity(mapOf("error" to "File not found"))
                .type(MediaType.APPLICATION_JSON)
                .build()

        if (file.user?.id != principal.getUserId() && !principal.isAdmin()) {
            logger.warn("Unauthorized download: user=${principal.name}, fileOwner=${file.user?.id}")
            return Response.status(Response.Status.FORBIDDEN)
                .entity(mapOf("error" to "Access denied"))
                .type(MediaType.APPLICATION_JSON)
                .build()
        }

        // Get content stream
        val contentStream = fileService.getFileContent(fileId, principal.getUserId())
            ?: return Response.status(Response.Status.NOT_FOUND)
                .entity(mapOf("error" to "File content missing"))
                .type(MediaType.APPLICATION_JSON)
                .build()

        logger.info(
            "Streaming file '${file.originalName}' of size=${file.size} bytes have contentStream = $contentStream"
        )

        return Response.ok(streamFile(contentStream))
            .type(file.contentType)
            // CONTENT_DISPOSITION to download
            .header("CONTENT_DISPOSITION", "attachment; filename=\"${file.originalName}\"")
            .header("CONTENT_LENGTH", file.size.toString())
            .build()
    }

    private fun streamFile(input: InputStream): StreamingOutput =
        StreamingOutput { output ->
            input.use { ins ->
                val buffer = ByteArray(4096)
                while (true) {
                    val read = ins.read(buffer).takeIf { it > 0 } ?: break
                    output.write(buffer, 0, read)
                }
                output.flush()
            }
        }

    @GET
    @Path("/{id}/text")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured
    @UnitOfWork(readOnly = true)
    fun getFileTextContent(
        @Auth principal: UserPrincipal,
        @PathParam("id") fileId: Long
    ): Response {
        logger.info("User {} requested text content for fileId={}", principal.name, fileId)

        val file = fileService.getFile(fileId)
            ?: return Response.status(Response.Status.NOT_FOUND)
                .entity(ErrorResponse("File not found"))
                .build()

        if (file.user?.id != principal.getUserId() && !principal.isAdmin()) {
            logger.warn("Access denied: user={} tried to access file={}", principal.name, fileId)
            return Response.status(Response.Status.FORBIDDEN)
                .entity(ErrorResponse("You don't have access to this file"))
                .build()
        }

        val text = file.textContent
            ?: return Response.status(Response.Status.NOT_FOUND)
                .entity(ErrorResponse("Text content not available"))
                .build()

        return Response.ok(FileTextContentResponse(text)).build()
    }

    /**
     * Delete a file
     */
    @DELETE
    @Path("/{fileId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured
    @UnitOfWork
    fun deleteFile(
        @Auth principal: UserPrincipal,
        @PathParam("fileId") fileId: Long
    ): Response {
        logger.info("User {} requests delete for fileId={}", principal.name, fileId)

        val file = fileService.getFile(fileId)
            ?: return Response.status(Response.Status.NOT_FOUND)
                .entity(ErrorResponse("File not found"))
                .build()

        if (file.user?.id != principal.getUserId() && !principal.isAdmin()) {
            logger.warn(
                "Access denied: user={} tried to delete file owned by user={}",
                principal.name,
                file.user?.id
            )
            return Response.status(Response.Status.FORBIDDEN)
                .entity(ErrorResponse("You don't have permission to delete this file"))
                .build()
        }

        return try {
            fileService.deleteFile(fileId, principal.getUserId())
            logger.info("File {} deleted successfully by user={}", fileId, principal.name)
            Response.ok(DeleteFileResponse(true, "File deleted successfully"))
                .build()
        } catch (e: Exception) {
            logger.error("Error deleting file {}: {}", fileId, e.message, e)
            Response.serverError()
                .entity(ErrorResponse("Failed to delete file: ${e.message}"))
                .build()
        }
    }

    @GET
    @Path("/limits")
    @Produces(MediaType.APPLICATION_JSON)
    @UnitOfWork
    fun getFileLimits(): Response {
        logger.info("Getting file limits and supported formats")

        val limits = mapOf(
            "maxFileSize" to configuration.storage.maxFileSizeBytes,
            "maxFileSizeMB" to (configuration.storage.maxFileSizeBytes / (1024 * 1024)),
            "supportedFormats" to configuration.storage.supportedFormats,
            "maxStorageBytes" to configuration.storage.maxStorageBytes,
            "maxStorageGB" to (configuration.storage.maxStorageBytes / (1024 * 1024 * 1024))
        )

        return Response.ok(limits).build()
    }
}
