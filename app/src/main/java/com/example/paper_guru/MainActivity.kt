package com.example.paper_guru

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.paper_guru.ui.AppScreen
import com.example.paper_guru.ui.PaperViewModel
import com.example.paper_guru.ui.screens.HomeScreen
import com.example.paper_guru.ui.screens.PaperDetailScreen
import com.example.paper_guru.ui.screens.PdfViewerScreen
import com.example.paper_guru.ui.screens.SubjectScreen
import com.example.paper_guru.ui.screens.YearsScreen
import com.example.paper_guru.ui.theme.PaperGuruTheme

class MainActivity : ComponentActivity() {

    private val viewModel: PaperViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PaperGuruTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PaperGuruApp(viewModel = viewModel)
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
                onSelectClass = { viewModel.selectClass(it) }
            )
        }
        AppScreen.SUBJECTS -> {
            val selectedClass = state.selectedClass ?: return
            SubjectScreen(
                classLevel = selectedClass,
                subjects = viewModel.availableSubjects,
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
                onSelectYear = { viewModel.selectYear(context, it) },
                onBack = { viewModel.navigateBack() }
            )
        }
        AppScreen.PAPER_DETAIL -> {
            val selectedClass = state.selectedClass ?: return
            val selectedSubject = state.selectedSubject ?: return
            val selectedYear = state.selectedYear ?: return
            PaperDetailScreen(
                classLevel = selectedClass,
                subject = selectedSubject,
                year = selectedYear,
                onViewPaper = { viewModel.viewPaper(context) },
                onDownloadPaper = { viewModel.onDownloadClicked() },
                snackBarMessage = state.snackBarMessage,
                onDismissSnackBar = { viewModel.dismissSnackBar() },
                onBack = { viewModel.navigateBack() }
            )
        }
        AppScreen.PDF_VIEWER -> {
            val selectedSubject = state.selectedSubject ?: return
            val selectedYear = state.selectedYear ?: return
            PdfViewerScreen(
                subject = selectedSubject,
                year = selectedYear,
                isLoading = state.isPdfLoading,
                bitmaps = state.pdfBitmaps,
                errorMessage = state.pdfError,
                onBack = { viewModel.navigateBack() }
            )
        }
    }
}
