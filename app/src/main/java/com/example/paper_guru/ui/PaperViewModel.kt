package com.example.paper_guru.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paper_guru.data.PaperRepository
import com.example.paper_guru.model.ClassLevel
import com.example.paper_guru.model.PaperItem
import com.example.paper_guru.model.SubjectItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class AppScreen {
    HOME,
    SUBJECTS,
    YEARS,
    PAPER_DETAIL,
    PDF_VIEWER
}

data class PaperUiState(
    val currentScreen: AppScreen = AppScreen.HOME,
    val selectedClass: ClassLevel? = null,
    val selectedSubject: SubjectItem? = null,
    val selectedYear: String? = null,
    val currentPaper: PaperItem? = null,
    val searchQuery: String = "",
    val snackBarMessage: String? = null,
    val isPdfLoading: Boolean = false,
    val pdfBitmaps: List<Bitmap> = emptyList(),
    val pdfError: String? = null
)

class PaperViewModel(
    private val repository: PaperRepository = PaperRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaperUiState())
    val uiState: StateFlow<PaperUiState> = _uiState.asStateFlow()

    val availableClasses = repository.classes
    val availableSubjects = repository.subjects
    val availableYears = repository.years

    fun selectClass(classLevel: ClassLevel) {
        _uiState.update {
            it.copy(
                selectedClass = classLevel,
                currentScreen = AppScreen.SUBJECTS,
                searchQuery = ""
            )
        }
    }

    fun selectSubject(subject: SubjectItem) {
        _uiState.update {
            it.copy(
                selectedSubject = subject,
                currentScreen = AppScreen.YEARS
            )
        }
    }

    fun selectYear(context: Context, year: String) {
        val currentClass = _uiState.value.selectedClass ?: return
        val currentSubject = _uiState.value.selectedSubject ?: return
        val paper = repository.resolvePaperAsset(
            context = context,
            folderName = currentClass.folderName,
            subjectName = currentSubject.name,
            year = year
        )
        _uiState.update {
            it.copy(
                selectedYear = year,
                currentPaper = paper,
                currentScreen = AppScreen.PAPER_DETAIL
            )
        }
    }

    fun viewPaper(context: Context) {
        val paper = _uiState.value.currentPaper ?: return
        _uiState.update {
            it.copy(
                currentScreen = AppScreen.PDF_VIEWER,
                isPdfLoading = true,
                pdfBitmaps = emptyList(),
                pdfError = null
            )
        }

        if (!paper.isAvailable || paper.assetPath == null) {
            _uiState.update {
                it.copy(
                    isPdfLoading = false,
                    pdfError = "یہ paper ابھی upload نہیں ہوا۔\n\nExpected file:\n${paper.assetPath ?: "N/A"}"
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                val bitmaps = renderPdfPagesFromAsset(context, paper.assetPath)
                _uiState.update {
                    it.copy(
                        isPdfLoading = false,
                        pdfBitmaps = bitmaps,
                        pdfError = if (bitmaps.isEmpty()) "Failed to render PDF pages" else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isPdfLoading = false,
                        pdfError = "Error loading PDF: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    private suspend fun renderPdfPagesFromAsset(context: Context, assetPath: String): List<Bitmap> {
        return withContext(Dispatchers.IO) {
            val bitmaps = mutableListOf<Bitmap>()
            val tempFile = File(context.cacheDir, "current_paper.pdf")
            context.assets.open(assetPath).use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            try {
                val pageCount = renderer.pageCount
                for (i in 0 until pageCount) {
                    val page = renderer.openPage(i)
                    val width = page.width * 2
                    val height = page.height * 2
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bitmaps.add(bitmap)
                }
            } finally {
                renderer.close()
                pfd.close()
            }
            bitmaps
        }
    }

    fun onDownloadClicked() {
        _uiState.update {
            it.copy(snackBarMessage = "Download feature اگلے مرحلے میں add ہو گا۔")
        }
    }

    fun dismissSnackBar() {
        _uiState.update { it.copy(snackBarMessage = null) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun navigateBack(): Boolean {
        return when (_uiState.value.currentScreen) {
            AppScreen.PDF_VIEWER -> {
                _uiState.update { it.copy(currentScreen = AppScreen.PAPER_DETAIL) }
                true
            }
            AppScreen.PAPER_DETAIL -> {
                _uiState.update { it.copy(currentScreen = AppScreen.YEARS) }
                true
            }
            AppScreen.YEARS -> {
                _uiState.update { it.copy(currentScreen = AppScreen.SUBJECTS) }
                true
            }
            AppScreen.SUBJECTS -> {
                _uiState.update { it.copy(currentScreen = AppScreen.HOME) }
                true
            }
            AppScreen.HOME -> false
        }
    }
}
