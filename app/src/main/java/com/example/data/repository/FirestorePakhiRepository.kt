package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.FirestoreAdminLog
import com.example.data.model.FirestoreAnnouncement
import com.example.data.model.FirestoreAppConfig
import com.example.data.model.FirestoreBanner
import com.example.data.model.FirestoreChat
import com.example.data.model.FirestoreMessage
import com.example.data.model.PublicUserProfile
import com.example.data.model.UserProfile
import com.example.util.NotificationHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.random.Random

class FirestorePakhiRepository(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
        context.getString(R.string.firestore_database_id)
    )

    private val prefs = context.getSharedPreferences("pakhi_admin_prefs", Context.MODE_PRIVATE)

    private val _currentUserProfile = MutableStateFlow<UserProfile?>(null)
    val currentUserProfile = _currentUserProfile.asStateFlow()

    // Admin Session State
    private val _isAdminLoggedIn = MutableStateFlow(false)
    val isAdminLoggedIn = _isAdminLoggedIn.asStateFlow()

    private val _adminMustChangePassword = MutableStateFlow(false)
    val adminMustChangePassword = _adminMustChangePassword.asStateFlow()

    private val usersRef = db.collection("users")
    private val publicProfilesRef = db.collection("public_profiles")
    private val chatsRef = db.collection("chats")
    private val announcementsRef = db.collection("announcements")
    private val bannersRef = db.collection("banners")
    private val appConfigRef = db.collection("app_config")
    private val adminLogsRef = db.collection("admin_logs")

    init {
        // Observe auth state changes
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                scope.launch {
                    loadOrCreateUserProfile(user)
                    updatePresence(true)
                }
            } else {
                _currentUserProfile.value = null
            }
        }
    }

    val authUserFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("ব্যবহারকারীকে অবশ্যই সাইন ইন থাকতে হবে।")
    }

    suspend fun loadOrCreateUserProfile(firebaseUser: FirebaseUser): UserProfile {
        return withContext(Dispatchers.IO) {
            val uid = firebaseUser.uid
            val doc = usersRef.document(uid).get().await()

            if (doc.exists()) {
                val profile = doc.toObject(UserProfile::class.java) ?: UserProfile(uid = uid)
                _currentUserProfile.value = profile
                profile
            } else {
                // Generate a unique Pakhi ID: PK- + 6 digits
                var generatedPakhiId: String
                var isUnique = false
                var attempts = 0

                do {
                    val randNum = Random.nextInt(100000, 999999)
                    generatedPakhiId = "PK-$randNum"
                    val pubDoc = publicProfilesRef.document(generatedPakhiId).get().await()
                    if (!pubDoc.exists()) {
                        isUnique = true
                    }
                    attempts++
                } while (!isUnique && attempts < 10)

                val displayName = firebaseUser.displayName?.ifBlank { "পাখি ব্যবহারকারী" } ?: "পাখি ব্যবহারকারী"
                val email = firebaseUser.email ?: ""
                val photoUrl = firebaseUser.photoUrl?.toString()

                val newProfile = UserProfile(
                    id = uid,
                    uid = uid,
                    pakhiId = generatedPakhiId,
                    displayName = displayName,
                    email = email,
                    photoUrl = photoUrl,
                    status = "active",
                    isOnline = true
                )

                // 1. Save private profile
                val userPayload = mutableMapOf<String, Any>(
                    "uid" to uid,
                    "pakhiId" to generatedPakhiId,
                    "displayName" to displayName,
                    "email" to email,
                    "status" to "active",
                    "isOnline" to true,
                    "lastSeen" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (photoUrl != null) {
                    userPayload["photoUrl"] = photoUrl
                }

                usersRef.document(uid).set(userPayload).await()

                // 2. Save public profile for lookup WITHOUT EMAIL (privacy preserved)
                val publicPayload = mutableMapOf<String, Any>(
                    "pakhiId" to generatedPakhiId,
                    "uid" to uid,
                    "displayName" to displayName,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                if (photoUrl != null) {
                    publicPayload["photoUrl"] = photoUrl
                }

                publicProfilesRef.document(generatedPakhiId).set(publicPayload).await()

                _currentUserProfile.value = newProfile
                newProfile
            }
        }
    }

    suspend fun updatePresence(isOnline: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        withContext(Dispatchers.IO) {
            try {
                usersRef.document(uid).update(
                    mapOf(
                        "isOnline" to isOnline,
                        "lastSeen" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (e: Exception) {
                Log.w("FirestoreRepo", "Failed to update presence: ${e.message}")
            }
        }
    }

    // Search user by Pakhi ID - STRICTLY RETURNS PUBLIC DATA (NO EMAIL)
    suspend fun searchUserByPakhiId(pakhiIdInput: String): Result<PublicUserProfile> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUid = auth.currentUser?.uid ?: ""
                var trimmed = pakhiIdInput.trim()
                if (trimmed.startsWith("@")) {
                    trimmed = trimmed.substring(1)
                }
                trimmed = trimmed.uppercase()

                val pubDoc = publicProfilesRef.document(trimmed).get().await()
                if (!pubDoc.exists()) {
                    return@withContext Result.failure(Exception("এই আইডি দিয়ে কাউকে পাওয়া যায়নি। আইডি আবার যাচাই করুন।"))
                }

                val publicProfile = pubDoc.toObject(PublicUserProfile::class.java)
                if (publicProfile == null) {
                    return@withContext Result.failure(Exception("ব্যবহারকারীর তথ্য লোড করা যায়নি।"))
                }

                if (publicProfile.uid == currentUid) {
                    return@withContext Result.failure(Exception("এটি আপনার নিজের Pakhi ID!"))
                }

                Result.success(publicProfile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Real-time Chat List for Current User
    fun observeUserChats(): Flow<List<FirestoreChat>> {
        val uid = auth.currentUser?.uid ?: return flow { emit(emptyList()) }
        return chatsRef.whereArrayContains("members", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FirestoreChat::class.java)
                    .sortedByDescending { it.lastMessageAt?.toDate()?.time ?: 0L }
            }
            .catch { error ->
                Log.e("FirestoreRepo", "Error observing chats: ${error.message}")
                emit(emptyList())
            }
    }

    suspend fun getOrCreateChat(otherPublicUser: PublicUserProfile): String {
        return withContext(Dispatchers.IO) {
            val myUid = requireUserId()
            var myProfile = _currentUserProfile.value
            if (myProfile == null) {
                val doc = usersRef.document(myUid).get().await()
                myProfile = doc.toObject(UserProfile::class.java)
                if (myProfile != null) {
                    _currentUserProfile.value = myProfile
                }
            }

            val myPakhiId = myProfile?.pakhiId ?: "PK-USER"
            val myName = myProfile?.displayName ?: "পাখি"

            val sortedUids = listOf(myUid, otherPublicUser.uid).sorted()
            val chatId = "${sortedUids[0]}_${sortedUids[1]}"

            val chatDoc = chatsRef.document(chatId).get().await()
            if (!chatDoc.exists()) {
                val newChatPayload = mutableMapOf<String, Any>(
                    "chatId" to chatId,
                    "members" to sortedUids,
                    "memberPakhiIds" to mapOf(
                        myUid to myPakhiId,
                        otherPublicUser.uid to otherPublicUser.pakhiId
                    ),
                    "memberNames" to mapOf(
                        myUid to myName,
                        otherPublicUser.uid to otherPublicUser.displayName
                    ),
                    "lastMessageText" to "",
                    "lastMessageSenderId" to "",
                    "lastMessageAt" to FieldValue.serverTimestamp(),
                    "unreadCount" to mapOf(
                        myUid to 0L,
                        otherPublicUser.uid to 0L
                    ),
                    "createdAt" to FieldValue.serverTimestamp()
                )
                chatsRef.document(chatId).set(newChatPayload).await()
            }
            chatId
        }
    }

    // Real-time Messages in a Chat
    fun observeMessages(chatId: String): Flow<List<FirestoreMessage>> {
        return chatsRef.document(chatId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FirestoreMessage::class.java)
            }
            .catch { error ->
                Log.e("FirestoreRepo", "Error observing messages for $chatId: ${error.message}")
                emit(emptyList())
            }
    }

    suspend fun sendMessage(chatId: String, text: String, recipientId: String) {
        withContext(Dispatchers.IO) {
            val myUid = requireUserId()
            val messageId = "msg_${UUID.randomUUID().toString().take(12)}"

            val msgPayload = mutableMapOf<String, Any>(
                "messageId" to messageId,
                "chatId" to chatId,
                "senderId" to myUid,
                "recipientId" to recipientId,
                "text" to text.trim(),
                "status" to "sent",
                "createdAt" to FieldValue.serverTimestamp()
            )

            val chatDocRef = chatsRef.document(chatId)
            val messageDocRef = chatDocRef.collection("messages").document(messageId)

            db.runBatch { batch ->
                batch.set(messageDocRef, msgPayload)
                batch.update(
                    chatDocRef,
                    mapOf(
                        "lastMessageText" to text.trim(),
                        "lastMessageSenderId" to myUid,
                        "lastMessageAt" to FieldValue.serverTimestamp(),
                        "unreadCount.$recipientId" to FieldValue.increment(1)
                    )
                )
            }.await()
        }
    }

    suspend fun markChatSeen(chatId: String) {
        val myUid = auth.currentUser?.uid ?: return
        withContext(Dispatchers.IO) {
            try {
                // Reset unread count for current user
                chatsRef.document(chatId).update("unreadCount.$myUid", 0L).await()

                // Mark unseen messages addressed to current user as seen
                val unseenSnapshot = chatsRef.document(chatId)
                    .collection("messages")
                    .whereEqualTo("recipientId", myUid)
                    .whereIn("status", listOf("sent", "delivered"))
                    .get()
                    .await()

                if (!unseenSnapshot.isEmpty) {
                    val batch = db.batch()
                    for (doc in unseenSnapshot.documents) {
                        batch.update(
                            doc.reference,
                            mapOf(
                                "status" to "seen",
                                "seenAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    batch.commit().await()
                }
            } catch (e: Exception) {
                Log.w("FirestoreRepo", "Failed to mark chat seen: ${e.message}")
            }
        }
    }

    suspend fun deleteMessageForEveryone(chatId: String, messageId: String) {
        val myUid = requireUserId()
        withContext(Dispatchers.IO) {
            try {
                chatsRef.document(chatId).collection("messages").document(messageId).update(
                    mapOf(
                        "deletedForEveryone" to true,
                        "text" to "এই বার্তাটি মুছে ফেলা হয়েছে"
                    )
                ).await()
            } catch (e: Exception) {
                Log.e("FirestoreRepo", "Failed to delete message: ${e.message}")
            }
        }
    }

    // ----------------------------------------------------
    // REAL CLOUD FIRESTORE ADMIN & METRICS IMPLEMENTATION
    // ----------------------------------------------------

    fun observeAllUsers(): Flow<List<UserProfile>> {
        return usersRef
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(UserProfile::class.java).sortedByDescending {
                    it.createdAt?.toDate()?.time ?: 0L
                }
            }
            .catch { error ->
                Log.e("FirestoreRepo", "Error observing all users: ${error.message}")
                emit(emptyList())
            }
    }

    suspend fun toggleUserStatus(uid: String, newStatus: String) {
        withContext(Dispatchers.IO) {
            try {
                usersRef.document(uid).update(
                    mapOf(
                        "status" to newStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                logAdminAction(
                    action = if (newStatus == "active") "ইউজার সক্রিয় করা হয়েছে" else "ইউজার নিষ্ক্রিয়/ব্লক করা হয়েছে",
                    targetType = "USER",
                    targetId = uid,
                    meta = "স্ট্যাটাস পরিবর্তিত: $newStatus"
                )
            } catch (e: Exception) {
                Log.e("FirestoreRepo", "Failed to toggle user status: ${e.message}")
            }
        }
    }

    fun observeAnnouncements(): Flow<List<FirestoreAnnouncement>> {
        return announcementsRef
            .whereEqualTo("published", true)
            .snapshots()
            .map { it.toObjects(FirestoreAnnouncement::class.java).sortedByDescending { ann -> ann.pinned } }
            .catch { emit(emptyList()) }
    }

    fun observeAllAnnouncements(): Flow<List<FirestoreAnnouncement>> {
        return announcementsRef
            .snapshots()
            .map { it.toObjects(FirestoreAnnouncement::class.java).sortedByDescending { ann -> ann.pinned } }
            .catch { emit(emptyList()) }
    }

    suspend fun saveAnnouncement(
        title: String,
        body: String,
        imageUrl: String? = null,
        pinned: Boolean = false,
        id: String? = null
    ): String {
        return withContext(Dispatchers.IO) {
            val announcementId = id ?: "ann_${System.currentTimeMillis()}"
            val payload = mutableMapOf<String, Any>(
                "title" to title.trim(),
                "body" to body.trim(),
                "pinned" to pinned,
                "published" to true,
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (!imageUrl.isNullOrBlank()) {
                payload["imageUrl"] = imageUrl.trim()
            }

            announcementsRef.document(announcementId).set(payload).await()

            // Trigger local push notification on device
            NotificationHelper.showPushNotification(
                context = context,
                title = if (pinned) "📌 $title" else title,
                body = body
            )

            logAdminAction(
                action = "ঘোষণা প্রকাশ",
                targetType = "ANNOUNCEMENT",
                targetId = announcementId,
                meta = "শিরোনাম: $title (পিন: $pinned)"
            )

            announcementId
        }
    }

    suspend fun deleteAnnouncement(id: String) {
        withContext(Dispatchers.IO) {
            try {
                announcementsRef.document(id).delete().await()
                logAdminAction(
                    action = "ঘোষণা মোছা",
                    targetType = "ANNOUNCEMENT",
                    targetId = id,
                    meta = "ঘোষণা মুছে ফেলা হয়েছে"
                )
            } catch (e: Exception) {
                Log.e("FirestoreRepo", "Failed to delete announcement: ${e.message}")
            }
        }
    }

    fun observeAllBanners(): Flow<List<FirestoreBanner>> {
        return bannersRef
            .snapshots()
            .map { it.toObjects(FirestoreBanner::class.java) }
            .catch { emit(emptyList()) }
    }

    suspend fun saveBanner(
        title: String,
        description: String,
        imageUrl: String? = null,
        active: Boolean = true,
        id: String? = null
    ) {
        withContext(Dispatchers.IO) {
            val bannerId = id ?: "banner_${System.currentTimeMillis()}"
            val payload = mutableMapOf<String, Any>(
                "title" to title.trim(),
                "description" to description.trim(),
                "active" to active,
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (!imageUrl.isNullOrBlank()) {
                payload["imageUrl"] = imageUrl.trim()
            }

            bannersRef.document(bannerId).set(payload).await()

            logAdminAction(
                action = "ব্যানার আপডেট",
                targetType = "BANNER",
                targetId = bannerId,
                meta = "ব্যানার: $title"
            )
        }
    }

    suspend fun deleteBanner(id: String) {
        withContext(Dispatchers.IO) {
            try {
                bannersRef.document(id).delete().await()
                logAdminAction(
                    action = "ব্যানার মোছা",
                    targetType = "BANNER",
                    targetId = id,
                    meta = "ব্যানার মুছে ফেলা হয়েছে"
                )
            } catch (e: Exception) {
                Log.e("FirestoreRepo", "Failed to delete banner: ${e.message}")
            }
        }
    }

    fun observeAppConfig(): Flow<FirestoreAppConfig?> {
        return appConfigRef.document("global")
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(FirestoreAppConfig::class.java)
                } else {
                    FirestoreAppConfig()
                }
            }
            .catch { emit(FirestoreAppConfig()) }
    }

    suspend fun updateAppConfig(maintenance: Boolean, message: String, emergencyMsg: String?) {
        withContext(Dispatchers.IO) {
            val payload = mutableMapOf<String, Any>(
                "maintenanceEnabled" to maintenance,
                "maintenanceMessage" to message.trim(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!emergencyMsg.isNullOrBlank()) {
                payload["emergencyMessage"] = emergencyMsg.trim()
            } else {
                payload["emergencyMessage"] = FieldValue.delete()
            }

            appConfigRef.document("global").set(payload).await()

            logAdminAction(
                action = "সিস্টেম সেটিংস আপডেট",
                targetType = "CONFIG",
                targetId = "global",
                meta = "মেইনটেন্যান্স: $maintenance | জরুরি বার্তা: ${emergencyMsg ?: "নেই"}"
            )
        }
    }

    fun observeAdminLogs(): Flow<List<FirestoreAdminLog>> {
        return adminLogsRef
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { it.toObjects(FirestoreAdminLog::class.java) }
            .catch { emit(emptyList()) }
    }

    suspend fun logAdminAction(action: String, targetType: String, targetId: String, meta: String) {
        withContext(Dispatchers.IO) {
            try {
                val logPayload = mapOf(
                    "action" to action,
                    "targetType" to targetType,
                    "targetId" to targetId,
                    "meta" to meta,
                    "timestamp" to FieldValue.serverTimestamp()
                )
                adminLogsRef.add(logPayload).await()
            } catch (e: Exception) {
                Log.w("FirestoreRepo", "Failed to write admin log: ${e.message}")
            }
        }
    }

    suspend fun getDashboardStats(): DashboardStats {
        return withContext(Dispatchers.IO) {
            try {
                val usersSnap = usersRef.get().await()
                val totalUsers = usersSnap.size()
                val activeUsers = usersSnap.documents.count {
                    it.getString("status") == "active"
                }

                val chatsSnap = chatsRef.get().await()
                val totalChats = chatsSnap.size()

                // Approximate messages or count from chats
                var totalMessages = 0
                for (doc in chatsSnap.documents) {
                    if (doc.getString("lastMessageText")?.isNotBlank() == true) {
                        totalMessages++
                    }
                }

                DashboardStats(
                    totalUsers = totalUsers,
                    activeUsers = activeUsers,
                    totalChats = totalChats,
                    totalMessages = totalMessages.coerceAtLeast(totalChats)
                )
            } catch (e: Exception) {
                Log.w("FirestoreRepo", "Error fetching stats: ${e.message}")
                DashboardStats()
            }
        }
    }

    // ----------------------------------------------------
    // SECURE ADMIN AUTHENTICATION
    // User Id: pakhi
    // Password: pakhi@112233#
    // ----------------------------------------------------

    private fun getStoredAdminPassword(): String {
        return prefs.getString("admin_password", "pakhi@112233#") ?: "pakhi@112233#"
    }

    fun loginAdmin(id: String, pass: String): Result<Boolean> {
        val currentPass = getStoredAdminPassword()
        val trimmedId = id.trim()
        val trimmedPass = pass.trim()

        if (trimmedId.equals("pakhi", ignoreCase = true) && trimmedPass == currentPass) {
            _isAdminLoggedIn.value = true
            return Result.success(true)
        }
        return Result.failure(Exception("ভুল ইউজার আইডি অথবা পাসওয়ার্ড।"))
    }

    fun logoutAdmin() {
        _isAdminLoggedIn.value = false
    }

    fun changeAdminPassword(newPass: String): Result<Boolean> {
        if (newPass.length < 6) {
            return Result.failure(Exception("পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে।"))
        }
        prefs.edit().putString("admin_password", newPass).apply()
        _adminMustChangePassword.value = false
        scope.launch {
            logAdminAction("পাসওয়ার্ড পরিবর্তন", "ADMIN", "pakhi", "অ্যাডমিন পাসওয়ার্ড সফলভাবে পরিবর্তন করা হয়েছে")
        }
        return Result.success(true)
    }

    suspend fun signOut() {
        updatePresence(false)
        auth.signOut()
        _currentUserProfile.value = null
    }
}
