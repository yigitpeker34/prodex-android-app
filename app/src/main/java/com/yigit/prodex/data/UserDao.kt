package com.yigit.prodex.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Query("SELECT * FROM user_profile WHERE id = :userId")
    fun getUserById(userId: Int): Flow<User?>

    @Query("SELECT * FROM user_profile WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("UPDATE user_profile SET coins = coins + :amount WHERE id = :userId")
    suspend fun addCoins(userId: Int, amount: Int)

    @Query("UPDATE user_profile SET coins = coins - :amount, isRockersUnlocked = 1 WHERE id = :userId")
    suspend fun unlockRockers(userId: Int, amount: Int)

    @Delete
    suspend fun deleteUser(user: User)
}
