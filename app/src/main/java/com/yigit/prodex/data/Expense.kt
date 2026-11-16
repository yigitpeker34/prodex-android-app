package com.yigit.prodex.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int, // Foreign key to User
    val name: String, // Defaults to category name, or custom if "Other"
    val amount: Double,
    val category: ExpenseCategory,
    @ColumnInfo(defaultValue = "0")
    val date: Long? = System.currentTimeMillis(), // User-selected date
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

enum class ExpenseCategory(val displayName: String, val colorValue: Long) {
    OTHER("Other", 0xFF90A4AE),
    BILLS("Bills", 0xFFE57373),
    DINING_OUT("Dining Out", 0xFFFF8A65),
    EDUCATION("Education", 0xFF4DB6AC),
    ENTERTAINMENT("Entertainment", 0xFFEC407A),
    FITNESS("Fitness", 0xFFAED581),
    GIFTS("Gifts", 0xFFFFB74D),
    GROCERIES("Groceries", 0xFF81C784),
    HEALTH("Health", 0xFFFFD54F),
    RENT("Rent", 0xFFBA68C8),
    SHOPPING("Shopping", 0xFFF06292),
    SUBSCRIPTIONS("Subscriptions", 0xFF7986CB),
    TRANSPORT("Transport", 0xFF4FC3F7),
    TRAVEL("Travel", 0xFF9575CD)
}
