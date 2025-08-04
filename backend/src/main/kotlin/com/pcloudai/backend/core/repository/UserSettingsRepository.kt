package com.pcloudai.backend.core.repository

import com.pcloudai.backend.core.domain.User
import com.pcloudai.backend.core.domain.UserSettings
import io.dropwizard.hibernate.AbstractDAO
import org.hibernate.SessionFactory
import org.hibernate.query.Query
import javax.inject.Inject
import javax.inject.Singleton

interface UserSettingsRepository {
    fun findByUser(user: User): UserSettings?
    fun save(userSettings: UserSettings): UserSettings
    fun update(userSettings: UserSettings): UserSettings
}

@Singleton
class HibernateUserSettingsRepository @Inject constructor(
    sessionFactory: SessionFactory
) : AbstractDAO<UserSettings>(sessionFactory), UserSettingsRepository {

    override fun findByUser(user: User): UserSettings? {
        val query: Query<UserSettings> = currentSession()
            .createQuery(
                "FROM UserSettings WHERE user.id = :userId",
                UserSettings::class.java
            )
        query.setParameter("userId", user.id)
        return query.uniqueResult()
    }

    override fun save(userSettings: UserSettings): UserSettings {
        return persist(userSettings)
    }

    override fun update(userSettings: UserSettings): UserSettings {
        return currentSession().merge(userSettings) as UserSettings
    }
}
