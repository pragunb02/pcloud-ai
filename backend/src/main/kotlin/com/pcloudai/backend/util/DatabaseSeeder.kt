package com.pcloudai.backend.util

import com.pcloudai.backend.core.domain.Role
import com.pcloudai.backend.core.domain.User
import com.pcloudai.backend.core.repository.UserRepository
import org.slf4j.LoggerFactory
import javax.inject.Inject

/**
 * Database seeder for creating initial users.
 */
@Suppress("TooGenericExceptionCaught")
open class DatabaseSeeder @Inject constructor(
    private val passwordUtils: PasswordUtils,
    private val userRepository: UserRepository
) {
    private val logger = LoggerFactory.getLogger(DatabaseSeeder::class.java)

    /**
     * Seed the database with initial users
     */
    fun seed() {
        logger.info("Seeding database with initial users")

        try {
            // Create a regular user if it doesn't exist
            if (userRepository.findByUsername("alice") == null) {
                logger.info("Creating user: alice")

                val alice = User(
                    username = "alice",
                    password = passwordUtils.hashPassword("password123"),
                    firstName = "Alice",
                    lastName = "User",
                    email = "alice@example.com",
                    role = Role.USER
                )

                userRepository.save(alice)
            }

            // Create an admin user if it doesn't exist
            if (userRepository.findByUsername("admin") == null) {
                logger.info("Creating user: admin")

                val admin = User(
                    username = "admin",
                    password = passwordUtils.hashPassword("adminPass"),
                    firstName = "Admin",
                    lastName = "User",
                    email = "admin@example.com",
                    role = Role.ADMIN
                )

                userRepository.save(admin)
            }

            logger.info("Database seeding completed successfully")
        } catch (e: Exception) {
            logger.error("Error seeding database", e)
        }
    }
}

// package com.pcloudai.backend.util
//
// import com.pcloudai.backend.core.domain.Role
// import com.pcloudai.backend.core.domain.User
// import com.pcloudai.backend.core.repository.UserRepository
// import io.dropwizard.db.DataSourceFactory
// import io.dropwizard.hibernate.HibernateBundle
// import org.hibernate.SessionFactory
// import org.hibernate.boot.registry.StandardServiceRegistryBuilder
// import org.hibernate.cfg.Configuration
// import org.hibernate.cfg.Environment
// import org.mindrot.jbcrypt.BCrypt
// import org.slf4j.LoggerFactory
// import java.util.Properties
//
// /**
// * Standalone database seeder for creating initial users.
// * Can be run independently to populate the database.
// */
// @Suppress("TooGenericExceptionCaught")
// class DatabaseSeeder {
//    private val logger = LoggerFactory.getLogger(DatabaseSeeder::class.java)
//    private lateinit var sessionFactory: SessionFactory
//
//    /**
//     * Initialize the database connection
//     */
//    fun initDatabase(
//        driver: String = "org.postgresql.Driver",
//        url: String = "jdbc:postgresql://localhost:5432/pcloud_ai_dev",
//        username: String = "pcloud_user",
//        password: String = "pcloud_password"
//    ) {
//        logger.info("Initializing database connection to: $url")
//
//        try {
//            val configuration = Configuration()
//
//            // Set Hibernate properties
//            val properties = Properties()
//            properties.put(Environment.DRIVER, driver)
//            properties.put(Environment.URL, url)
//            properties.put(Environment.USER, username)
//            properties.put(Environment.PASS, password)
//            properties.put(Environment.DIALECT, "org.hibernate.dialect.PostgreSQLDialect")
//            properties.put(Environment.SHOW_SQL, "true")
//            properties.put(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread")
//            properties.put(Environment.HBM2DDL_AUTO, "update")
//
//            configuration.setProperties(properties)
//
//            // Add entity classes
//            configuration.addAnnotatedClass(User::class.java)
//
//            // Build session factory
//            val serviceRegistry = StandardServiceRegistryBuilder()
//                .applySettings(configuration.properties)
//                .build()
//
//            sessionFactory = configuration.buildSessionFactory(serviceRegistry)
//            logger.info("Database connection initialized successfully")
//
//        } catch (e: Exception) {
//            logger.error("Error initializing database connection", e)
//            throw e
//        }
//    }
//
//    /**
//     * Seed the database with initial users
//     */
//    fun seed() {
//        logger.info("Seeding database with initial users")
//
//        val session = sessionFactory.openSession()
//        val transaction = session.beginTransaction()
//
//        try {
//            // Create a regular user if it doesn't exist
//            var user = session.createQuery("FROM User WHERE username = :username", User::class.java)
//                .setParameter("username", "alice")
//                .uniqueResult()
//
//            if (user == null) {
//                logger.info("Creating user: alice")
//
//                user = User(
//                    username = "alice",
//                    password = BCrypt.hashpw("password123", BCrypt.gensalt()),
//                    role = Role.USER
//                )
//
//                session.save(user)
//            }
//
//            // Create an admin user if it doesn't exist
//            user = session.createQuery("FROM User WHERE username = :username", User::class.java)
//                .setParameter("username", "admin")
//                .uniqueResult()
//
//            if (user == null) {
//                logger.info("Creating user: admin")
//
//                user = User(
//                    username = "admin",
//                    password = BCrypt.hashpw("adminPass", BCrypt.gensalt()),
//                    role = Role.ADMIN
//                )
//
//                session.save(user)
//            }
//
//            transaction.commit()
//            logger.info("Database seeding completed successfully")
//
//        } catch (e: Exception) {
//            transaction.rollback()
//            logger.error("Error seeding database", e)
//        } finally {
//            session.close()
//        }
//    }
//
//    /**
//     * Close database connection
//     */
//    fun closeConnection() {
//        if (::sessionFactory.isInitialized) {
//            sessionFactory.close()
//            logger.info("Database connection closed")
//        }
//    }
//
//    companion object {
//        /**
//         * Main method to run the database seeder
//         */
//        @JvmStatic
//        fun main(args: Array<String>) {
//            val seeder = DatabaseSeeder()
//
//            try {
//                // Initialize database connection
//                seeder.initDatabase()
//
//                // Seed the database
//                seeder.seed()
//
//            } catch (e: Exception) {
//                println("Error: ${e.message}")
//                e.printStackTrace()
//            } finally {
//                // Close connection
//                seeder.closeConnection()
//            }
//        }
//    }
// }
