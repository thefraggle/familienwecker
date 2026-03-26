package de.familienwecker.famwake.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.stringResource
import de.familienwecker.famwake.R
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import de.familienwecker.famwake.ui.theme.LocalDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
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
import android.view.WindowManager
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun FamilySetupScreen(
    viewModel: FamilyViewModel,
    onSetupComplete: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    BackHandler {
        (context as? Activity)?.finish()
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
        if (pendingJoinCode != null && familyId == null && !isLoading) {
            isLoading = true
            viewModel.joinFamily(pendingJoinCode!!) { success ->
                isLoading = false
                viewModel.clearPendingJoinCode()
                if (success) onSetupComplete()
            }
        }
    }

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
                        TabRow(
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
                                        modifier = Modifier.fillMaxWidth()
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
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .bounceClick(createInteractionSource),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
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
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text(stringResource(R.string.setup_join_code_placeholder)) },
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Characters,
                                            autoCorrectEnabled = false
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
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .bounceClick(joinInteractionSource),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
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
                                text = errorMessage!!.asString(),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }


                Spacer(modifier = Modifier.height(32.dp))
                
                val logoutInteractionSource = remember { MutableInteractionSource() }
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
