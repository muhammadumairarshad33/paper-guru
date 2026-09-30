package com.example.paper_guru.data

import android.content.Context
import android.os.Environment
import com.example.paper_guru.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

object PaperRepository {
    private val repoScope = CoroutineScope(Dispatchers.IO)
    private val dynamicPapers = mutableListOf<PaperItem>()
    private var prefsLoaded = false

    val classes: List<ClassLevel> = ACTIVE_CLASSES
    val subjects: List<SubjectItem> = ALL_SUBJECTS
    val years: List<String> = listOf("2024", "2023", "2022", "2021", "2019", "2018")
    val boards: List<EducationalBoard> = ALL_BOARDS

    fun isClassSuspended(classFolderName: String): Boolean = SUSPENDED_CLASS_IDS.contains(classFolderName)

    private fun ensurePrefsLoaded(context: Context) {
        if (prefsLoaded) return
        val prefs = context.getSharedPreferences("paper_guru_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("dynamic_papers", null)
        if (!json.isNullOrEmpty()) {
            try {
                val array = JSONArray(json)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val paper = PaperItem(
                        classFolderName = obj.getString("classFolderName"),
                        subjectName = obj.getString("subjectName"),
                        year = obj.getString("year"),
                        board = obj.optString("board", "LHR"),
                        remoteUrl = obj.optString("remoteUrl", ""),
                        isAvailable = true,
                        isRemote = true
                    )
                    // Skip 9th and 10th past papers for the time being
                    if (isClassSuspended(paper.classFolderName)) continue

                    if (dynamicPapers.none { it.uniqueKey == paper.uniqueKey }) {
                        dynamicPapers.add(paper)
                    }
                }
            } catch (_: Exception) {}
        }
        // Ensure no suspended 9th or 10th papers remain in active memory
        dynamicPapers.removeAll { isClassSuspended(it.classFolderName) }
        prefsLoaded = true
    }

    fun getFavoriteKeys(context: Context): Set<String> {
        val prefs = context.getSharedPreferences("paper_guru_bookmarks", Context.MODE_PRIVATE)
        return prefs.getStringSet("bookmarked_papers", emptySet()) ?: emptySet()
    }

    fun isBookmarked(context: Context, key: String): Boolean {
        return getFavoriteKeys(context).contains(key)
    }

    fun toggleBookmark(context: Context, key: String): Boolean {
        val prefs = context.getSharedPreferences("paper_guru_bookmarks", Context.MODE_PRIVATE)
        val currentSet = prefs.getStringSet("bookmarked_papers", emptySet())?.toMutableSet() ?: mutableSetOf()
        val newState = if (currentSet.contains(key)) {
            currentSet.remove(key)
            false
        } else {
            currentSet.add(key)
            true
        }
        prefs.edit().putStringSet("bookmarked_papers", currentSet).apply()
        return newState
    }

    fun saveDynamicPaper(context: Context, paper: PaperItem) {
        // Class 9th and 10th are temporarily restricted due to paper rumors
        if (isClassSuspended(paper.classFolderName)) return

        ensurePrefsLoaded(context)
        dynamicPapers.removeAll {
            it.classFolderName == paper.classFolderName &&
            it.subjectName.equals(paper.subjectName, ignoreCase = true) &&
            it.board.equals(paper.board, ignoreCase = true) &&
            it.year == paper.year
        }
        dynamicPapers.add(paper)

        val prefs = context.getSharedPreferences("paper_guru_prefs", Context.MODE_PRIVATE)
        val array = JSONArray()
        for (item in dynamicPapers) {
            val obj = JSONObject()
            obj.put("classFolderName", item.classFolderName)
            obj.put("subjectName", item.subjectName)
            obj.put("year", item.year)
            obj.put("board", item.board)
            obj.put("remoteUrl", item.remoteUrl)
            array.put(obj)
        }
        prefs.edit().putString("dynamic_papers", array.toString()).apply()

        // Sync paper upload to Firebase Firestore
        repoScope.launch {
            try {
                FirebaseFirestoreService.uploadPaper(context, paper)
            } catch (_: Exception) {}
        }
    }

