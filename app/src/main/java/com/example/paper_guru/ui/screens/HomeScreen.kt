package com.example.paper_guru.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paper_guru.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    classes: List<ClassLevel>,
    subjects: List<SubjectItem>,
    boards: List<EducationalBoard>,
    onSelectClass: (ClassLevel) -> Unit,
    // Search & Filters
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedClassFilter: String,
    onClassFilterChange: (String) -> Unit,
    selectedCategoryFilter: SubjectCategory,
    onCategoryFilterChange: (SubjectCategory) -> Unit,
    selectedBoardFilter: String,
    onBoardFilterChange: (String) -> Unit,
    showBookmarksOnly: Boolean,
    onToggleBookmarksOnly: () -> Unit,
    searchResults: List<SearchPaperResult>,
    onSelectSearchResult: (ClassLevel, SubjectItem, String, String) -> Unit,
    onToggleBookmarkResult: (String) -> Unit,
    onClearSearch: () -> Unit,
    // Pro & Auth
    isPaidMember: Boolean,
    onOpenPaywall: () -> Unit,
    currentUserEmail: String?,
    onOpenAuthDialog: () -> Unit,
    // Admin Mode state
    isAdminMode: Boolean,
    unreadAdminNotifsCount: Int = 0,
    trackedEventsCount: Int = 0,
    isStudentBlocked: Boolean = false,
    activeAnnouncement: AdminAnnouncement? = null,
    onDismissAnnouncement: () -> Unit = {},
    onOpenAdminDialog: () -> Unit,
    onOpenAdminPortal: () -> Unit,
    onOpenAdminLogin: () -> Unit,
    onExitAdminMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSearching = searchQuery.isNotBlank() ||
            selectedClassFilter != "ALL" ||
            selectedCategoryFilter != SubjectCategory.ALL ||
            selectedBoardFilter != "ALL" ||
            showBookmarksOnly

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Paper Guru",
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp
                            )
                            Text(
                                text = "طالب علم پورٹل (Student Portal)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    // Pro Badge
                    IconButton(
                        onClick = onOpenPaywall,
                        modifier = Modifier.testTag("home_pro_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = "PRO Membership",
                            tint = if (isPaidMember) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Student Profile / Account
                    IconButton(
                        onClick = onOpenAuthDialog,
                        modifier = Modifier.testTag("home_auth_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Student Profile",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            if (isPaidMember) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFB300))
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Admin Mode Banner if teacher/admin is logged in
            if (isAdminMode) {
                Surface(
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Admin Mode Active (Live Tracking On)",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (unreadAdminNotifsCount > 0) {
                                    Text(
                                        text = "🔴 $unreadAdminNotifsCount Alerts",
                                        color = Color(0xFFFCA5A5),
                                        fontSize = 10.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                if (trackedEventsCount > 0) {
                                    Text(
                                        text = "📊 $trackedEventsCount Events Logged",
                                        color = Color(0xFF93C5FD),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                        Button(
                            onClick = onOpenAdminPortal,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300), contentColor = Color.Black),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Open Portal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        TextButton(onClick = onExitAdminMode) {
                            Text("Exit", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Admin Announcement Banner (Broadcast to Students)
            if (activeAnnouncement != null) {
                Surface(
                    color = if (activeAnnouncement.priority == "HIGH") Color(0xFF7C2D12) else Color(0xFF1E3A8A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeAnnouncement.title,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = activeAnnouncement.message,
                                color = Color(0xFFE2E8F0),
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                        IconButton(
                            onClick = onDismissAnnouncement,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss announcement",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Blocked Student Warning Banner
            if (isStudentBlocked) {
                Surface(
                    color = Color(0xFFDC2626),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "اکاؤنٹ بلاک ہے! ایڈمنسٹریٹر نے آپ کے اکاؤنٹ کی پرچے دیکھنے کی سہولت معطل کر دی ہے۔",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onOpenAuthDialog) {
                            Text("Details", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Student Search Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_search_bar"),
                    placeholder = {
                        Text(
                            text = "Search papers by subject, board or year...",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (isSearching) {
                            IconButton(onClick = onClearSearch) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            // Filter Chips (Categories & Bookmarks)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bookmarks Filter Chip
                item {
                    FilterChip(
                        selected = showBookmarksOnly,
                        onClick = onToggleBookmarksOnly,
                        label = { Text("Bookmarked ⭐", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (showBookmarksOnly) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (showBookmarksOnly) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }

                // Subject Categories
                items(SubjectCategory.values()) { cat ->
                    FilterChip(
                        selected = selectedCategoryFilter == cat,
                        onClick = { onCategoryFilterChange(cat) },
                        label = { Text(cat.displayName, fontSize = 12.sp) }
                    )
                }
            }

            // Main Content Area
            if (isSearching) {
                // Search Results
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Found ${searchResults.size} past papers",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = onClearSearch) {
                            Text("Clear Filter", fontSize = 12.sp)
                        }
                    }

                    if (searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "کوئی پرچہ نہیں ملا (No paper found)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "براہ کرم کوئی دوسرا مضمون یا سال تلاش کریں۔",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(searchResults) { res ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelectSearchResult(res.classLevel, res.subject, res.year, res.board)
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    border = CardDefaults.outlinedCardBorder()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = res.subject.color.copy(alpha = 0.15f),
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = res.subject.icon,
                                                    contentDescription = null,
                                                    tint = res.subject.color,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${res.subject.name} - ${res.year}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = "${res.classLevel.displayName} • ${res.board} Board",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { onToggleBookmarkResult(res.paperItem.uniqueKey) }) {
                                            Icon(
                                                imageVector = if (res.paperItem.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                                contentDescription = "Bookmark",
                                                tint = if (res.paperItem.isBookmarked) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Student Home Dashboard
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Welcoming Hero Card for Students
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "خوش آمدید! (Welcome Students)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "پنجاب و فیڈرل بورڈز کے اصل اور مستند پرچے باآسانی ڈاؤنلوڈ کریں اور پڑھیں۔ اپنی کلاس منتخب کر کے امتحان کی بہترین تیاری کریں۔",
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Select Class Header
                    item {
                        Text(
                            text = "اپنی کلاس منتخب کریں (Select Your Class)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Class Cards List
                    items(classes) { cls ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectClass(cls) }
                                .testTag("class_card_${cls.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = cls.accentColor.copy(alpha = 0.15f),
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = cls.icon,
                                            contentDescription = null,
                                            tint = cls.accentColor,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cls.displayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = cls.subtitle,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Open",
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    // Temporary Rumors Review Notice for 9th and 10th Class
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFFEF3C7),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = Color(0xFFB45309),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "9th & 10th Class Papers Temporarily Withheld",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF92400E)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "افواہوں اور مبینہ اطلاعات کی تصدیق تک نویں اور دسویں کے تمام پرچے ایپ سے عارضی طور پر ہٹا دیے گئے ہیں۔ 11th اور 12th کے پرچے دستیاب ہیں۔",
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        color = Color(0xFF78350F)
                                    )
                                }
                            }
                        }
                    }

                    // Quick Exam Preparation Tips for Students
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "امتحانی مشورہ (Exam Tip)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = "پچھلے 5 سالہ پرچے حل کرنے سے بورڈ میں 80٪ سے زائد نمبر باآسانی حاصل کیے جا سکتے ہیں۔",
                                        fontSize = 12.sp,
                                        color = Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }

                    // Discrete Footer with Administrative Access link (Separate from Student view)
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Paper Guru • پنجاب اور فیڈرل بورڈ امتحانی پورٹل",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(
                                onClick = onOpenAdminLogin,
                                modifier = Modifier.testTag("home_footer_admin_login_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Admin Access Portal (محفوظ ایڈمن رسائی)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
