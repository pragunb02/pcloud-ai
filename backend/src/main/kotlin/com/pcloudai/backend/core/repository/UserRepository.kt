package com.pcloudai.backend.core.repository

import com.pcloudai.backend.core.domain.User
import io.dropwizard.hibernate.AbstractDAO
import org.hibernate.SessionFactory
import javax.inject.Inject
import javax.inject.Singleton

interface UserRepository {
    fun findById(id: Long): User?
    fun findByUsername(username: String): User?
    fun findAll(): List<User>
    fun save(user: User): User
    fun update(user: User): User
    fun deleteById(id: Long): Boolean
}

/**
 * Hibernate implementation of UserRepository
 */
@Singleton
class HibernateUserRepository @Inject constructor(
    sessionFactory: SessionFactory
) : AbstractDAO<User>(sessionFactory), UserRepository {

    override fun findById(id: Long): User? {
        return get(id)
    }

    override fun findByUsername(username: String): User? {
        return query("FROM User WHERE username = :username")
            .setParameter("username", username)
            .uniqueResult()
    }

    override fun findAll(): List<User> {
        return list(query("FROM User"))
    }

    override fun save(user: User): User {
        return persist(user)
    }

    override fun update(user: User): User {
        return currentSession().merge(user) as User
    }

    override fun deleteById(id: Long): Boolean {
        val user = get(id) ?: return false
        currentSession().delete(user)
        return true
    }
}
