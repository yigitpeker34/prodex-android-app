package com.yigit.prodex.util

import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64

object PasswordHasher {

    fun hashPassword(password: String, salt: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        val hashedPassword = md.digest(password.toByteArray())
        return Base64.encodeToString(hashedPassword, Base64.NO_WRAP)
    }

    fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt
    }

    fun verifyPassword(password: String, salt: ByteArray, hashedPassword: String): Boolean {
        val newHash = hashPassword(password, salt)
        return newHash == hashedPassword
    }
}