    fun resolvePaperAsset(
        context: Context,
        folderName: String,
        subjectName: String,
        year: String,
        board: String = "LHR"
    ): PaperItem {
        // If 9th or 10th class is requested, mark unavailable due to rumors review
        if (isClassSuspended(folderName)) {
            return PaperItem(
                classFolderName = folderName,
                subjectName = subjectName,
                year = year,
                board = board,
                assetPath = null,
                remoteUrl = null,
                isAvailable = false,
                isRemote = false
            )
        }

        ensurePrefsLoaded(context)

        // 1. Dynamic / remote match exact board
        var dynamic = dynamicPapers.firstOrNull {
            it.classFolderName.equals(folderName, ignoreCase = true) &&
            it.subjectName.equals(subjectName, ignoreCase = true) &&
            it.year == year &&
            it.board.equals(board, ignoreCase = true)
        }

        // 2. Dynamic / remote match any board
        if (dynamic == null) {
            dynamic = dynamicPapers.firstOrNull {
                it.classFolderName.equals(folderName, ignoreCase = true) &&
                it.subjectName.equals(subjectName, ignoreCase = true) &&
                it.year == year
            }
        }

        if (dynamic != null) {
            return dynamic.copy(isBookmarked = isBookmarked(context, dynamic.uniqueKey))
        }

        // 3. Local APK asset checking
        val subClean = subjectName.lowercase().replace(" ", "_")
        val possibleAssetPaths = listOf(
            "papers/$folderName/$subClean/${subClean}_${folderName.lowercase()}_${board.lowercase()}_$year.pdf",
            "papers/$folderName/$subClean/${subClean}_${folderName.lowercase()}_lhr_$year.pdf",
            "papers/$folderName/$subClean/${subClean}_${folderName.lowercase()}_$year.pdf",
            "papers/$folderName/$subClean/${subClean}_$year.pdf"
        )

        for (path in possibleAssetPaths) {
            try {
                context.assets.open(path).use {
                    val paper = PaperItem(
                        classFolderName = folderName,
                        subjectName = subjectName,
                        year = year,
                        board = board,
                        assetPath = path,
                        isAvailable = true,
                        isRemote = false
                    )
                    return paper.copy(isBookmarked = isBookmarked(context, paper.uniqueKey))
                }
            } catch (_: IOException) {}
        }

        // 4. Directory scan of assets
        try {
            val dirPath = "papers/$folderName/$subClean"
            val list = context.assets.list(dirPath)
            if (!list.isNullOrEmpty()) {
                val matched = list.firstOrNull { it.contains(year) && it.endsWith(".pdf", ignoreCase = true) }
                if (matched != null) {
                    val paper = PaperItem(
                        classFolderName = folderName,
                        subjectName = subjectName,
                        year = year,
                        board = board,
                        assetPath = "$dirPath/$matched",
                        isAvailable = true,
                        isRemote = false
                    )
                    return paper.copy(isBookmarked = isBookmarked(context, paper.uniqueKey))
                }
            }
        } catch (_: Exception) {}

        val fallback = PaperItem(
            classFolderName = folderName,
            subjectName = subjectName,
            year = year,
            board = board,
            assetPath = null,
            isAvailable = false,
            isRemote = false
        )
        return fallback.copy(isBookmarked = isBookmarked(context, fallback.uniqueKey))
    }

    suspend fun syncFromGitHub(context: Context, repo: String): Result<Int> = withContext(Dispatchers.IO) {
        val cleanRepo = repo.trim().ifEmpty { GitHubReleaseManager.DEFAULT_REPO }
        GitHubReleaseManager.saveRepo(context, cleanRepo)

        val releasesRes = GitHubReleaseManager.fetchReleases(cleanRepo)
        if (releasesRes.isFailure) {
            return@withContext Result.failure(releasesRes.exceptionOrNull() ?: Exception("Failed to fetch releases"))
        }

        val releases = releasesRes.getOrNull() ?: emptyList()
        var addedCount = 0

        for (release in releases) {
            for (asset in release.assets) {
                val paper = GitHubReleaseManager.parseFilenameToPaper(asset.name, asset.downloadUrl)
                if (paper != null) {
                    saveDynamicPaper(context, paper)
                    addedCount++
                }
            }
            for (link in release.bodyLinks) {
                val fileName = link.substringAfterLast("/")
                val paper = GitHubReleaseManager.parseFilenameToPaper(fileName, link)
                if (paper != null) {
                    saveDynamicPaper(context, paper)
                    addedCount++
                }
            }
        }

        Result.success(addedCount)
    }

    fun searchPapers(
        context: Context,
        query: String,
        classFilter: String? = null,
        categoryFilter: SubjectCategory? = null,
        yearFilter: String? = null,
        boardFilter: String? = null,
        onlyBookmarked: Boolean = false
    ): List<SearchPaperResult> {
        // If class filter specifies 9th or 10th, return empty because they are withheld
        if (classFilter != null && isClassSuspended(classFilter)) {
            return emptyList()
        }

        ensurePrefsLoaded(context)
        val cleanQuery = query.trim().lowercase()
        val resultsMap = mutableMapOf<String, SearchPaperResult>()
        val bookmarks = getFavoriteKeys(context)

        val targetClasses = if (classFilter.isNullOrBlank() || classFilter == "ALL") {
            classes.filter { !isClassSuspended(it.id) }
        } else {
            classes.filter { it.id.equals(classFilter, ignoreCase = true) && !isClassSuspended(it.id) }
        }

        val targetSubjects = if (categoryFilter == null || categoryFilter == SubjectCategory.ALL) {
            subjects
        } else {
            subjects.filter { it.category == categoryFilter }
        }

        val targetYears = if (yearFilter.isNullOrBlank() || yearFilter == "ALL") {
            years
        } else {
            listOf(yearFilter)
        }

        for (cls in targetClasses) {
            for (sub in targetSubjects) {
                for (yr in targetYears) {
                    val key = "${cls.id}_${sub.name}_${yr}"
                    val isQueryMatch = cleanQuery.isEmpty() ||
                            sub.name.lowercase().contains(cleanQuery) ||
                            sub.urduName.contains(cleanQuery) ||
                            cls.displayName.lowercase().contains(cleanQuery) ||
                            yr.contains(cleanQuery)

                    if (!isQueryMatch) continue

                    val paper = resolvePaperAsset(context, cls.folderName, sub.name, yr, boardFilter ?: "LHR")
                    if (onlyBookmarked && !bookmarks.contains(paper.uniqueKey)) continue

                    resultsMap[key] = SearchPaperResult(
                        classLevel = cls,
                        subject = sub,
                        year = yr,
                        board = paper.board,
                        paperItem = paper
                    )
                }
            }
        }

        return resultsMap.values.toList()
    }

