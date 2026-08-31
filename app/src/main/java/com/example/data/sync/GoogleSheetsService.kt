package com.example.data.sync

import com.example.data.local.entity.LoanEntity
import com.example.data.local.entity.PaymentEntity
import com.example.data.local.entity.PersonEntity
import com.example.data.model.Formatters
import com.example.data.model.LoanWithDetails
import com.example.data.model.PersonSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GoogleSheetsService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    companion object {
        const val SPREADSHEET_TITLE = "Lending Tracker Database"
    }

    /**
     * Finds an existing "Lending Tracker Database" spreadsheet in user's Google Drive or creates a new one.
     * Ensures the required sheets ("People", "Loans", "Payments") exist in the spreadsheet.
     */
    suspend fun getOrCreateDatabaseSpreadsheet(
        accessToken: String,
        existingId: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 1. Verify if the provided existingId is still valid and accessible
            if (!existingId.isNullOrBlank()) {
                val verifyRequest = Request.Builder()
                    .url("https://sheets.googleapis.com/v4/spreadsheets/$existingId?fields=spreadsheetId,properties.title,sheets.properties")
                    .addHeader("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                client.newCall(verifyRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        ensureRequiredTabsExist(accessToken, existingId, body)
                        return@withContext Result.success(existingId)
                    }
                }
            }

            // 2. Search on user's Google Drive for an existing spreadsheet named "Lending Tracker Database"
            val query = "name = '$SPREADSHEET_TITLE' and mimeType = 'application/vnd.google-apps.spreadsheet' and trashed = false"
            val driveUrl = "https://www.googleapis.com/drive/v3/files?q=${java.net.URLEncoder.encode(query, "UTF-8")}&fields=files(id,name)&spaces=drive"

            val searchRequest = Request.Builder()
                .url(driveUrl)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            client.newCall(searchRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val files = json.optJSONArray("files")
                    if (files != null && files.length() > 0) {
                        val fileId = files.getJSONObject(0).getString("id")
                        ensureRequiredTabsExist(accessToken, fileId, null)
                        return@withContext Result.success(fileId)
                    }
                }
            }

            // 3. Create a brand-new Spreadsheet with styled sheets (People, Loans, Payments)
            val createPayload = JSONObject().apply {
                put("properties", JSONObject().apply {
                    put("title", SPREADSHEET_TITLE)
                })
                put("sheets", JSONArray().apply {
                    put(createSheetConfig("People", 0))
                    put(createSheetConfig("Loans", 1))
                    put(createSheetConfig("Payments", 2))
                })
            }

            val createRequest = Request.Builder()
                .url("https://sheets.googleapis.com/v4/spreadsheets")
                .addHeader("Authorization", "Bearer $accessToken")
                .post(createPayload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(createRequest).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Failed to create spreadsheet: HTTP ${response.code} $body"))
                }
                val json = JSONObject(body)
                val newId = json.getString("spreadsheetId")

                // Apply professional styling to headers
                applyHeaderFormatting(accessToken, newId)

                Result.success(newId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createSheetConfig(title: String, index: Int): JSONObject {
        return JSONObject().apply {
            put("properties", JSONObject().apply {
                put("title", title)
                put("index", index)
                put("gridProperties", JSONObject().apply {
                    put("frozenRowCount", 1)
                })
            })
        }
    }

    /**
     * Ensures all 3 tabs ("People", "Loans", "Payments") exist in the spreadsheet.
     */
    private fun ensureRequiredTabsExist(accessToken: String, spreadsheetId: String, existingBody: String?) {
        try {
            val json = if (existingBody != null) {
                JSONObject(existingBody)
            } else {
                val req = Request.Builder()
                    .url("https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId?fields=sheets.properties")
                    .addHeader("Authorization", "Bearer $accessToken")
                    .get()
                    .build()
                client.newCall(req).execute().use { res ->
                    if (!res.isSuccessful) return
                    JSONObject(res.body?.string() ?: "")
                }
            }

            val sheets = json.optJSONArray("sheets") ?: return
            val existingTitles = mutableSetOf<String>()
            for (i in 0 until sheets.length()) {
                val sheetProp = sheets.getJSONObject(i).optJSONObject("properties")
                sheetProp?.optString("title")?.let { existingTitles.add(it) }
            }

            val required = listOf("People", "Loans", "Payments")
            val missing = required.filter { it !in existingTitles }

            if (missing.isNotEmpty()) {
                val requestsArray = JSONArray()
                missing.forEach { title ->
                    requestsArray.put(JSONObject().apply {
                        put("addSheet", JSONObject().apply {
                            put("properties", JSONObject().apply {
                                put("title", title)
                                put("gridProperties", JSONObject().apply {
                                    put("frozenRowCount", 1)
                                })
                            })
                        })
                    })
                }

                val batchPayload = JSONObject().apply {
                    put("requests", requestsArray)
                }

                val batchReq = Request.Builder()
                    .url("https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId:batchUpdate")
                    .addHeader("Authorization", "Bearer $accessToken")
                    .post(batchPayload.toString().toRequestBody(jsonMediaType))
                    .build()

                client.newCall(batchReq).execute().close()
            }
        } catch (_: Exception) {}
    }

    /**
     * Applies styling (header background color, bold text, frozen row)
     */
    private fun applyHeaderFormatting(accessToken: String, spreadsheetId: String) {
        try {
            // Style header formatting via batchUpdate if possible
            val req = Request.Builder()
                .url("https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId?fields=sheets.properties")
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val sheetIds = mutableListOf<Int>()
            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return
                val json = JSONObject(res.body?.string() ?: "")
                val sheets = json.optJSONArray("sheets") ?: return
                for (i in 0 until sheets.length()) {
                    val props = sheets.getJSONObject(i).optJSONObject("properties")
                    val sheetId = props?.optInt("sheetId", -1) ?: -1
                    if (sheetId >= 0) {
                        sheetIds.add(sheetId)
                    }
                }
            }

            val requestsArray = JSONArray()
            sheetIds.forEach { sheetId ->
                // Format header row (Row 0)
                requestsArray.put(JSONObject().apply {
                    put("repeatCell", JSONObject().apply {
                        put("range", JSONObject().apply {
                            put("sheetId", sheetId)
                            put("startRowIndex", 0)
                            put("endRowIndex", 1)
                        })
                        put("cell", JSONObject().apply {
                            put("userEnteredFormat", JSONObject().apply {
                                put("backgroundColor", JSONObject().apply {
                                    put("red", 0.12)
                                    put("green", 0.25)
                                    put("blue", 0.55)
                                })
                                put("textFormat", JSONObject().apply {
                                    put("foregroundColor", JSONObject().apply {
                                        put("red", 1.0)
                                        put("green", 1.0)
                                        put("blue", 1.0)
                                    })
                                    put("bold", true)
                                    put("fontSize", 11)
                                })
                            })
                        })
                        put("fields", "userEnteredFormat(backgroundColor,textFormat)")
                    })
                })
            }

            if (requestsArray.length() > 0) {
                val batchPayload = JSONObject().apply {
                    put("requests", requestsArray)
                }
                val batchReq = Request.Builder()
                    .url("https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId:batchUpdate")
                    .addHeader("Authorization", "Bearer $accessToken")
                    .post(batchPayload.toString().toRequestBody(jsonMediaType))
                    .build()

                client.newCall(batchReq).execute().close()
            }
        } catch (_: Exception) {}
    }

    /**
     * Uploads the entire local state (People, Loans, Payments) to the connected Google Sheet.
     */
    suspend fun syncAllDataToSheet(
        accessToken: String,
        spreadsheetId: String,
        people: List<PersonSummary>,
        loans: List<LoanWithDetails>,
        payments: List<PaymentEntity>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            ensureRequiredTabsExist(accessToken, spreadsheetId, null)

            // 1. Prepare People Sheet Data
            val peopleRows = mutableListOf<List<Any>>()
            peopleRows.add(
                listOf(
                    "Person ID", "Name", "Phone", "Email", "Notes",
                    "Total Lent", "Total Borrowed", "Net Balance", "Active Loans", "Settled Loans", "Created Date"
                )
            )
            people.forEach { p ->
                peopleRows.add(
                    listOf(
                        p.id,
                        p.name,
                        p.phone,
                        p.email,
                        p.notes,
                        p.totalLent,
                        p.totalBorrowed,
                        p.netBalanceOwedToUser,
                        p.activeLoansCount,
                        p.settledLoansCount,
                        Formatters.formatDateTime(p.createdAt)
                    )
                )
            }

            // 2. Prepare Loans Sheet Data
            val loanRows = mutableListOf<List<Any>>()
            loanRows.add(
                listOf(
                    "Loan ID", "Person ID", "Person Name", "Direction", "Original Amount",
                    "Currency", "Total Paid", "Outstanding Balance", "Status", "Loan Date",
                    "Due Date", "Purpose / Category", "Payment Method", "Notes", "Last Updated"
                )
            )
            loans.forEach { l ->
                loanRows.add(
                    listOf(
                        l.id,
                        l.personId,
                        l.personName,
                        l.direction.name,
                        l.originalAmount,
                        l.currency,
                        l.totalPaid,
                        l.outstanding,
                        l.status.name,
                        Formatters.formatDate(l.loanDate),
                        if (l.dueDate != null) Formatters.formatDate(l.dueDate) else "No fixed date",
                        l.purpose,
                        l.paymentMethod,
                        l.note,
                        Formatters.formatDateTime(l.updatedAt)
                    )
                )
            }

            // 3. Prepare Payments Sheet Data
            val loanMap = loans.associateBy { it.id }
            val paymentRows = mutableListOf<List<Any>>()
            paymentRows.add(
                listOf(
                    "Payment ID", "Loan ID", "Person Name", "Amount",
                    "Payment Date", "Payment Method", "Notes", "Recorded At"
                )
            )
            payments.forEach { pay ->
                val loan = loanMap[pay.loanId]
                val personName = loan?.personName ?: "Unknown"
                paymentRows.add(
                    listOf(
                        pay.id,
                        pay.loanId,
                        personName,
                        pay.amount,
                        Formatters.formatDate(pay.paymentDate),
                        pay.paymentMethod,
                        pay.note,
                        Formatters.formatDateTime(pay.createdAt)
                    )
                )
            }

            // Execute writes for People, Loans, Payments
            clearAndWriteRange(accessToken, spreadsheetId, "People!A1:Z500", peopleRows)
            clearAndWriteRange(accessToken, spreadsheetId, "Loans!A1:Z500", loanRows)
            clearAndWriteRange(accessToken, spreadsheetId, "Payments!A1:Z500", paymentRows)

            applyHeaderFormatting(accessToken, spreadsheetId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun clearAndWriteRange(
        accessToken: String,
        spreadsheetId: String,
        range: String,
        rows: List<List<Any>>
    ) {
        // Step A: Clear existing range
        val clearUrl = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/$range:clear"
        val clearReq = Request.Builder()
            .url(clearUrl)
            .addHeader("Authorization", "Bearer $accessToken")
            .post("{}".toRequestBody(jsonMediaType))
            .build()
        try {
            client.newCall(clearReq).execute().close()
        } catch (_: Exception) {}

        // Step B: Write new values
        val updateUrl = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/${range.substringBefore(':')}?valueInputOption=USER_ENTERED"
        val valuesArray = JSONArray()
        rows.forEach { row ->
            val rowArray = JSONArray()
            row.forEach { cell ->
                rowArray.put(cell)
            }
            valuesArray.put(rowArray)
        }

        val updatePayload = JSONObject().apply {
            put("range", range.substringBefore(':'))
            put("majorDimension", "ROWS")
            put("values", valuesArray)
        }

        val updateReq = Request.Builder()
            .url(updateUrl)
            .addHeader("Authorization", "Bearer $accessToken")
            .put(updatePayload.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(updateReq).execute().use { response ->
            if (!response.isSuccessful) {
                val err = response.body?.string() ?: ""
                throw Exception("Failed to write to $range: HTTP ${response.code} $err")
            }
        }
    }

    /**
     * Reads values from Google Sheet to import / restore into local Room Database.
     */
    suspend fun readSpreadsheetData(
        accessToken: String,
        spreadsheetId: String
    ): Result<Triple<List<PersonEntity>, List<LoanEntity>, List<PaymentEntity>>> = withContext(Dispatchers.IO) {
        try {
            val people = readPeopleSheet(accessToken, spreadsheetId)
            val loans = readLoansSheet(accessToken, spreadsheetId)
            val payments = readPaymentsSheet(accessToken, spreadsheetId)

            Result.success(Triple(people, loans, payments))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun readPeopleSheet(accessToken: String, spreadsheetId: String): List<PersonEntity> {
        val url = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/People!A2:K500"
        val req = Request.Builder().url(url).addHeader("Authorization", "Bearer $accessToken").get().build()
        val list = mutableListOf<PersonEntity>()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return list
            val json = JSONObject(res.body?.string() ?: "")
            val values = json.optJSONArray("values") ?: return list
            for (i in 0 until values.length()) {
                val row = values.getJSONArray(i)
                if (row.length() >= 2) {
                    val id = row.optString(0).toLongOrNull() ?: (i + 1L)
                    val name = row.optString(1)
                    val phone = row.optString(2)
                    val email = row.optString(3)
                    val notes = row.optString(4)
                    if (name.isNotBlank()) {
                        list.add(PersonEntity(id = id, name = name, phone = phone, email = email, notes = notes))
                    }
                }
            }
        }
        return list
    }

    private fun readLoansSheet(accessToken: String, spreadsheetId: String): List<LoanEntity> {
        val url = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/Loans!A2:O500"
        val req = Request.Builder().url(url).addHeader("Authorization", "Bearer $accessToken").get().build()
        val list = mutableListOf<LoanEntity>()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return list
            val json = JSONObject(res.body?.string() ?: "")
            val values = json.optJSONArray("values") ?: return list
            for (i in 0 until values.length()) {
                val row = values.getJSONArray(i)
                if (row.length() >= 5) {
                    val id = row.optString(0).toLongOrNull() ?: (i + 1L)
                    val personId = row.optString(1).toLongOrNull() ?: 1L
                    val direction = row.optString(3).ifBlank { "LENT" }
                    val amount = row.optString(4).replace(",", "").toDoubleOrNull() ?: 0.0
                    val currency = row.optString(5).ifBlank { "BDT" }
                    val purpose = row.optString(11)
                    val paymentMethod = row.optString(12).ifBlank { "Cash" }
                    val note = row.optString(13)

                    if (amount > 0) {
                        list.add(
                            LoanEntity(
                                id = id,
                                personId = personId,
                                direction = direction,
                                originalAmount = amount,
                                currency = currency,
                                purpose = purpose,
                                paymentMethod = paymentMethod,
                                note = note
                            )
                        )
                    }
                }
            }
        }
        return list
    }

    private fun readPaymentsSheet(accessToken: String, spreadsheetId: String): List<PaymentEntity> {
        val url = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/Payments!A2:H500"
        val req = Request.Builder().url(url).addHeader("Authorization", "Bearer $accessToken").get().build()
        val list = mutableListOf<PaymentEntity>()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return list
            val json = JSONObject(res.body?.string() ?: "")
            val values = json.optJSONArray("values") ?: return list
            for (i in 0 until values.length()) {
                val row = values.getJSONArray(i)
                if (row.length() >= 4) {
                    val id = row.optString(0).toLongOrNull() ?: (i + 1L)
                    val loanId = row.optString(1).toLongOrNull() ?: 1L
                    val amount = row.optString(3).replace(",", "").toDoubleOrNull() ?: 0.0
                    val paymentMethod = row.optString(5).ifBlank { "Cash" }
                    val note = row.optString(6)

                    if (amount > 0) {
                        list.add(
                            PaymentEntity(
                                id = id,
                                loanId = loanId,
                                amount = amount,
                                paymentMethod = paymentMethod,
                                note = note
                            )
                        )
                    }
                }
            }
        }
        return list
    }
}
