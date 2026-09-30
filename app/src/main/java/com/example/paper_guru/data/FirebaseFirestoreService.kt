package com.example.paper_guru.data

import android.content.Context
import android.util.Log
import com.example.paper_guru.model.*
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object FirebaseFirestoreService {
    private const val TAG = "FirebaseFirestore"

    private const val COLLECTION_STUDENTS = "students"
    private const val COLLECTION_PAPERS = "papers"
    private const val COLLECTION_SUBSCRIPTIONS = "subscriptions"
    private const val COLLECTION_NOTIFICATIONS = "admin_notifications"
    private const val COLLECTION_ACTIVITIES = "student_activities"
    private const val COLLECTION_ANNOUNCEMENTS = "announcements"

    private var firestoreInstance: FirebaseFirestore? = null
    private var isInitialized = false

    var currentSyncStatus: FirestoreSyncStatus = FirestoreSyncStatus.OFFLINE_PERSISTENT
        private set

    /**
     * Initializes Firebase and Firestore with offline caching support.
     * Works seamlessly both with google-services.json and programmatically.
     */
    fun initialize(context: Context): FirebaseFirestore? {
        if (isInitialized && firestoreInstance != null) {
            return firestoreInstance
        }

        try {
            val app = if (FirebaseApp.getApps(context).isEmpty()) {
                try {
                    FirebaseApp.initializeApp(context)
                } catch (e: Exception) {
                    Log.d(TAG, "Default initializeApp failed, applying programmatic fallback: ${e.message}")
                    val options = FirebaseOptions.Builder()
                        .setApplicationId(context.packageName)
                        .setProjectId("paperguru-b7e12")
                        .setApiKey("AIzaSyPaperGuruFirebaseLiveClient2026")
                        .setDatabaseUrl("https://paperguru-b7e12.firebaseio.com")
                        .setStorageBucket("paperguru-b7e12.appspot.com")
                        .build()
                    FirebaseApp.initializeApp(context, options, "PaperGuruFirestoreApp")
                }
            } else {
                FirebaseApp.getInstance()
            }

            val db = if (app != null) FirebaseFirestore.getInstance(app) else FirebaseFirestore.getInstance()

            // Enable local disk persistence for seamless offline-first capability
            try {
                val settings = FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build()
                db.firestoreSettings = settings
            } catch (e: Exception) {
                Log.w(TAG, "Persistence setting: ${e.message}")
            }

            firestoreInstance = db
            isInitialized = true
            currentSyncStatus = FirestoreSyncStatus.CONNECTED
            Log.i(TAG, "Firebase Firestore initialized successfully.")
            return db
        } catch (e: Exception) {
            Log.e(TAG, "Firestore initialization error: ${e.message}")
            currentSyncStatus = FirestoreSyncStatus.ERROR
            return null
        }
    }

    private fun getDb(context: Context): FirebaseFirestore? {
        return firestoreInstance ?: initialize(context)
    }

    // -------------------------------------------------------------
    // Student User Profiles & Subscription Statuses
    // -------------------------------------------------------------

    suspend fun fetchStudents(context: Context): Result<List<StudentUser>> = withContext(Dispatchers.IO) {
        val db = getDb(context) ?: return@withContext Result.failure(Exception("Firestore not initialized"))
        try {
            val snapshot = db.collection(COLLECTION_STUDENTS).get().awaitTask()
            val list = mutableListOf<StudentUser>()
            for (doc in snapshot.documents) {
                val data = doc.data
                if (data != null) {
                    list.add(StudentUser.fromFirestore(doc.id, data))
                }
            }
            currentSyncStatus = FirestoreSyncStatus.CONNECTED
            Result.success(list)
        } catch (e: Exception) {
            Log.w(TAG, "fetchStudents Firestore error: ${e.message}")
            currentSyncStatus = FirestoreSyncStatus.OFFLINE_PERSISTENT
            Result.failure(e)
        }
    }

    suspend fun saveOrUpdateStudent(context: Context, student: StudentUser): Result<Unit> = withContext(Dispatchers.IO) {
        val db = getDb(context) ?: return@withContext Result.failure(Exception("Firestore not initialized"))
        try {
            val docRef = db.collection(COLLECTION_STUDENTS).document(student.id)
            docRef.set(student.toFirestoreMap(), SetOptions.merge()).awaitTask()
            currentSyncStatus = FirestoreSyncStatus.CONNECTED
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "saveOrUpdateStudent error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateStudentBlockStatus(context: Context, studentId: String, isBlocked: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val db = getDb(context) ?: return@withContext Result.failure(Exception("Firestore not initialized"))
        try {
            val updates = mapOf(
                "isBlocked" to isBlocked,
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection(COLLECTION_STUDENTS).document(studentId).update(updates).awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "updateStudentBlockStatus error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateStudentSubscription(
        context: Context,
        studentId: String,
        hasAdvance: Boolean,
        plan: String?,
        expiry: String?,
        feeStatus: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val db = getDb(context) ?: return@withContext Result.failure(Exception("Firestore not initialized"))
        try {
            val updates = mapOf(
                "hasAdvanceSubscription" to hasAdvance,
                "subscriptionPlan" to plan,
                "subscriptionExpiry" to expiry,
                "feePaymentStatus" to feeStatus,
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection(COLLECTION_STUDENTS).document(studentId).update(updates).awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "updateStudentSubscription error: ${e.message}")
            Result.failure(e)
        }
    }

    fun listenToStudents(context: Context, onUpdate: (List<StudentUser>) -> Unit): ListenerRegistration? {
        val db = getDb(context) ?: return null
        return try {
            db.collection(COLLECTION_STUDENTS).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "listenToStudents error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = mutableListOf<StudentUser>()
                    for (doc in snapshot.documents) {
                        val data = doc.data
                        if (data != null) {
                            list.add(StudentUser.fromFirestore(doc.id, data))
                        }
                    }
                    onUpdate(list)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "listenToStudents setup error: ${e.message}")
            null
        }
    }

    // -------------------------------------------------------------
    // Paper Documents & Uploads
    // -------------------------------------------------------------

    suspend fun fetchPaperUploads(context: Context): Result<List<PaperItem>> = withContext(Dispatchers.IO) {
        val db = getDb(context) ?: return@withContext Result.failure(Exception("Firestore not initialized"))
        try {
            val snapshot = db.collection(COLLECTION_PAPERS).get().awaitTask()
            val list = mutableListOf<PaperItem>()
            for (doc in snapshot.documents) {
                val data = doc.data
                if (data != null) {
                    list.add(PaperItem.fromFirestore(doc.id, data))
                }
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.w(TAG, "fetchPaperUploads error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun uploadPaper(context: Context, paper: PaperItem): Result<Unit> = withContext(Dispatchers.IO) {
        val db = getDb(context) ?: return@withContext Result.failure(Exception("Firestore not initialized"))
        try {
            val docRef = db.collection(COLLECTION_PAPERS).document(paper.uniqueKey)
            docRef.set(paper.toFirestoreMap(), SetOptions.merge()).awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "uploadPaper error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deletePaper(context: Context, paperKey: String): Result<Unit> = withContext(Dispatchers.IO) {
        val db = getDb(context) ?: return@withContext Result.failure(Exception("Firestore not initialized"))
        try {
            db.collection(COLLECTION_PAPERS).document(paperKey).delete().awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "deletePaper error: ${e.message}")
            Result.failure(e)
        }
    }

    fun listenToPaperUploads(context: Context, onUpdate: (List<PaperItem>) -> Unit): ListenerRegistration? {
        val db = getDb(context) ?: return null
        return try {
            db.collection(COLLECTION_PAPERS).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "listenToPaperUploads error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = mutableListOf<PaperItem>()
                    for (doc in snapshot.documents) {
                        val data = doc.data
                        if (data != null) {
                            list.add(PaperItem.fromFirestore(doc.id, data))
                        }
                    }
                    onUpdate(list)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "listenToPaperUploads setup error: ${e.message}")
            null
        }
    }

    // -------------------------------------------------------------
    // Subscription Records & Fee Verifications
    // -------------------------------------------------------------

    suspend fun submitSubscriptionRecord(context: Context, record: SubscriptionRecord): Result<Unit> = withContext(Dispatchers.IO) {
        val db = getDb(context) ?: return@withContext Result.failure(Exception("Firestore not initialized"))
        try {
            val docId = if (record.id.isNotBlank()) record.id else "sub_${System.currentTimeMillis()}"
            val docRef = db.collection(COLLECTION_SUBSCRIPTIONS).document(docId)
            docRef.set(record.copy(id = docId).toFirestoreMap(), SetOptions.merge()).awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "submitSubscriptionRecord error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun fetchSubscriptionRecords(context: Context): Result<List<SubscriptionRecord>> = withContext(Dispatchers.IO) {
        val db = getDb(context) ?: return@withContext Result.failure(Exception("Firestore not initialized"))
        try {
            val snapshot = db.collection(COLLECTION_SUBSCRIPTIONS).get().awaitTask()
            val list = mutableListOf<SubscriptionRecord>()
            for (doc in snapshot.documents) {
                val data = doc.data
                if (data != null) {
                    list.add(SubscriptionRecord.fromFirestore(doc.id, data))
                }
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.w(TAG, "fetchSubscriptionRecords error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateSubscriptionStatus(context: Context, recordId: String, status: String): Result<Unit> = withContext(Dispatchers.IO) {
        val db = getDb(context) ?: return@withContext Result.failure(Exception("Firestore not initialized"))
        try {
            val updates = mapOf(
                "status" to status,
                "verifiedAt" to System.currentTimeMillis()
            )
            db.collection(COLLECTION_SUBSCRIPTIONS).document(recordId).update(updates).awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "updateSubscriptionStatus error: ${e.message}")
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // Admin Notifications & Real-Time Alerts
    // -------------------------------------------------------------

    suspend fun saveNotification(context: Context, notif: AdminNotification): Result<Unit> = withContext(Dispatchers.IO) {
        val db = getDb(context) ?: return@withContext Result.failure(Exception("Firestore not initialized"))
        try {
            val docRef = db.collection(COLLECTION_NOTIFICATIONS).document(notif.id)
            docRef.set(notif.toFirestoreMap(), SetOptions.merge()).awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "saveNotification error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun fetchNotifications(context: Context): Result<List<AdminNotification>> = withContext(Dispatchers.IO) {
        val db = getDb(context) ?: return@withContext Result.failure(Exception("Firestore not initialized"))
        try {
            val snapshot = db.collection(COLLECTION_NOTIFICATIONS).get().awaitTask()
            val list = mutableListOf<AdminNotification>()
            for (doc in snapshot.documents) {
                val data = doc.data
                if (data != null) {
                    list.add(AdminNotification.fromFirestore(doc.id, data))
                }
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.w(TAG, "fetchNotifications error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(context: Context, notifId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val db = getDb(context) ?: return@withContext Result.failure(Exception("Firestore not initialized"))
        try {
            db.collection(COLLECTION_NOTIFICATIONS).document(notifId).update("isRead", true).awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "markNotificationRead error: ${e.message}")
            Result.failure(e)
        }
    }

    fun listenToNotifications(context: Context, onUpdate: (List<AdminNotification>) -> Unit): ListenerRegistration? {
        val db = getDb(context) ?: return null
        return try {
            db.collection(COLLECTION_NOTIFICATIONS).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "listenToNotifications error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = mutableListOf<AdminNotification>()
                    for (doc in snapshot.documents) {
                        val data = doc.data
                        if (data != null) {
                            list.add(AdminNotification.fromFirestore(doc.id, data))
                        }
                    }
                    onUpdate(list)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "listenToNotifications setup error: ${e.message}")
            null
        }
    }

    // -------------------------------------------------------------
    // Student Activity & Event Tracking
    // -------------------------------------------------------------

    suspend fun saveActivity(context: Context, activity: StudentActivityEvent): Result<Unit> = withContext(Dispatchers.IO) {
        val db = getDb(context) ?: return@withContext Result.failure(Exception("Firestore not initialized"))
        try {
            val docId = if (activity.id.isNotBlank()) activity.id else "act_${System.currentTimeMillis()}"
            val docRef = db.collection(COLLECTION_ACTIVITIES).document(docId)
            docRef.set(activity.copy(id = docId).toFirestoreMap(), SetOptions.merge()).awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "saveActivity error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun fetchActivities(context: Context): Result<List<StudentActivityEvent>> = withContext(Dispatchers.IO) {
        val db = getDb(context) ?: return@withContext Result.failure(Exception("Firestore not initialized"))
        try {
            val snapshot = db.collection(COLLECTION_ACTIVITIES).get().awaitTask()
            val list = mutableListOf<StudentActivityEvent>()
            for (doc in snapshot.documents) {
                val data = doc.data
                if (data != null) {
                    list.add(StudentActivityEvent.fromFirestore(doc.id, data))
                }
            }
            list.sortByDescending { it.timestamp }
            Result.success(list)
        } catch (e: Exception) {
            Log.w(TAG, "fetchActivities error: ${e.message}")
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // Admin Announcements & Portal Broadcasts
    // -------------------------------------------------------------

    suspend fun saveAnnouncement(context: Context, announcement: AdminAnnouncement): Result<Unit> = withContext(Dispatchers.IO) {
        val db = getDb(context) ?: return@withContext Result.failure(Exception("Firestore not initialized"))
        try {
            val docId = if (announcement.id.isNotBlank()) announcement.id else "ann_${System.currentTimeMillis()}"
            val docRef = db.collection(COLLECTION_ANNOUNCEMENTS).document(docId)
            docRef.set(announcement.copy(id = docId).toFirestoreMap(), SetOptions.merge()).awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "saveAnnouncement error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun fetchAnnouncements(context: Context): Result<List<AdminAnnouncement>> = withContext(Dispatchers.IO) {
        val db = getDb(context) ?: return@withContext Result.failure(Exception("Firestore not initialized"))
        try {
            val snapshot = db.collection(COLLECTION_ANNOUNCEMENTS).get().awaitTask()
            val list = mutableListOf<AdminAnnouncement>()
            for (doc in snapshot.documents) {
                val data = doc.data
                if (data != null) {
                    list.add(AdminAnnouncement.fromFirestore(doc.id, data))
                }
            }
            list.sortByDescending { it.timestamp }
            Result.success(list)
        } catch (e: Exception) {
            Log.w(TAG, "fetchAnnouncements error: ${e.message}")
            Result.failure(e)
        }
    }
}

/**
 * Lightweight cancellation-aware extension to await Google Play Tasks
 * without requiring additional third-party dependencies.
 */
suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        if (cont.isActive) cont.resume(result)
    }
    addOnFailureListener { exception ->
        if (cont.isActive) cont.resumeWithException(exception)
    }
    addOnCanceledListener {
        if (cont.isActive) cont.cancel()
    }
}
