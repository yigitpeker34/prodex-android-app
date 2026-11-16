package com.yigit.prodex.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE userId = :userId AND deletedAt IS NULL ORDER BY id DESC")
    fun getAllTasks(userId: Int): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("UPDATE tasks SET deletedAt = :timestamp WHERE id = :taskId")
    suspend fun softDeleteTask(taskId: Int, timestamp: Long)
}
