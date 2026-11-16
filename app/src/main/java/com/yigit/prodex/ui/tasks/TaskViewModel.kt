package com.yigit.prodex.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yigit.prodex.data.Task
import com.yigit.prodex.data.TaskDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.*

class TaskViewModel(private val taskDao: TaskDao) : ViewModel() {

    fun getTasksForUser(userId: Int): Flow<List<Task>> {
        return taskDao.getAllTasks(userId)
    }

    fun addTask(userId: Int, text: String, dueDate: Long? = null) {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            
            // Prevent adding tasks with due dates in the past
            if (dueDate != null && dueDate < calendar.timeInMillis) {
                return@launch
            }

            val currentTime = System.currentTimeMillis()
            taskDao.insertTask(
                Task(
                    userId = userId,
                    text = text,
                    isChecked = false,
                    createdAt = currentTime,
                    updatedAt = currentTime,
                    dueDate = dueDate
                )
            )
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskDao.updateTask(task.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            val currentTime = System.currentTimeMillis()
            taskDao.updateTask(task.copy(deletedAt = currentTime, updatedAt = currentTime))
        }
    }
}

class TaskViewModelFactory(private val taskDao: TaskDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(taskDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
