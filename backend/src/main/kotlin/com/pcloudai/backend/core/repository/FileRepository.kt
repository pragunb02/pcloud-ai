package com.pcloudai.backend.core.repository

import com.pcloudai.backend.core.domain.File
import com.pcloudai.backend.core.domain.FileStatus
import io.dropwizard.hibernate.AbstractDAO
import org.hibernate.SessionFactory
import javax.inject.Singleton

interface FileRepository {
    fun findById(id: Long): File?
    fun findByUserId(userId: Long): List<File>
    fun findActiveByUserId(userId: Long): List<File>
    fun findActiveByUserIdPaginated(userId: Long, offset: Int, limit: Int, search: String? = null): List<File>
    fun countActiveByUserId(userId: Long, search: String? = null): Long
    fun findAll(): List<File>
    fun save(file: File): File
    fun update(file: File): File
    fun delete(id: Long): Boolean
}

@Singleton
class HibernateFileRepository(sessionFactory: SessionFactory) :
    AbstractDAO<File>(sessionFactory), FileRepository {

    override fun findById(id: Long): File? {
        return get(id)
    }

    override fun findByUserId(userId: Long): List<File> {
        return list(
            query("FROM File f WHERE f.user.id = :userId")
                .setParameter("userId", userId)
        )
    }

    override fun findActiveByUserId(userId: Long): List<File> {
        return list(
            query("FROM File f WHERE f.user.id = :userId AND f.status = :status")
                .setParameter("userId", userId)
                .setParameter("status", FileStatus.ACTIVE)
        )
    }

    override fun findActiveByUserIdPaginated(userId: Long, offset: Int, limit: Int, search: String?): List<File> {
        val hql = if (search != null) {
            "FROM File f WHERE f.user.id = :userId AND f.status = :status" +
                " AND f.originalName LIKE :search ORDER BY f.createdAt DESC"
        } else {
            "FROM File f WHERE f.user.id = :userId AND f.status = :status ORDER BY f.createdAt DESC"
        }

        val query = query(hql)
            .setParameter("userId", userId)
            .setParameter("status", FileStatus.ACTIVE)
            .setFirstResult(offset)
            .setMaxResults(limit)

        if (search != null) {
            query.setParameter("search", "%$search%")
        }

        return list(query)
    }

    override fun countActiveByUserId(userId: Long, search: String?): Long {
        val sql = if (search != null) {
            "SELECT COUNT(*) FROM files WHERE user_id = :userId AND " +
                "status = :status AND original_name LIKE :search"
        } else {
            "SELECT COUNT(*) FROM files WHERE user_id = :userId AND status = :status"
        }

        val query = currentSession().createNativeQuery(sql)
            .setParameter("userId", userId)
            .setParameter("status", FileStatus.ACTIVE.name)

        if (search != null) {
            query.setParameter("search", "%$search%")
        }

        val result = query.uniqueResult()
        return when (result) {
            is Number -> result.toLong()
            else -> 0L
        }
    }

    override fun findAll(): List<File> {
        return list(query("FROM File"))
    }

    override fun save(file: File): File {
        return persist(file)
    }

    override fun update(file: File): File {
        return currentSession().merge(file) as File
    }

    override fun delete(id: Long): Boolean {
        val file = get(id) ?: return false
        file.markAsDeleted()
        persist(file)
        return true
    }
}
