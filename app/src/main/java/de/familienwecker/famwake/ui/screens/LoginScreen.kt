package de.familienwecker.famwake.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import de.familienwecker.famwake.ui.theme.LocalDarkTheme
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import de.familienwecker.famwake.R
import de.familienwecker.famwake.ui.viewmodel.AuthViewModel
import de.familienwecker.famwake.ui.viewmodel.FamilyViewModel
import android.net.Uri
import android.content.Intent
import de.familienwecker.famwake.ui.util.UiText
import de.familienwecker.famwake.ui.components.bounceClick
import androidx.compose.ui.graphics.Color
import androidx.activity.compose.BackHandler
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.semantics.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    familyViewModel: FamilyViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isRegistering by remember { mutableStateOf(false) }

    BackHandler {
        (context as? android.app.Activity)?.finish()
    }

    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    LaunchedEffect(authState) {
        if (authState is AuthViewModel.AuthState.Authenticated) {
            onLoginSuccess()
        }
    }

    val themePreference by familyViewModel.themePreference.collectAsStateWithLifecycle()
    val isDarkTheme = when (themePreference) {
        "dark" -> true
        "light" -> false
        else -> LocalDarkTheme.current
    }

    val backgroundGradient = androidx.compose.ui.graphics.Brush.verticalGradient(
        colors = if (isDarkTheme) {
            listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background)
        } else {
            listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), MaterialTheme.colorScheme.background)
        }
    )

    Box(modifier = Modifier.fillMaxSize().background(backgroundGradient)) {
        Column(modifier = Modifier.fillMaxSize()) {
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics {
                                    set(SemanticsPropertyKey<List<AutofillType>>("AutofillHints"), listOf(AutofillType.EmailAddress))
                                }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(stringResource(R.string.password_label)) },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                val image = if (passwordVisible)
                                    Icons.Filled.Visibility
                                else Icons.Filled.VisibilityOff

                                val description = if (passwordVisible) "Hide password" else "Show password"

                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(imageVector = image, contentDescription = description)
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics {
                                    set(SemanticsPropertyKey<List<AutofillType>>("AutofillHints"), listOf(AutofillType.Password))
                                }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        if (authState is AuthViewModel.AuthState.Loading) {
                            CircularProgressIndicator()
                        } else if (authState is AuthViewModel.AuthState.AwaitingEmailVerification) {
                            val userEmail = remember { authViewModel.currentUserEmail ?: email }

                            Text(
                                text = stringResource(R.string.login_verify_email_title),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.login_verify_email_text, userEmail),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { authViewModel.checkEmailVerified() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.login_verify_email_confirm))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { authViewModel.resendVerificationEmail() }) {
                                Text(stringResource(R.string.login_verify_email_resend))
                            }
                            TextButton(onClick = {
                                authViewModel.logout()
                                isRegistering = true
                            }) {
                                Text(
                                    stringResource(R.string.cancel_button),
                                    color = MaterialTheme.colorScheme.error
                                )
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

                            if (isRegistering) {
                                Spacer(modifier = Modifier.height(16.dp))
                                val termsOfUse = stringResource(R.string.registration_terms_of_use)
                                val privacyPolicy = stringResource(R.string.registration_privacy_policy)
                                val disclaimer = stringResource(R.string.registration_disclaimer, termsOfUse, privacyPolicy)
                                
                                val annotatedString = buildAnnotatedString {
                                    val termsStart = disclaimer.indexOf(termsOfUse)
                                    val privacyStart = disclaimer.indexOf(privacyPolicy)
                                    
                                    append(disclaimer)
                                    
                                    if (termsStart != -1) {
                                        addStyle(
                                            style = SpanStyle(
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                            ),
                                            start = termsStart,
                                            end = termsStart + termsOfUse.length
                                        )
                                        addStringAnnotation(
                                            tag = "URL",
                                            annotation = stringResource(R.string.settings_terms_of_use_url),
                                            start = termsStart,
                                            end = termsStart + termsOfUse.length
                                        )
                                    }
                                    
                                    if (privacyStart != -1) {
                                        addStyle(
                                            style = SpanStyle(
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                            ),
                                            start = privacyStart,
                                            end = privacyStart + privacyPolicy.length
                                        )
                                        addStringAnnotation(
                                            tag = "URL",
                                            annotation = stringResource(R.string.settings_privacy_policy_url),
                                            start = privacyStart,
                                            end = privacyStart + privacyPolicy.length
                                        )
                                    }
                                }
                                
                                androidx.compose.foundation.text.ClickableText(
                                    text = annotatedString,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    onClick = { offset ->
                                        annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                            .firstOrNull()?.let { annotation ->
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                                                context.startActivity(intent)
                                            }
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp)
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
                                TextButton(onClick = { authViewModel.resetPassword(email) }) {
                                    Text(stringResource(R.string.login_forgot_password))
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Google Sign-In – der Flow läuft vollständig im ViewModel
                            val googleInteractionSource = remember { MutableInteractionSource() }
                            OutlinedButton(
                                onClick = { authViewModel.signInWithGoogle(context) },
                                modifier = Modifier.fillMaxWidth().bounceClick(googleInteractionSource),
                                interactionSource = googleInteractionSource
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_google),
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
                            val errorUiText = (authState as AuthViewModel.AuthState.Error).message

                            val isEmailNotVerified = errorUiText is UiText.DynamicString && errorUiText.value == "EMAIL_NOT_VERIFIED"

                            val displayMsg = if (isEmailNotVerified) {
                                stringResource(R.string.login_verify_email_not_verified)
                            } else errorUiText.asString()
                            Text(
                                text = displayMsg,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (isEmailNotVerified) {
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = { authViewModel.resendVerificationEmail() }) {
                                    Text(stringResource(R.string.login_verify_email_resend))
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
