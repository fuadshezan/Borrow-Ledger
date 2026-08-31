package com.example.data.sync

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

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

    companion object {
        /**
         * Web Client ID (OAuth 2.0 client) from Firebase Console (borrow-money-11b9a).
         * Used as the serverClientId for Google Sign-In and Firebase credential.
         */
        private const val WEB_CLIENT_ID =
            "740333374448-u1s8n2hne03hmnbgdmpef7kjgknt58sn.apps.googleusercontent.com"

        /** Google API scopes required for Sheets + Drive file access. */
        private val OAUTH_SCOPES = listOf(
            "https://www.googleapis.com/auth/spreadsheets",
            "https://www.googleapis.com/auth/drive.file"
        )
    }

    private fun getWebClientId(): String {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (resId != 0) {
            try { context.getString(resId) } catch (_: Exception) { WEB_CLIENT_ID }
        } else {
            WEB_CLIENT_ID
        }
    }

    private val prefs = context.getSharedPreferences("google_sync_prefs", Context.MODE_PRIVATE)
    private val firebaseAuth = FirebaseAuth.getInstance()

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

    /**
     * Processes the result Intent from the Google Sign-In activity.
     *
     * Flow:
     * 1. Extract [GoogleSignInAccount] from the result data.
     * 2. Authenticate with Firebase using the Google ID token.
     * 3. Acquire an OAuth access token with Sheets + Drive scopes via [GoogleAuthUtil].
     * 4. Persist user info and update [userState].
     *
     * @return [Result.success] with the saved [GoogleUserState] on success,
     *         or [Result.failure] with a descriptive exception on error.
     */
    suspend fun handleSignInResult(data: Intent?): Result<GoogleUserState> =
        withContext(Dispatchers.IO) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                    ?: return@withContext Result.failure(Exception("Sign-in was cancelled"))

                // Step 1 – Sign in to Firebase with the Google credential
                val firebaseCredential = GoogleAuthProvider.getCredential(account.idToken, null)
                firebaseAuth.signInWithCredential(firebaseCredential).await()

                // Step 2 – Acquire an OAuth access token with Sheets/Drive scopes.
                // GoogleAuthUtil.getToken() is a blocking IO call; must run on IO dispatcher.
                val accessToken = acquireAccessToken(account)

                val email = account.email ?: ""
                val name = account.displayName ?: ""
                val photo = account.photoUrl?.toString() ?: ""

                saveUser(email, name, photo, accessToken)
                Result.success(_userState.value)

            } catch (e: ApiException) {
                // Status code 12501 = SIGN_IN_CANCELLED (user backed out)
                val cancelled = e.statusCode == 12501 || e.statusCode == 12502
                val msg = if (cancelled) "Sign-in cancelled" else "Google Sign-In failed (code ${e.statusCode})"
                Result.failure(Exception(msg))
            } catch (e: Exception) {
                Result.failure(Exception("Sign-in error: ${e.message}"))
            }
        }

    /**
     * Retrieves a valid OAuth 2.0 access token for Google Sheets & Drive.
     * Uses the last signed-in Google account and requests a token via [GoogleAuthUtil].
     * If the token was previously cached and needs refreshing, [forceRefresh] will invalidate it first.
     */
    suspend fun getOrRefreshAccessToken(forceRefresh: Boolean = false): String? =
        withContext(Dispatchers.IO) {
            try {
                val account = GoogleSignIn.getLastSignedInAccount(context)
                val androidAccount = account?.account
                if (androidAccount == null) {
                    android.util.Log.w("GoogleAuthManager", "No Google account found. Sign-in required.")
                    return@withContext null
                }

                val scopeString = "oauth2:${OAUTH_SCOPES.joinToString(" ")}"

                if (forceRefresh) {
                    val currentToken = _userState.value.accessToken
                    if (currentToken.isNotBlank()) {
                        try {
                            GoogleAuthUtil.clearToken(context, currentToken)
                        } catch (_: Exception) {}
                    }
                }

                val token = GoogleAuthUtil.getToken(context, androidAccount, scopeString)
                if (!token.isNullOrBlank()) {
                    updateAccessToken(token)
                }
                token
            } catch (e: Exception) {
                android.util.Log.e("GoogleAuthManager", "Token acquisition error: ${e.message}", e)
                null
            }
        }

    /**
     * Uses [GoogleAuthUtil.getToken] to fetch a short-lived OAuth 2.0 access token
     * for the Sheets and Drive scopes. Returns an empty string on failure so callers
     * can surface a sync error rather than crashing at auth time.
     */
    private fun acquireAccessToken(account: GoogleSignInAccount): String {
        val androidAccount = account.account ?: return ""
        return try {
            val scopeString = "oauth2:${OAUTH_SCOPES.joinToString(" ")}"
            val token = GoogleAuthUtil.getToken(context, androidAccount, scopeString)
            token ?: ""
        } catch (e: Exception) {
            android.util.Log.e("GoogleAuthManager", "Initial acquireAccessToken error: ${e.message}", e)
            ""
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sign-Out
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun signOut() {
        try {
            firebaseAuth.signOut()
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            GoogleSignIn.getClient(context, gso).signOut().await()
        } catch (_: Exception) { /* Ignore sign-out errors */ }

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

    // ─────────────────────────────────────────────────────────────────────────
    // State Persistence
    // ─────────────────────────────────────────────────────────────────────────

    /** Persists user info to SharedPreferences and updates [userState]. */
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
        prefs.edit().putString("spreadsheet_id", spreadsheetId).apply()
        _userState.value = _userState.value.copy(
            spreadsheetId = spreadsheetId,
            spreadsheetUrl = spreadsheetUrl(spreadsheetId)
        )
    }

    fun updateAccessToken(token: String) {
        prefs.edit().putString("access_token", token).apply()
        _userState.value = _userState.value.copy(accessToken = token)
    }

    private fun loadUserState(): GoogleUserState {
        val isSignedIn = prefs.getBoolean("is_signed_in", false)
        val email = prefs.getString("user_email", "") ?: ""
        val displayName = prefs.getString("user_name", "") ?: ""
        val photoUrl = prefs.getString("user_photo", "") ?: ""
        val accessToken = prefs.getString("access_token", "") ?: ""
        val spreadsheetId = prefs.getString("spreadsheet_id", "") ?: ""
        return GoogleUserState(
            isSignedIn = isSignedIn,
            email = email,
            displayName = displayName,
            photoUrl = photoUrl,
            accessToken = accessToken,
            spreadsheetId = spreadsheetId,
            spreadsheetUrl = spreadsheetUrl(spreadsheetId)
        )
    }

    private fun spreadsheetUrl(id: String) =
        if (id.isNotBlank()) "https://docs.google.com/spreadsheets/d/$id" else ""
}
