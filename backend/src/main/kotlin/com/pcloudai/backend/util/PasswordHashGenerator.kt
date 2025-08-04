package com.pcloudai.backend.util

import org.mindrot.jbcrypt.BCrypt

/**
 * Utility to generate BCrypt password hashes for testing
 */
object PasswordHashGenerator {
    @JvmStatic
    fun main(args: Array<String>) {
        val password = "password123"
        val hash = BCrypt.hashpw(password, BCrypt.gensalt())
        println("Password: $password")
        println("Hash: $hash")

        // Verify the hash works
        val isValid = BCrypt.checkpw(password, hash)
        println("Hash verification: $isValid")
    }
}

// package com.pcloudai.backend.util
//
// import org.mindrot.jbcrypt.BCrypt
// import org.slf4j.LoggerFactory
//
// /**
// * Utility for generating password hashes.
// * Run this script to generate BCrypt hashes for passwords.
// */
// class PasswordHashGenerator {
//    private val logger = LoggerFactory.getLogger(PasswordHashGenerator::class.java)
//
//    /**
//     * Generate a BCrypt hash for a password
//     * @param password The password to hash
//     * @param logRounds The log rounds to use (default: 12)
//     * @return The hashed password
//     */
//    fun generateHash(password: String, logRounds: Int = 12): String {
//        return BCrypt.hashpw(password, BCrypt.gensalt(logRounds))
//    }
//
//    /**
//     * Generate hashes for a list of passwords
//     * @param passwords Map of username to password
//     * @param logRounds The log rounds to use (default: 12)
//     */
//    fun generateHashes(passwords: Map<String, String>, logRounds: Int = 12) {
//        logger.info("Generating password hashes with log rounds: $logRounds")
//
//        passwords.forEach { (username, password) ->
//            val hash = generateHash(password, logRounds)
//            logger.info("Username: $username")
//            logger.info("Password: $password")
//            logger.info("Hash: $hash")
//            logger.info("-------------------")
//        }
//    }
//
//    companion object {
//        /**
//         * Main method to run the password hash generator
//         */
//        @JvmStatic
//        fun main(args: Array<String>) {
//            val generator = PasswordHashGenerator()
//
//            // Define passwords to hash
//            val passwords = mapOf(
//                "alice" to "password123",
//                "admin" to "adminPass",
//                "bob" to "bobsecure"
//            )
//
//            // Generate hashes
//            generator.generateHashes(passwords)
//
//            // Example of generating a single hash
//            if (args.isNotEmpty()) {
//                val customPassword = args[0]
//                val hash = generator.generateHash(customPassword)
//                println("Custom password hash: $hash")
//            }
//        }
//    }
// }
