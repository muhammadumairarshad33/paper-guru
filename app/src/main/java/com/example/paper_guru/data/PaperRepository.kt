package com.example.paper_guru.data

import android.content.Context
import com.example.paper_guru.model.ClassLevel
import com.example.paper_guru.model.PaperItem
import com.example.paper_guru.model.SubjectItem
import java.io.IOException

class PaperRepository {

    val classes: List<ClassLevel> = listOf(
        ClassLevel(
            id = "9th",
            displayName = "9th Class",
            folderName = "9th",
            description = "Matriculation Part 1 Past Papers (BISE)"
        ),
        ClassLevel(
            id = "10th",
            displayName = "10th Class",
            folderName = "10th",
            description = "Matriculation Part 2 Past Papers (BISE)"
        )
    )

    val subjects: List<SubjectItem> = listOf(
        SubjectItem("english", "English"),
        SubjectItem("urdu", "Urdu"),
        SubjectItem("mathematics", "Mathematics"),
        SubjectItem("physics", "Physics"),
        SubjectItem("chemistry", "Chemistry"),
        SubjectItem("biology", "Biology"),
        SubjectItem("computer_science", "Computer Science")
    )

    val years: List<String> = listOf(
        "2024",
        "2023",
        "2022",
        "2021",
        "2020",
        "2019",
        "2018"
    )

    fun resolvePaperAsset(context: Context, folderName: String, subjectName: String, year: String): PaperItem {
        val subjectLower = subjectName.lowercase()
        val possiblePaths = listOf(
            "papers/$folderName/$subjectLower/${subjectLower}_9_lhr_$year.pdf",
            "papers/$folderName/$subjectLower/math_9_lhr_$year.pdf.pdf",
            "papers/$folderName/$subjectLower/math_9_lhr_$year.pdf",
            "papers/$folderName/$subjectLower/${subjectLower}_${folderName}_lhr_$year.pdf",
            "papers/$folderName/$subjectName/${subjectName}_$year.pdf",
            "papers/$folderName/$subjectLower/${subjectLower}_$year.pdf"
        )

        for (path in possiblePaths) {
            try {
                context.assets.open(path).use {
                    return PaperItem(
                        classFolderName = folderName,
                        subjectName = subjectName,
                        year = year,
                        assetPath = path,
                        isAvailable = true
                    )
                }
            } catch (_: IOException) {
                // Try next pattern
            }
        }

        // Also check by listing directory
        try {
            val dirPath = "papers/$folderName/$subjectLower"
            val list = context.assets.list(dirPath)
            if (list != null) {
                val matched = list.firstOrNull { it.contains(year) && it.endsWith(".pdf") }
                if (matched != null) {
                    return PaperItem(
                        classFolderName = folderName,
                        subjectName = subjectName,
                        year = year,
                        assetPath = "$dirPath/$matched",
                        isAvailable = true
                    )
                }
            }
        } catch (_: Exception) {
            // Ignore
        }

        return PaperItem(
            classFolderName = folderName,
            subjectName = subjectName,
            year = year,
            assetPath = "papers/$folderName/$subjectName/${subjectName}_$year.pdf",
            isAvailable = false
        )
    }
}
