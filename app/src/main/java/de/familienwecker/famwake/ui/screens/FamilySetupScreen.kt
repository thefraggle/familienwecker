package de.familienwecker.famwake.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.stringResource
import de.familienwecker.famwake.R
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import de.familienwecker.famwake.ui.theme.LocalDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import de.familienwecker.famwake.ui.viewmodel.FamilyViewModel
import de.familienwecker.famwake.ui.viewmodel.*
import de.familienwecker.famwake.ui.components.bounceClick
import androidx.activity.compose.BackHandler
import android.app.Activity
import de.familienwecker.famwake.util.findActivity
import android.view.WindowManager
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun FamilySetupScreen(
    viewModel: FamilyViewModel,
    onSetupComplete: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToLogin: () -> Unit = {},
    isAnonymous: Boolean = false
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    BackHandler {
        context.findActivity()?.finish()
    }


    var isCreateMode by remember { mutableStateOf(true) }
    var familyName by remember { mutableStateOf("") }
    var joinCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val themePreference by viewModel.themePreference.collectAsStateWithLifecycle()
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

    // Deep Link Auto-Join (falls noch keine Familie vorhanden)
    val pendingJoinCode by viewModel.pendingJoinCode.collectAsStateWithLifecycle()
    val familyId by viewModel.familyId.collectAsStateWithLifecycle()

    LaunchedEffect(pendingJoinCode, familyId) {
        val code = pendingJoinCode ?: return@LaunchedEffect
        if (familyId == null && !isLoading) {
            isLoading = true
            viewModel.joinFamily(code) { success ->
                isLoading = false
                viewModel.clearPendingJoinCode()
                if (success) onSetupComplete()
            }
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(backgroundGradient)
        // Tastatur schließen, wenn der User außerhalb eines Feldes tippt
        .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = {
                    Text(
                        buildAnnotatedString {
                            val appShortName = stringResource(R.string.app_name_short)
                            // M14: Brand-Prefix aus String-Resource statt hardcoded
                            val prefix = stringResource(R.string.app_name_brand_prefix)
                            val suffix = appShortName.removePrefix(prefix).trim()
                            withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                                append(prefix)
                            }
                            if (suffix.isNotEmpty()) {
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                                    append(" $suffix")
                                }
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
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // M18: Prominenter Hinweis für anonyme User – Registrierung empfehlen
                if (isAnonymous) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToLogin() },
                        shape = MaterialTheme.shapes.extraLarge,
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 2.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkTheme) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                             else MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.anonymous_warning_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.anonymous_warning_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceContainerHigh 
                                         else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PrimaryTabRow(
                            selectedTabIndex = if (isCreateMode) 0 else 1,
                            containerColor = Color.Transparent,
                            divider = {}
                        ) {
                            Tab(selected = isCreateMode, onClick = { isCreateMode = true }) {
                                Text(stringResource(R.string.setup_create_tab), modifier = Modifier.padding(16.dp))
                            }
                            Tab(selected = !isCreateMode, onClick = { isCreateMode = false }) {
                                Text(stringResource(R.string.setup_join_tab), modifier = Modifier.padding(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Crossfade(targetState = isCreateMode, label = "SetupMode") { mode ->
                            if (mode) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = familyName,
                                        onValueChange = { familyName = it },
                                        label = { Text(stringResource(R.string.setup_family_name)) },
                                        singleLine = true,
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(
                                            onDone = { focusManager.clearFocus() }
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    val createInteractionSource = remember { MutableInteractionSource() }
 
                                    Button(
                                        onClick = {
                                            isLoading = true
                                            viewModel.createFamily(familyName) { success ->
                                                isLoading = false
                                                if (success) onSetupComplete()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        ),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .bounceClick(createInteractionSource),
                                        interactionSource = createInteractionSource,
                                        enabled = familyName.isNotBlank() && !isLoading
                                    ) {
                                        Text(stringResource(R.string.setup_create_button), style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                            } else {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = joinCode,
                                        onValueChange = { input ->
                                            val sanitized = input.filter { it.isLetterOrDigit() }.uppercase()
                                            if (sanitized.length <= 6) {
                                                joinCode = sanitized
                                            }
                                        },
                                        label = { Text(stringResource(R.string.setup_join_code_label)) },
                                        singleLine = true,
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text(stringResource(R.string.setup_join_code_placeholder)) },
                                        keyboardOptions = KeyboardOptions(
                                            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Characters,
                                            autoCorrectEnabled = false,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = { focusManager.clearFocus() }
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    val joinInteractionSource = remember { MutableInteractionSource() }
 
                                    Button(
                                        onClick = {
                                            isLoading = true
                                            viewModel.joinFamily(joinCode) { success ->
                                                isLoading = false
                                                if (success) onSetupComplete()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        ),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .bounceClick(joinInteractionSource),
                                        interactionSource = joinInteractionSource,
                                        enabled = joinCode.length == 6 && !isLoading
                                    ) {
                                        Text(stringResource(R.string.setup_join_button), style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                            }
                        }

                        if (isLoading) {
                            Spacer(modifier = Modifier.height(16.dp))
                            CircularProgressIndicator()
                        }

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = errorMessage?.asString() ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }


                Spacer(modifier = Modifier.height(32.dp))
                
                val logoutInteractionSource = remember { MutableInteractionSource() }
                if (isAnonymous) {
                    TextButton(
                        onClick = onNavigateToLogin,
                        modifier = Modifier.fillMaxWidth().bounceClick(logoutInteractionSource),
                        interactionSource = logoutInteractionSource,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(stringResource(R.string.settings_anonymous_login_button))
                    }
                } else {
                    TextButton(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth().bounceClick(logoutInteractionSource),
                        interactionSource = logoutInteractionSource,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.settings_logout))
                    }
                }
            }
        }
    }
}
