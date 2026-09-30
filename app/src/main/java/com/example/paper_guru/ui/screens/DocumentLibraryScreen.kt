package com.example.paper_guru.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paper_guru.model.DocumentLibraryItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentLibraryScreen(
    documents: List<DocumentLibraryItem>,
    isLoading: Boolean,
    repositoryPath: String,
    onRefresh: () -> Unit,
    onOpenDocument: (DocumentLibraryItem) -> Unit,
    onDownloadDocument: (DocumentLibraryItem) -> Unit,
    onToggleBookmark: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var searchQuery by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("ALL") }
    var selectedYear by remember { mutableStateOf("ALL") }
    var selectedClass by remember { mutableStateOf("ALL") }
    var showOfflineOnly by remember { mutableStateOf(false) }

    // Filter documents
    val filteredDocuments = remember(documents, searchQuery, selectedSubject, selectedYear, selectedClass, showOfflineOnly) {
        val q = searchQuery.trim().lowercase()
        documents.filter { doc ->
            val matchesQuery = q.isEmpty() ||
                    doc.title.lowercase().contains(q) ||
                    doc.subject.lowercase().contains(q) ||
                    doc.year.contains(q) ||
                    doc.board.lowercase().contains(q) ||
                    doc.fileName.lowercase().contains(q)

            val matchesSubject = selectedSubject == "ALL" || doc.subject.equals(selectedSubject, ignoreCase = true)
            val matchesYear = selectedYear == "ALL" || doc.year == selectedYear
            val matchesClass = selectedClass == "ALL" || doc.classFolderName.equals(selectedClass, ignoreCase = true)
            val matchesOffline = !showOfflineOnly || doc.isOfflineAvailable

            matchesQuery && matchesSubject && matchesYear && matchesClass && matchesOffline
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Document Library",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                        Text(
                            text = "GitHub Releases Metadata • Retrofit",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("library_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onRefresh,
                        enabled = !isLoading,
                        modifier = Modifier.testTag("library_refresh_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh from GitHub",
                                tint = MaterialTheme.colorScheme.primary
                            )
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
            // Repo status header banner
            Surface(
                color = Color(0xFF24292E),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = repositoryPath,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${documents.size} Past Paper Documents • Room DB Cached",
                            color = Color(0xFFB0BEC5),
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1B5E20).copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "Room Offline",
                            color = Color(0xFFA5D6A7),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF37474F)
                    ) {
                        Text(
                            text = "Release v1.0.0",
                            color = Color(0xFF81C784),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Search bar in Document Library
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter library documents by title, subject, or year...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Library",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("library_search_field")
                )

                // Quick Subject Chips Row
                val subjects = listOf("Physics", "Chemistry", "Mathematics", "Biology", "Computer Science", "English", "Urdu")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedSubject == "ALL",
                        onClick = { selectedSubject = "ALL" },
                        label = { Text("All Subjects", fontSize = 12.sp) }
                    )
                    subjects.forEach { s ->
                        FilterChip(
                            selected = selectedSubject.equals(s, ignoreCase = true),
                            onClick = { selectedSubject = if (selectedSubject.equals(s, ignoreCase = true)) "ALL" else s },
                            label = { Text(s, fontSize = 12.sp) }
                        )
                    }
                }

                // Quick Year & Class Chips Row
                val years = listOf("2024", "2023", "2022", "2021", "2019", "2018")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedYear == "ALL",
                        onClick = { selectedYear = "ALL" },
                        label = { Text("All Years", fontSize = 12.sp) }
                    )
                    years.forEach { yr ->
                        FilterChip(
                            selected = selectedYear == yr,
                            onClick = { selectedYear = if (selectedYear == yr) "ALL" else yr },
                            label = { Text(yr, fontSize = 12.sp) }
                        )
                    }
                    FilterChip(
                        selected = selectedClass == "9th",
                        onClick = { selectedClass = if (selectedClass == "9th") "ALL" else "9th" },
                        label = { Text("9th Class", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = selectedClass == "10th",
                        onClick = { selectedClass = if (selectedClass == "10th") "ALL" else "10th" },
                        label = { Text("10th Class", fontSize = 12.sp) }
                    )
                }
            }

            // Results summary header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Library Documents (${filteredDocuments.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (searchQuery.isNotEmpty() || selectedSubject != "ALL" || selectedYear != "ALL" || selectedClass != "ALL") {
                    TextButton(onClick = {
                        searchQuery = ""
                        selectedSubject = "ALL"
                        selectedYear = "ALL"
                        selectedClass = "ALL"
                    }) {
                        Text("Reset Filters", fontSize = 12.sp)
                    }
                }
            }

            // Document list
            if (filteredDocuments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (isLoading) "Fetching Release Metadata..." else "No matching documents found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isLoading) "Querying GitHub REST API for release assets..."
                            else "Try adjusting your subject or year filters, or tap Refresh to reload from GitHub.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(onClick = onRefresh) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Refresh Metadata from GitHub")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredDocuments, key = { it.id }) { doc ->
                        DocumentItemCard(
                            document = doc,
                            onOpen = { onOpenDocument(doc) },
                            onDownload = { onDownloadDocument(doc) },
                            onToggleBookmark = { onToggleBookmark(doc.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentItemCard(
    document: DocumentLibraryItem,
    onOpen: () -> Unit,
    onDownload: () -> Unit,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .testTag("doc_card_${document.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // PDF Document Icon Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFD32F2F).copy(alpha = 0.12f),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "PDF Document",
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = document.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${document.classLevel} • ${document.board} Board • Exam Year ${document.year}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (document.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (document.isBookmarked) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metadata Badges & Pill Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Subject Tag
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = document.subject,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                // File Size Tag
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF263238).copy(alpha = 0.08f)
                ) {
                    Text(
                        text = document.formattedSize,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                // Release Tag / Offline Tag
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (document.isOfflineAvailable) Color(0xFFE8F5E9) else Color(0xFFE3F2FD)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (document.isOfflineAvailable) Icons.Default.CheckCircle else Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = if (document.isOfflineAvailable) Color(0xFF2E7D32) else Color(0xFF1976D2),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (document.isOfflineAvailable) "Offline Ready" else "GitHub Release",
                            color = if (document.isOfflineAvailable) Color(0xFF2E7D32) else Color(0xFF1976D2),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDownload,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Download", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onOpen,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("View Paper", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
