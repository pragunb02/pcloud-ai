package com.pcloudai.backend.core.domain

import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

enum class Role {
    USER,
    ADMIN
}

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "username", unique = true, nullable = false)
    var username: String = "",

    @Column(name = "password", nullable = false)
    var password: String = "",

    @Column(name = "first_name", nullable = false)
    var firstName: String = "",

    @Column(name = "last_name", nullable = false)
    var lastName: String = "",

    @Column(name = "email", unique = true, nullable = true)
    var email: String = "",

    @Column(name = "bio", length = 500, nullable = true)
    var bio: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    var role: Role = Role.USER,

    @CreationTimestamp
    @Column(name = "created_at", nullable = true, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null
) {
    fun isAdmin(): Boolean = role == Role.ADMIN

//    override fun toString(): String {
//        return "User(id=$id, username='$username', role=$role)"
//    }
//
//    override fun equals(other: Any?): Boolean {
//        if (this === other) return true
//        if (javaClass != other?.javaClass) return false
//
//        other as User
//
//        return id == other.id
//    }
//
//    override fun hashCode(): Int {
//        return id.hashCode()
//    }
}
