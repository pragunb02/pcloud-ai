package com.pcloudai.backend.core.repository

import com.pcloudai.backend.core.domain.User
import io.dropwizard.hibernate.AbstractDAO
import org.hibernate.SessionFactory
import javax.inject.Inject
import javax.inject.Singleton
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Root

interface UserRepository {
    fun findById(id: Long): User?
    fun findByUsername(username: String): User?
    fun findAll(): List<User>
    fun save(user: User): User
    fun update(user: User): User
    fun deleteById(id: Long): Boolean
}

@Singleton
class HibernateUserRepository @Inject constructor(
    sessionFactory: SessionFactory
) : AbstractDAO<User>(sessionFactory), UserRepository {

    override fun findById(id: Long): User? {
        return get(id)
    }

    override fun findByUsername(username: String): User? {
        // Using the type-safe JPA Criteria API
        val builder: CriteriaBuilder = currentSession().criteriaBuilder
        val query: CriteriaQuery<User> = builder.createQuery(User::class.java)
        val root: Root<User> = query.from(User::class.java)

        query.select(root).where(builder.equal(root.get<String>("username"), username))

        return uniqueResult(currentSession().createQuery(query))
    }

//    override fun findByUsername(username: String): User? {
//        return query("FROM User WHERE username = :username")
//            .setParameter("username", username)
//            .uniqueResult()
//    }

    override fun findAll(): List<User> {
        val builder: CriteriaBuilder = currentSession().criteriaBuilder
        val query: CriteriaQuery<User> = builder.createQuery(User::class.java)
        query.from(User::class.java)

        return list(currentSession().createQuery(query))
    }

//    override fun findAll(): List<User> {
//        return list(query("FROM User"))
//    }

    override fun save(user: User): User {
        return persist(user)
    }

    override fun update(user: User): User {
        // Use currentSession() to access the session, not sessionFactory.
        // The sessionFactory field is private in the AbstractDAO parent class.
        return currentSession().merge(user) as User
    }

    override fun deleteById(id: Long): Boolean {
        return findById(id)?.let { userToDelete ->
            currentSession().delete(userToDelete)
            true
        } ?: false
    }
}
