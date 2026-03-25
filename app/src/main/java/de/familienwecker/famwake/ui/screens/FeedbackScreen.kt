package de.familienwecker.famwake.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.familienwecker.famwake.BuildConfig
import de.familienwecker.famwake.R
import de.familienwecker.famwake.ui.theme.LocalDarkTheme
import de.familienwecker.famwake.ui.viewmodel.FamilyViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    viewModel: FamilyViewModel,
    onNavigateBack: () -> Unit
) {
    val themePreference by viewModel.themePreference.collectAsStateWithLifecycle()
    val isDarkTheme = when (themePreference) {
        "dark" -> true
        "light" -> false
        else -> LocalDarkTheme.current
    }

    val categories = listOf(
        stringResource(R.string.feedback_category_bug),
        stringResource(R.string.feedback_category_feature),
        stringResource(R.string.feedback_category_praise),
        stringResource(R.string.feedback_category_other)
    )

    var categoryExpanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var message by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    
    val isLoading by viewModel.isSendingFeedback.collectAsStateWithLifecycle()
    val submitted by viewModel.feedbackSubmitted.collectAsStateWithLifecycle()
    val errorMessage by viewModel.feedbackError.collectAsStateWithLifecycle()

    val deviceModel = remember { "${Build.MANUFACTURER} ${Build.MODEL}" }
    val appVersion = BuildConfig.VERSION_NAME

    val backgroundGradient = androidx.compose.ui.graphics.Brush.verticalGradient(
        colors = if (isDarkTheme) {
            listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background)
        } else {
            listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), MaterialTheme.colorScheme.background)
        }
    )

    // Validierung
    val isEmailValid = email.isBlank() || android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val canSend = message.isNotBlank() && isEmailValid && !isLoading

    // Auto-Close nach Erfolg
    LaunchedEffect(submitted) {
        if (submitted) {
            delay(2500)
            viewModel.resetFeedbackState()
            onNavigateBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(backgroundGradient)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.feedback_title)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (submitted) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.feedback_success_title),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = stringResource(R.string.feedback_success_message),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    AnimatedVisibility(visible = errorMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.feedback_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Kategorie
                    Text(
                        text = stringResource(R.string.feedback_category_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = { selectedCategory = cat; categoryExpanded = false }
                                )
                            }
                        }
                    }

                    // Nachricht
                    Text(
                        text = stringResource(R.string.feedback_message_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        placeholder = { Text(stringResource(R.string.feedback_message_placeholder)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        maxLines = 8,
                        minLines = 4
                    )

                    // E-Mail (optional)
                    Text(
                        text = stringResource(R.string.feedback_email_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        isError = !isEmailValid,
                        placeholder = { Text(stringResource(R.string.feedback_email_placeholder)) },
                        supportingText = if (!isEmailValid) {
                            { Text(stringResource(R.string.error_invalid_email)) }
                        } else null,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
                        )
                    )

                    // Hintergrund-Info
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.feedback_auto_info_title),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.feedback_auto_version, appVersion),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.feedback_auto_device, deviceModel),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading
                        ) {
                            Text(stringResource(R.string.feedback_cancel))
                        }
                        Button(
                            onClick = {
                                viewModel.sendFeedback(
                                    category = selectedCategory,
                                    message = message,
                                    email = email,
                                    appVersion = appVersion,
                                    device = deviceModel
                                )
                            },
                            enabled = canSend,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(stringResource(R.string.feedback_send))
                            }
                        }
                    }
                }
            }
        }
    }
}
