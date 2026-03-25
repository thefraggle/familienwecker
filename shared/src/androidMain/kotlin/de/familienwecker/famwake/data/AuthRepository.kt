package de.familienwecker.famwake.data

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.AuthCredential
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.functions.functions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

class LoginFailedException : Exception()
class RegistrationFailedException : Exception()
class GoogleSignInFailedException : Exception()

class AuthRepository {
    private val auth = Firebase.auth

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun login(email: String, pass: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || pass.isEmpty()) {
            return@withContext Result.failure(Exception("EMAIL_OR_PASSWORD_EMPTY"))
        }
        try {
            val result = auth.signInWithEmailAndPassword(trimmedEmail, pass)
            val user = result.user
            if (user != null) Result.success(user) else Result.failure(Exception("LOGIN_FAILED"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, pass: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || pass.isEmpty()) {
            return@withContext Result.failure(Exception("EMAIL_OR_PASSWORD_EMPTY"))
        }
        try {
            val result = auth.createUserWithEmailAndPassword(trimmedEmail, pass)
            val user = result.user
            if (user != null) Result.success(user) else Result.failure(Exception("REGISTRATION_FAILED"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        try { auth.signOut() } catch (_: Exception) {}
    }

    suspend fun sendPasswordResetEmail(email: String, language: String = "de"): Result<Unit> {
        return try {
            val data = mapOf("email" to email.trim(), "language" to language)
            Firebase.functions("europe-west3")
                .httpsCallable("sendBrandedResetEmail")
                .invoke(data)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "sendBrandedResetEmail failed: ${e.message}", e)
            val msg = e.message?.uppercase() ?: ""
            when {
                msg.contains("NOT_FOUND") || msg.contains("USER_NOT_FOUND") ->
                    Result.failure(Exception("USER_NOT_FOUND"))
                msg.contains("INVALID_ARGUMENT") || msg.contains("INVALID_EMAIL") ->
                    Result.failure(Exception("INVALID_EMAIL"))
                msg.contains("RESOURCE_EXHAUSTED") || msg.contains("TOO_MANY") ->
                    Result.failure(Exception("TOO_MANY_REQUESTS"))
                msg.contains("PERMISSION_DENIED") || msg.contains("UNAUTHENTICATED") -> {
                    try {
                        auth.sendPasswordResetEmail(email.trim())
                        Result.success(Unit)
                    } catch (fallbackEx: Exception) {
                        Result.failure(Exception("RESET_FAILED"))
                    }
                }
                else -> Result.failure(Exception("RESET_FAILED"))
            }
        }
    }

    suspend fun signInWithGoogleCredential(credential: AuthCredential): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            val result = auth.signInWithCredential(credential)
            val user = result.user
            if (user != null) Result.success(user) else Result.failure(GoogleSignInFailedException())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendVerificationEmail(email: String, language: String = "de"): Result<Unit> {
        return try {
            val data = mapOf("email" to email.trim(), "language" to language)
            Firebase.functions("europe-west3")
                .httpsCallable("sendVerificationEmail")
                .invoke(data)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "sendVerificationEmail failed: ${e.message}", e)
            try {
                auth.currentUser?.sendEmailVerification()
                Result.success(Unit)
            } catch (fallbackEx: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun reloadUser(): Boolean {
        return try {
            auth.currentUser?.reload()
            auth.currentUser?.isEmailVerified ?: false
        } catch (e: Exception) {
            false
        }
    }
}
