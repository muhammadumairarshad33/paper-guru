package com.example.paper_guru.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paper_guru.model.ALL_BOARDS
import com.example.paper_guru.model.ClassLevel
import com.example.paper_guru.model.SubjectItem
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperDetailScreen(
    classLevel: ClassLevel,
    subject: SubjectItem,
    year: String,
    board: String,
    isBookmarked: Boolean,
    isAvailable: Boolean,
    isPaidMember: Boolean,
    isDownloading: Boolean,
    downloadedFile: File?,
    onToggleBookmark: () -> Unit,
    onViewPaper: () -> Unit,
    onDownloadPaper: () -> Unit,
    onViewSolution: () -> Unit,
    onViewExamInspiration: () -> Unit,
    onOpenDownloaded: () -> Unit,
    onShareDownloaded: () -> Unit,
    snackBarMessage: String?,
    onDismissSnackBar: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val snackbarHostState = remember { SnackbarHostState() }
    val boardObj = ALL_BOARDS.firstOrNull { it.code.equals(board, ignoreCase = true) }

    LaunchedEffect(snackBarMessage) {
        snackBarMessage?.let {
            snackbarHostState.showSnackbar(it)
            onDismissSnackBar()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "پرچہ تفصیل (Past Paper)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("paper_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier.testTag("paper_detail_bookmark_button")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Paper Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = subject.color.copy(alpha = 0.15f),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = subject.icon,
                                    contentDescription = null,
                                    tint = subject.color,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${subject.name} ($year)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = subject.urduName,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Detail Items
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("کلاس (Class)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(classLevel.displayName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        Column {
                            Text("امتحانی بورڈ (Board)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(boardObj?.name ?: "$board Board", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        Column {
                            Text("سال (Year)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(year, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Primary Action: View Paper
            Button(
                onClick = onViewPaper,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("view_paper_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "View Past Paper (پرچہ پڑھیں)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            // Secondary Action: Download to Device
            OutlinedButton(
                onClick = onDownloadPaper,
                enabled = !isDownloading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("download_paper_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ڈاؤنلوڈ ہو رہا ہے...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (downloadedFile != null) "Re-download Paper" else "Download to Device (ڈاؤنلوڈ کریں)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }

            // Downloaded File Actions if present
            if (downloadedFile != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpenDownloaded,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open PDF", fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = onShareDownloaded,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Pro Features: Solutions & Inspiration
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFEF3C7),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "حل شدہ پرچہ اور نوٹس (Solved Paper & Notes)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF92400E)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onViewSolution,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                        ) {
                            Text("View Solution", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = onViewExamInspiration,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Exam Tips", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
