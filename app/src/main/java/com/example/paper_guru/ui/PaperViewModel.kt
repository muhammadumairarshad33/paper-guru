package com.example.paper_guru.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.paper_guru.data.FirebaseFirestoreService
import com.example.paper_guru.data.GitHubReleaseManager
import com.example.paper_guru.data.HttpDownloadHelper
import com.example.paper_guru.data.PaperRepository
import com.example.paper_guru.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class PaperUiState(
    val currentScreen: AppScreen = AppScreen.HOME,
    val selectedClass: ClassLevel? = null,
    val selectedSubject: SubjectItem? = null,
    val selectedYear: String? = null,
    val currentPaper: PaperItem? = null,
    // Search & Filters
    val homeSearchQuery: String = "",
    val homeClassFilter: String = "ALL",
    val homeSubjectFilter: String = "ALL",
    val homeYearFilter: String = "ALL",
    val homeCategoryFilter: SubjectCategory = SubjectCategory.ALL,
    val homeBoardFilter: String = "ALL",
    val showBookmarksOnly: Boolean = false,
    val homeSearchResults: List<SearchPaperResult> = emptyList(),
    // Subject & Year Screen state
    val subjectSearchQuery: String = "",
    val subjectCategoryFilter: SubjectCategory = SubjectCategory.ALL,
    val yearSearchQuery: String = "",
    val yearBoardFilter: String = "ALL",
    // PDF Rendering state
    val isPdfLoading: Boolean = false,
    val pdfBitmaps: List<Bitmap> = emptyList(),
    val pdfError: String? = null,
    // Download state
    val isDownloading: Boolean = false,
    val lastDownloadedFile: File? = null,
    // PRO Membership & Auth state
    val isPaidMember: Boolean = false,
    val showPaywallDialog: Boolean = false,
    val paywallTriggerFeature: String = "PRO Membership",
    val showAuthDialog: Boolean = false,
    val currentUserEmail: String? = null,
    val currentUserId: String? = null,
    val isUserAnonymous: Boolean = true,
    // Student Profile & Directory
    val currentStudent: StudentUser? = null,
    val studentsList: List<StudentUser> = emptyList(),
    val isStudentBlocked: Boolean = false,
    val showBlockedDialog: Boolean = false,
    val studentSearchQuery: String = "",
    val studentFilterStatus: String = "ALL", // "ALL", "ACTIVE", "BLOCKED", "SUBSCRIBED", "FEE_PENDING"
    val showFeePaymentDialog: Boolean = false,
    // Admin Mode state
    val isAdminMode: Boolean = false,
    val showAdminDialog: Boolean = false,
    val adminUsernameInput: String = "admin",
    val adminPasswordInput: String = "",
    val adminPinInput: String = "",
    val adminErrorMessage: String? = null,
    val isAdminAuthenticating: Boolean = false,
    val adminSelectedTab: AdminTab = AdminTab.UPLOAD_PAPER,
    val adminNotifications: List<AdminNotification> = emptyList(),
    val dynamicUploadedPapers: List<PaperItem> = emptyList(),
    val isGitHubSyncing: Boolean = false,
    val gitHubRepoInput: String = GitHubReleaseManager.DEFAULT_REPO,
    val gitHubSyncMessage: String? = null,
    // Firebase Firestore Cloud Sync state
    val firestoreStatus: FirestoreSyncStatus = FirestoreSyncStatus.OFFLINE_PERSISTENT,
    val isFirestoreSyncing: Boolean = false,
    val firestoreSyncMessage: String? = null,
    // Interlinked Student Activity Tracking
    val trackedActivities: List<StudentActivityEvent> = emptyList(),
    val activityFilterType: ActivityEventType? = null,
    // Announcements & Broadcasts
    val announcements: List<AdminAnnouncement> = emptyList(),
    val activeAnnouncement: AdminAnnouncement? = null,
    // Global notifications
    val snackBarMessage: String? = null
) {
    val unreadNotificationsCount: Int
        get() = adminNotifications.count { !it.isRead }
}

class PaperViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PaperUiState())
    val uiState: StateFlow<PaperUiState> = _uiState.asStateFlow()

    val availableClasses = PaperRepository.classes
    val availableSubjects = PaperRepository.subjects
    val availableYears = PaperRepository.years
    val availableBoards = PaperRepository.boards

    private val screenStack = mutableListOf<AppScreen>()

    init {
        val savedRepo = GitHubReleaseManager.getSavedRepo(application)
        val students = PaperRepository.getStudents(application)
        val notifs = PaperRepository.getAdminNotifications(application)
        val dynamicPapers = PaperRepository.getAllDynamicPapers(application)
        val initialStudent = students.firstOrNull { !it.isBlocked } ?: students.firstOrNull()
        val activities = PaperRepository.getTrackedActivities(application)
        val allAnnouncements = PaperRepository.getAnnouncements(application)
        val topAnnouncement = allAnnouncements.firstOrNull { it.isActive }

        _uiState.update {
            it.copy(
                gitHubRepoInput = savedRepo,
                studentsList = students,
                adminNotifications = notifs,
                dynamicUploadedPapers = dynamicPapers,
                currentStudent = initialStudent,
                currentUserEmail = initialStudent?.email,
                currentUserId = initialStudent?.id,
                isStudentBlocked = initialStudent?.isBlocked ?: false,
                isPaidMember = initialStudent?.hasAdvanceSubscription ?: false,
                trackedActivities = activities,
                announcements = allAnnouncements,
                activeAnnouncement = topAnnouncement
            )
        }

        // Initialize Firebase Firestore with offline cache
        viewModelScope.launch {
            try {
                FirebaseFirestoreService.initialize(application)
                _uiState.update { it.copy(firestoreStatus = FirebaseFirestoreService.currentSyncStatus) }

                // Fetch latest student records, paper uploads, activities and notifications from Firestore
                val remoteStudents = FirebaseFirestoreService.fetchStudents(application).getOrNull()
                if (!remoteStudents.isNullOrEmpty()) {
                    for (std in remoteStudents) {
                        PaperRepository.saveOrUpdateStudent(application, std)
                    }
                    val mergedStudents = PaperRepository.getStudents(application)
                    _uiState.update { it.copy(studentsList = mergedStudents) }
                }

                val remotePapers = FirebaseFirestoreService.fetchPaperUploads(application).getOrNull()
                if (!remotePapers.isNullOrEmpty()) {
                    for (paper in remotePapers) {
                        PaperRepository.saveDynamicPaper(application, paper)
                    }
                    val mergedPapers = PaperRepository.getAllDynamicPapers(application)
                    _uiState.update { it.copy(dynamicUploadedPapers = mergedPapers) }
                }

                val remoteNotifs = FirebaseFirestoreService.fetchNotifications(application).getOrNull()
                if (!remoteNotifs.isNullOrEmpty()) {
                    for (notif in remoteNotifs) {
                        PaperRepository.addAdminNotification(application, notif)
                    }
                    val mergedNotifs = PaperRepository.getAdminNotifications(application)
                    _uiState.update { it.copy(adminNotifications = mergedNotifs) }
                }

                val remoteActivities = FirebaseFirestoreService.fetchActivities(application).getOrNull()
                if (!remoteActivities.isNullOrEmpty()) {
                    for (act in remoteActivities) {
                        PaperRepository.logStudentActivity(application, act)
                    }
                    val mergedActivities = PaperRepository.getTrackedActivities(application)
                    _uiState.update { it.copy(trackedActivities = mergedActivities) }
                }

                val remoteAnnouncements = FirebaseFirestoreService.fetchAnnouncements(application).getOrNull()
                if (!remoteAnnouncements.isNullOrEmpty()) {
                    for (ann in remoteAnnouncements) {
                        PaperRepository.publishAnnouncement(application, ann)
                    }
                    val mergedAnn = PaperRepository.getAnnouncements(application)
                    _uiState.update {
                        it.copy(
                            announcements = mergedAnn,
                            activeAnnouncement = mergedAnn.firstOrNull { a -> a.isActive }
                        )
                    }
                }
            } catch (_: Exception) {}
        }

        // Background Silent Sync: Automatically fetch updated papers from GitHub without bothering students!
        viewModelScope.launch {
            try {
                PaperRepository.syncFromGitHub(application, savedRepo)
                val updatedPapers = PaperRepository.getAllDynamicPapers(application)
                _uiState.update { it.copy(dynamicUploadedPapers = updatedPapers) }
            } catch (_: Exception) {}
        }
    }

    private fun navigateTo(screen: AppScreen) {
        screenStack.add(_uiState.value.currentScreen)
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun navigateBack() {
        if (screenStack.isNotEmpty()) {
            val prev = screenStack.removeAt(screenStack.size - 1)
            _uiState.update { it.copy(currentScreen = prev, pdfError = null) }
        } else {
            _uiState.update { it.copy(currentScreen = AppScreen.HOME, pdfError = null) }
        }
    }

    // --- Event & Activity Tracking ---
    fun logActivity(
        eventType: ActivityEventType,
        sectionName: String,
        details: String,
        classLevel: String? = null,
        subject: String? = null,
        year: String? = null,
        board: String? = null
    ) {
        val std = _uiState.value.currentStudent
        val studentId = std?.id ?: _uiState.value.currentUserId ?: "guest_std"
        val studentName = std?.fullName ?: if (_uiState.value.isUserAnonymous) "Guest Student" else "Student"

        val event = StudentActivityEvent(
            id = "act_${System.currentTimeMillis()}_${(100..999).random()}",
            studentId = studentId,
            studentName = studentName,
            eventType = eventType,
            sectionName = sectionName,
            details = details,
            timestamp = System.currentTimeMillis(),
            classLevel = classLevel,
            subject = subject,
            year = year,
            board = board
        )
        PaperRepository.logStudentActivity(getApplication(), event)
        val updated = PaperRepository.getTrackedActivities(getApplication())
        _uiState.update { it.copy(trackedActivities = updated) }
    }

    // --- Navigation Flow ---
    fun selectClass(classLevel: ClassLevel) {
        _uiState.update {
            it.copy(
                selectedClass = classLevel,
                subjectSearchQuery = "",
                subjectCategoryFilter = SubjectCategory.ALL
            )
        }
        logActivity(
            eventType = ActivityEventType.SCREEN_VIEW,
            sectionName = "Class: ${classLevel.displayName}",
            details = "Browsing ${classLevel.displayName} subjects",
            classLevel = classLevel.folderName
        )
        navigateTo(AppScreen.SUBJECTS)
    }

    fun selectSubject(subject: SubjectItem) {
        val currentCls = _uiState.value.selectedClass
        _uiState.update {
            it.copy(
                selectedSubject = subject,
                yearSearchQuery = "",
                yearBoardFilter = "ALL"
            )
        }
        logActivity(
            eventType = ActivityEventType.SCREEN_VIEW,
            sectionName = "Subject: ${subject.name}",
            details = "Browsing ${subject.name} years and boards for ${currentCls?.displayName ?: ""}",
            classLevel = currentCls?.folderName,
            subject = subject.name
        )
        navigateTo(AppScreen.YEARS)
    }

    fun selectYear(context: Context, year: String, board: String = "LHR") {
        val cls = _uiState.value.selectedClass ?: return
        val sub = _uiState.value.selectedSubject ?: return
        val paper = PaperRepository.resolvePaperAsset(context, cls.folderName, sub.name, year, board)

        _uiState.update {
            it.copy(
                selectedYear = year,
                currentPaper = paper
            )
        }
        logActivity(
            eventType = ActivityEventType.SCREEN_VIEW,
            sectionName = "Paper: ${sub.name} $year ($board)",
            details = "Viewing past paper detail for ${cls.displayName} ${sub.name}",
            classLevel = cls.folderName,
            subject = sub.name,
            year = year,
            board = board
        )
        navigateTo(AppScreen.PAPER_DETAIL)
    }

    fun openPaperDirectly(context: Context, classLevel: ClassLevel, subject: SubjectItem, year: String, board: String) {
        val paper = PaperRepository.resolvePaperAsset(context, classLevel.folderName, subject.name, year, board)
        _uiState.update {
            it.copy(
                selectedClass = classLevel,
                selectedSubject = subject,
                selectedYear = year,
                currentPaper = paper
            )
        }
        logActivity(
            eventType = ActivityEventType.PAPER_VIEW,
            sectionName = "${subject.name} $year ($board)",
            details = "Directly opened ${classLevel.displayName} ${subject.name} past paper",
            classLevel = classLevel.folderName,
            subject = subject.name,
            year = year,
            board = board
        )
        navigateTo(AppScreen.PAPER_DETAIL)
    }

    // --- Student Viewing & PDF Rendering ---
    fun viewPaper(context: Context) {
        val paper = _uiState.value.currentPaper ?: return
        if (_uiState.value.isStudentBlocked) {
            _uiState.update { it.copy(showBlockedDialog = true) }
            return
        }
        _uiState.update {
            it.copy(
                isPdfLoading = true,
                pdfBitmaps = emptyList(),
                pdfError = null
            )
        }
        logActivity(
            eventType = ActivityEventType.PAPER_VIEW,
            sectionName = "PDF: ${paper.subjectName} ${paper.year} (${paper.board})",
            details = "Opened full past paper in PDF viewer (${paper.classFolderName} Class)",
            classLevel = paper.classFolderName,
            subject = paper.subjectName,
            year = paper.year,
            board = paper.board
        )
        navigateTo(AppScreen.PDF_VIEWER)

        viewModelScope.launch {
            // If paper is marked not available, check GitHub releases in background silently first!
            var targetPaper = paper
            if (!targetPaper.isAvailable) {
                val savedRepo = GitHubReleaseManager.getSavedRepo(context)
                PaperRepository.syncFromGitHub(context, savedRepo)
                targetPaper = PaperRepository.resolvePaperAsset(
                    context,
                    paper.classFolderName,
                    paper.subjectName,
                    paper.year,
                    paper.board
                )
                _uiState.update { it.copy(currentPaper = targetPaper) }
            }

            if (!targetPaper.isAvailable) {
                _uiState.update {
                    it.copy(
                        isPdfLoading = false,
                        pdfError = "یہ پرچہ ابھی اپلوڈ نہیں ہوا۔ جلد شامل کر دیا جائے گا۔"
                    )
                }
                return@launch
            }

            renderPaperBitmaps(context, targetPaper)
        }
    }

    private suspend fun renderPaperBitmaps(context: Context, paper: PaperItem) = withContext(Dispatchers.IO) {
        try {
            val fileToRender: File = if (paper.isRemote && !paper.remoteUrl.isNullOrEmpty()) {
                val cacheFile = File(context.cacheDir, "pdf_${paper.uniqueKey}.pdf")
                if (!cacheFile.exists() || cacheFile.length() == 0L) {
                    val downloadRes = HttpDownloadHelper.downloadToFile(paper.remoteUrl, cacheFile)
                    downloadRes.getOrThrow()
                } else {
                    cacheFile
                }
            } else if (paper.assetPath != null) {
                val assetCache = File(context.cacheDir, "asset_${paper.uniqueKey}.pdf")
                if (!assetCache.exists() || assetCache.length() == 0L) {
                    context.assets.open(paper.assetPath).use { input ->
                        FileOutputStream(assetCache).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                assetCache
            } else {
                throw Exception("Paper source file not found")
            }

            val pfd = ParcelFileDescriptor.open(fileToRender, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount
            val bitmaps = mutableListOf<Bitmap>()

            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                val width = page.width * 2
                val height = page.height * 2
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmaps.add(bitmap)
                page.close()
            }
            renderer.close()
            pfd.close()

            _uiState.update {
                it.copy(
                    isPdfLoading = false,
                    pdfBitmaps = bitmaps,
                    pdfError = null
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isPdfLoading = false,
                    pdfBitmaps = emptyList(),
                    pdfError = "پرچہ اوپن کرنے میں مسئلہ آیا: ${e.localizedMessage}"
                )
            }
        }
    }

    // --- Bookmarks Operation ---
    fun toggleBookmark(context: Context, paperKey: String) {
        val isNowBookmarked = PaperRepository.toggleBookmark(context, paperKey)
        val current = _uiState.value.currentPaper
        if (current != null && current.uniqueKey == paperKey) {
            _uiState.update { it.copy(currentPaper = current.copy(isBookmarked = isNowBookmarked)) }
        }
        _uiState.update {
            it.copy(snackBarMessage = if (isNowBookmarked) "Saved to Bookmarks ⭐" else "Removed from Bookmarks")
        }
        logActivity(
            eventType = ActivityEventType.PAPER_BOOKMARK,
            sectionName = "Bookmark",
            details = if (isNowBookmarked) "Saved paper $paperKey to offline bookmarks" else "Removed paper $paperKey from bookmarks"
        )
        executeSearch(context)
    }

    // --- Download Operations ---
    fun downloadCurrentPaper(context: Context) {
        val paper = _uiState.value.currentPaper ?: return
        if (_uiState.value.isStudentBlocked) {
            _uiState.update { it.copy(showBlockedDialog = true) }
            return
        }
        if (!paper.isAvailable) {
            _uiState.update { it.copy(snackBarMessage = "Paper is not available to download.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isDownloading = true) }
            val res = PaperRepository.downloadPaperToDevice(context, paper)
            _uiState.update {
                it.copy(
                    isDownloading = false,
                    lastDownloadedFile = res.getOrNull(),
                    snackBarMessage = if (res.isSuccess) "Paper saved to Downloads! 📄" else "Download failed: ${res.exceptionOrNull()?.localizedMessage}"
                )
            }
            if (res.isSuccess) {
                logActivity(
                    eventType = ActivityEventType.PAPER_DOWNLOAD,
                    sectionName = "Download: ${paper.subjectName} ${paper.year}",
                    details = "Downloaded past paper (${paper.classFolderName}, ${paper.board} board)",
                    classLevel = paper.classFolderName,
                    subject = paper.subjectName,
                    year = paper.year,
                    board = paper.board
                )
            }
        }
    }

    fun openDownloadedPaper(context: Context) {
        val file = _uiState.value.lastDownloadedFile ?: return
        try {
            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            _uiState.update { it.copy(snackBarMessage = "Could not open external viewer: ${e.localizedMessage}") }
        }
    }

    fun shareDownloadedPaper(context: Context) {
        val file = _uiState.value.lastDownloadedFile ?: return
        try {
            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Past Paper"))
        } catch (e: Exception) {
            _uiState.update { it.copy(snackBarMessage = "Share error: ${e.localizedMessage}") }
        }
    }

    // --- Search & Filters ---
    fun onHomeSearchChanged(context: Context, query: String) {
        _uiState.update { it.copy(homeSearchQuery = query) }
        if (query.trim().length >= 3) {
            logActivity(
                eventType = ActivityEventType.SEARCH,
                sectionName = "Search: ${query.trim()}",
                details = "Searched across past papers: '${query.trim()}'"
            )
        }
        executeSearch(context)
    }

    fun setHomeClassFilter(context: Context, classId: String) {
        _uiState.update { it.copy(homeClassFilter = classId) }
        executeSearch(context)
    }

    fun setHomeCategoryFilter(context: Context, category: SubjectCategory) {
        _uiState.update { it.copy(homeCategoryFilter = category) }
        executeSearch(context)
    }

    fun setHomeSubjectFilter(context: Context, subjectName: String) {
        _uiState.update { it.copy(homeSubjectFilter = subjectName) }
        executeSearch(context)
    }

    fun setHomeYearFilter(context: Context, year: String) {
        _uiState.update { it.copy(homeYearFilter = year) }
        executeSearch(context)
    }

    fun setHomeBoardFilter(context: Context, boardCode: String) {
        _uiState.update { it.copy(homeBoardFilter = boardCode) }
        executeSearch(context)
    }

    fun toggleBookmarksFilter(context: Context) {
        _uiState.update { it.copy(showBookmarksOnly = !it.showBookmarksOnly) }
        executeSearch(context)
    }

    fun clearHomeSearch(context: Context) {
        _uiState.update {
            it.copy(
                homeSearchQuery = "",
                homeClassFilter = "ALL",
                homeSubjectFilter = "ALL",
                homeYearFilter = "ALL",
                homeCategoryFilter = SubjectCategory.ALL,
                homeBoardFilter = "ALL",
                showBookmarksOnly = false,
                homeSearchResults = emptyList()
            )
        }
    }

    private fun executeSearch(context: Context) {
        val state = _uiState.value
        val hasFilter = state.homeSearchQuery.isNotBlank() ||
                state.homeClassFilter != "ALL" ||
                state.homeSubjectFilter != "ALL" ||
                state.homeYearFilter != "ALL" ||
                state.homeCategoryFilter != SubjectCategory.ALL ||
                state.homeBoardFilter != "ALL" ||
                state.showBookmarksOnly

        if (!hasFilter) {
            _uiState.update { it.copy(homeSearchResults = emptyList()) }
            return
        }

        val results = PaperRepository.searchPapers(
            context = context,
            query = state.homeSearchQuery,
            classFilter = state.homeClassFilter,
            categoryFilter = state.homeCategoryFilter,
            yearFilter = state.homeYearFilter,
            boardFilter = state.homeBoardFilter,
            onlyBookmarked = state.showBookmarksOnly
        )
        _uiState.update { it.copy(homeSearchResults = results) }
    }

    // --- Sub-screen filters ---
    fun setSubjectSearchQuery(query: String) {
        _uiState.update { it.copy(subjectSearchQuery = query) }
    }

    fun setSubjectCategoryFilter(category: SubjectCategory) {
        _uiState.update { it.copy(subjectCategoryFilter = category) }
    }

    fun setYearSearchQuery(query: String) {
        _uiState.update { it.copy(yearSearchQuery = query) }
    }

    fun setYearBoardFilter(board: String) {
        _uiState.update { it.copy(yearBoardFilter = board) }
    }

    // --- Solutions & Exam Inspiration (Student Portal) ---
    fun openSolutionScreen() {
        navigateTo(AppScreen.SOLUTION_VIEWER)
    }

    fun openExamInspirationScreen() {
        navigateTo(AppScreen.EXAM_INSPIRATION)
    }

    // --- Dialogs (Auth & Pro) ---
    fun openPaywallDialog(trigger: String) {
        _uiState.update { it.copy(showPaywallDialog = true, paywallTriggerFeature = trigger) }
    }

    fun closePaywallDialog() {
        _uiState.update { it.copy(showPaywallDialog = false) }
    }

    fun activateProMembership(promoCode: String) {
        if (promoCode.trim().equals("STUDENT2024", ignoreCase = true) || promoCode.trim().equals("FREEPRO", ignoreCase = true)) {
            _uiState.update {
                it.copy(
                    isPaidMember = true,
                    showPaywallDialog = false,
                    snackBarMessage = "PRO Membership Activated! 🎉 All solutions & notes unlocked."
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    isPaidMember = true,
                    showPaywallDialog = false,
                    snackBarMessage = "Welcome to PRO Student Plan! All papers unlocked."
                )
            }
        }
    }

    fun openAuthDialog() {
        _uiState.update { it.copy(showAuthDialog = true) }
    }

    fun closeAuthDialog() {
        _uiState.update { it.copy(showAuthDialog = false) }
    }

    fun signInGuest() {
        _uiState.update {
            it.copy(
                isUserAnonymous = true,
                currentUserId = "guest_student_01",
                currentUserEmail = "student@paperguru.edu.pk",
                showAuthDialog = false,
                snackBarMessage = "Welcome, Student! Logged in as Guest."
            )
        }
    }

    fun signInEmail(email: String) {
        _uiState.update {
            it.copy(
                isUserAnonymous = false,
                currentUserId = "user_${email.hashCode()}",
                currentUserEmail = email,
                showAuthDialog = false,
                snackBarMessage = "Welcome back, $email!"
            )
        }
    }

    fun signOut() {
        _uiState.update {
            it.copy(
                currentUserEmail = null,
                currentUserId = null,
                isUserAnonymous = true,
                isPaidMember = false,
                showAuthDialog = false,
                snackBarMessage = "Signed out successfully."
            )
        }
    }

    // --- Teacher & Admin Portal (PIN Protected) ---
    fun openAdminDialog() {
        _uiState.update { it.copy(showAdminDialog = true, adminPinInput = "", adminErrorMessage = null) }
    }

    fun closeAdminDialog() {
        _uiState.update { it.copy(showAdminDialog = false, adminPinInput = "", adminErrorMessage = null) }
    }

    fun updateAdminPin(pin: String) {
        _uiState.update { it.copy(adminPinInput = pin, adminErrorMessage = null) }
    }

    fun verifyAdminPin() {
        val pin = _uiState.value.adminPinInput.trim()
        if (pin == "1234" || pin.equals("admin", ignoreCase = true) || pin == "786") {
            _uiState.update {
                it.copy(
                    isAdminMode = true,
                    showAdminDialog = false,
                    currentScreen = AppScreen.ADMIN_PORTAL,
                    snackBarMessage = "ایڈمن پورٹل فعال ہو گیا ہے (Admin Portal Open)"
                )
            }
        } else {
            _uiState.update { it.copy(adminErrorMessage = "غلط پن کوڈ درج کیا گیا ہے (Default PIN: 1234)") }
        }
    }

    fun openAdminLoginScreen() {
        _uiState.update {
            it.copy(
                adminErrorMessage = null,
                adminPasswordInput = "",
                showAdminDialog = false
            )
        }
        navigateTo(AppScreen.ADMIN_LOGIN)
    }

    fun updateAdminUsername(username: String) {
        _uiState.update { it.copy(adminUsernameInput = username, adminErrorMessage = null) }
    }

    fun updateAdminPassword(password: String) {
        _uiState.update { it.copy(adminPasswordInput = password, adminErrorMessage = null) }
    }

    fun loginAdmin(username: String, passcode: String) {
        val u = username.trim()
        val p = passcode.trim()
        val validUser = u.equals("admin", ignoreCase = true) ||
                u.equals("admin@paperguru.edu.pk", ignoreCase = true) ||
                u.equals("administrator", ignoreCase = true) ||
                u.equals("guru", ignoreCase = true)
        val validPass = p == "1234" || p == "admin123" || p == "admin" || p == "786"

        if (validUser && validPass) {
            _uiState.update {
                it.copy(
                    isAdminMode = true,
                    adminErrorMessage = null,
                    adminPasswordInput = "",
                    currentScreen = AppScreen.ADMIN_PORTAL,
                    snackBarMessage = "خوش آمدید ایڈمنسٹریٹر! Admin Portal Open."
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    adminErrorMessage = "درست یوزرنیم اور پاس ورڈ درج کریں! (Default: admin / 1234)"
                )
            }
        }
    }

    fun openAdminPortalScreen() {
        navigateTo(AppScreen.ADMIN_PORTAL)
    }

    fun exitAdminMode() {
        _uiState.update {
            it.copy(
                isAdminMode = false,
                currentScreen = AppScreen.HOME,
                snackBarMessage = "طالب علم پورٹل فعال ہے (Student Mode)"
            )
        }
    }

    fun setAdminSelectedTab(tab: AdminTab) {
        _uiState.update { it.copy(adminSelectedTab = tab) }
    }

    fun updateGitHubRepoInput(repo: String) {
        _uiState.update { it.copy(gitHubRepoInput = repo) }
    }

    fun syncFromGitHubAdmin(context: Context) {
        val repo = _uiState.value.gitHubRepoInput.trim()
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGitHubSyncing = true,
                    gitHubSyncMessage = "GitHub ریلیزز سے رابطہ ہو رہا ہے..."
                )
            }
            val res = PaperRepository.syncFromGitHub(context, repo)
            if (res.isSuccess) {
                val count = res.getOrNull() ?: 0
                val updatedPapers = PaperRepository.getAllDynamicPapers(context)
                _uiState.update {
                    it.copy(
                        isGitHubSyncing = false,
                        dynamicUploadedPapers = updatedPapers,
                        gitHubSyncMessage = "کامیابی! $count پرچے GitHub Releases سے شامل ہو گئے ہیں۔",
                        snackBarMessage = "Synced $count papers from GitHub Releases!"
                    )
                }
            } else {
                val err = res.exceptionOrNull()?.localizedMessage ?: "Sync Error"
                _uiState.update {
                    it.copy(
                        isGitHubSyncing = false,
                        gitHubSyncMessage = "Sync Error: $err"
                    )
                }
            }
            executeSearch(context)
        }
    }

    fun syncFromFirestoreAdmin(context: Context) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isFirestoreSyncing = true,
                    firestoreSyncMessage = "Firebase Firestore سے ڈیٹا سنک ہو رہا ہے..."
                )
            }
            try {
                FirebaseFirestoreService.initialize(context)
                val remoteStudents = FirebaseFirestoreService.fetchStudents(context).getOrNull()
                if (!remoteStudents.isNullOrEmpty()) {
                    remoteStudents.forEach { PaperRepository.saveOrUpdateStudent(context, it) }
                }
                val remotePapers = FirebaseFirestoreService.fetchPaperUploads(context).getOrNull()
                if (!remotePapers.isNullOrEmpty()) {
                    remotePapers.forEach { PaperRepository.saveDynamicPaper(context, it) }
                }
                val remoteNotifs = FirebaseFirestoreService.fetchNotifications(context).getOrNull()
                if (!remoteNotifs.isNullOrEmpty()) {
                    remoteNotifs.forEach { PaperRepository.addAdminNotification(context, it) }
                }

                val updatedStudents = PaperRepository.getStudents(context)
                val updatedPapers = PaperRepository.getAllDynamicPapers(context)
                val updatedNotifs = PaperRepository.getAdminNotifications(context)

                _uiState.update {
                    it.copy(
                        isFirestoreSyncing = false,
                        firestoreStatus = FirestoreSyncStatus.CONNECTED,
                        studentsList = updatedStudents,
                        dynamicUploadedPapers = updatedPapers,
                        adminNotifications = updatedNotifs,
                        firestoreSyncMessage = "کامیابی! Firestore کلاؤڈ سے سنک مکمل ہو گیا۔ (${updatedStudents.size} طلباء، ${updatedPapers.size} پرچے)",
                        snackBarMessage = "Firebase Firestore synchronized! (${updatedStudents.size} students)"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isFirestoreSyncing = false,
                        firestoreStatus = FirestoreSyncStatus.OFFLINE_PERSISTENT,
                        firestoreSyncMessage = "Offline Persistent Mode: مقامی ڈسک کیش فعال ہے۔ کلاؤڈ سرور دستیاب ہونے پر خودکار سنک ہو گا۔"
                    )
                }
            }
            executeSearch(context)
        }
    }

    fun uploadPaperAdmin(
        context: Context,
        classId: String,
        subjectName: String,
        year: String,
        board: String,
        url: String
    ) {
        val paper = PaperItem(
            classFolderName = classId,
            subjectName = subjectName,
            year = year,
            board = board,
            remoteUrl = url.trim(),
            isAvailable = true,
            isRemote = true
        )
        PaperRepository.saveDynamicPaper(context, paper)
        val updatedPapers = PaperRepository.getAllDynamicPapers(context)

        // Add real notification to Admin
        val notif = AdminNotification(
            id = "notif_${System.currentTimeMillis()}",
            title = "نیا پرچہ کامیابی سے اپلوڈ ہو گیا 📄",
            message = "$subjectName ($classId) سال $year بورڈ $board کا پرچہ کامیابی سے اپلوڈ ہو چکا ہے۔",
            timestamp = System.currentTimeMillis(),
            type = "PAPER_UPLOAD"
        )
        PaperRepository.addAdminNotification(context, notif)
        val updatedNotifs = PaperRepository.getAdminNotifications(context)

        _uiState.update {
            it.copy(
                dynamicUploadedPapers = updatedPapers,
                adminNotifications = updatedNotifs,
                snackBarMessage = "Paper added successfully! ($subjectName $year $board)"
            )
        }
        executeSearch(context)
    }

    fun deleteUploadedPaperAdmin(context: Context, paperKey: String) {
        PaperRepository.deleteDynamicPaper(context, paperKey)
        val updated = PaperRepository.getAllDynamicPapers(context)
        _uiState.update {
            it.copy(
                dynamicUploadedPapers = updated,
                snackBarMessage = "Paper deleted successfully."
            )
        }
        executeSearch(context)
    }

    // --- Student Directory Operations (Admin Portal) ---
    fun blockStudentAdmin(context: Context, studentId: String, isBlocked: Boolean) {
        val updatedStudents = PaperRepository.updateStudentBlockStatus(context, studentId, isBlocked)
        val updatedNotifs = PaperRepository.getAdminNotifications(context)

        val currentStd = _uiState.value.currentStudent
        val updatedCurrent = if (currentStd?.id == studentId) currentStd.copy(isBlocked = isBlocked) else currentStd
        val isCurrentBlocked = updatedCurrent?.isBlocked ?: false

        _uiState.update {
            it.copy(
                studentsList = updatedStudents,
                adminNotifications = updatedNotifs,
                currentStudent = updatedCurrent,
                isStudentBlocked = isCurrentBlocked,
                snackBarMessage = if (isBlocked) "طالب علم کو بلاک کر دیا گیا ہے 🚫" else "طالب علم بحال کر دیا گیا ہے ✅"
            )
        }
    }

    fun grantAdvanceSubscriptionAdmin(
        context: Context,
        studentId: String,
        planName: String = "Advance VIP Pass",
        expiryDate: String = "2025-06-30"
    ) {
        val updatedStudents = PaperRepository.grantAdvanceSubscription(context, studentId, planName, expiryDate)
        val updatedNotifs = PaperRepository.getAdminNotifications(context)

        val currentStd = _uiState.value.currentStudent
        val updatedCurrent = if (currentStd?.id == studentId) {
            currentStd.copy(
                hasAdvanceSubscription = true,
                subscriptionPlan = planName,
                subscriptionExpiry = expiryDate,
                feePaymentStatus = "PAID_VERIFIED"
            )
        } else currentStd

        _uiState.update {
            it.copy(
                studentsList = updatedStudents,
                adminNotifications = updatedNotifs,
                currentStudent = updatedCurrent,
                isPaidMember = updatedCurrent?.hasAdvanceSubscription ?: it.isPaidMember,
                snackBarMessage = "ایڈوانس سبسکرپشن کامیابی سے الاٹ کر دی گئی ہے ⭐"
            )
        }
    }

    fun revokeAdvanceSubscriptionAdmin(context: Context, studentId: String) {
        val updatedStudents = PaperRepository.revokeAdvanceSubscription(context, studentId)
        val currentStd = _uiState.value.currentStudent
        val updatedCurrent = if (currentStd?.id == studentId) {
            currentStd.copy(
                hasAdvanceSubscription = false,
                subscriptionPlan = null,
                subscriptionExpiry = null,
                feePaymentStatus = "UNPAID"
            )
        } else currentStd

        _uiState.update {
            it.copy(
                studentsList = updatedStudents,
                currentStudent = updatedCurrent,
                isPaidMember = updatedCurrent?.hasAdvanceSubscription ?: false,
                snackBarMessage = "سبسکرپشن منسوخ کر دی گئی ہے۔"
            )
        }
    }

    fun setStudentSearchQuery(query: String) {
        _uiState.update { it.copy(studentSearchQuery = query) }
    }

    fun setStudentFilterStatus(status: String) {
        _uiState.update { it.copy(studentFilterStatus = status) }
    }

    // --- Admin Alerts & Notifications ---
    fun markNotificationRead(context: Context, notificationId: String) {
        PaperRepository.markNotificationRead(context, notificationId)
        val updated = PaperRepository.getAdminNotifications(context)
        _uiState.update { it.copy(adminNotifications = updated) }
    }

    fun clearAllNotifications(context: Context) {
        PaperRepository.clearAllNotifications(context)
        _uiState.update { it.copy(adminNotifications = emptyList()) }
    }

    // --- Student Profile & Fee Payment by Student ---
    fun registerOrUpdateStudent(
        context: Context,
        fullName: String,
        age: Int,
        email: String,
        telephone: String,
        cnic: String?,
        selectedClass: String,
        selectedBoard: String
    ) {
        val current = _uiState.value.currentStudent
        val id = current?.id ?: "std_${System.currentTimeMillis()}"
        val updatedStudent = StudentUser(
            id = id,
            fullName = fullName.trim(),
            age = age,
            email = email.trim(),
            telephone = telephone.trim(),
            cnic = cnic?.trim()?.ifEmpty { null },
            selectedClass = selectedClass,
            selectedBoard = selectedBoard,
            isBlocked = current?.isBlocked ?: false,
            hasAdvanceSubscription = current?.hasAdvanceSubscription ?: false,
            subscriptionPlan = current?.subscriptionPlan,
            subscriptionExpiry = current?.subscriptionExpiry,
            feePaymentStatus = current?.feePaymentStatus ?: "UNPAID",
            feeTransactionRef = current?.feeTransactionRef,
            feeAmount = current?.feeAmount ?: 0,
            registeredAt = current?.registeredAt ?: "2024-09-29"
        )
        val students = PaperRepository.saveOrUpdateStudent(context, updatedStudent)

        // Notification to admin of student registration / profile update
        val cnicText = if (updatedStudent.cnic != null) ", شناختی کارڈ: ${updatedStudent.cnic}" else ""
        val notif = AdminNotification(
            id = "notif_${System.currentTimeMillis()}",
            title = "طالب علم پروفائل معلومات",
            message = "${updatedStudent.fullName} (عمر: ${updatedStudent.age}, ای میل: ${updatedStudent.email}, فون: ${updatedStudent.telephone}$cnicText) نے پروفائل اپڈیٹ کی ہے۔",
            timestamp = System.currentTimeMillis(),
            type = "NEW_STUDENT",
            studentId = updatedStudent.id,
            studentEmail = updatedStudent.email
        )
        PaperRepository.addAdminNotification(context, notif)
        val notifs = PaperRepository.getAdminNotifications(context)

        logActivity(
            eventType = ActivityEventType.PROFILE_UPDATE,
            sectionName = "Profile: ${updatedStudent.fullName}",
            details = "Student profile saved: ${updatedStudent.age} yrs, ${updatedStudent.telephone} ($selectedClass Class, $selectedBoard Board)",
            classLevel = selectedClass,
            board = selectedBoard
        )

        _uiState.update {
            it.copy(
                currentStudent = updatedStudent,
                studentsList = students,
                adminNotifications = notifs,
                currentUserEmail = updatedStudent.email,
                currentUserId = updatedStudent.id,
                isStudentBlocked = updatedStudent.isBlocked,
                isPaidMember = updatedStudent.hasAdvanceSubscription,
                showAuthDialog = false,
                snackBarMessage = "پروفائل کامیابی سے محفوظ ہو گئی ہے ✅"
            )
        }
    }

    fun submitStudentFeeProof(
        context: Context,
        amount: Int,
        trxRef: String
    ) {
        val current = _uiState.value.currentStudent ?: return
        val updatedStudents = PaperRepository.recordFeePayment(context, current.id, amount, trxRef)
        val updatedNotifs = PaperRepository.getAdminNotifications(context)
        val updatedStudent = updatedStudents.firstOrNull { it.id == current.id } ?: current.copy(
            feePaymentStatus = "PENDING_VERIFICATION",
            feeTransactionRef = trxRef,
            feeAmount = amount
        )

        logActivity(
            eventType = ActivityEventType.FEE_SUBMISSION,
            sectionName = "Fee: Rs. $amount",
            details = "Payment verification proof submitted (Trx: $trxRef) for ${current.fullName}"
        )

        _uiState.update {
            it.copy(
                studentsList = updatedStudents,
                adminNotifications = updatedNotifs,
                currentStudent = updatedStudent,
                showFeePaymentDialog = false,
                snackBarMessage = "فیس ادائیگی کی تصدیق کی درخواست ایڈمن کو بھیج دی گئی ہے۔ جلد ایڈوانس سبسکرپشن فعال ہو جائے گی 💰"
            )
        }
    }

    // --- Interlinked Portal Operations (Admin <-> Student) ---

    fun jumpToPaperFromTracking(
        context: Context,
        classId: String,
        subjectName: String,
        year: String,
        board: String
    ) {
        if (PaperRepository.isClassSuspended(classId)) {
            _uiState.update {
                it.copy(
                    snackBarMessage = "$classId class past papers are temporarily withheld due to rumors review ⚠️"
                )
            }
            return
        }

        val classLevel = availableClasses.firstOrNull { it.folderName.equals(classId, ignoreCase = true) }
            ?: availableClasses.first()
        val subject = availableSubjects.firstOrNull { it.name.equals(subjectName, ignoreCase = true) }
            ?: availableSubjects.first()

        openPaperDirectly(context, classLevel, subject, year, board)
        _uiState.update {
            it.copy(
                snackBarMessage = "Viewing ${subject.name} $year ($board) in Student Portal"
            )
        }
    }

    fun switchStudentPersona(student: StudentUser) {
        _uiState.update {
            it.copy(
                currentStudent = student,
                currentUserEmail = student.email,
                currentUserId = student.id,
                isStudentBlocked = student.isBlocked,
                isPaidMember = student.hasAdvanceSubscription,
                snackBarMessage = "Switched to student view: ${student.fullName}"
            )
        }
        logActivity(
            eventType = ActivityEventType.LOGIN,
            sectionName = "Persona Switch",
            details = "Admin switched persona to ${student.fullName} (${student.selectedClass}, ${student.selectedBoard})"
        )
    }

    fun publishAnnouncementAdmin(
        context: Context,
        title: String,
        message: String,
        targetClass: String = "ALL",
        priority: String = "NORMAL"
    ) {
        val ann = AdminAnnouncement(
            id = "ann_${System.currentTimeMillis()}",
            title = title.trim(),
            message = message.trim(),
            targetClass = targetClass,
            timestamp = System.currentTimeMillis(),
            isActive = true,
            priority = priority
        )
        PaperRepository.publishAnnouncement(context, ann)
        val updated = PaperRepository.getAnnouncements(context)
        _uiState.update {
            it.copy(
                announcements = updated,
                activeAnnouncement = updated.firstOrNull { a -> a.isActive },
                snackBarMessage = "Announcement published across Student Portal! 📢"
            )
        }
    }

    fun dismissActiveAnnouncement() {
        _uiState.update { it.copy(activeAnnouncement = null) }
    }

    fun clearActivitiesAdmin(context: Context) {
        PaperRepository.clearTrackedActivities(context)
        _uiState.update { it.copy(trackedActivities = emptyList()) }
    }

    fun setActivityFilterType(type: ActivityEventType?) {
        _uiState.update { it.copy(activityFilterType = type) }
    }

    fun openFeePaymentDialog() {
        _uiState.update { it.copy(showFeePaymentDialog = true) }
    }

    fun closeFeePaymentDialog() {
        _uiState.update { it.copy(showFeePaymentDialog = false) }
    }

    fun closeBlockedDialog() {
        _uiState.update { it.copy(showBlockedDialog = false) }
    }

    fun dismissSnackBar() {
        _uiState.update { it.copy(snackBarMessage = null) }
    }
}
