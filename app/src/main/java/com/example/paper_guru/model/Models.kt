package com.example.paper_guru.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppScreen {
    HOME,
    SUBJECTS,
    YEARS,
    PAPER_DETAIL,
    PDF_VIEWER,
    SOLUTION_VIEWER,
    EXAM_INSPIRATION,
    ADMIN_LOGIN,
    ADMIN_PORTAL
}

enum class SubjectCategory(val displayName: String) {
    ALL("All Subjects"),
    SCIENCE("Science"),
    LANGUAGES("Languages"),
    COMPULSORY("Compulsory"),
    ARTS("Arts & Humanities")
}

data class ClassLevel(
    val id: String,
    val displayName: String,
    val folderName: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color
)

data class SubjectItem(
    val name: String,
    val urduName: String,
    val icon: ImageVector,
    val color: Color,
    val category: SubjectCategory = SubjectCategory.COMPULSORY
)

data class PaperItem(
    val classFolderName: String = "9th",
    val subjectName: String = "English",
    val year: String = "2024",
    val board: String = "LHR",
    val assetPath: String? = null,
    val remoteUrl: String? = null,
    val solutionUrl: String? = null,
    val medium: String = "Both",
    val group: String = "Annual",
    val isAvailable: Boolean = true,
    val isRemote: Boolean = false,
    val isBookmarked: Boolean = false,
    val uploadedBy: String = "admin",
    val uploadedAt: Long = System.currentTimeMillis()
) {
    val uniqueKey: String
        get() = "${classFolderName}_${subjectName}_${board}_${year}".lowercase().replace(" ", "_")

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "classFolderName" to classFolderName,
        "subjectName" to subjectName,
        "year" to year,
        "board" to board,
        "assetPath" to assetPath,
        "remoteUrl" to remoteUrl,
        "solutionUrl" to solutionUrl,
        "medium" to medium,
        "group" to group,
        "isAvailable" to isAvailable,
        "isRemote" to isRemote,
        "uploadedBy" to uploadedBy,
        "uploadedAt" to uploadedAt
    )

    companion object {
        fun fromFirestore(docId: String, map: Map<String, Any?>): PaperItem {
            return PaperItem(
                classFolderName = map["classFolderName"] as? String ?: "9th",
                subjectName = map["subjectName"] as? String ?: "English",
                year = map["year"] as? String ?: "2024",
                board = map["board"] as? String ?: "LHR",
                assetPath = map["assetPath"] as? String,
                remoteUrl = map["remoteUrl"] as? String,
                solutionUrl = map["solutionUrl"] as? String,
                medium = map["medium"] as? String ?: "Both",
                group = map["group"] as? String ?: "Annual",
                isAvailable = map["isAvailable"] as? Boolean ?: true,
                isRemote = map["isRemote"] as? Boolean ?: true,
                uploadedBy = map["uploadedBy"] as? String ?: "admin",
                uploadedAt = (map["uploadedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

data class EducationalBoard(
    val code: String,
    val name: String,
    val division: String
)

val ALL_BOARDS = listOf(
    EducationalBoard("ALL", "All Boards (پنجاب و فیڈرل)", "All"),
    EducationalBoard("LHR", "Lahore Board (BISE Lahore)", "Punjab"),
    EducationalBoard("RWP", "Rawalpindi Board (BISE Rawalpindi)", "Punjab"),
    EducationalBoard("FSD", "Faisalabad Board (BISE Faisalabad)", "Punjab"),
    EducationalBoard("MLT", "Multan Board (BISE Multan)", "Punjab"),
    EducationalBoard("GRW", "Gujranwala Board (BISE Gujranwala)", "Punjab"),
    EducationalBoard("SWL", "Sahiwal Board (BISE Sahiwal)", "Punjab"),
    EducationalBoard("BWP", "Bahawalpur Board (BISE Bahawalpur)", "Punjab"),
    EducationalBoard("SGD", "Sargodha Board (BISE Sargodha)", "Punjab"),
    EducationalBoard("DGK", "D.G. Khan Board (BISE D.G. Khan)", "Punjab"),
    EducationalBoard("FBISE", "Federal Board (FBISE Islamabad)", "Federal")
)

val ALL_CLASSES = listOf(
    ClassLevel("9th", "9th Class (Matric Part 1)", "9th", "پہلا سال میٹرک - تمام مضامین", Icons.Default.School, Color(0xFF1976D2)),
    ClassLevel("10th", "10th Class (Matric Part 2)", "10th", "دوسرا سال میٹرک - بورڈ امتحانات", Icons.Default.MenuBook, Color(0xFF00796B)),
    ClassLevel("11th", "11th Class (FSc / FA / ICS Part 1)", "11th", "انٹرمیڈیٹ پارٹ 1 - پری میڈیکل و انجینئرنگ", Icons.Default.AutoStories, Color(0xFFE65100)),
    ClassLevel("12th", "12th Class (FSc / FA / ICS Part 2)", "12th", "انٹرمیڈیٹ پارٹ 2 - فائنل بورڈ پرچے", Icons.Default.WorkspacePremium, Color(0xFF6A1B9A))
)

// Classes temporarily suspended due to paper rumors
val SUSPENDED_CLASS_IDS = setOf("9th", "10th")
val ACTIVE_CLASSES = ALL_CLASSES.filter { !SUSPENDED_CLASS_IDS.contains(it.id) }

val ALL_SUBJECTS = listOf(
    SubjectItem("Urdu", "اردو لازمی", Icons.Default.Translate, Color(0xFF2E7D32), SubjectCategory.LANGUAGES),
    SubjectItem("English", "انگریزی لازمی", Icons.Default.Language, Color(0xFF1565C0), SubjectCategory.LANGUAGES),
    SubjectItem("Mathematics", "ریاضی (Science / Arts)", Icons.Default.Calculate, Color(0xFFD84315), SubjectCategory.SCIENCE),
    SubjectItem("Physics", "طبیعیات (Physics)", Icons.Default.Bolt, Color(0xFF6A1B9A), SubjectCategory.SCIENCE),
    SubjectItem("Chemistry", "کیمیا (Chemistry)", Icons.Default.Science, Color(0xFF00838F), SubjectCategory.SCIENCE),
    SubjectItem("Biology", "حیاتیات (Biology)", Icons.Default.Biotech, Color(0xFF388E3C), SubjectCategory.SCIENCE),
    SubjectItem("Computer Science", "کمپیوٹر سائنس", Icons.Default.Computer, Color(0xFF4527A0), SubjectCategory.SCIENCE),
    SubjectItem("Pak Studies", "مطالعہ پاکستان", Icons.Default.Public, Color(0xFF00695C), SubjectCategory.COMPULSORY),
    SubjectItem("Islamiat", "اسلامیات لازمی / اختیاری", Icons.Default.BookmarkBorder, Color(0xFF4E342E), SubjectCategory.COMPULSORY),
    SubjectItem("Tarjuma Quran", "ترجمۃ القرآن المجید", Icons.Default.AutoStories, Color(0xFF1B5E20), SubjectCategory.COMPULSORY),
    SubjectItem("General Science", "جنرل سائنس", Icons.Default.Lightbulb, Color(0xFFF57F17), SubjectCategory.ARTS)
)

data class SearchPaperResult(
    val classLevel: ClassLevel,
    val subject: SubjectItem,
    val year: String,
    val board: String,
    val paperItem: PaperItem
)

data class StudentUser(
    val id: String = "",
    val fullName: String = "",
    val age: Int = 16,
    val email: String = "",
    val telephone: String = "",
    val cnic: String? = null, // Optional CNIC or B-Form
    val selectedClass: String = "10th",
    val selectedBoard: String = "LHR",
    val isBlocked: Boolean = false,
    val hasAdvanceSubscription: Boolean = false,
    val subscriptionPlan: String? = null,
    val subscriptionExpiry: String? = null,
    val feePaymentStatus: String = "UNPAID", // UNPAID, PENDING_VERIFICATION, PAID_VERIFIED
    val feeTransactionRef: String? = null,
    val feeAmount: Int = 0,
    val registeredAt: String = "2024-09-29",
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "fullName" to fullName,
        "age" to age,
        "email" to email,
        "telephone" to telephone,
        "cnic" to cnic,
        "selectedClass" to selectedClass,
        "selectedBoard" to selectedBoard,
        "isBlocked" to isBlocked,
        "hasAdvanceSubscription" to hasAdvanceSubscription,
        "subscriptionPlan" to subscriptionPlan,
        "subscriptionExpiry" to subscriptionExpiry,
        "feePaymentStatus" to feePaymentStatus,
        "feeTransactionRef" to feeTransactionRef,
        "feeAmount" to feeAmount,
        "registeredAt" to registeredAt,
        "updatedAt" to updatedAt
    )

    companion object {
        fun fromFirestore(docId: String, map: Map<String, Any?>): StudentUser {
            return StudentUser(
                id = (map["id"] as? String)?.ifEmpty { docId } ?: docId,
                fullName = map["fullName"] as? String ?: "Student",
                age = (map["age"] as? Number)?.toInt() ?: 16,
                email = map["email"] as? String ?: "",
                telephone = map["telephone"] as? String ?: "",
                cnic = map["cnic"] as? String,
                selectedClass = map["selectedClass"] as? String ?: "10th",
                selectedBoard = map["selectedBoard"] as? String ?: "LHR",
                isBlocked = map["isBlocked"] as? Boolean ?: false,
                hasAdvanceSubscription = map["hasAdvanceSubscription"] as? Boolean ?: false,
                subscriptionPlan = map["subscriptionPlan"] as? String,
                subscriptionExpiry = map["subscriptionExpiry"] as? String,
                feePaymentStatus = map["feePaymentStatus"] as? String ?: "UNPAID",
                feeTransactionRef = map["feeTransactionRef"] as? String,
                feeAmount = (map["feeAmount"] as? Number)?.toInt() ?: 0,
                registeredAt = map["registeredAt"] as? String ?: "2024-09-29",
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

data class SubscriptionRecord(
    val id: String = "",
    val studentId: String = "",
    val studentEmail: String = "",
    val studentName: String = "",
    val amount: Int = 500,
    val paymentMethod: String = "EasyPaisa",
    val transactionRef: String = "",
    val status: String = "PENDING_VERIFICATION", // PENDING_VERIFICATION, APPROVED, REJECTED
    val planName: String = "PRO Annual VIP",
    val submittedAt: Long = System.currentTimeMillis(),
    val verifiedAt: Long? = null
) {
    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "studentId" to studentId,
        "studentEmail" to studentEmail,
        "studentName" to studentName,
        "amount" to amount,
        "paymentMethod" to paymentMethod,
        "transactionRef" to transactionRef,
        "status" to status,
        "planName" to planName,
        "submittedAt" to submittedAt,
        "verifiedAt" to verifiedAt
    )

    companion object {
        fun fromFirestore(docId: String, map: Map<String, Any?>): SubscriptionRecord {
            return SubscriptionRecord(
                id = docId,
                studentId = map["studentId"] as? String ?: "",
                studentEmail = map["studentEmail"] as? String ?: "",
                studentName = map["studentName"] as? String ?: "",
                amount = (map["amount"] as? Number)?.toInt() ?: 500,
                paymentMethod = map["paymentMethod"] as? String ?: "EasyPaisa",
                transactionRef = map["transactionRef"] as? String ?: "",
                status = map["status"] as? String ?: "PENDING_VERIFICATION",
                planName = map["planName"] as? String ?: "PRO Annual VIP",
                submittedAt = (map["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                verifiedAt = (map["verifiedAt"] as? Number)?.toLong()
            )
        }
    }
}

data class AdminNotification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "INFO", // "FEE_PAID", "NEW_STUDENT", "SUBSCRIPTION_REQUEST", "PAPER_UPLOAD"
    val studentId: String? = null,
    val studentEmail: String? = null,
    val isRead: Boolean = false
) {
    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "title" to title,
        "message" to message,
        "timestamp" to timestamp,
        "type" to type,
        "studentId" to studentId,
        "studentEmail" to studentEmail,
        "isRead" to isRead
    )

    companion object {
        fun fromFirestore(docId: String, map: Map<String, Any?>): AdminNotification {
            return AdminNotification(
                id = docId,
                title = map["title"] as? String ?: "",
                message = map["message"] as? String ?: "",
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                type = map["type"] as? String ?: "INFO",
                studentId = map["studentId"] as? String,
                studentEmail = map["studentEmail"] as? String,
                isRead = map["isRead"] as? Boolean ?: false
            )
        }
    }
}

enum class FirestoreSyncStatus(val label: String, val isConnected: Boolean) {
    CONNECTED("Connected (Live Cloud Sync)", true),
    SYNCING("Syncing with Firestore...", true),
    OFFLINE_PERSISTENT("Offline Cached (Cloud Sync Ready)", false),
    ERROR("Connection Error (Local Fallback)", false)
}

enum class AdminTab(val title: String, val urduTitle: String) {
    UPLOAD_PAPER("Upload Paper", "پرچہ اپلوڈ"),
    STUDENTS("Students Directory", "طلباء کی فہرست"),
    ACTIVITY_TRACKING("Live Tracking", "سرگرمی ٹریکنگ"),
    NOTIFICATIONS("Admin Alerts", "ایڈمن الرٹس"),
    GITHUB_SYNC("Cloud & Sync", "کلاؤڈ سنک")
}

enum class ActivityEventType(val displayName: String, val urduLabel: String, val emoji: String) {
    SCREEN_VIEW("Navigation", "صفحہ رسائی", "🧭"),
    PAPER_VIEW("Paper Read", "پرچہ مطالعہ", "📖"),
    PAPER_DOWNLOAD("Downloaded", "ڈاؤن لوڈ", "⬇️"),
    PAPER_BOOKMARK("Bookmarked", "بُک مارک", "🔖"),
    SEARCH("Search Query", "تلاش", "🔍"),
    PROFILE_UPDATE("Profile Saved", "پروفائل اپڈیٹ", "👤"),
    FEE_SUBMISSION("Fee Proof", "فیس ادائیگی", "💳"),
    LOGIN("Student Session", "سیشن", "🔐")
}

data class StudentActivityEvent(
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val eventType: ActivityEventType = ActivityEventType.SCREEN_VIEW,
    val sectionName: String = "",
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val classLevel: String? = null,
    val subject: String? = null,
    val year: String? = null,
    val board: String? = null
) {
    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "studentId" to studentId,
        "studentName" to studentName,
        "eventType" to eventType.name,
        "sectionName" to sectionName,
        "details" to details,
        "timestamp" to timestamp,
        "classLevel" to classLevel,
        "subject" to subject,
        "year" to year,
        "board" to board
    )

    companion object {
        fun fromFirestore(docId: String, map: Map<String, Any?>): StudentActivityEvent {
            val typeStr = map["eventType"] as? String ?: ActivityEventType.SCREEN_VIEW.name
            val type = try {
                ActivityEventType.valueOf(typeStr)
            } catch (_: Exception) {
                ActivityEventType.SCREEN_VIEW
            }
            return StudentActivityEvent(
                id = docId,
                studentId = map["studentId"] as? String ?: "",
                studentName = map["studentName"] as? String ?: "Student",
                eventType = type,
                sectionName = map["sectionName"] as? String ?: "",
                details = map["details"] as? String ?: "",
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                classLevel = map["classLevel"] as? String,
                subject = map["subject"] as? String,
                year = map["year"] as? String,
                board = map["board"] as? String
            )
        }
    }
}

data class AdminAnnouncement(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val targetClass: String = "ALL", // "ALL", "9th", "10th", "11th", "12th"
    val timestamp: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val priority: String = "NORMAL" // "HIGH", "NORMAL"
) {
    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "title" to title,
        "message" to message,
        "targetClass" to targetClass,
        "timestamp" to timestamp,
        "isActive" to isActive,
        "priority" to priority
    )

    companion object {
        fun fromFirestore(docId: String, map: Map<String, Any?>): AdminAnnouncement {
            return AdminAnnouncement(
                id = docId,
                title = map["title"] as? String ?: "",
                message = map["message"] as? String ?: "",
                targetClass = map["targetClass"] as? String ?: "ALL",
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                isActive = map["isActive"] as? Boolean ?: true,
                priority = map["priority"] as? String ?: "NORMAL"
            )
        }
    }
}
