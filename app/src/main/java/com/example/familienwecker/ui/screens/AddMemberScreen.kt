package com.example.familienwecker.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.familienwecker.ui.viewmodel.FamilyViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import androidx.compose.ui.res.stringResource
import com.example.familienwecker.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemberScreen(
    viewModel: FamilyViewModel,
    memberId: String? = null,
    onNavigateBack: () -> Unit
) {
    val members by viewModel.members.collectAsState()
    val memberToEdit = remember(memberId, members) { members.find { it.id == memberId } }

    var name by remember(memberToEdit) { mutableStateOf(memberToEdit?.name ?: "") }
    var earliestWakeUp by remember(memberToEdit) { mutableStateOf(memberToEdit?.earliestWakeUp ?: LocalTime.of(6, 0)) }
    var latestWakeUp by remember(memberToEdit) { mutableStateOf(memberToEdit?.latestWakeUp ?: LocalTime.of(7, 30)) }
    var bathroomDuration by remember(memberToEdit) { mutableStateOf(memberToEdit?.bathroomDurationMinutes?.toString() ?: "20") }
    var wantsBreakfast by remember(memberToEdit) { mutableStateOf(memberToEdit?.wantsBreakfast ?: true) }
    var leaveHomeTime by remember(memberToEdit) { mutableStateOf(memberToEdit?.leaveHomeTime ?: LocalTime.of(8, 0)) }

    val formatter = DateTimeFormatter.ofPattern("HH:mm")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (memberId == null) stringResource(R.string.add_member_title_add) else stringResource(R.string.add_member_title_edit)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_desc))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) 
                        MaterialTheme.colorScheme.surface 
                    else 
                        MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            val unknownStr = stringResource(R.string.add_member_unknown)
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClick = {
                    val memberToSave = com.example.familienwecker.model.FamilyMember(
                        id = memberId ?: java.util.UUID.randomUUID().toString(),
                        name = name.ifEmpty { unknownStr },
                        earliestWakeUp = earliestWakeUp,
                        latestWakeUp = latestWakeUp,
                        bathroomDurationMinutes = bathroomDuration.toLongOrNull() ?: 20L,
                        wantsBreakfast = wantsBreakfast,
                        leaveHomeTime = leaveHomeTime
                    )
                    viewModel.addOrUpdateMember(memberToSave)
                    onNavigateBack()
                }
            ) {
                Text(stringResource(R.string.save_button))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.add_member_name_label)) },
                modifier = Modifier.fillMaxWidth()
            )

            TimePickerRow(stringResource(R.string.add_member_earliest_wake), earliestWakeUp) { earliestWakeUp = it }
            TimePickerRow(stringResource(R.string.add_member_latest_wake), latestWakeUp) { latestWakeUp = it }
            
            OutlinedTextField(
                value = bathroomDuration,
                onValueChange = { bathroomDuration = it },
                label = { Text(stringResource(R.string.add_member_bathroom_duration)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = wantsBreakfast,
                    onCheckedChange = { wantsBreakfast = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.add_member_wants_breakfast))
            }

            TimePickerRow(stringResource(R.string.add_member_leave_home), leaveHomeTime ?: LocalTime.of(8,0)) { 
                leaveHomeTime = it 
            }
        }
    }
}

@Composable
fun TimePickerRow(label: String, time: LocalTime, onTimeSelected: (LocalTime) -> Unit) {
    val context = LocalContext.current
    val formatter = DateTimeFormatter.ofPattern("HH:mm")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                TimePickerDialog(
                    context,
                    { _, hour, minute -> onTimeSelected(LocalTime.of(hour, minute)) },
                    time.hour,
                    time.minute,
                    true
                ).show()
            }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(time.format(formatter), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}
