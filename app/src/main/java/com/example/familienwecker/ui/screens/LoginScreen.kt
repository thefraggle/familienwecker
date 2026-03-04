package com.example.familienwecker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.familienwecker.R
import com.example.familienwecker.ui.viewmodel.AuthViewModel
import com.example.familienwecker.ui.viewmodel.FamilyViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import com.example.familienwecker.ui.components.bounceClick
import androidx.compose.ui.graphics.Color
import java.security.MessageDigest
import java.util.UUID

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    familyViewModel: FamilyViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegistering by remember { mutableStateOf(false) }

    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    // Automatische Weiterleitung, wenn der User erfolgreich eingeloggt ist
    LaunchedEffect(authState) {
        if (authState is AuthViewModel.AuthState.Authenticated) {
            onLoginSuccess()
        }
        // Bei EMAIL_NOT_VERIFIED zurück in AwaitingEmailVerification (State bleibt Error -> wird im UI angezeigt)
    }

    val themePreference by familyViewModel.themePreference.collectAsStateWithLifecycle()
    val isDarkTheme = when (themePreference) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    
    val backgroundGradient = androidx.compose.ui.graphics.Brush.verticalGradient(
        colors = if (isDarkTheme) {
            listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background)
        } else {
            listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), MaterialTheme.colorScheme.background)
        }
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = {
                    Text(
                        buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                                append("FamWake")
                            }
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                                append(" " + stringResource(R.string.app_name_short))
                            }
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surface else Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(backgroundGradient)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) 
                                         else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.email_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.password_label)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (authState is AuthViewModel.AuthState.Loading) {
                CircularProgressIndicator()
            } else if (authState is AuthViewModel.AuthState.AwaitingEmailVerification) {
                // --- Double Opt-In: Warte auf E-Mail-Bestätigung ---
                val isDe = java.util.Locale.getDefault().language == "de"
                val userEmail = remember { authViewModel.currentUserEmail ?: email }

                Text(
                    text = if (isDe) "✉️ E-Mail bestätigen" else "✉️ Confirm your email",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isDe)
                        "Wir haben eine Bestätigungs-Mail an\n$userEmail\ngesendet. Bitte klicke auf den Link in der Mail, um deinen Account zu aktivieren."
                    else
                        "We sent a confirmation email to\n$userEmail\nPlease click the link in the email to activate your account.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { authViewModel.checkEmailVerified() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isDe) "Ich habe bestätigt ✓" else "I have confirmed ✓")
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { authViewModel.resendVerificationEmail() }) {
                    Text(if (isDe) "E-Mail erneut senden" else "Resend email")
                }
                TextButton(onClick = {
                    authViewModel.logout()
                    isRegistering = true
                }) {
                    Text(if (isDe) "Abbrechen" else "Cancel",
                        color = MaterialTheme.colorScheme.error)
                }
            } else {
                val loginInteractionSource = remember { MutableInteractionSource() }

                Button(
                    onClick = {
                        if (isRegistering) {
                            authViewModel.register(email, password)
                        } else {
                            authViewModel.login(email, password)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .bounceClick(loginInteractionSource),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    interactionSource = loginInteractionSource,
                    enabled = email.isNotBlank() && password.isNotBlank()
                ) {
                    Text(
                        text = if (isRegistering) stringResource(R.string.register_button) else stringResource(R.string.login_button),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = { isRegistering = !isRegistering }) {
                    Text(
                        if (isRegistering) stringResource(R.string.already_have_account)
                        else stringResource(R.string.no_account)
                    )
                }

                if (!isRegistering) {
                    TextButton(onClick = {
                        authViewModel.resetPassword(email)
                    }) {
                        Text(stringResource(R.string.login_forgot_password))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                    val googleInteractionSource = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val credentialManager = CredentialManager.create(context)
                                    
                                    val rawNonce = UUID.randomUUID().toString()
                                    val bytes = rawNonce.toByteArray()
                                    val md = MessageDigest.getInstance("SHA-256")
                                    val digest = md.digest(bytes)
                                    val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

                                    val googleIdOption = GetGoogleIdOption.Builder()
                                        .setFilterByAuthorizedAccounts(false)
                                        .setServerClientId(context.getString(R.string.default_web_client_id))
                                        .setNonce(hashedNonce)
                                        .setAutoSelectEnabled(true)
                                        .build()

                                    val request = GetCredentialRequest.Builder()
                                        .addCredentialOption(googleIdOption)
                                        .build()

                                    val result = credentialManager.getCredential(context, request)
                                    val credential = result.credential
                                    
                                    if (credential is androidx.credentials.CustomCredential &&
                                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                    ) {
                                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                                        authViewModel.signInWithGoogle(firebaseCredential)
                                    }
                                } catch (_: NoCredentialException) {
                                    authViewModel.setError(context.getString(R.string.login_google_error_no_account))
                                } catch (e: GetCredentialException) {
                                    authViewModel.setError("Google Login failed: ${e.message}")
                                } catch (e: Exception) {
                                    authViewModel.setError("Unerwarteter Fehler: ${e.message}")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().bounceClick(googleInteractionSource),
                        interactionSource = googleInteractionSource
                    ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_google),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.login_with_google))
                }
            }

            if (authState is AuthViewModel.AuthState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                val isDe = java.util.Locale.getDefault().language == "de"
                val errMsg = (authState as AuthViewModel.AuthState.Error).message
                val displayMsg = if (errMsg == "EMAIL_NOT_VERIFIED") {
                    if (isDe) "Die E-Mail-Adresse wurde noch nicht bestätigt. Bitte prüfe dein Postfach."
                    else "Email address not yet confirmed. Please check your inbox."
                } else errMsg
                Text(
                    text = displayMsg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                // Nach EMAIL_NOT_VERIFIED zurück zum AwaitingEmailVerification Screen
                if (errMsg == "EMAIL_NOT_VERIFIED") {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { authViewModel.resendVerificationEmail() }) {
                        Text(if (isDe) "E-Mail erneut senden" else "Resend email")
                    }
                }
            }

            if (authState is AuthViewModel.AuthState.PasswordResetSuccess) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.login_password_reset_sent),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
                    }
                }
            }
        }
    }
}
}
