package com.yigit.prodex.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val lastName: String,
    val email: String,
    val hashedPassword: String,
    val salt: ByteArray,
    val birthDate: Long? = null,
    val profilePictureUri: String? = null,
    val gender: String? = null,
    val coins: Int = 0,
    val isRockersUnlocked: Boolean = false,
    val securityQuestion: String? = null,
    val securityAnswer: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        if (id != other.id) return false
        if (name != other.name) return false
        if (lastName != other.lastName) return false
        if (email != other.email) return false
        if (hashedPassword != other.hashedPassword) return false
        if (!salt.contentEquals(other.salt)) return false
        if (birthDate != other.birthDate) return false
        if (profilePictureUri != other.profilePictureUri) return false
        if (gender != other.gender) return false
        if (coins != other.coins) return false
        if (isRockersUnlocked != other.isRockersUnlocked) return false
        if (securityQuestion != other.securityQuestion) return false
        if (securityAnswer != other.securityAnswer) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + name.hashCode()
        result = 31 * result + lastName.hashCode()
        result = 31 * result + email.hashCode()
        result = 31 * result + hashedPassword.hashCode()
        result = 31 * result + salt.contentHashCode()
        result = 31 * result + (birthDate?.hashCode() ?: 0)
        result = 31 * result + (profilePictureUri?.hashCode() ?: 0)
        result = 31 * result + (gender?.hashCode() ?: 0)
        result = 31 * result + coins
        result = 31 * result + isRockersUnlocked.hashCode()
        result = 31 * result + (securityQuestion?.hashCode() ?: 0)
        result = 31 * result + (securityAnswer?.hashCode() ?: 0)
        return result
    }
}