    suspend fun downloadPaperToDevice(context: Context, paper: PaperItem): Result<File> = withContext(Dispatchers.IO) {
        try {
            val fileName = "${paper.classFolderName}_${paper.subjectName}_${paper.board}_${paper.year}.pdf"
                .replace(" ", "_")
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            val destFile = File(downloadsDir, fileName)

            if (paper.isRemote && !paper.remoteUrl.isNullOrEmpty()) {
                HttpDownloadHelper.downloadToFile(paper.remoteUrl, destFile)
            } else if (paper.assetPath != null) {
                context.assets.open(paper.assetPath).use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Result.success(destFile)
            } else {
                Result.failure(Exception("Paper asset not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAllDynamicPapers(context: Context): List<PaperItem> {
        ensurePrefsLoaded(context)
        return dynamicPapers.toList()
    }

    fun deleteDynamicPaper(context: Context, paperKey: String): Boolean {
        ensurePrefsLoaded(context)
        val removed = dynamicPapers.removeAll { it.uniqueKey == paperKey }
        if (removed) {
            val prefs = context.getSharedPreferences("paper_guru_prefs", Context.MODE_PRIVATE)
            val array = JSONArray()
            for (item in dynamicPapers) {
                val obj = JSONObject()
                obj.put("classFolderName", item.classFolderName)
                obj.put("subjectName", item.subjectName)
                obj.put("year", item.year)
                obj.put("board", item.board)
                obj.put("remoteUrl", item.remoteUrl)
                array.put(obj)
            }
            prefs.edit().putString("dynamic_papers", array.toString()).apply()

            // Delete from Firebase Firestore
            repoScope.launch {
                try {
                    FirebaseFirestoreService.deletePaper(context, paperKey)
                } catch (_: Exception) {}
            }
        }
        return removed
    }

    // --- Student Directory Management (Admin Portal) ---
    private val defaultStudents = listOf(
        StudentUser(
            id = "std_101",
            fullName = "Muhammad Ali",
            age = 16,
            email = "ali.matric24@gmail.com",
            telephone = "0302-4589123",
            cnic = "35201-8934125-1",
            selectedClass = "10th",
            selectedBoard = "LHR",
            isBlocked = false,
            hasAdvanceSubscription = true,
            subscriptionPlan = "PRO Annual VIP",
            subscriptionExpiry = "2025-06-30",
            feePaymentStatus = "PAID_VERIFIED",
            feeTransactionRef = "EP-984321045",
            feeAmount = 1000,
            registeredAt = "2024-08-15"
        ),
        StudentUser(
            id = "std_102",
            fullName = "Fatima Noor",
            age = 15,
            email = "fatima.noor@outlook.com",
            telephone = "0315-7762341",
            cnic = "35202-6543210-2",
            selectedClass = "9th",
            selectedBoard = "RWP",
            isBlocked = false,
            hasAdvanceSubscription = false,
            subscriptionPlan = null,
            subscriptionExpiry = null,
            feePaymentStatus = "PENDING_VERIFICATION",
            feeTransactionRef = "JC-77341209",
            feeAmount = 500,
            registeredAt = "2024-09-10"
        ),
        StudentUser(
            id = "std_103",
            fullName = "Hamza Tariq",
            age = 17,
            email = "hamza.ics@yahoo.com",
            telephone = "0333-8901234",
            cnic = "33100-7812345-7",
            selectedClass = "11th",
            selectedBoard = "FSD",
            isBlocked = false,
            hasAdvanceSubscription = false,
            subscriptionPlan = null,
            subscriptionExpiry = null,
            feePaymentStatus = "UNPAID",
            feeTransactionRef = null,
            feeAmount = 0,
            registeredAt = "2024-09-18"
        ),
        StudentUser(
            id = "std_104",
            fullName = "Zainab Bibi",
            age = 18,
            email = "zainab.fsc@gmail.com",
            telephone = "0345-1234567",
            cnic = null, // Optional CNIC
            selectedClass = "12th",
            selectedBoard = "FBISE",
            isBlocked = false,
            hasAdvanceSubscription = true,
            subscriptionPlan = "Lifetime Advance Pass",
            subscriptionExpiry = "Lifetime",
            feePaymentStatus = "PAID_VERIFIED",
            feeTransactionRef = "BA-8812903",
            feeAmount = 2000,
            registeredAt = "2024-07-20"
        ),
        StudentUser(
            id = "std_105",
            fullName = "Bilal Ahmed",
            age = 17,
            email = "bilal.spam@hotmail.com",
            telephone = "0300-9988776",
            cnic = "35404-1239876-5",
            selectedClass = "11th",
            selectedBoard = "GRW",
            isBlocked = true, // Blocked user
            hasAdvanceSubscription = false,
            subscriptionPlan = null,
            subscriptionExpiry = null,
            feePaymentStatus = "UNPAID",
            feeTransactionRef = null,
            feeAmount = 0,
            registeredAt = "2024-06-01"
        )
    )

    fun getStudents(context: Context): List<StudentUser> {
        val prefs = context.getSharedPreferences("paper_guru_students", Context.MODE_PRIVATE)
        val json = prefs.getString("students_list", null)
        if (json.isNullOrEmpty()) {
            saveStudentsList(context, defaultStudents)
            return defaultStudents
        }
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<StudentUser>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    StudentUser(
                        id = obj.getString("id"),
                        fullName = obj.getString("fullName"),
                        age = obj.optInt("age", 16),
                        email = obj.getString("email"),
                        telephone = obj.optString("telephone", ""),
                        cnic = if (obj.has("cnic") && !obj.isNull("cnic")) obj.getString("cnic") else null,
                        selectedClass = obj.optString("selectedClass", "10th"),
                        selectedBoard = obj.optString("selectedBoard", "LHR"),
                        isBlocked = obj.optBoolean("isBlocked", false),
                        hasAdvanceSubscription = obj.optBoolean("hasAdvanceSubscription", false),
                        subscriptionPlan = if (obj.has("subscriptionPlan") && !obj.isNull("subscriptionPlan")) obj.getString("subscriptionPlan") else null,
                        subscriptionExpiry = if (obj.has("subscriptionExpiry") && !obj.isNull("subscriptionExpiry")) obj.getString("subscriptionExpiry") else null,
                        feePaymentStatus = obj.optString("feePaymentStatus", "UNPAID"),
                        feeTransactionRef = if (obj.has("feeTransactionRef") && !obj.isNull("feeTransactionRef")) obj.getString("feeTransactionRef") else null,
                        feeAmount = obj.optInt("feeAmount", 0),
                        registeredAt = obj.optString("registeredAt", "2024-09-29")
                    )
                )
            }
            list
        } catch (_: Exception) {
            defaultStudents
        }
    }

    private fun saveStudentsList(context: Context, students: List<StudentUser>) {
        val prefs = context.getSharedPreferences("paper_guru_students", Context.MODE_PRIVATE)
        val array = JSONArray()
        for (s in students) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("fullName", s.fullName)
            obj.put("age", s.age)
            obj.put("email", s.email)
            obj.put("telephone", s.telephone)
            if (s.cnic != null) obj.put("cnic", s.cnic) else obj.put("cnic", JSONObject.NULL)
            obj.put("selectedClass", s.selectedClass)
            obj.put("selectedBoard", s.selectedBoard)
            obj.put("isBlocked", s.isBlocked)
            obj.put("hasAdvanceSubscription", s.hasAdvanceSubscription)
            obj.put("subscriptionPlan", s.subscriptionPlan ?: JSONObject.NULL)
            obj.put("subscriptionExpiry", s.subscriptionExpiry ?: JSONObject.NULL)
            obj.put("feePaymentStatus", s.feePaymentStatus)
            obj.put("feeTransactionRef", s.feeTransactionRef ?: JSONObject.NULL)
            obj.put("feeAmount", s.feeAmount)
            obj.put("registeredAt", s.registeredAt)
            array.put(obj)
        }
        prefs.edit().putString("students_list", array.toString()).apply()
    }

