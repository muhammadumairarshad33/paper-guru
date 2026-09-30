package com.example.paper_guru.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paper_guru.model.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPortalScreen(
    selectedTab: AdminTab,
    onTabSelected: (AdminTab) -> Unit,
    // Paper upload
    dynamicUploadedPapers: List<PaperItem>,
    onUploadPaper: (classId: String, subjectName: String, year: String, board: String, url: String) -> Unit,
    onDeleteUploadedPaper: (String) -> Unit,
    // Student directory
    studentsList: List<StudentUser>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    filterStatus: String,
    onFilterStatusChange: (String) -> Unit,
    onBlockStudent: (studentId: String, isBlocked: Boolean) -> Unit,
    onGrantAdvanceSubscription: (studentId: String, plan: String, expiry: String) -> Unit,
    onRevokeAdvanceSubscription: (studentId: String) -> Unit,
    // Admin alerts
    adminNotifications: List<AdminNotification>,
    unreadCount: Int,
    onMarkNotificationRead: (String) -> Unit,
    onClearAllNotifications: () -> Unit,
    // Firebase Firestore Cloud Sync
    firestoreStatus: FirestoreSyncStatus = FirestoreSyncStatus.OFFLINE_PERSISTENT,
    isFirestoreSyncing: Boolean = false,
    firestoreSyncMessage: String? = null,
    onSyncFirestore: () -> Unit = {},
    // Interlinked Student Activity Tracking & Navigation
    trackedActivities: List<StudentActivityEvent> = emptyList(),
    activityFilterType: ActivityEventType? = null,
    onActivityFilterChange: (ActivityEventType?) -> Unit = {},
    onClearActivities: () -> Unit = {},
    onJumpToPaperFromTracking: (classId: String, subjectName: String, year: String, board: String) -> Unit = { _, _, _, _ -> },
    onSwitchStudentPersona: (StudentUser) -> Unit = {},
    onPublishAnnouncement: (title: String, message: String, targetClass: String, priority: String) -> Unit = { _, _, _, _ -> },
    onOpenStudentPortal: () -> Unit = {},
    // GitHub sync
    isGitHubSyncing: Boolean,
    gitHubRepoInput: String,
    gitHubSyncMessage: String?,
    onUpdateRepoInput: (String) -> Unit,
    onSyncGitHub: () -> Unit,
    // Navigation
    onExitAdmin: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler {
        onBack()
    }

    // Grant Subscription Dialog state
    var selectedStudentForVip by remember { mutableStateOf<StudentUser?>(null) }
    var selectedVipPlan by remember { mutableStateOf("PRO Annual VIP Pass") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFFB300),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Admin Portal (ایڈمن پورٹل)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "Past Papers & Student Tracking",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Student Portal"
                        )
                    }
                },
                actions = {
                    // Quick Switch to Student Portal
                    FilledTonalButton(
                        onClick = onOpenStudentPortal,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Student View", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Notification Bell with unread badge
                    IconButton(
                        onClick = { onTabSelected(AdminTab.NOTIFICATIONS) },
                        modifier = Modifier.testTag("admin_notif_bell")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge(
                                        containerColor = Color(0xFFDC2626),
                                        contentColor = Color.White
                                    ) {
                                        Text("$unreadCount")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Alerts"
                            )
                        }
                    }

                    TextButton(onClick = onExitAdmin) {
                        Text("Exit Admin", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tabs
            SecondaryTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                AdminTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (tab) {
                                        AdminTab.UPLOAD_PAPER -> Icons.Default.CloudUpload
                                        AdminTab.STUDENTS -> Icons.Default.People
                                        AdminTab.ACTIVITY_TRACKING -> Icons.Default.Timeline
                                        AdminTab.NOTIFICATIONS -> Icons.Default.NotificationsActive
                                        AdminTab.GITHUB_SYNC -> Icons.Default.Sync
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (tab == AdminTab.NOTIFICATIONS && unreadCount > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFDC2626))
                                    )
                                }
                            }
                        }
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                AdminTab.UPLOAD_PAPER -> {
                    UploadPaperSection(
                        dynamicUploadedPapers = dynamicUploadedPapers,
                        onUploadPaper = onUploadPaper,
                        onDeletePaper = onDeleteUploadedPaper
                    )
                }
                AdminTab.STUDENTS -> {
                    StudentsDirectorySection(
                        students = studentsList,
                        searchQuery = searchQuery,
                        onSearchQueryChange = onSearchQueryChange,
                        filterStatus = filterStatus,
                        onFilterStatusChange = onFilterStatusChange,
                        onBlockStudent = onBlockStudent,
                        onOpenVipModal = { student ->
                            selectedStudentForVip = student
                        },
                        onRevokeVip = onRevokeAdvanceSubscription,
                        onSwitchPersona = { student ->
                            onSwitchStudentPersona(student)
                            onOpenStudentPortal()
                        },
                        onViewActivities = {
                            onTabSelected(AdminTab.ACTIVITY_TRACKING)
                        }
                    )
                }
                AdminTab.ACTIVITY_TRACKING -> {
                    AdminActivityTrackingSection(
                        activities = trackedActivities,
                        selectedFilterType = activityFilterType,
                        onFilterTypeChange = onActivityFilterChange,
                        onClearActivities = onClearActivities,
                        onJumpToPaper = onJumpToPaperFromTracking,
                        onSwitchPersona = { student ->
                            onSwitchStudentPersona(student)
                            onOpenStudentPortal()
                        },
                        onPublishAnnouncement = onPublishAnnouncement,
                        onOpenStudentPortal = onOpenStudentPortal,
                        students = studentsList
                    )
                }
                AdminTab.NOTIFICATIONS -> {
                    AdminNotificationsSection(
                        notifications = adminNotifications,
                        onMarkRead = onMarkNotificationRead,
                        onClearAll = onClearAllNotifications,
                        onApproveStudentVip = { studentId ->
                            onGrantAdvanceSubscription(studentId, "Advance VIP Pass", "2025-06-30")
                        }
                    )
                }
                AdminTab.GITHUB_SYNC -> {
                    AdminCloudSyncSection(
                        firestoreStatus = firestoreStatus,
                        isFirestoreSyncing = isFirestoreSyncing,
                        firestoreSyncMessage = firestoreSyncMessage,
                        onSyncFirestore = onSyncFirestore,
                        studentsCount = studentsList.size,
                        papersCount = dynamicUploadedPapers.size,
                        notifsCount = adminNotifications.size,
                        isGitHubSyncing = isGitHubSyncing,
                        gitHubRepoInput = gitHubRepoInput,
                        gitHubSyncMessage = gitHubSyncMessage,
                        onUpdateRepoInput = onUpdateRepoInput,
                        onSyncGitHub = onSyncGitHub
                    )
                }
            }
        }
    }

    // Modal to Grant Advance Subscription
    if (selectedStudentForVip != null) {
        val student = selectedStudentForVip!!
        AlertDialog(
            onDismissRequest = { selectedStudentForVip = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Grant Advance Subscription (ایڈوانس سبسکرپشن دیں)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "طالب علم: ${student.fullName} (${student.email})",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "فون: ${student.telephone} | کلاس: ${student.selectedClass} | بورڈ: ${student.selectedBoard}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (student.feePaymentStatus == "PENDING_VERIFICATION") {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF3C7),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "💰 فیس کی تفصیلات:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    text = "رقم: Rs. ${student.feeAmount} | رسید ID: ${student.feeTransactionRef ?: "N/A"}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF92400E)
                                )
                            }
                        }
                    }

                    Text("سبسکرپشن پلان منتخب کریں:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    listOf(
                        "PRO Annual VIP Pass (1 سال مکمل رسائی)",
                        "6 Months Exam Booster VIP",
                        "Lifetime VIP All Subjects Pass"
                    ).forEach { plan ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedVipPlan = plan }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedVipPlan == plan,
                                onClick = { selectedVipPlan = plan }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = plan, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val expiry = if (selectedVipPlan.contains("Lifetime")) "Lifetime" else "2025-06-30"
                        onGrantAdvanceSubscription(student.id, selectedVipPlan, expiry)
                        selectedStudentForVip = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF16A34A)
                    )
                ) {
                    Text("Approve & Grant VIP")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedStudentForVip = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UploadPaperSection(
    dynamicUploadedPapers: List<PaperItem>,
    onUploadPaper: (classId: String, subjectName: String, year: String, board: String, url: String) -> Unit,
    onDeletePaper: (String) -> Unit
) {
    var selectedClassId by remember { mutableStateOf("10th") }
    var selectedSubjectName by remember { mutableStateOf("Physics") }
    var selectedYear by remember { mutableStateOf("2024") }
    var selectedBoard by remember { mutableStateOf("LHR") }
    var paperPdfUrl by remember { mutableStateOf("") }
    var uploadStatusMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Upload Past Paper (نیا پرچہ اپلوڈ کریں)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Text(
                        text = "ایڈمن یہاں سے کسی بھی کلاس، بورڈ اور مضمون کا پرچہ کلاؤڈ یا ڈائریکٹ لنک کے ذریعے شامل کر سکتا ہے۔",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Class Selector
                    Text("Class (کلاس منتخب کریں):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ALL_CLASSES.forEach { c ->
                            val isSuspended = SUSPENDED_CLASS_IDS.contains(c.id)
                            FilterChip(
                                selected = selectedClassId == c.id,
                                onClick = { selectedClassId = c.id },
                                label = { Text(if (isSuspended) "${c.id} (Withheld ⚠️)" else c.id, fontSize = 11.sp) }
                            )
                        }
                    }

                    if (SUSPENDED_CLASS_IDS.contains(selectedClassId)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF3C7),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$selectedClassId class past papers are temporarily withheld from the Student Portal due to paper rumors under investigation.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF92400E)
                                )
                            }
                        }
                    }

                    // Subject Selector Chips
                    Text("Subject (مضمون):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(ALL_SUBJECTS) { s ->
                            FilterChip(
                                selected = selectedSubjectName == s.name,
                                onClick = { selectedSubjectName = s.name },
                                label = { Text(s.name, fontSize = 12.sp) }
                            )
                        }
                    }

                    // Year and Board
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = selectedYear,
                            onValueChange = { selectedYear = it },
                            label = { Text("Year (سال)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = selectedBoard,
                            onValueChange = { selectedBoard = it },
                            label = { Text("Board (بورڈ e.g. LHR, FBISE)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // PDF Direct URL
                    OutlinedTextField(
                        value = paperPdfUrl,
                        onValueChange = {
                            paperPdfUrl = it
                            uploadStatusMessage = null
                        },
                        label = { Text("PDF Direct URL / Cloud Link") },
                        placeholder = { Text("https://example.com/papers/physics_10th_2024.pdf") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Link, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_paper_url_input")
                    )

                    // Quick Sample Link Helper
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                paperPdfUrl = "https://raw.githubusercontent.com/umairarshad33/paper_guru/main/papers/${selectedClassId}/${selectedSubjectName.lowercase()}_${selectedClassId}_${selectedBoard.lowercase()}_${selectedYear}.pdf"
                            }
                        ) {
                            Text("⚡ Use Standard GitHub URL format", fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = {
                            if (paperPdfUrl.isNotBlank()) {
                                onUploadPaper(
                                    selectedClassId,
                                    selectedSubjectName,
                                    selectedYear,
                                    selectedBoard.uppercase(),
                                    paperPdfUrl.trim()
                                )
                                uploadStatusMessage = "پرچہ کامیابی سے اپلوڈ ہو گیا: $selectedSubjectName ($selectedClassId) $selectedYear"
                                paperPdfUrl = ""
                            }
                        },
                        enabled = paperPdfUrl.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_upload_paper_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload & Publish Paper (پرچہ شائع کریں)")
                    }

                    if (uploadStatusMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFDCFCE7),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = uploadStatusMessage!!,
                                color = Color(0xFF15803D),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Uploaded Papers Library
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dynamic Uploaded Papers (${dynamicUploadedPapers.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Active in Student Portal",
                    fontSize = 11.sp,
                    color = Color(0xFF16A34A)
                )
            }
        }

        if (dynamicUploadedPapers.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ابھی تک کوئی نیا پرچہ ایڈمن کی طرف سے اپلوڈ نہیں ہوا۔ اوپر والے فارم سے نیا پرچہ شامل کریں۔",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(dynamicUploadedPapers) { paper ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${paper.subjectName} - ${paper.classFolderName}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Year ${paper.year} • Board: ${paper.board}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!paper.remoteUrl.isNullOrEmpty()) {
                                Text(
                                    text = paper.remoteUrl,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(onClick = { onDeletePaper(paper.uniqueKey) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentsDirectorySection(
    students: List<StudentUser>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    filterStatus: String,
    onFilterStatusChange: (String) -> Unit,
    onBlockStudent: (studentId: String, isBlocked: Boolean) -> Unit,
    onOpenVipModal: (StudentUser) -> Unit,
    onRevokeVip: (studentId: String) -> Unit,
    onSwitchPersona: (StudentUser) -> Unit = {},
    onViewActivities: (StudentUser) -> Unit = {}
) {
    val totalStudents = students.size
    val activeCount = students.count { !it.isBlocked }
    val blockedCount = students.count { it.isBlocked }
    val vipCount = students.count { it.hasAdvanceSubscription }
    val pendingFeeCount = students.count { it.feePaymentStatus == "PENDING_VERIFICATION" }

    val filteredList = students.filter { s ->
        val query = searchQuery.trim().lowercase()
        val matchQuery = query.isEmpty() ||
                s.fullName.lowercase().contains(query) ||
                s.email.lowercase().contains(query) ||
                s.telephone.contains(query) ||
                (s.cnic?.contains(query) == true)

        val matchFilter = when (filterStatus) {
            "ACTIVE" -> !s.isBlocked
            "BLOCKED" -> s.isBlocked
            "SUBSCRIBED" -> s.hasAdvanceSubscription
            "FEE_PENDING" -> s.feePaymentStatus == "PENDING_VERIFICATION"
            else -> true
        }

        matchQuery && matchFilter
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Summary Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(title = "Total", value = "$totalStudents", color = Color(0xFF1565C0), modifier = Modifier.weight(1f))
                StatCard(title = "Active", value = "$activeCount", color = Color(0xFF16A34A), modifier = Modifier.weight(1f))
                StatCard(title = "VIP Sub", value = "$vipCount", color = Color(0xFFFFB300), modifier = Modifier.weight(1f))
                StatCard(title = "Blocked", value = "$blockedCount", color = Color(0xFFDC2626), modifier = Modifier.weight(1f))
                if (pendingFeeCount > 0) {
                    StatCard(title = "Pending", value = "$pendingFeeCount", color = Color(0xFF9333EA), modifier = Modifier.weight(1f))
                }
            }
        }

        // Search Input
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_student_search"),
                placeholder = { Text("Search by name, email, phone, cnic...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Status Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val filters = listOf(
                    "ALL" to "All Students",
                    "ACTIVE" to "Active ✅",
                    "BLOCKED" to "Blocked 🚫",
                    "SUBSCRIBED" to "Advance VIP 👑",
                    "FEE_PENDING" to "Fee Pending 💳"
                )
                items(filters) { (key, label) ->
                    FilterChip(
                        selected = filterStatus == key,
                        onClick = { onFilterStatusChange(key) },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }
        }

        // Section Title
        item {
            Text(
                text = "طلباء کی تفصیلات (Student Profiles: Age, Email, Phone, CNIC)",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        if (filteredList.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("کوئی طالب علم نہیں ملا۔", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(filteredList) { student ->
                StudentDetailCard(
                    student = student,
                    onBlockToggle = { onBlockStudent(student.id, !student.isBlocked) },
                    onOpenVipModal = { onOpenVipModal(student) },
                    onRevokeVip = { onRevokeVip(student.id) },
                    onSwitchPersona = { onSwitchPersona(student) },
                    onViewActivities = { onViewActivities(student) }
                )
            }
        }
    }
}

@Composable
private fun StudentDetailCard(
    student: StudentUser,
    onBlockToggle: () -> Unit,
    onOpenVipModal: () -> Unit,
    onRevokeVip: () -> Unit,
    onSwitchPersona: () -> Unit = {},
    onViewActivities: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (student.isBlocked) Color(0xFFFEF2F2) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (student.isBlocked) Color(0xFFFCA5A5) else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Avatar, Name, Status Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (student.isBlocked) Color(0xFFFEE2E2) else MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (student.isBlocked) Icons.Default.Block else Icons.Default.Person,
                            contentDescription = null,
                            tint = if (student.isBlocked) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = student.fullName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Reg: ${student.registeredAt} • ID: ${student.id}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Badges
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (student.isBlocked) Color(0xFFDC2626) else Color(0xFF16A34A)
                    ) {
                        Text(
                            text = if (student.isBlocked) "بلاک شدہ (Blocked)" else "فعال (Active)",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (student.hasAdvanceSubscription) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFFB300)
                        ) {
                            Text(
                                text = "Advance VIP 👑",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Student Information Grid (Age, Email, Phone, CNIC, Class, Board)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                InfoRow(label = "عمر (Age):", value = "${student.age} سال (Years)")
                InfoRow(label = "ای میل (Email):", value = student.email)
                InfoRow(label = "ٹیلی فون (Phone):", value = student.telephone)
                InfoRow(
                    label = "شناختی کارڈ (CNIC/B-Form):",
                    value = student.cnic ?: "فراہم نہیں کیا گیا (اختیاری / Optional)",
                    isEmphasized = student.cnic != null
                )
                InfoRow(label = "کلاس و بورڈ:", value = "${student.selectedClass} Class • ${student.selectedBoard} Board")

                // Fee Status
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "فیس کی کیفیت (Fee):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(130.dp)
                    )
                    when (student.feePaymentStatus) {
                        "PAID_VERIFIED" -> {
                            Text(
                                text = "فیس ادا شدہ: Rs. ${student.feeAmount} ${if (student.feeTransactionRef != null) "(${student.feeTransactionRef})" else ""}",
                                fontSize = 12.sp,
                                color = Color(0xFF16A34A),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        "PENDING_VERIFICATION" -> {
                            Text(
                                text = "ادائیگی موصول (تصدیق طلب): Rs. ${student.feeAmount} (${student.feeTransactionRef ?: "N/A"})",
                                fontSize = 12.sp,
                                color = Color(0xFFD97706),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        else -> {
                            Text(
                                text = "غیر ادا شدہ (Unpaid)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Admin Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Block / Unblock Button
                Button(
                    onClick = onBlockToggle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (student.isBlocked) Color(0xFF16A34A) else Color(0xFFDC2626)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (student.isBlocked) Icons.Default.CheckCircle else Icons.Default.Block,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (student.isBlocked) "Unblock Student" else "Block Student",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Grant / Revoke Advance VIP Subscription Button
                if (!student.hasAdvanceSubscription) {
                    Button(
                        onClick = onOpenVipModal,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFB300),
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Grant Advance VIP",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onRevokeVip,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFDC2626)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Revoke VIP", fontSize = 12.sp)
                    }
                }
            }

            // Direct Student Interlink Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onSwitchPersona,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(imageVector = Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Emulate View (طلباء پورٹل)", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onViewActivities,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(imageVector = Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Activities (سرگرمیاں)", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, isEmphasized: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(130.dp)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = if (isEmphasized) FontWeight.Bold else FontWeight.Normal,
            color = if (isEmphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
            Text(text = title, fontSize = 10.sp, color = color, maxLines = 1)
        }
    }
}

@Composable
private fun AdminNotificationsSection(
    notifications: List<AdminNotification>,
    onMarkRead: (String) -> Unit,
    onClearAll: () -> Unit,
    onApproveStudentVip: (String) -> Unit
) {
    val sdf = remember { SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ایڈمن الرٹس و اطلاعات (Alerts)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                if (notifications.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text("Clear All", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        if (notifications.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("کوئی نیا الرٹ نہیں ہے۔", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(notifications) { notif ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (!notif.isRead) Color(0xFFEFF6FF) else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMarkRead(notif.id) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (notif.type) {
                                    "FEE_PAID" -> Icons.Default.MonetizationOn
                                    "NEW_STUDENT" -> Icons.Default.PersonAdd
                                    "SUBSCRIPTION_GRANTED" -> Icons.Default.WorkspacePremium
                                    "STUDENT_STATUS" -> Icons.Default.Shield
                                    else -> Icons.Default.Notifications
                                },
                                contentDescription = null,
                                tint = when (notif.type) {
                                    "FEE_PAID" -> Color(0xFF16A34A)
                                    "SUBSCRIPTION_GRANTED" -> Color(0xFFFFB300)
                                    "STUDENT_STATUS" -> Color(0xFFDC2626)
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = notif.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = sdf.format(Date(notif.timestamp)),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = notif.message,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // If fee notification and studentId is present, provide instant approve button!
                        if (notif.type == "FEE_PAID" && notif.studentId != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    onApproveStudentVip(notif.studentId)
                                    onMarkRead(notif.id)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Approve & Grant Advance VIP", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminCloudSyncSection(
    firestoreStatus: FirestoreSyncStatus,
    isFirestoreSyncing: Boolean,
    firestoreSyncMessage: String?,
    onSyncFirestore: () -> Unit,
    studentsCount: Int,
    papersCount: Int,
    notifsCount: Int,
    isGitHubSyncing: Boolean,
    gitHubRepoInput: String,
    gitHubSyncMessage: String?,
    onUpdateRepoInput: (String) -> Unit,
    onSyncGitHub: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Firebase Firestore Cloud Database Section ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEA580C).copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CloudQueue,
                                    contentDescription = null,
                                    tint = Color(0xFFEA580C),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Firebase Firestore Database",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Live Cloud Sync for Students & Papers",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Connection status pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (firestoreStatus.isConnected) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (firestoreStatus.isConnected) Color(0xFF16A34A) else Color(0xFFD97706))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (firestoreStatus.isConnected) "Live Connected" else "Offline Cache",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (firestoreStatus.isConnected) Color(0xFF15803D) else Color(0xFFB45309)
                            )
                        }
                    }
                }

                Text(
                    text = "طالب علموں کے پروفائلز، فیس ادائیگی کی تفصیلات، اور اپلوڈ شدہ پرچے فائر بیس کلاؤڈ ڈیٹا بیس میں خودکار طریقے سے محفوظ اور سنکرونائز ہوتے ہیں۔",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                // Firestore Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "Students (طلباء)",
                        value = "$studentsCount",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Papers (پرچے)",
                        value = "$papersCount",
                        color = Color(0xFF16A34A),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Alerts (اطلاعات)",
                        value = "$notifsCount",
                        color = Color(0xFFEA580C),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (firestoreSyncMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = firestoreSyncMessage,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Button(
                    onClick = onSyncFirestore,
                    enabled = !isFirestoreSyncing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEA580C)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_sync_firestore_btn")
                ) {
                    if (isFirestoreSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Syncing Firestore...")
                    } else {
                        Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Force Synchronize with Firestore (فائر بیس سنک)")
                    }
                }
            }
        }

        // --- 2. GitHub Releases Past Paper Sync Section ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FolderZip,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GitHub Releases Asset Sync",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Text(
                    text = "Sync past paper PDF assets directly from GitHub release tags without needing APK rebuilds.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = gitHubRepoInput,
                    onValueChange = onUpdateRepoInput,
                    label = { Text("GitHub Repo (owner/repo)") },
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

                OutlinedButton(
                    onClick = onSyncGitHub,
                    enabled = !isGitHubSyncing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isGitHubSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Syncing Releases...")
                    } else {
                        Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sync GitHub Releases Papers")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminActivityTrackingSection(
    activities: List<StudentActivityEvent>,
    selectedFilterType: ActivityEventType?,
    onFilterTypeChange: (ActivityEventType?) -> Unit,
    onClearActivities: () -> Unit,
    onJumpToPaper: (classId: String, subjectName: String, year: String, board: String) -> Unit,
    onSwitchPersona: (StudentUser) -> Unit,
    onPublishAnnouncement: (title: String, message: String, targetClass: String, priority: String) -> Unit,
    onOpenStudentPortal: () -> Unit,
    students: List<StudentUser>
) {
    var showBroadcastDialog by remember { mutableStateOf(false) }
    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastMessage by remember { mutableStateOf("") }
    var broadcastTargetClass by remember { mutableStateOf("ALL") }
    var broadcastPriority by remember { mutableStateOf("NORMAL") }

    val filteredActivities = remember(activities, selectedFilterType) {
        if (selectedFilterType == null) activities
        else activities.filter { it.eventType == selectedFilterType }
    }

    val totalEvents = activities.size
    val uniqueStudentsCount = activities.map { it.studentId }.distinct().count { it.isNotBlank() }
    val paperReadsCount = activities.count { it.eventType == ActivityEventType.PAPER_VIEW }
    val downloadsCount = activities.count { it.eventType == ActivityEventType.PAPER_DOWNLOAD }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF16A34A))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Live Activity Tracking (سرگرمی ٹریکنگ)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                    Text(
                        text = "Real-time audit log of student interactions across every section",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { showBroadcastDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E3A8A),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Broadcast Notice", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Summary Metric Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TrackingMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Events",
                    value = "$totalEvents",
                    icon = Icons.Default.Timeline,
                    color = MaterialTheme.colorScheme.primary
                )
                TrackingMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Active Students",
                    value = "$uniqueStudentsCount",
                    icon = Icons.Default.People,
                    color = Color(0xFF16A34A)
                )
                TrackingMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Paper Reads",
                    value = "$paperReadsCount",
                    icon = Icons.Default.MenuBook,
                    color = Color(0xFFD97706)
                )
                TrackingMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Downloads",
                    value = "$downloadsCount",
                    icon = Icons.Default.Download,
                    color = Color(0xFF9333EA)
                )
            }
        }

        // Quick Interlink Ribbon
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SyncAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Portal Interlink Active (باہمی رابطہ فعال ہے)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Click 'View Paper' to jump to Student Portal or emulate student view.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = onOpenStudentPortal,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Student View", fontSize = 11.sp)
                    }
                }
            }
        }

        // Filter chips row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filter Events (${filteredActivities.size})",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (activities.isNotEmpty()) {
                        TextButton(
                            onClick = onClearActivities,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Clear Logs", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilterType == null,
                            onClick = { onFilterTypeChange(null) },
                            label = { Text("All (${activities.size})", fontSize = 11.sp) }
                        )
                    }
                    items(ActivityEventType.entries.toTypedArray()) { type ->
                        val count = activities.count { it.eventType == type }
                        FilterChip(
                            selected = selectedFilterType == type,
                            onClick = { onFilterTypeChange(if (selectedFilterType == type) null else type) },
                            label = { Text("${type.emoji} ${type.displayName} ($count)", fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // Activity Events list
        if (filteredActivities.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Timeline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "کوئی سرگرمی ریکارڈ نہیں ہوئی۔",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Student actions (reading papers, downloading, searching) will appear here instantly.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        } else {
            items(filteredActivities, key = { it.id }) { event ->
                StudentActivityCard(
                    event = event,
                    onJumpToPaper = onJumpToPaper,
                    onSwitchPersona = {
                        val matchingStudent = students.firstOrNull { it.id == event.studentId }
                            ?: StudentUser(
                                id = event.studentId.ifBlank { "std_guest" },
                                fullName = event.studentName.ifBlank { "Student" },
                                email = "student@paperguru.edu.pk",
                                telephone = "0300-1234567",
                                selectedClass = event.classLevel ?: "10th",
                                selectedBoard = event.board ?: "LHR"
                            )
                        onSwitchPersona(matchingStudent)
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showBroadcastDialog) {
        AlertDialog(
            onDismissRequest = { showBroadcastDialog = false },
            icon = {
                Icon(imageVector = Icons.Default.Campaign, contentDescription = null, tint = Color(0xFF1E3A8A), modifier = Modifier.size(32.dp))
            },
            title = {
                Text("Broadcast Notice to Student Portal", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "This notice will be displayed immediately at the top of the Student Portal for all students.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = broadcastTitle,
                        onValueChange = { broadcastTitle = it },
                        label = { Text("Notice Title (عنوان)") },
                        placeholder = { Text("📢 10th Physics Past Papers Added") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = broadcastMessage,
                        onValueChange = { broadcastMessage = it },
                        label = { Text("Notice Message (تفصیل)") },
                        placeholder = { Text("Solved papers and guess papers are now live...") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Target Class (مطلوبہ کلاس):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("ALL", "9th", "10th", "11th", "12th").forEach { cls ->
                            FilterChip(
                                selected = broadcastTargetClass == cls,
                                onClick = { broadcastTargetClass = cls },
                                label = { Text(cls, fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = broadcastPriority == "NORMAL",
                            onClick = { broadcastPriority = "NORMAL" },
                            label = { Text("Normal Notice", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = broadcastPriority == "HIGH",
                            onClick = { broadcastPriority = "HIGH" },
                            label = { Text("🚨 High Priority", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (broadcastTitle.isNotBlank()) {
                            onPublishAnnouncement(
                                broadcastTitle,
                                broadcastMessage,
                                broadcastTargetClass,
                                broadcastPriority
                            )
                            showBroadcastDialog = false
                            broadcastTitle = ""
                            broadcastMessage = ""
                        }
                    },
                    enabled = broadcastTitle.isNotBlank()
                ) {
                    Text("Broadcast Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBroadcastDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StudentActivityCard(
    event: StudentActivityEvent,
    onJumpToPaper: (classId: String, subjectName: String, year: String, board: String) -> Unit,
    onSwitchPersona: () -> Unit
) {
    val timeAgo = remember(event.timestamp) {
        val diffMs = System.currentTimeMillis() - event.timestamp
        val minutes = diffMs / (1000 * 60)
        val hours = minutes / 60
        val days = hours / 24
        when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            else -> "${days}d ago"
        }
    }

    val typeBg = when (event.eventType) {
        ActivityEventType.FEE_SUBMISSION -> Color(0xFFDCFCE7)
        ActivityEventType.PAPER_VIEW -> Color(0xFFDBEAFE)
        ActivityEventType.PAPER_DOWNLOAD -> Color(0xFFF3E8FF)
        ActivityEventType.PAPER_BOOKMARK -> Color(0xFFFEF3C7)
        ActivityEventType.SEARCH -> Color(0xFFFFEDD5)
        ActivityEventType.PROFILE_UPDATE -> Color(0xFFE0E7FF)
        ActivityEventType.SCREEN_VIEW -> Color(0xFFF1F5F9)
        ActivityEventType.LOGIN -> Color(0xFFE2E8F0)
    }
    val typeText = when (event.eventType) {
        ActivityEventType.FEE_SUBMISSION -> Color(0xFF166534)
        ActivityEventType.PAPER_VIEW -> Color(0xFF1E40AF)
        ActivityEventType.PAPER_DOWNLOAD -> Color(0xFF6B21A8)
        ActivityEventType.PAPER_BOOKMARK -> Color(0xFF92400E)
        ActivityEventType.SEARCH -> Color(0xFF9A3412)
        ActivityEventType.PROFILE_UPDATE -> Color(0xFF3730A3)
        ActivityEventType.SCREEN_VIEW -> Color(0xFF334155)
        ActivityEventType.LOGIN -> Color(0xFF1E293B)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = typeBg,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(event.eventType.emoji, fontSize = 16.sp)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = event.studentName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        if (event.studentId.isNotBlank()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = event.studentId,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Section: ${event.sectionName}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = typeBg
                    ) {
                        Text(
                            text = event.eventType.displayName,
                            color = typeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = timeAgo,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (event.details.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = event.details,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            val hasPaperMeta = event.classLevel != null || event.subject != null || event.year != null || event.board != null
            if (hasPaperMeta) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (event.classLevel != null) {
                        MetadataChip(text = "${event.classLevel} Class")
                    }
                    if (event.subject != null) {
                        MetadataChip(text = event.subject)
                    }
                    if (event.year != null) {
                        MetadataChip(text = "Year ${event.year}")
                    }
                    if (event.board != null) {
                        MetadataChip(text = "Board ${event.board}")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (event.classLevel != null && event.subject != null) {
                    FilledTonalButton(
                        onClick = {
                            onJumpToPaper(
                                event.classLevel,
                                event.subject,
                                event.year ?: "2024",
                                event.board ?: "LHR"
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f).height(30.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View Paper in Student Portal", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onSwitchPersona,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f).height(30.dp)
                ) {
                    Icon(imageVector = Icons.Default.SwitchAccount, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Emulate Student View", fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun MetadataChip(text: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun TrackingMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
                text = title,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
