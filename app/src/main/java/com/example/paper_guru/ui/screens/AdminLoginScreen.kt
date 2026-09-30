package com.example.paper_guru.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLoginScreen(
    usernameInput: String,
    passwordInput: String,
    errorMessage: String?,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: (username: String, passcode: String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler {
        onBack()
    }

    var passwordVisible by remember { mutableStateOf(false) }
    var rememberDevice by remember { mutableStateOf(true) }

    // Distinct High-Security Dark Palette for Admin Portal
    val darkBackground = Color(0xFF0F172A)
    val cardSurface = Color(0xFF1E293B)
    val cardBorder = Color(0xFF334155)
    val accentGold = Color(0xFFFFB300)
    val accentBlue = Color(0xFF38BDF8)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B132B),
                        Color(0xFF1C2541),
                        Color(0xFF0F172A)
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Admin Access Gateway",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Return to Student Portal",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        TextButton(onClick = onBack) {
                            Text(
                                text = "Student Portal",
                                color = accentBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Security Shield Emblem
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF1E3A8A).copy(alpha = 0.6f),
                                        Color(0xFF0F172A).copy(alpha = 0.2f)
                                    )
                                )
                            )
                    )
                    Surface(
                        shape = CircleShape,
                        color = cardSurface,
                        border = androidx.compose.foundation.BorderStroke(2.dp, accentGold),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = accentGold,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }

                // Title & Subtitle
                Text(
                    text = "محفوظ ایڈمن گیٹ وے",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Paper Guru • Administrative Control Center",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = accentBlue,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "Restricted Area — Authorized Staff & Teachers Only",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp, bottom = 20.dp)
                )

                // Login Card Container
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Admin Credentials (لاگ ان معلومات)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        // Error Banner if invalid credentials
                        if (errorMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF7F1D1D).copy(alpha = 0.6f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFF87171),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = errorMessage,
                                        color = Color(0xFFFEE2E2),
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        // Username / Admin ID Field
                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = onUsernameChange,
                            label = { Text("Admin ID / Username", color = Color(0xFF94A3B8)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = accentGold
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = accentGold,
                                unfocusedBorderColor = cardBorder,
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_login_username")
                        )

                        // Security Passcode / Password Field
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = onPasswordChange,
                            label = { Text("Admin Passcode / PIN", color = Color(0xFF94A3B8)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = null,
                                    tint = accentGold
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Hide passcode" else "Show passcode",
                                        tint = Color(0xFF94A3B8)
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (usernameInput.isNotBlank() && passwordInput.isNotBlank()) {
                                        onLogin(usernameInput, passwordInput)
                                    }
                                }
                            ),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = accentGold,
                                unfocusedBorderColor = cardBorder,
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_login_password")
                        )

                        // Helper Autofill Chip for easy access & testing
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Default PIN: 1234",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                            SuggestionChip(
                                onClick = {
                                    onUsernameChange("admin")
                                    onPasswordChange("1234")
                                },
                                label = { Text("⚡ Autofill Demo PIN", fontSize = 11.sp, color = accentGold) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = Color(0xFF0F172A)
                                ),
                                border = SuggestionChipDefaults.suggestionChipBorder(
                                    borderColor = accentGold.copy(alpha = 0.5f),
                                    enabled = true
                                )
                            )
                        }

                        // Remember Device Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = rememberDevice,
                                onCheckedChange = { rememberDevice = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = accentGold,
                                    uncheckedColor = Color(0xFF64748B),
                                    checkmarkColor = Color.Black
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Remember administrative session on this device",
                                color = Color(0xFFCBD5E1),
                                fontSize = 12.sp
                            )
                        }

                        // Primary Sign In Button
                        Button(
                            onClick = { onLogin(usernameInput, passwordInput) },
                            enabled = usernameInput.isNotBlank() && passwordInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentGold,
                                contentColor = Color(0xFF0F172A)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("admin_login_submit_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Login,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Authorize & Enter Admin Portal",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Security & Privacy Notice
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.7f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "End-to-End Administrative Security",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "All actions on past papers and student permissions are cryptographically verified and logged.",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8),
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Return to Student Portal Link
                OutlinedButton(
                    onClick = onBack,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF94A3B8)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("← Return to Student Portal (واپس طالب علم پورٹل)")
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
