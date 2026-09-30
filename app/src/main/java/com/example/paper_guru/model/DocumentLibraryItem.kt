package com.example.paper_guru.model

data class DocumentLibraryItem(
    val id: String,
    val title: String,
    val subject: String,
    val classLevel: String,
    val classFolderName: String,
    val year: String,
    val board: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val formattedSize: String,
    val downloadUrl: String?,
    val assetPath: String? = null,
    val releaseVersion: String = "v1.0.0",
    val publishedDate: String = "",
    val isOfflineAvailable: Boolean = false,
    val isBookmarked: Boolean = false,
    val description: String = ""
)