    fun saveOrUpdateStudent(context: Context, student: StudentUser): List<StudentUser> {
        val current = getStudents(context).toMutableList()
        val idx = current.indexOfFirst { it.id == student.id || it.email.equals(student.email, ignoreCase = true) }
        if (idx >= 0) {
            current[idx] = student
        } else {
            current.add(0, student)
        }
        saveStudentsList(context, current)

        // Sync to Firebase Firestore
        repoScope.launch {
            try {
                FirebaseFirestoreService.saveOrUpdateStudent(context, student)
            } catch (_: Exception) {}
        }
        return current
    }

    fun updateStudentBlockStatus(context: Context, studentId: String, isBlocked: Boolean): List<StudentUser> {
        val current = getStudents(context).toMutableList()
        val idx = current.indexOfFirst { it.id == studentId }
        if (idx >= 0) {
            val student = current[idx]
            current[idx] = student.copy(isBlocked = isBlocked)
            saveStudentsList(context, current)

            // Sync to Firebase Firestore
            repoScope.launch {
                try {
                    FirebaseFirestoreService.updateStudentBlockStatus(context, studentId, isBlocked)
                } catch (_: Exception) {}
            }

            // Trigger admin notification
            val notif = AdminNotification(
                id = "notif_${System.currentTimeMillis()}",
                title = if (isBlocked) "طالب علم بلاک کر دیا گیا 🚫" else "طالب علم بحال کر دیا گیا ✅",
                message = "${student.fullName} (${student.email}) کو ایڈمن نے ${if (isBlocked) "بلاک" else "بحال"} کر دیا ہے۔",
                timestamp = System.currentTimeMillis(),
                type = "STUDENT_STATUS",
                studentId = student.id,
                studentEmail = student.email
            )
            addAdminNotification(context, notif)
        }
        return current
    }

