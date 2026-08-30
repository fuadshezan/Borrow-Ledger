package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "loans",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["personId"])]
)
data class LoanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val personId: Long,
    val direction: String, // "LENT" or "BORROWED"
    val originalAmount: Double,
    val currency: String = "BDT",
    val loanDate: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val purpose: String = "",
    val note: String = "",
    val paymentMethod: String = "Cash",
    val installmentAmount: Double? = null,
    val installmentFrequency: String? = null,
    val installmentCount: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
