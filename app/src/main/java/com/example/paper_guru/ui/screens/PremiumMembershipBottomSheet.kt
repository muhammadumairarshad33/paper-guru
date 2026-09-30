package com.example.paper_guru.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumMembershipBottomSheet(
    triggerFeature: String,
    isPaidMember: Boolean,
    onActivatePro: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var promoCode by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = null,
                tint = Color(0xFFFFB300),
                modifier = Modifier.size(52.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Paper Guru PRO (طالب علم پریمیئم)",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "بورڈ امتحانات کے حل شدہ پرچے، ماڈل پیپرز اور ٹاپرز کے نوٹس تک لامحدود رسائی حاصل کریں۔",
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            val benefits = listOf(
                "تمام سالوں کے حل شدہ پیپرز (Solved Papers)",
                "بورڈ کے اہم گیس پیپرز (Board Guess Papers)",
                "آف لائن لامحدود ڈاؤنلوڈنگ (Offline Reading)",
                "ٹاپرز کی پریزنٹیشن گائیڈز (Presentation Tips)"
            )

            benefits.forEach { b ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(b, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = promoCode,
                onValueChange = { promoCode = it },
                label = { Text("پرومو کوڈ درج کریں (e.g. STUDENT2024)") },
                placeholder = { Text("STUDENT2024") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = { onActivatePro(promoCode) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
            ) {
                Text("مفت پریمیئم پلان فعال کریں (Activate PRO)")
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onDismiss) {
                Text("بعد میں دیکھیں (Dismiss)")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