    fun grantAdvanceSubscription(
        context: Context,
        studentId: String,
        planName: String = "Advance VIP Pass",
        expiryDate: String = "2025-06-30"
    ): List<StudentUser> {
        val current = getStudents(context).toMutableList()
        val idx = current.indexOfFirst { it.id == studentId }
        if (idx >= 0) {
            val student = current[idx]
            current[idx] = student.copy(
                hasAdvanceSubscription = true,
                subscriptionPlan = planName,
                subscriptionExpiry = expiryDate,
                feePaymentStatus = "PAID_VERIFIED"
            )
            saveStudentsList(context, current)

            // Sync to Firebase Firestore
            repoScope.launch {
                try {
                    FirebaseFirestoreService.updateStudentSubscription(
                        context,
                        studentId,
                        hasAdvance = true,
                        plan = planName,
                        expiry = expiryDate,
                        feeStatus = "PAID_VERIFIED"
                    )
                } catch (_: Exception) {}
            }

            // Trigger admin notification
            val notif = AdminNotification(
                id = "notif_${System.currentTimeMillis()}",
                title = "ایڈوانس سبسکرپشن الاٹ ہو گئی ⭐",
                message = "${student.fullName} (${student.email}) کو $planName دے دی گئی ہے (فیس تصدیق شدہ)۔",
                timestamp = System.currentTimeMillis(),
                type = "SUBSCRIPTION_GRANTED",
                studentId = student.id,
                studentEmail = student.email
            )
            addAdminNotification(context, notif)
        }
        return current
    }

    fun revokeAdvanceSubscription(context: Context, studentId: String): List<StudentUser> {
        val current = getStudents(context).toMutableList()
        val idx = current.indexOfFirst { it.id == studentId }
        if (idx >= 0) {
            val student = current[idx]
            current[idx] = student.copy(
                hasAdvanceSubscription = false,
                subscriptionPlan = null,
                subscriptionExpiry = null,
                feePaymentStatus = "UNPAID"
            )
            saveStudentsList(context, current)

            // Sync to Firebase Firestore
            repoScope.launch {
                try {
                    FirebaseFirestoreService.updateStudentSubscription(
                        context,
                        studentId,
                        hasAdvance = false,
                        plan = null,
                        expiry = null,
                        feeStatus = "UNPAID"
                    )
                } catch (_: Exception) {}
            }
        }
        return current
    }

    fun recordFeePayment(
        context: Context,
        studentId: String,
        amount: Int,
        trxRef: String
    ): List<StudentUser> {
        val current = getStudents(context).toMutableList()
        val idx = current.indexOfFirst { it.id == studentId }
        if (idx >= 0) {
            val student = current[idx]
            current[idx] = student.copy(
                feePaymentStatus = "PENDING_VERIFICATION",
                feeTransactionRef = trxRef,
                feeAmount = amount
            )
            saveStudentsList(context, current)

            // Sync Subscription Record & Student Status to Firebase Firestore
            val subRecord = SubscriptionRecord(
                id = "sub_${System.currentTimeMillis()}",
                studentId = student.id,
                studentEmail = student.email,
                studentName = student.fullName,
                amount = amount,
                paymentMethod = "EasyPaisa",
                transactionRef = trxRef,
                status = "PENDING_VERIFICATION",
                planName = "PRO Annual VIP"
            )
            repoScope.launch {
                try {
                    FirebaseFirestoreService.submitSubscriptionRecord(context, subRecord)
                    FirebaseFirestoreService.updateStudentSubscription(
                        context,
                        student.id,
                        student.hasAdvanceSubscription,
                        student.subscriptionPlan,
                        student.subscriptionExpiry,
                        "PENDING_VERIFICATION"
                    )
                } catch (_: Exception) {}
            }

            // Real Admin Notification!
            val notif = AdminNotification(
                id = "notif_${System.currentTimeMillis()}",
                title = "نئی فیس ادائیگی کی وصولی 💰",
                message = "${student.fullName} (${student.telephone}) نے Rs. $amount ادا کیے ہیں۔ Trx ID: $trxRef۔ براہ کرم ایڈوانس سبسکرپشن الاٹ کریں۔",
                timestamp = System.currentTimeMillis(),
                type = "FEE_PAID",
                studentId = student.id,
                studentEmail = student.email
            )
            addAdminNotification(context, notif)
        }
        return current
    }

