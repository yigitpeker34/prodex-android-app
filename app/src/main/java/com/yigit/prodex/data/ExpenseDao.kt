package com.yigit.prodex.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE userId = :userId AND deletedAt IS NULL ORDER BY id DESC")
    fun getAllExpenses(userId: Int): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("UPDATE expenses SET deletedAt = :timestamp WHERE id = :expenseId")
    suspend fun softDeleteExpense(expenseId: Int, timestamp: Long)
}
