package com.example.paper_guru.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paper_guru.model.ALL_BOARDS
import com.example.paper_guru.model.ALL_CLASSES
import com.example.paper_guru.model.ALL_SUBJECTS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPortalDialog(
    isAdminMode: Boolean,
    adminPinInput: String,
    adminErrorMessage: String?,
    onUpdatePin: (String) -> Unit,
    onVerifyPin: () -> Unit,
    onExitAdminMode: () -> Unit,
    // GitHub sync
    isGitHubSyncing: Boolean,
    gitHubRepoInput: String,
    gitHubSyncMessage: String?,
    onUpdateRepoInput: (String) -> Unit,
    onSyncGitHub: () -> Unit,
    // Add paper
    onAddPaper: (classId: String, subjectName: String, year: String, board: String, url: String) -> Unit,
    onOpenFullAdminPortal: () -> Unit = {},
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    // Add paper form states
    var selectedClassId by remember { mutableStateOf("9th") }
    var selectedSubjectName by remember { mutableStateOf("Urdu") }
    var selectedYear by remember { mutableStateOf("2024") }
    var selectedBoard by remember { mutableStateOf("LHR") }
    var directPdfUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (!isAdminMode) "Admin / Teacher Login" else "Admin Dashboard",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "Manage & Upload Past Papers",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isAdminMode) {
                    Text(
                        text = "یہ سیکشن صرف اساتذہ اور ایڈمن کے لیے ہے جہاں سے نئے پیپرز کلاؤڈ یا گٹ ہب سے سنک کیے جا سکتے ہیں۔",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = adminPinInput,
                        onValueChange = onUpdatePin,
                        label = { Text("Enter Admin PIN (Default: 1234)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_pin_input")
                    )

                    if (adminErrorMessage != null) {
                        Text(
                            text = adminErrorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = onVerifyPin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_login_button")
                    ) {
                        Text("Verify & Enter Admin Mode")
                    }
                } else {
                    Button(
                        onClick = {
                            onDismiss()
                            onOpenFullAdminPortal()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300), contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Full Admin Portal (طلباء، پرچے اور الرٹس)")
                    }

                    // Admin Tabs
                    SecondaryTabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("GitHub Sync", fontSize = 12.sp) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Add Paper", fontSize = 12.sp) }
                        )
                    }

                    if (selectedTab == 0) {
                        // GitHub Releases Sync Tab
                        Text(
                            text = "Sync past paper PDFs uploaded to GitHub releases assets.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = gitHubRepoInput,
                            onValueChange = onUpdateRepoInput,
                            label = { Text("GitHub Repository (owner/repo)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (gitHubSyncMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = gitHubSyncMessage,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Button(
                            onClick = onSyncGitHub,
                            enabled = !isGitHubSyncing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_sync_github_button")
                        ) {
                            if (isGitHubSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Syncing...")
                            } else {
                                Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync All Papers from GitHub")
                            }
                        }
                    } else {
                        // Add Single Paper Tab
                        Text(
                            text = "Add single paper with direct PDF URL (Firebase / Cloud):",
                            fontSize = 12.sp
                        )

                        // Class Dropdown or Row
                        Text("Class:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ALL_CLASSES.forEach { c ->
                                FilterChip(
                                    selected = selectedClassId == c.id,
                                    onClick = { selectedClassId = c.id },
                                    label = { Text(c.id, fontSize = 11.sp) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = selectedSubjectName,
                            onValueChange = { selectedSubjectName = it },
                            label = { Text("Subject (e.g. Urdu, Math)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = selectedYear,
                                onValueChange = { selectedYear = it },
                                label = { Text("Year (2024)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = selectedBoard,
                                onValueChange = { selectedBoard = it },
                                label = { Text("Board (LHR)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = directPdfUrl,
                            onValueChange = { directPdfUrl = it },
                            label = { Text("PDF Direct URL") },
                            placeholder = { Text("https://.../paper.pdf") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (directPdfUrl.isNotBlank()) {
                                    onAddPaper(selectedClassId, selectedSubjectName, selectedYear, selectedBoard, directPdfUrl)
                                    directPdfUrl = ""
                                }
                            },
                            enabled = directPdfUrl.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Paper")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onExitAdminMode,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Exit Admin Mode (Back to Student Portal)")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
