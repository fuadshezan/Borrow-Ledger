package com.example.data.local

import com.example.data.local.entity.LoanEntity
import com.example.data.local.entity.PaymentEntity
import com.example.data.local.entity.PersonEntity
import com.example.data.local.entity.ReminderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SeedData {
    suspend fun populateIfEmpty(db: AppDatabase) = withContext(Dispatchers.IO) {
        val count = db.personDao().getPeopleCount()
        if (count > 0) return@withContext

        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L

        // 1. Rahim Ahmed
        val rahimId = db.personDao().insertPerson(
            PersonEntity(
                name = "Rahim Ahmed",
                phone = "01712345678",
                email = "rahim.ahmed@example.com",
                notes = "University friend & colleague",
                createdAt = now - (30 * dayMs)
            )
        )

        // Rahim Loan 1: Emergency help
        val rahimLoan1 = db.loanDao().insertLoan(
            LoanEntity(
                personId = rahimId,
                direction = "LENT",
                originalAmount = 50000.0,
                loanDate = now - (25 * dayMs),
                dueDate = now + (5 * dayMs), // Due soon (within 5 days)
                purpose = "Emergency Help",
                note = "Hospital expenses. Promised to pay in installments.",
                paymentMethod = "bKash",
                installmentAmount = 10000.0,
                installmentFrequency = "Monthly",
                installmentCount = 5,
                createdAt = now - (25 * dayMs)
            )
        )
        // Payments for Rahim Loan 1
        db.paymentDao().insertPayment(
            PaymentEntity(
                loanId = rahimLoan1,
                amount = 10000.0,
                paymentDate = now - (18 * dayMs),
                paymentMethod = "bKash",
                note = "First installment",
                createdAt = now - (18 * dayMs)
            )
        )
        db.paymentDao().insertPayment(
            PaymentEntity(
                loanId = rahimLoan1,
                amount = 10000.0,
                paymentDate = now - (10 * dayMs),
                paymentMethod = "Cash",
                note = "Second installment",
                createdAt = now - (10 * dayMs)
            )
        )
        db.paymentDao().insertPayment(
            PaymentEntity(
                loanId = rahimLoan1,
                amount = 15000.0,
                paymentDate = now - (2 * dayMs),
                paymentMethod = "bKash",
                note = "Third installment",
                createdAt = now - (2 * dayMs)
            )
        )

        // Rahim Loan 2: Laptop purchase
        val rahimLoan2 = db.loanDao().insertLoan(
            LoanEntity(
                personId = rahimId,
                direction = "LENT",
                originalAmount = 20000.0,
                loanDate = now - (14 * dayMs),
                dueDate = null, // No fixed due date
                purpose = "Laptop Purchase",
                note = "Short loan for buying work laptop",
                paymentMethod = "Bank Transfer",
                createdAt = now - (14 * dayMs)
            )
        )
        db.paymentDao().insertPayment(
            PaymentEntity(
                loanId = rahimLoan2,
                amount = 10000.0,
                paymentDate = now - (7 * dayMs),
                paymentMethod = "bKash",
                note = "Half payment returned",
                createdAt = now - (7 * dayMs)
            )
        )

        // 2. Karim Hasan
        val karimId = db.personDao().insertPerson(
            PersonEntity(
                name = "Karim Hasan",
                phone = "01887654321",
                email = "karim.hasan@example.com",
                notes = "Neighborhood cousin",
                createdAt = now - (40 * dayMs)
            )
        )
        val karimLoan = db.loanDao().insertLoan(
            LoanEntity(
                personId = karimId,
                direction = "LENT",
                originalAmount = 10000.0,
                loanDate = now - (20 * dayMs),
                dueDate = now - (2 * dayMs), // Overdue by 2 days!
                purpose = "Home Renovation",
                note = "Urgent repair materials",
                paymentMethod = "Cash",
                createdAt = now - (20 * dayMs)
            )
        )

        // 3. Hasan Mahmud (Settled)
        val hasanId = db.personDao().insertPerson(
            PersonEntity(
                name = "Hasan Mahmud",
                phone = "01911223344",
                email = "hasan.m@example.com",
                notes = "High school friend",
                createdAt = now - (60 * dayMs)
            )
        )
        val hasanLoan = db.loanDao().insertLoan(
            LoanEntity(
                personId = hasanId,
                direction = "LENT",
                originalAmount = 15000.0,
                loanDate = now - (50 * dayMs),
                dueDate = now - (10 * dayMs),
                purpose = "Travel Expenses",
                note = "Sajek tour trip booking",
                paymentMethod = "Nagad",
                createdAt = now - (50 * dayMs)
            )
        )
        db.paymentDao().insertPayment(
            PaymentEntity(
                loanId = hasanLoan,
                amount = 15000.0,
                paymentDate = now - (15 * dayMs),
                paymentMethod = "Nagad",
                note = "Full repayment settled",
                createdAt = now - (15 * dayMs)
            )
        )

        // 4. Sakib (Money borrowed by user)
        val sakibId = db.personDao().insertPerson(
            PersonEntity(
                name = "Sakib",
                phone = "01699887766",
                email = "sakib@example.com",
                notes = "Office team lead",
                createdAt = now - (12 * dayMs)
            )
        )
        db.loanDao().insertLoan(
            LoanEntity(
                personId = sakibId,
                direction = "BORROWED",
                originalAmount = 5000.0,
                loanDate = now - (10 * dayMs),
                dueDate = now + (15 * dayMs),
                purpose = "Temporary Cash Borrowed",
                note = "Needed cash for restaurant dinner bill",
                paymentMethod = "Cash",
                createdAt = now - (10 * dayMs)
            )
        )

        // Add reminders
        db.reminderDao().insertReminder(
            ReminderEntity(
                loanId = karimLoan,
                reminderType = "OVERDUE",
                reminderDate = now - (1 * dayMs),
                isCompleted = false,
                notes = "Karim's ৳10,000 repayment is overdue by 2 days."
            )
        )
        db.reminderDao().insertReminder(
            ReminderEntity(
                loanId = rahimLoan1,
                reminderType = "UPCOMING_7D",
                reminderDate = now + (5 * dayMs),
                isCompleted = false,
                notes = "Rahim's repayment is due in 5 days."
            )
        )
    }
}
