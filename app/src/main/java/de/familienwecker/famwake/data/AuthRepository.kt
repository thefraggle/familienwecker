package de.familienwecker.famwake.data

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LoginFailedException : Exception()
class RegistrationFailedException : Exception()
class GoogleSignInFailedException : Exception()

class AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun login(email: String, pass: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            val result = auth.signInWithEmailAndPassword(email.trim(), pass).await()
            val user = result.user
            if (user != null) Result.success(user) else Result.failure(LoginFailedException())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, pass: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            val result = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val user = result.user
            if (user != null) Result.success(user) else Result.failure(RegistrationFailedException())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }

    private val functions: com.google.firebase.functions.FirebaseFunctions = 
        com.google.firebase.functions.FirebaseFunctions.getInstance("europe-west3")

    suspend fun sendPasswordResetEmail(email: String, language: String = "de"): Result<Unit> {
        return try {
            val data = hashMapOf(
                "email" to email.trim(),
                "language" to language
            )
            functions
                .getHttpsCallable("sendBrandedResetEmail")
                .call(data)
                .await()
            Result.success(Unit)
        } catch (e: com.google.firebase.functions.FirebaseFunctionsException) {
            when (e.code) {
                // Gen2 public function shouldn't return these, but keep as safety net
                com.google.firebase.functions.FirebaseFunctionsException.Code.PERMISSION_DENIED,
                com.google.firebase.functions.FirebaseFunctionsException.Code.UNAUTHENTICATED -> {
                    try {
                        auth.sendPasswordResetEmail(email.trim()).await()
                        Result.success(Unit)
                    } catch (fallbackEx: Exception) {
                        Result.failure(Exception("RESET_FAILED"))
                    }
                }
                com.google.firebase.functions.FirebaseFunctionsException.Code.INVALID_ARGUMENT ->
                    Result.failure(Exception("INVALID_EMAIL"))
                com.google.firebase.functions.FirebaseFunctionsException.Code.NOT_FOUND ->
                    Result.failure(Exception("USER_NOT_FOUND"))
                com.google.firebase.functions.FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED ->
                    Result.failure(Exception("TOO_MANY_REQUESTS"))
                else -> Result.failure(Exception("RESET_FAILED"))
            }
        } catch (e: Exception) {
            // Fallback: FirebaseFunctionsException may not be caught for v2 functions
            // on older SDK versions. Try to extract the error code from the raw message.
            val msg = e.message?.uppercase() ?: ""
            when {
                msg.contains("NOT_FOUND") || msg.contains("USER_NOT_FOUND") ->
                    Result.failure(Exception("USER_NOT_FOUND"))
                msg.contains("INVALID_ARGUMENT") || msg.contains("INVALID_EMAIL") ->
                    Result.failure(Exception("INVALID_EMAIL"))
                msg.contains("RESOURCE_EXHAUSTED") || msg.contains("TOO_MANY") ->
                    Result.failure(Exception("TOO_MANY_REQUESTS"))
                else -> Result.failure(Exception("RESET_FAILED"))
            }
        }
    }

    suspend fun signInWithGoogleCredential(credential: AuthCredential): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            val result = auth.signInWithCredential(credential).await()
            val user = result.user
            if (user != null) Result.success(user) else Result.failure(GoogleSignInFailedException())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendVerificationEmail(email: String, language: String = "de"): Result<Unit> {
        return try {
            val data = hashMapOf(
                "email" to email.trim(),
                "language" to language
            )
            functions
                .getHttpsCallable("sendVerificationEmail")
                .call(data)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                android.util.Log.e("AuthRepository", "Cloud function sendVerificationEmail failed, fallback to default", e)
            }
            try {
                auth.currentUser?.sendEmailVerification()?.await()
                Result.success(Unit)
            } catch (fallbackEx: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun reloadUser(): Boolean {
        return try {
            auth.currentUser?.reload()?.await()
            auth.currentUser?.isEmailVerified ?: false
        } catch (e: Exception) {
            false
        }
    }
}