    // --- Admin Notification System ---
    private val defaultNotifications = listOf(
        AdminNotification(
            id = "notif_001",
            title = "نئی فیس ادائیگی (EasyPaisa)",
            message = "طالب علم Fatima Noor (0315-7762341) نے Rs. 500 فیس ادا کی ہے۔ Trx Ref: JC-77341209۔ برائے مہربانی تصدیق کر کے ایڈوانس سبسکرپشن الاٹ کریں۔",
            timestamp = System.currentTimeMillis() - 3600000 * 2,
            type = "FEE_PAID",
            studentId = "std_102",
            studentEmail = "fatima.noor@outlook.com",
            isRead = false
        ),
        AdminNotification(
            id = "notif_002",
            title = "نیا طالب علم رجسٹرڈ",
            message = "طالب علم Hamza Tariq (hamza.ics@yahoo.com, فون: 0333-8901234, عمر: 17) نے پورٹل جوائن کیا ہے۔",
            timestamp = System.currentTimeMillis() - 3600000 * 24,
            type = "NEW_STUDENT",
            studentId = "std_103",
            studentEmail = "hamza.ics@yahoo.com",
            isRead = true
        )
    )

    fun getAdminNotifications(context: Context): List<AdminNotification> {
        val prefs = context.getSharedPreferences("paper_guru_admin_notifs", Context.MODE_PRIVATE)
        val json = prefs.getString("notifs_list", null)
        if (json.isNullOrEmpty()) {
            saveAdminNotifications(context, defaultNotifications)
            return defaultNotifications
        }
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<AdminNotification>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    AdminNotification(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        message = obj.getString("message"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        type = obj.optString("type", "INFO"),
                        studentId = if (obj.has("studentId") && !obj.isNull("studentId")) obj.getString("studentId") else null,
                        studentEmail = if (obj.has("studentEmail") && !obj.isNull("studentEmail")) obj.getString("studentEmail") else null,
                        isRead = obj.optBoolean("isRead", false)
                    )
                )
            }
            list
        } catch (_: Exception) {
            defaultNotifications
        }
    }

    fun saveAdminNotifications(context: Context, notifications: List<AdminNotification>) {
        val prefs = context.getSharedPreferences("paper_guru_admin_notifs", Context.MODE_PRIVATE)
        val array = JSONArray()
        for (n in notifications) {
            val obj = JSONObject()
            obj.put("id", n.id)
            obj.put("title", n.title)
            obj.put("message", n.message)
            obj.put("timestamp", n.timestamp)
            obj.put("type", n.type)
            if (n.studentId != null) obj.put("studentId", n.studentId) else obj.put("studentId", JSONObject.NULL)
            if (n.studentEmail != null) obj.put("studentEmail", n.studentEmail) else obj.put("studentEmail", JSONObject.NULL)
            obj.put("isRead", n.isRead)
            array.put(obj)
        }
        prefs.edit().putString("notifs_list", array.toString()).apply()
    }

    fun addAdminNotification(context: Context, notification: AdminNotification) {
        val current = getAdminNotifications(context).toMutableList()
        current.add(0, notification)
        saveAdminNotifications(context, current)

        // Sync to Firebase Firestore
        repoScope.launch {
            try {
                FirebaseFirestoreService.saveNotification(context, notification)
            } catch (_: Exception) {}
        }
    }

    fun markNotificationRead(context: Context, notificationId: String) {
        val current = getAdminNotifications(context).toMutableList()
        val idx = current.indexOfFirst { it.id == notificationId }
        if (idx >= 0) {
            current[idx] = current[idx].copy(isRead = true)
            saveAdminNotifications(context, current)

            repoScope.launch {
                try {
                    FirebaseFirestoreService.markNotificationRead(context, notificationId)
                } catch (_: Exception) {}
            }
        }
    }

    fun clearAllNotifications(context: Context) {
        saveAdminNotifications(context, emptyList())
    }

    // --- Bi-directional Sync with Firebase Firestore ---
    suspend fun syncAllFromFirestore(context: Context): Result<Int> = withContext(Dispatchers.IO) {
        var totalSynced = 0
        try {
            FirebaseFirestoreService.initialize(context)

            // 1. Sync paper uploads from Firestore
            val papersRes = FirebaseFirestoreService.fetchPaperUploads(context)
            if (papersRes.isSuccess) {
                val papers = papersRes.getOrNull() ?: emptyList()
                ensurePrefsLoaded(context)
                for (p in papers) {
                    if (dynamicPapers.none { it.uniqueKey == p.uniqueKey }) {
                        dynamicPapers.add(p)
                        totalSynced++
                    }
                }
                val prefs = context.getSharedPreferences("paper_guru_prefs", Context.MODE_PRIVATE)
                val array = JSONArray()
                for (item in dynamicPapers) {
                    val obj = JSONObject()
                    obj.put("classFolderName", item.classFolderName)
                    obj.put("subjectName", item.subjectName)
                    obj.put("year", item.year)
                    obj.put("board", item.board)
                    obj.put("remoteUrl", item.remoteUrl)
                    array.put(obj)
                }
                prefs.edit().putString("dynamic_papers", array.toString()).apply()
            }

            // 2. Sync students from Firestore
            val studentsRes = FirebaseFirestoreService.fetchStudents(context)
            if (studentsRes.isSuccess) {
                val fsStudents = studentsRes.getOrNull() ?: emptyList()
                if (fsStudents.isNotEmpty()) {
                    val local = getStudents(context).toMutableList()
                    for (fs in fsStudents) {
                        val idx = local.indexOfFirst { it.id == fs.id || it.email.equals(fs.email, ignoreCase = true) }
                        if (idx >= 0) {
                            local[idx] = fs
                        } else {
                            local.add(fs)
                        }
                        totalSynced++
                    }
                    saveStudentsList(context, local)
                }
            }

            // 3. Sync admin notifications
            val notifsRes = FirebaseFirestoreService.fetchNotifications(context)
            if (notifsRes.isSuccess) {
                val fsNotifs = notifsRes.getOrNull() ?: emptyList()
                if (fsNotifs.isNotEmpty()) {
                    val local = getAdminNotifications(context).toMutableList()
                    for (fn in fsNotifs) {
                        if (local.none { it.id == fn.id }) {
                            local.add(0, fn)
                            totalSynced++
                        }
                    }
                    saveAdminNotifications(context, local)
                }
            }

            Result.success(totalSynced)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Student Activity Tracking ---
    private val defaultActivities = listOf(
        StudentActivityEvent(
            id = "act_001",
            studentId = "std_102",
            studentName = "Fatima Noor",
            eventType = ActivityEventType.FEE_SUBMISSION,
            sectionName = "PRO Subscription",
            details = "Rs. 500 fee proof submitted via EasyPaisa (Trx: JC-77341209)",
            timestamp = System.currentTimeMillis() - 3600000 * 2,
            classLevel = "9th"
        ),
        StudentActivityEvent(
            id = "act_002",
            studentId = "std_101",
            studentName = "Ali Raza",
            eventType = ActivityEventType.PAPER_VIEW,
            sectionName = "10th Class Physics",
            details = "Viewed Physics 2024 past paper in PDF Viewer (Lahore Board)",
            timestamp = System.currentTimeMillis() - 3600000 * 4,
            classLevel = "10th",
            subject = "Physics",
            year = "2024",
            board = "LHR"
        ),
        StudentActivityEvent(
            id = "act_003",
            studentId = "std_104",
            studentName = "Zainab Bibi",
            eventType = ActivityEventType.PAPER_DOWNLOAD,
            sectionName = "12th Class Chemistry",
            details = "Downloaded Chemistry 2023 FBISE board paper",
            timestamp = System.currentTimeMillis() - 3600000 * 8,
            classLevel = "12th",
            subject = "Chemistry",
            year = "2023",
            board = "FBISE"
        ),
        StudentActivityEvent(
            id = "act_004",
            studentId = "std_103",
            studentName = "Hamza Tariq",
            eventType = ActivityEventType.SEARCH,
            sectionName = "Home Search",
            details = "Searched for 'computer science 11th fsd board'",
            timestamp = System.currentTimeMillis() - 3600000 * 12,
            classLevel = "11th"
        ),
        StudentActivityEvent(
            id = "act_005",
            studentId = "std_101",
            studentName = "Ali Raza",
            eventType = ActivityEventType.PAPER_BOOKMARK,
            sectionName = "10th Class Mathematics",
            details = "Bookmarked Mathematics 2024 (LHR) for offline revision",
            timestamp = System.currentTimeMillis() - 3600000 * 18,
            classLevel = "10th",
            subject = "Mathematics",
            year = "2024",
            board = "LHR"
        )
    )

    fun getTrackedActivities(context: Context): List<StudentActivityEvent> {
        val prefs = context.getSharedPreferences("paper_guru_activities", Context.MODE_PRIVATE)
        val json = prefs.getString("activities_list", null)
        if (json.isNullOrEmpty()) {
            saveActivitiesList(context, defaultActivities)
            return defaultActivities
        }
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<StudentActivityEvent>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val typeStr = obj.optString("eventType", "SCREEN_VIEW")
                val type = try {
                    ActivityEventType.valueOf(typeStr)
                } catch (_: Exception) {
                    ActivityEventType.SCREEN_VIEW
                }
                list.add(
                    StudentActivityEvent(
                        id = obj.getString("id"),
                        studentId = obj.getString("studentId"),
                        studentName = obj.optString("studentName", "Student"),
                        eventType = type,
                        sectionName = obj.optString("sectionName", ""),
                        details = obj.optString("details", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        classLevel = if (obj.has("classLevel") && !obj.isNull("classLevel")) obj.getString("classLevel") else null,
                        subject = if (obj.has("subject") && !obj.isNull("subject")) obj.getString("subject") else null,
                        year = if (obj.has("year") && !obj.isNull("year")) obj.getString("year") else null,
                        board = if (obj.has("board") && !obj.isNull("board")) obj.getString("board") else null
                    )
                )
            }
            list.sortByDescending { it.timestamp }
            list
        } catch (_: Exception) {
            defaultActivities
        }
    }

    private fun saveActivitiesList(context: Context, activities: List<StudentActivityEvent>) {
        val prefs = context.getSharedPreferences("paper_guru_activities", Context.MODE_PRIVATE)
        val array = JSONArray()
        for (a in activities.take(100)) {
            val obj = JSONObject()
            obj.put("id", a.id)
            obj.put("studentId", a.studentId)
            obj.put("studentName", a.studentName)
            obj.put("eventType", a.eventType.name)
            obj.put("sectionName", a.sectionName)
            obj.put("details", a.details)
            obj.put("timestamp", a.timestamp)
            if (a.classLevel != null) obj.put("classLevel", a.classLevel) else obj.put("classLevel", JSONObject.NULL)
            if (a.subject != null) obj.put("subject", a.subject) else obj.put("subject", JSONObject.NULL)
            if (a.year != null) obj.put("year", a.year) else obj.put("year", JSONObject.NULL)
            if (a.board != null) obj.put("board", a.board) else obj.put("board", JSONObject.NULL)
            array.put(obj)
        }
        prefs.edit().putString("activities_list", array.toString()).apply()
    }

    fun logStudentActivity(context: Context, event: StudentActivityEvent) {
        val current = getTrackedActivities(context).toMutableList()
        current.add(0, event)
        saveActivitiesList(context, current)

        // Sync to Firebase Firestore
        repoScope.launch {
            try {
                FirebaseFirestoreService.saveActivity(context, event)
            } catch (_: Exception) {}
        }
    }

    fun clearTrackedActivities(context: Context) {
        val prefs = context.getSharedPreferences("paper_guru_activities", Context.MODE_PRIVATE)
        prefs.edit().remove("activities_list").apply()
    }

    // --- Admin Broadcast Announcements ---
    private val defaultAnnouncement = AdminAnnouncement(
        id = "ann_rumors_9th_10th",
        title = "⚠️ نویں اور دسویں جماعت کے پرچے عارضی طور پر ہٹا دیے گئے ہیں",
        message = "افواہوں اور مبینہ پیپر لیک کی اطلاعات کی جانچ پڑتال کے پیش نظر 9th اور 10th کلاس کے تمام پرچے ایپ سے عارضی طور پر ہٹا دیے گئے ہیں۔ 11th اور 12th کے پرچے معمول کے مطابق دستیاب ہیں۔",
        targetClass = "ALL",
        timestamp = System.currentTimeMillis(),
        isActive = true,
        priority = "HIGH"
    )

    fun getAnnouncements(context: Context): List<AdminAnnouncement> {
        val prefs = context.getSharedPreferences("paper_guru_announcements", Context.MODE_PRIVATE)
        val json = prefs.getString("announcements_list", null)
        val list = if (json.isNullOrEmpty()) {
            mutableListOf(defaultAnnouncement)
        } else {
            try {
                val array = JSONArray(json)
                val parsed = mutableListOf<AdminAnnouncement>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    parsed.add(
                        AdminAnnouncement(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            message = obj.getString("message"),
                            targetClass = obj.optString("targetClass", "ALL"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            isActive = obj.optBoolean("isActive", true),
                            priority = obj.optString("priority", "NORMAL")
                        )
                    )
                }
                parsed
            } catch (_: Exception) {
                mutableListOf(defaultAnnouncement)
            }
        }

        // Guarantee that the 9th/10th rumor withdrawal announcement is prominently active
        if (list.none { it.id == defaultAnnouncement.id }) {
            list.add(0, defaultAnnouncement)
            saveAnnouncementsList(context, list)
        }
        return list
    }

    private fun saveAnnouncementsList(context: Context, list: List<AdminAnnouncement>) {
        val prefs = context.getSharedPreferences("paper_guru_announcements", Context.MODE_PRIVATE)
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("message", item.message)
            obj.put("targetClass", item.targetClass)
            obj.put("timestamp", item.timestamp)
            obj.put("isActive", item.isActive)
            obj.put("priority", item.priority)
            array.put(obj)
        }
        prefs.edit().putString("announcements_list", array.toString()).apply()
    }

    fun publishAnnouncement(context: Context, announcement: AdminAnnouncement) {
        val current = getAnnouncements(context).toMutableList()
        current.add(0, announcement)
        saveAnnouncementsList(context, current)

        repoScope.launch {
            try {
                FirebaseFirestoreService.saveAnnouncement(context, announcement)
            } catch (_: Exception) {}
        }
    }
}
