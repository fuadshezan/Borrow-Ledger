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
         * Web Client ID (OAuth 2.0 client) from Firebase Console.
         * Used as the serverClientId for Google Sign-In and Firebase credential.
         */
        private const val WEB_CLIENT_ID =
            "30361113842-mhom26a8fusdvsrm1rkb3cvugbphmesf.apps.googleusercontent.com"

        /** Google API scopes required for Sheets + Drive file access. */
        private val OAUTH_SCOPES = listOf(
            "https://www.googleapis.com/auth/spreadsheets",
            "https://www.googleapis.com/auth/drive.file"
        )
    }

    private val prefs = context.getSharedPreferences("google_sync_prefs", Context.MODE_PRIVATE)
    private val firebaseAuth = FirebaseAuth.getInstance()

    private val _userState = MutableStateFlow(loadUserState())
    val userState: StateFlow<GoogleUserState> = _userState.asStateFlow()

    // ─────────────────────────────────────────────────────────────────────────
    // Sign-In
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds the Google Sign-In intent that launches the system account picker.
     * Requests the Google ID token (for Firebase auth) and OAuth scopes (for Sheets/Drive REST APIs).
     */
    fun buildSignInIntent(): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .requestProfile()
            .requestScopes(
                Scope(OAUTH_SCOPES[0]),  // spreadsheets
                Scope(OAUTH_SCOPES[1])   // drive.file
            )
            .build()
        return GoogleSignIn.getClient(context, gso).signInIntent
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
     * Uses [GoogleAuthUtil.getToken] to fetch a short-lived OAuth 2.0 access token
     * for the Sheets and Drive scopes. Returns an empty string on failure so callers
     * can surface a sync error rather than crashing at auth time.
     */
    private fun acquireAccessToken(account: GoogleSignInAccount): String {
        return try {
            val scopeString = "oauth2:${OAUTH_SCOPES.joinToString(" ")}"
            GoogleAuthUtil.getToken(context, account.account, scopeString)
        } catch (e: Exception) {
            // Token acquisition can fail if consent was not fully granted.
            // Return empty string; sync will report the error via its error state.
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
        val currentSpreadsheetId = prefs.getString("spreadsheet_id", "") ?: ""
        prefs.edit()
            .putBoolean("is_signed_in", true)
            .putString("user_email", email)
            .putString("user_name", displayName)
            .putString("user_photo", photoUrl)
            .putString("access_token", accessToken)
            .apply()

        _userState.value = GoogleUserState(
            isSignedIn = true,
            email = email,
            displayName = displayName,
            photoUrl = photoUrl,
            accessToken = accessToken,
            spreadsheetId = currentSpreadsheetId,
            spreadsheetUrl = spreadsheetUrl(currentSpreadsheetId)
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
