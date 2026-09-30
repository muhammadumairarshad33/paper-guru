package com.example.paper_guru

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paper_guru.model.AppScreen
import com.example.paper_guru.ui.PaperViewModel
import com.example.paper_guru.ui.screens.*
import com.example.paper_guru.ui.theme.PaperGuruTheme

class MainActivity : ComponentActivity() {

    private val viewModel: PaperViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            PaperGuruTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PaperGuruApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun PaperGuruApp(viewModel: PaperViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    when (state.currentScreen) {
        AppScreen.HOME -> {
            HomeScreen(
                classes = viewModel.availableClasses,
                subjects = viewModel.availableSubjects,
                boards = viewModel.availableBoards,
                onSelectClass = { viewModel.selectClass(it) },
                searchQuery = state.homeSearchQuery,
                onSearchQueryChange = { viewModel.onHomeSearchChanged(context, it) },
                selectedClassFilter = state.homeClassFilter,
                onClassFilterChange = { viewModel.setHomeClassFilter(context, it) },
                selectedCategoryFilter = state.homeCategoryFilter,
                onCategoryFilterChange = { viewModel.setHomeCategoryFilter(context, it) },
                selectedBoardFilter = state.homeBoardFilter,
                onBoardFilterChange = { viewModel.setHomeBoardFilter(context, it) },
                showBookmarksOnly = state.showBookmarksOnly,
                onToggleBookmarksOnly = { viewModel.toggleBookmarksFilter(context) },
                searchResults = state.homeSearchResults,
                onSelectSearchResult = { cls, sub, yr, brd ->
                    viewModel.openPaperDirectly(context, cls, sub, yr, brd)
                },
                onToggleBookmarkResult = { key -> viewModel.toggleBookmark(context, key) },
                onClearSearch = { viewModel.clearHomeSearch(context) },
                isPaidMember = state.isPaidMember,
                onOpenPaywall = { viewModel.openPaywallDialog("PRO Membership") },
                currentUserEmail = state.currentUserEmail,
                onOpenAuthDialog = { viewModel.openAuthDialog() },
                isAdminMode = state.isAdminMode,
                unreadAdminNotifsCount = state.unreadNotificationsCount,
                trackedEventsCount = state.trackedActivities.size,
                isStudentBlocked = state.isStudentBlocked,
                activeAnnouncement = state.activeAnnouncement,
                onDismissAnnouncement = { viewModel.dismissActiveAnnouncement() },
                onOpenAdminDialog = { viewModel.openAdminDialog() },
                onOpenAdminPortal = { viewModel.openAdminPortalScreen() },
                onOpenAdminLogin = { viewModel.openAdminLoginScreen() },
                onExitAdminMode = { viewModel.exitAdminMode() }
            )
        }

        AppScreen.SUBJECTS -> {
            val selectedClass = state.selectedClass ?: return
            SubjectScreen(
                classLevel = selectedClass,
                subjects = viewModel.availableSubjects,
                searchQuery = state.subjectSearchQuery,
                onSearchQueryChange = { viewModel.setSubjectSearchQuery(it) },
                selectedCategory = state.subjectCategoryFilter,
                onCategoryChange = { viewModel.setSubjectCategoryFilter(it) },
                onSelectSubject = { viewModel.selectSubject(it) },
                onBack = { viewModel.navigateBack() }
            )
        }

        AppScreen.YEARS -> {
            val selectedClass = state.selectedClass ?: return
            val selectedSubject = state.selectedSubject ?: return
            YearsScreen(
                classLevel = selectedClass,
                subject = selectedSubject,
                years = viewModel.availableYears,
                selectedBoard = state.yearBoardFilter,
                onBoardChange = { viewModel.setYearBoardFilter(it) },
                onSelectYear = { yr, brd -> viewModel.selectYear(context, yr, brd) },
                onBack = { viewModel.navigateBack() }
            )
        }

        AppScreen.PAPER_DETAIL -> {
            val selectedClass = state.selectedClass ?: return
            val selectedSubject = state.selectedSubject ?: return
            val selectedYear = state.selectedYear ?: return
            val paper = state.currentPaper
            PaperDetailScreen(
                classLevel = selectedClass,
                subject = selectedSubject,
                year = selectedYear,
                board = paper?.board ?: "LHR",
                isBookmarked = paper?.isBookmarked ?: false,
                isAvailable = paper?.isAvailable ?: false,
                isPaidMember = state.isPaidMember,
                isDownloading = state.isDownloading,
                downloadedFile = state.lastDownloadedFile,
                onToggleBookmark = {
                    paper?.let { viewModel.toggleBookmark(context, it.uniqueKey) }
                },
                onViewPaper = { viewModel.viewPaper(context) },
                onDownloadPaper = { viewModel.downloadCurrentPaper(context) },
                onViewSolution = {
                    if (state.isPaidMember) {
                        viewModel.openSolutionScreen()
                    } else {
                        viewModel.openPaywallDialog("Past Paper Solution")
                    }
                },
                onViewExamInspiration = {
                    if (state.isPaidMember) {
                        viewModel.openExamInspirationScreen()
                    } else {
                        viewModel.openPaywallDialog("Exam Tips")
                    }
                },
                onOpenDownloaded = { viewModel.openDownloadedPaper(context) },
                onShareDownloaded = { viewModel.shareDownloadedPaper(context) },
                snackBarMessage = state.snackBarMessage,
                onDismissSnackBar = { viewModel.dismissSnackBar() },
                onBack = { viewModel.navigateBack() }
            )
        }

        AppScreen.PDF_VIEWER -> {
            val selectedSubject = state.selectedSubject ?: return
            val selectedYear = state.selectedYear ?: return
            val paper = state.currentPaper
            PdfViewerScreen(
                subject = selectedSubject,
                year = selectedYear,
                isLoading = state.isPdfLoading,
                bitmaps = state.pdfBitmaps,
                errorMessage = state.pdfError,
                isBookmarked = paper?.isBookmarked ?: false,
                onToggleBookmark = {
                    paper?.let { viewModel.toggleBookmark(context, it.uniqueKey) }
                },
                onDownload = { viewModel.downloadCurrentPaper(context) },
                onOpenAdminLogin = { viewModel.openAdminDialog() },
                onBack = { viewModel.navigateBack() }
            )
        }

        AppScreen.SOLUTION_VIEWER -> {
            val selectedClass = state.selectedClass ?: return
            val selectedSubject = state.selectedSubject ?: return
            val selectedYear = state.selectedYear ?: return
            val paper = state.currentPaper
            SolutionViewerScreen(
                classLevel = selectedClass,
                subject = selectedSubject,
                year = selectedYear,
                board = paper?.board ?: "LHR",
                isPaidMember = state.isPaidMember,
                onOpenPaywall = { viewModel.openPaywallDialog("Past Paper Solution") },
                onBack = { viewModel.navigateBack() }
            )
        }

        AppScreen.EXAM_INSPIRATION -> {
            val selectedClass = state.selectedClass ?: return
            val selectedSubject = state.selectedSubject ?: return
            ExamInspirationScreen(
                classLevel = selectedClass,
                subject = selectedSubject,
                isPaidMember = state.isPaidMember,
                onOpenPaywall = { viewModel.openPaywallDialog("Next Exam Inspiration") },
                onBack = { viewModel.navigateBack() }
            )
        }

        AppScreen.ADMIN_LOGIN -> {
            AdminLoginScreen(
                usernameInput = state.adminUsernameInput,
                passwordInput = state.adminPasswordInput,
                errorMessage = state.adminErrorMessage,
                onUsernameChange = { viewModel.updateAdminUsername(it) },
                onPasswordChange = { viewModel.updateAdminPassword(it) },
                onLogin = { u, p -> viewModel.loginAdmin(u, p) },
                onBack = { viewModel.navigateBack() }
            )
        }

        AppScreen.ADMIN_PORTAL -> {
            AdminPortalScreen(
                selectedTab = state.adminSelectedTab,
                onTabSelected = { viewModel.setAdminSelectedTab(it) },
                dynamicUploadedPapers = state.dynamicUploadedPapers,
                onUploadPaper = { cls, sub, yr, brd, url ->
                    viewModel.uploadPaperAdmin(context, cls, sub, yr, brd, url)
                },
                onDeleteUploadedPaper = { viewModel.deleteUploadedPaperAdmin(context, it) },
                studentsList = state.studentsList,
                searchQuery = state.studentSearchQuery,
                onSearchQueryChange = { viewModel.setStudentSearchQuery(it) },
                filterStatus = state.studentFilterStatus,
                onFilterStatusChange = { viewModel.setStudentFilterStatus(it) },
                onBlockStudent = { id, blk -> viewModel.blockStudentAdmin(context, id, blk) },
                onGrantAdvanceSubscription = { id, pln, exp ->
                    viewModel.grantAdvanceSubscriptionAdmin(context, id, pln, exp)
                },
                onRevokeAdvanceSubscription = { viewModel.revokeAdvanceSubscriptionAdmin(context, it) },
                adminNotifications = state.adminNotifications,
                unreadCount = state.unreadNotificationsCount,
                onMarkNotificationRead = { viewModel.markNotificationRead(context, it) },
                onClearAllNotifications = { viewModel.clearAllNotifications(context) },
                firestoreStatus = state.firestoreStatus,
                isFirestoreSyncing = state.isFirestoreSyncing,
                firestoreSyncMessage = state.firestoreSyncMessage,
                onSyncFirestore = { viewModel.syncFromFirestoreAdmin(context) },
                trackedActivities = state.trackedActivities,
                activityFilterType = state.activityFilterType,
                onActivityFilterChange = { viewModel.setActivityFilterType(it) },
                onClearActivities = { viewModel.clearActivitiesAdmin(context) },
                onJumpToPaperFromTracking = { cls, sub, yr, brd ->
                    viewModel.jumpToPaperFromTracking(context, cls, sub, yr, brd)
                },
                onSwitchStudentPersona = { viewModel.switchStudentPersona(it) },
                onPublishAnnouncement = { title, msg, targetCls, prio ->
                    viewModel.publishAnnouncementAdmin(context, title, msg, targetCls, prio)
                },
                onOpenStudentPortal = { viewModel.exitAdminMode() },
                isGitHubSyncing = state.isGitHubSyncing,
                gitHubRepoInput = state.gitHubRepoInput,
                gitHubSyncMessage = state.gitHubSyncMessage,
                onUpdateRepoInput = { viewModel.updateGitHubRepoInput(it) },
                onSyncGitHub = { viewModel.syncFromGitHubAdmin(context) },
                onExitAdmin = { viewModel.exitAdminMode() },
                onBack = { viewModel.navigateBack() }
            )
        }
    }

    // PRO Modal Bottom Sheet
    if (state.showPaywallDialog) {
        PremiumMembershipBottomSheet(
            triggerFeature = state.paywallTriggerFeature,
            isPaidMember = state.isPaidMember,
            onActivatePro = { promo -> viewModel.activateProMembership(promo) },
            onDismiss = { viewModel.closePaywallDialog() }
        )
    }

    // Student Profile Dialog (Age, Email, Phone, CNIC, Fee Payment)
    if (state.showAuthDialog) {
        AuthAccountDialog(
            currentStudent = state.currentStudent,
            isPaidMember = state.isPaidMember,
            onSaveProfile = { fullName, age, email, telephone, cnic, classId, board ->
                viewModel.registerOrUpdateStudent(context, fullName, age, email, telephone, cnic, classId, board)
            },
            onSubmitFeePayment = { amount, trxRef ->
                viewModel.submitStudentFeeProof(context, amount, trxRef)
            },
            onSignOut = { viewModel.signOut() },
            onOpenPaywall = { viewModel.openPaywallDialog("PRO Membership") },
            onOpenAdminLogin = { viewModel.openAdminLoginScreen() },
            onDismiss = { viewModel.closeAuthDialog() }
        )
    }

    // Blocked Student Alert Dialog
    if (state.showBlockedDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.closeBlockedDialog() },
            icon = {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "اکاؤنٹ بلاک ہے (Account Blocked)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = "ایڈمنسٹریٹر نے آپ کا اکاؤنٹ معطل کر دیا ہے۔ آپ اس وقت تک پرچے نہیں دیکھ یا ڈاؤن لوڈ نہیں کر سکتے جب تک ایڈمن آپ کا اکاؤنٹ بحال نہ کر دے۔\n\nبرائے مہربانی ایڈمن سے رابطہ کریں۔",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.closeBlockedDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("سمجھ گیا (OK)")
                }
            }
        )
    }

    // Admin / Teacher Portal PIN Dialog
    if (state.showAdminDialog) {
        AdminPortalDialog(
            isAdminMode = state.isAdminMode,
            adminPinInput = state.adminPinInput,
            adminErrorMessage = state.adminErrorMessage,
            onUpdatePin = { viewModel.updateAdminPin(it) },
            onVerifyPin = { viewModel.verifyAdminPin() },
            onExitAdminMode = { viewModel.exitAdminMode() },
            isGitHubSyncing = state.isGitHubSyncing,
            gitHubRepoInput = state.gitHubRepoInput,
            gitHubSyncMessage = state.gitHubSyncMessage,
            onUpdateRepoInput = { viewModel.updateGitHubRepoInput(it) },
            onSyncGitHub = { viewModel.syncFromGitHubAdmin(context) },
            onAddPaper = { classId, subjectName, year, board, url ->
                viewModel.uploadPaperAdmin(context, classId, subjectName, year, board, url)
            },
            onOpenFullAdminPortal = { viewModel.openAdminPortalScreen() },
            onDismiss = { viewModel.closeAdminDialog() }
        )
    }
}
