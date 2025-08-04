package com.pcloudai.backend.core.service

import com.pcloudai.backend.PCloudConfiguration
import com.pcloudai.backend.core.domain.File
import com.pcloudai.backend.core.domain.FileStatus
import com.pcloudai.backend.core.repository.FileRepository
import com.pcloudai.backend.core.repository.UserRepository
import com.pcloudai.backend.dto.response.StorageUsageResponse
import com.pcloudai.backend.preview.queue.PreviewQueue
import io.dropwizard.hibernate.UnitOfWork
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

/**
 * Service for file operations.
 */
@Singleton
@Suppress("TooGenericExceptionCaught", "ForbiddenComment", "MagicNumber", "ReturnCount")
class FileService @Inject constructor(
    private val fileRepository: FileRepository,
    private val userRepository: UserRepository,
    private val configuration: PCloudConfiguration,
    private val previewQueue: PreviewQueue
) {
    private val logger = LoggerFactory.getLogger(FileService::class.java)

    private val storageBasePath = configuration.fileStorage.baseDirectory

    @UnitOfWork
    fun uploadFile(
        userId: Long,
        originalFilename: String,
        contentType: String,
        fileSize: Long,
        fileContent: InputStream
    ): File {
        logger.debug("Uploading file: $originalFilename for user ID: $userId with size: $fileSize bytes")

        val user = userRepository.findById(userId) ?: throw IllegalArgumentException("User not found")

        val safeName = Paths.get(originalFilename).fileName.toString().replace(" ", "_")
        val uniqueFilename = "${UUID.randomUUID()}_$safeName"

        val userDirectory = Paths.get(storageBasePath, userId.toString())
        Files.createDirectories(userDirectory)
        val targetPath = userDirectory.resolve(uniqueFilename)

        fileContent.use { input ->
            Files.copy(input, targetPath, StandardCopyOption.REPLACE_EXISTING)
        }

        val actualSize = Files.size(targetPath)
        logger.debug("Actual file size from disk: $actualSize bytes (reported size was: $fileSize bytes)")

        // TODO: Check file metadata non-nullable fields
        val file = File(
            name = uniqueFilename,
            originalName = originalFilename,
            contentType = contentType,
            size = actualSize,
            storagePath = targetPath.toString(),
            status = FileStatus.ACTIVE,
            user = user
        )

        val savedFile = fileRepository.save(file)

        // Enqueue preview generation jobs if preview system is enabled (non-blocking failure)
        if (configuration.preview.enabled) {
            try {
                logger.debug("Enqueuing preview jobs for file: ${savedFile.id}")
                val jobIds = previewQueue.enqueueByContentType(savedFile.id, contentType)
                logger.debug("Enqueued ${jobIds.size} preview jobs for file: ${savedFile.id}")
            } catch (e: Exception) {
                // Don't fail the upload if preview generation fails
                logger.error("Failed to enqueue preview jobs for file: ${savedFile.id}", e)
            }
        }

        return savedFile
    }

    @UnitOfWork(readOnly = true)
    fun getFile(fileId: Long): File? {
        return fileRepository.findById(fileId)
    }

    @UnitOfWork(readOnly = true)
    fun getFilesForUser(userId: Long): List<File> {
        return fileRepository.findActiveByUserId(userId)
    }

    @UnitOfWork(readOnly = true)
    fun getFilesForUser(
        userId: Long,
        page: Int,
        pageSize: Int,
        search: String? = null
    ): List<File> {
        val offset = (page - 1) * pageSize
        return fileRepository.findActiveByUserIdPaginated(userId, offset, pageSize, search)
    }

    @UnitOfWork(readOnly = true)
    fun getTotalFileCount(userId: Long, search: String? = null): Long {
        return fileRepository.countActiveByUserId(userId, search)
    }

    @UnitOfWork
    fun deleteFile(fileId: Long, userId: Long, isAdmin: Boolean = false): Boolean {
        logger.debug("Attempting soft delete of fileId={} by userId={}", fileId, userId)

        val file = fileRepository.findById(fileId)
        if (file == null) {
            logger.warn("File not found: fileId={}", fileId)
            return false
        }

        if (!isAdmin && file.user?.id != userId) {
            logger.warn(
                "Permission denied: userId={} cannot delete fileId={} owned by userId={}",
                userId,
                fileId,
                file.user?.id
            )
            return false
        }

        file.markAsDeleted()
        fileRepository.update(file)
        logger.info("File marked as deleted in DB: fileId={}", fileId)

        val filePath = Paths.get(file.storagePath)
        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath)
                logger.info("Physical file deleted from disk: {}", filePath)
            } else {
                logger.warn("No physical file to delete at path: {}", filePath)
            }
        } catch (ioe: IOException) {
            logger.error("Failed to delete physical file at {}: {}", filePath, ioe.message, ioe)
            // TODO
            // Optionally enqueue cleanup job here instead of failing
        }

        return true
    }

    fun getFileContent(
        fileId: Long,
        userId: Long,
        isAdmin: Boolean = false
    ): InputStream? {
        logger.debug("Fetching content stream for fileId={} by userId={}", fileId, userId)

        val file = fileRepository.findById(fileId)
        if (file == null) {
            logger.warn("File not found in DB: fileId={}", fileId)
            return null
        }

        val ownerId = file.user?.id
        val PUBLIC_USER_ID = 0L
        if (!isAdmin && ownerId != userId && ownerId != PUBLIC_USER_ID) {
            logger.warn(
                "Access denied: userId={} tried to read fileId={} owned by userId={}",
                userId,
                fileId,
                ownerId
            )
            return null
        }

        val path = Paths.get(file.storagePath)
        logger.debug("Resolved storage path for fileId={}: {}", fileId, path.toAbsolutePath())

        return try {
            Files.newInputStream(path).also {
                logger.info("Opened InputStream for fileId={} from path={}", fileId, path)
            }
        } catch (e: NoSuchFileException) {
            logger.error("Physical file not found: {}", path, e)
            null
        } catch (e: AccessDeniedException) {
            logger.error("Cannot read file (permission issue): {}", path, e)
            null
        } catch (e: IOException) {
            logger.error("I/O error opening file at path={}: {}", path, e.message, e)
            null
        }
    }

    fun calculateStorageUsage(userId: Long): StorageUsageResponse {
        logger.debug("Calculating storage usage for user ID: $userId")

        val files = getFilesForUser(userId)

        val usedBytes = files
            .filter { it.status != FileStatus.DELETED } // Only count non-deleted files
            .sumOf { it.size }

        /** TODO
         *   val usedBytes = fileRepository.sumSizeByUserAndStatusNot(
         *     userId = userId,
         *     status = FileStatus.DELETED
         *   ) ?: 0L
         */

        val totalBytes = configuration.storage.maxStorageBytes

        val gib = 1024.0.pow(3)
        val usedGB = usedBytes.toDouble() / gib
        val totalGB = totalBytes.toDouble() / gib

        val percentage = if (totalBytes > 0) {
            usedBytes * 100.0 / totalBytes
        } else {
            0.0
        }

        return StorageUsageResponse(
            usedBytes = usedBytes,
            totalBytes = totalBytes,
            usedGB = usedGB,
            totalGB = totalGB,
            percentage = percentage
        )
    }
}
