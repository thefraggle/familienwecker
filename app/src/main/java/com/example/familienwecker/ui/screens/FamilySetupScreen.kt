package com.example.familienwecker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.stringResource
import com.example.familienwecker.R
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import com.example.familienwecker.ui.viewmodel.FamilyViewModel

@Composable
fun FamilySetupScreen(
    viewModel: FamilyViewModel,
    onSetupComplete: () -> Unit
) {
    var isCreateMode by remember { mutableStateOf(true) }
    var familyName by remember { mutableStateOf("") }
    var joinCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val errorMessage by viewModel.errorMessage.collectAsState()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.setup_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(32.dp))

            TabRow(selectedTabIndex = if (isCreateMode) 0 else 1) {
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
                        Button(
                            onClick = {
                                isLoading = true
                                viewModel.createFamily(familyName) { success ->
                                    isLoading = false
                                    if (success) onSetupComplete()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = familyName.isNotBlank() && !isLoading
                        ) {
                            Text(stringResource(R.string.setup_create_button))
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = joinCode,
                            onValueChange = { joinCode = it.uppercase() },
                            label = { Text(stringResource(R.string.setup_join_code_label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                isLoading = true
                                viewModel.joinFamily(joinCode) { success ->
                                    isLoading = false
                                    if (success) onSetupComplete()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = joinCode.length >= 5 && !isLoading
                        ) {
                            Text(stringResource(R.string.setup_join_button))
                        }
                    }
                }
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
