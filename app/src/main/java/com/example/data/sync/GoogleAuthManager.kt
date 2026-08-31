package com.example.data.sync

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GoogleUserState(
    val isSignedIn: Boolean = false,
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val accessToken: String = "",
    val spreadsheetId: String = "",
    val spreadsheetUrl: String = ""
)

class GoogleAuthManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("google_sync_prefs", Context.MODE_PRIVATE)
    private val credentialManager = CredentialManager.create(context)

    private val _userState = MutableStateFlow(loadUserState())
    val userState: StateFlow<GoogleUserState> = _userState.asStateFlow()

    private fun loadUserState(): GoogleUserState {
        val isSignedIn = prefs.getBoolean("is_signed_in", false)
        val email = prefs.getString("user_email", "") ?: ""
        val displayName = prefs.getString("user_name", "") ?: ""
        val photoUrl = prefs.getString("user_photo", "") ?: ""
        val accessToken = prefs.getString("access_token", "") ?: ""
        var spreadsheetId = prefs.getString("spreadsheet_id", "") ?: ""
        if (spreadsheetId.startsWith("simulated_sheet_id") || spreadsheetId.startsWith("1LT_")) {
            spreadsheetId = ""
            prefs.edit().putString("spreadsheet_id", "").apply()
        }
        val spreadsheetUrl = if (spreadsheetId.isNotBlank()) {
            "https://docs.google.com/spreadsheets/d/$spreadsheetId"
        } else {
            ""
        }
        return GoogleUserState(
            isSignedIn = isSignedIn,
            email = email,
            displayName = displayName,
            photoUrl = photoUrl,
            accessToken = accessToken,
            spreadsheetId = spreadsheetId,
            spreadsheetUrl = spreadsheetUrl
        )
    }

    fun saveUser(
        email: String,
        displayName: String,
        photoUrl: String = "",
        accessToken: String = ""
    ) {
        val existingSpreadsheetId = prefs.getString("spreadsheet_id", "") ?: ""
        val cleanedSpreadsheetId = if (existingSpreadsheetId.startsWith("simulated_sheet_id") || existingSpreadsheetId.startsWith("1LT_")) {
            ""
        } else {
            existingSpreadsheetId
        }

        prefs.edit()
            .putBoolean("is_signed_in", true)
            .putString("user_email", email)
            .putString("user_name", displayName)
            .putString("user_photo", photoUrl)
            .putString("access_token", accessToken)
            .putString("spreadsheet_id", cleanedSpreadsheetId)
            .apply()

        _userState.value = GoogleUserState(
            isSignedIn = true,
            email = email,
            displayName = displayName,
            photoUrl = photoUrl,
            accessToken = accessToken,
            spreadsheetId = cleanedSpreadsheetId,
            spreadsheetUrl = if (cleanedSpreadsheetId.isNotBlank()) "https://docs.google.com/spreadsheets/d/$cleanedSpreadsheetId" else ""
        )
    }

    fun saveSpreadsheetId(spreadsheetId: String) {
        prefs.edit()
            .putString("spreadsheet_id", spreadsheetId)
            .apply()

        _userState.value = _userState.value.copy(
            spreadsheetId = spreadsheetId,
            spreadsheetUrl = if (spreadsheetId.isNotBlank()) "https://docs.google.com/spreadsheets/d/$spreadsheetId" else ""
        )
    }

    fun updateAccessToken(token: String) {
        prefs.edit().putString("access_token", token).apply()
        _userState.value = _userState.value.copy(accessToken = token)
    }

    suspend fun signOut() {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (_: Exception) {}

        prefs.edit()
            .putBoolean("is_signed_in", false)
            .putString("user_email", "")
            .putString("user_name", "")
            .putString("user_photo", "")
            .putString("access_token", "")
            .putString("spreadsheet_id", "")
            .apply()

        _userState.value = GoogleUserState()
    }
}
