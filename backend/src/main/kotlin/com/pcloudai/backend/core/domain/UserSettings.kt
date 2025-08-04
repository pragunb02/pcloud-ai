package com.pcloudai.backend.core.domain

import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToOne
import javax.persistence.PreUpdate
import javax.persistence.Table

@Entity
@Table(name = "user_settings")
data class UserSettings(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(name = "theme", nullable = false)
    var theme: String = "light", // "light", "dark", or "system"

    @Column(name = "email_notifications", nullable = false)
    var emailNotifications: Boolean = true,

    @Column(name = "push_notifications", nullable = false)
    var pushNotifications: Boolean = true,

    @Column(name = "weekly_report_notifications", nullable = false)
    var weeklyReportNotifications: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null
) {
    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
