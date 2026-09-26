package com.example.paper_guru.model

data class ClassLevel(
    val id: String,
    val displayName: String,
    val folderName: String,
    val description: String
)

data class SubjectItem(
    val id: String,
    val name: String,
    val urduSubtitle: String = "Past Papers دیکھنے کے لیے click کریں"
)

data class PaperItem(
    val classFolderName: String,
    val subjectName: String,
    val year: String,
    val assetPath: String?,
    val isAvailable: Boolean
)
