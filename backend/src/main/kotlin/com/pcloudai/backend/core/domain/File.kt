package com.pcloudai.backend.core.domain

import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

enum class FileStatus {
    ACTIVE,
    DELETED,
    PROCESSING
}

@Entity
@Table(name = "files")
data class File(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "name", nullable = false)
    var name: String = "",

    @Column(name = "original_name", nullable = false)
    var originalName: String = "",

    @Column(name = "content_type", nullable = false)
    var contentType: String = "",

    @Column(name = "size", nullable = false)
    var size: Long = 0,

    @Column(name = "storage_path", nullable = false)
    var storagePath: String = "",

    @Column(name = "thumbnail_url")
    var thumbnailUrl: String? = null,

    @Column(name = "preview_url")
    var previewUrl: String? = null,

    @Column(name = "text_content", columnDefinition = "TEXT")
    var textContent: String? = null,

    @Column(name = "has_text_content")
    var hasTextContent: Boolean = false,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: FileStatus = FileStatus.ACTIVE,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null
) {
    fun isActive(): Boolean = status == FileStatus.ACTIVE

    fun isDeleted(): Boolean = status == FileStatus.DELETED

    fun isProcessing(): Boolean = status == FileStatus.PROCESSING

    fun markAsDeleted() {
        status = FileStatus.DELETED
        updatedAt = LocalDateTime.now()
    }

    fun markAsProcessing() {
        status = FileStatus.PROCESSING
        updatedAt = LocalDateTime.now()
    }

    fun markAsActive() {
        status = FileStatus.ACTIVE
        updatedAt = LocalDateTime.now()
    }

//    override fun toString(): String {
//        return "File(id=$id, name='$name', size=$size, status=$status)"
//    }
//
//    override fun equals(other: Any?): Boolean {
//        if (this === other) return true
//        if (javaClass != other?.javaClass) return false
//
//        other as File
//
//        return id == other.id
//    }
//
//    override fun hashCode(): Int {
//        return id.hashCode()
//    }
}
