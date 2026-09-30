package com.example.paper_guru.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paper_guru.model.ALL_BOARDS
import com.example.paper_guru.model.ALL_CLASSES
import com.example.paper_guru.model.StudentUser

@Composable
fun AuthAccountDialog(
    currentStudent: StudentUser?,
    isPaidMember: Boolean,
    onSaveProfile: (fullName: String, age: Int, email: String, telephone: String, cnic: String?, classId: String, board: String) -> Unit,
    onSubmitFeePayment: (amount: Int, trxRef: String) -> Unit,
    onSignOut: () -> Unit,
    onOpenPaywall: () -> Unit,
    onOpenAdminLogin: () -> Unit,
    onDismiss: () -> Unit
) {
    var isEditingProfile by remember { mutableStateOf(currentStudent == null) }
    var showFeePaymentDialog by remember { mutableStateOf(false) }

    // Student fields
    var fullNameInput by remember { mutableStateOf(currentStudent?.fullName ?: "طالب علم (Student)") }
    var ageInput by remember { mutableStateOf(currentStudent?.age?.toString() ?: "16") }
    var emailInput by remember { mutableStateOf(currentStudent?.email ?: "student@paperguru.edu.pk") }
    var telephoneInput by remember { mutableStateOf(currentStudent?.telephone ?: "0300-1234567") }
    var cnicInput by remember { mutableStateOf(currentStudent?.cnic ?: "") }
    var selectedClass by remember { mutableStateOf(currentStudent?.selectedClass ?: "10th") }
    var selectedBoard by remember { mutableStateOf(currentStudent?.selectedBoard ?: "LHR") }

    // Fee payment inputs
    var feeAmountInput by remember { mutableStateOf("500") }
    var trxRefInput by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("EasyPaisa") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "طالب علم پروفائل (Student Profile)",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Paper Guru • Matric & Inter Portal",
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
                if (currentStudent?.isBlocked == true) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFEE2E2),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Block, contentDescription = null, tint = Color(0xFFDC2626))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "اکاؤنٹ بلاک ہے! ایڈمنسٹریٹر سے رابطہ کریں۔",
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                if (!isEditingProfile && currentStudent != null) {
                    // Profile Overview Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentStudent.fullName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                TextButton(onClick = { isEditingProfile = true }) {
                                    Text("Edit (تبدیل کریں)", fontSize = 12.sp)
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            ProfileFieldRow(label = "عمر (Age):", value = "${currentStudent.age} Years")
                            ProfileFieldRow(label = "ای میل (Email):", value = currentStudent.email)
                            ProfileFieldRow(label = "ٹیلی فون (Phone):", value = currentStudent.telephone)
                            ProfileFieldRow(
                                label = "شناختی کارڈ (CNIC):",
                                value = currentStudent.cnic ?: "اختیاری (Not Provided)"
                            )
                            ProfileFieldRow(label = "کلاس و بورڈ:", value = "${currentStudent.selectedClass} Class • ${currentStudent.selectedBoard}")

                            Spacer(modifier = Modifier.height(4.dp))

                            // Subscription Status Row
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (currentStudent.hasAdvanceSubscription) Color(0xFFFEF3C7) else Color(0xFFF1F5F9),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (currentStudent.hasAdvanceSubscription) Icons.Default.WorkspacePremium else Icons.Default.Info,
                                        contentDescription = null,
                                        tint = if (currentStudent.hasAdvanceSubscription) Color(0xFFD97706) else Color(0xFF64748B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (currentStudent.hasAdvanceSubscription) "Advance VIP Subscription Active 👑" else "Free Student Tier",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (currentStudent.hasAdvanceSubscription) Color(0xFFB45309) else Color(0xFF334155)
                                        )
                                        if (currentStudent.feePaymentStatus == "PENDING_VERIFICATION") {
                                            Text(
                                                text = "فیس کی تصدیق زیر التواء ہے (Pending Admin Approval)",
                                                fontSize = 10.sp,
                                                color = Color(0xFFD97706)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Pay Fee / Request Advance Subscription Button
                    if (!currentStudent.hasAdvanceSubscription) {
                        Button(
                            onClick = { showFeePaymentDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF16A34A)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pay Subscription Fee (ایڈوانس سبسکرپشن فیس ادا کریں)", fontSize = 12.sp)
                        }
                    }

                } else {
                    // Profile Edit / Registration Form
                    Text(
                        text = "طالب علم کی معلومات درج کریں:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    OutlinedTextField(
                        value = fullNameInput,
                        onValueChange = { fullNameInput = it },
                        label = { Text("طالب علم کا نام (Full Name) *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = ageInput,
                            onValueChange = { ageInput = it },
                            label = { Text("عمر (Age) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = selectedClass,
                            onValueChange = { selectedClass = it },
                            label = { Text("کلاس (Class)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("ای میل (Email) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = telephoneInput,
                        onValueChange = { telephoneInput = it },
                        label = { Text("ٹیلی فون / موبائل نمبر (Telephone) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        placeholder = { Text("0300-1234567") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = cnicInput,
                        onValueChange = { cnicInput = it },
                        label = { Text("شناختی کارڈ / ب فارم (CNIC - اختیاری / Optional)") },
                        placeholder = { Text("35201-1234567-1") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Board Selector Row
                    Text("بورڈ منتخب کریں (Select Board):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("LHR", "RWP", "FSD", "FBISE").forEach { b ->
                            FilterChip(
                                selected = selectedBoard == b,
                                onClick = { selectedBoard = b },
                                label = { Text(b, fontSize = 11.sp) }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val ageInt = ageInput.toIntOrNull() ?: 16
                            onSaveProfile(
                                fullNameInput.trim(),
                                ageInt,
                                emailInput.trim(),
                                telephoneInput.trim(),
                                cnicInput.trim().ifEmpty { null },
                                selectedClass,
                                selectedBoard
                            )
                            isEditingProfile = false
                        },
                        enabled = fullNameInput.isNotBlank() && emailInput.isNotBlank() && telephoneInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Profile (معلومات محفوظ کریں)")
                    }

                    if (currentStudent != null) {
                        TextButton(
                            onClick = { isEditingProfile = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel")
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Discrete link for Admin Login
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenAdminLogin()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.outline
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "🔒 Secure Admin Access (محفوظ ایڈمن لاگ ان)",
                        fontSize = 12.sp
                    )
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

    // Pay Subscription Fee Dialog
    if (showFeePaymentDialog) {
        AlertDialog(
            onDismissRequest = { showFeePaymentDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = null,
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "سبسکرپشن فیس ادائیگی (Subscription Fee)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFDCFCE7),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "ایڈمن بینک / ایزی پیسہ اکاؤنٹ:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF15803D)
                            )
                            Text(text = "EasyPaisa / JazzCash: 0300-9876543", fontSize = 12.sp, color = Color(0xFF15803D))
                            Text(text = "عنوان: Paper Guru Admin Services", fontSize = 11.sp, color = Color(0xFF15803D))
                            Text(text = "فیس: Rs. 500 (سالانہ ایڈوانس سبسکرپشن)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                        }
                    }

                    Text("ادائیگی کا طریقہ:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("EasyPaisa", "JazzCash", "Bank Transfer").forEach { m ->
                            FilterChip(
                                selected = paymentMethod == m,
                                onClick = { paymentMethod = m },
                                label = { Text(m, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = feeAmountInput,
                        onValueChange = { feeAmountInput = it },
                        label = { Text("ادا شدہ رقم (Amount Paid Rs.)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = trxRefInput,
                        onValueChange = { trxRefInput = it },
                        label = { Text("رسید نمبر / Trx ID / TID *") },
                        placeholder = { Text("e.g. EP-98341029") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "نوٹ: فیس جمع کروانے کے بعد رسید کا Transaction ID درج کریں۔ ایڈمن کو فوری نوٹیفکیشن جائے گا اور آپ کا اکاؤنٹ ایڈوانس VIP میں اپگریڈ ہو جائے گا۔",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = feeAmountInput.toIntOrNull() ?: 500
                        onSubmitFeePayment(amount, trxRefInput.trim())
                        showFeePaymentDialog = false
                    },
                    enabled = trxRefInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Text("Submit Fee Proof (فیس جمع کرا دی)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFeePaymentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProfileFieldRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
