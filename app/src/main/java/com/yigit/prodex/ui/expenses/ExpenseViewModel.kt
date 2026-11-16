package com.yigit.prodex.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yigit.prodex.data.Expense
import com.yigit.prodex.data.ExpenseCategory
import com.yigit.prodex.data.ExpenseDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ExpenseViewModel(private val expenseDao: ExpenseDao) : ViewModel() {

    fun getExpensesForUser(userId: Int): Flow<List<Expense>> {
        return expenseDao.getAllExpenses(userId)
    }

    fun addExpense(userId: Int, name: String, amount: Double, category: ExpenseCategory, date: Long?) {
        viewModelScope.launch {
            val currentTime = System.currentTimeMillis()
            expenseDao.insertExpense(
                Expense(
                    userId = userId,
                    name = name,
                    amount = amount,
                    category = category,
                    date = date ?: currentTime,
                    createdAt = currentTime,
                    updatedAt = currentTime
                )
            )
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            expenseDao.updateExpense(expense.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            val currentTime = System.currentTimeMillis()
            expenseDao.updateExpense(expense.copy(deletedAt = currentTime, updatedAt = currentTime))
        }
    }
}

class ExpenseViewModelFactory(private val expenseDao: ExpenseDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(expenseDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
