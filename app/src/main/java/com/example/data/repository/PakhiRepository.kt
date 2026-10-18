package com.example.data.repository

import com.example.data.local.PakhiDatabase
import com.example.data.local.entity.AdminLogEntity
import com.example.data.local.entity.AnnouncementEntity
import com.example.data.local.entity.AppConfigEntity
import com.example.data.local.entity.BannerEntity
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.random.Random

data class DashboardStats(
    val totalUsers: Int = 0,
    val activeUsers: Int = 0,
    val totalChats: Int = 0,
    val totalMessages: Int = 0
)

class PakhiRepository(
    private val database: PakhiDatabase,
    private val scope: CoroutineScope
) {
    private val userDao = database.userDao()
    private val chatDao = database.chatDao()
    private val messageDao = database.messageDao()
    private val announcementDao = database.announcementDao()
    private val bannerDao = database.bannerDao()
    private val adminLogDao = database.adminLogDao()
    private val appConfigDao = database.appConfigDao()

    // Current Logged-in User ID (Defaults to first user "usr_tanvir_1")
    private val _currentUserId = MutableStateFlow<String?>("usr_tanvir_1")
    val currentUserId = _currentUserId.asStateFlow()

    // Admin Session state
    private val _isAdminLoggedIn = MutableStateFlow(false)
    val isAdminLoggedIn = _isAdminLoggedIn.asStateFlow()

    private var adminPasswordCurrent = "pakhi@112233#"
    private val _adminMustChangePassword = MutableStateFlow(false)
    val adminMustChangePassword = _adminMustChangePassword.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUser: Flow<UserEntity?> = _currentUserId.flatMapLatest { uid ->
        if (uid != null) userDao.getUser(uid) else flowOf(null)
    }.distinctUntilChanged()

    val appConfig: Flow<AppConfigEntity?> = appConfigDao.getConfig()
    val publishedAnnouncements: Flow<List<AnnouncementEntity>> = announcementDao.getPublishedAnnouncements()
    val allAnnouncements: Flow<List<AnnouncementEntity>> = announcementDao.getAllAnnouncements()
    val activeBanners: Flow<List<BannerEntity>> = bannerDao.getActiveBanners()
    val allBanners: Flow<List<BannerEntity>> = bannerDao.getAllBanners()
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()
    val adminLogs: Flow<List<AdminLogEntity>> = adminLogDao.getLogs()

    init {
        // No dummy users seeded
    }

    fun switchAccount(uid: String) {
        _currentUserId.value = uid
    }

    suspend fun loginAsUser(uid: String) {
        _currentUserId.value = uid
        withContext(Dispatchers.IO) {
            val user = userDao.getUserSync(uid)
            if (user != null) {
                userDao.updateUser(user.copy(isOnline = true, lastSeen = System.currentTimeMillis()))
            }
        }
    }

    suspend fun logout() {
        val uid = _currentUserId.value
        if (uid != null) {
            withContext(Dispatchers.IO) {
                val user = userDao.getUserSync(uid)
                if (user != null) {
                    userDao.updateUser(user.copy(isOnline = false, lastSeen = System.currentTimeMillis()))
                }
            }
        }
        _currentUserId.value = null
    }

    // Google Sign-In / Account Creation
    suspend fun createOrLoginGoogleAccount(displayName: String, email: String, photoUrl: String? = null): UserEntity {
        return withContext(Dispatchers.IO) {
            val all = database.openHelper.readableDatabase
            // Check if user with this email exists
            val existing = userDao.searchUsers(email)
            // Generate unique Pakhi ID: "PK-" + 6 digits
            var generatedId: String
            do {
                val randNum = Random.nextInt(100000, 999999)
                generatedId = "PK-$randNum"
            } while (userDao.getUserByPakhiId(generatedId) != null)

            val uid = "usr_${UUID.randomUUID().toString().take(8)}"
            val newUser = UserEntity(
                uid = uid,
                pakhiId = generatedId,
                displayName = displayName.ifBlank { "পাখি ব্যবহারকারী" },
                email = email,
                photoUrl = photoUrl,
                createdAt = System.currentTimeMillis(),
                status = "active",
                isOnline = true,
                lastSeen = System.currentTimeMillis()
            )
            userDao.insertUser(newUser)
            _currentUserId.value = uid
            newUser
        }
    }

    // Search user by Pakhi ID
    suspend fun getUserByPakhiId(pakhiId: String): Result<UserEntity> {
        return withContext(Dispatchers.IO) {
            val trimmed = pakhiId.trim().uppercase()
            val currentUid = _currentUserId.value
            val user = userDao.getUserByPakhiId(trimmed)

            if (user == null) {
                Result.failure(Exception("এই আইডি দিয়ে কাউকে পাওয়া যায়নি। আইডি আবার যাচাই করুন।"))
            } else if (user.uid == currentUid) {
                Result.failure(Exception("এটি আপনার নিজের আইডি।"))
            } else if (user.status == "disabled") {
                Result.failure(Exception("এই ব্যবহারকারীর অ্যাকাউন্টটি বর্তমানে নিষ্ক্রিয় রয়েছে।"))
            } else {
                Result.success(user)
            }
        }
    }

    suspend fun getUserSync(uid: String): UserEntity? {
        return withContext(Dispatchers.IO) {
            userDao.getUserSync(uid)
        }
    }

    fun getUserFlow(uid: String): Flow<UserEntity?> {
        return userDao.getUser(uid)
    }

    // Direct Chats
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getChatsForCurrentUser(): Flow<List<ChatEntity>> {
        return _currentUserId.flatMapLatest { uid ->
            if (uid != null) chatDao.getChatsForUser(uid) else flowOf(emptyList())
        }
    }

    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>> {
        return messageDao.getMessagesForChat(chatId)
    }

    suspend fun getOrCreateChatWithUser(otherUserId: String): String {
        return withContext(Dispatchers.IO) {
            val myId = _currentUserId.value ?: throw IllegalStateException("Not logged in")
            val sorted = listOf(myId, otherUserId).sorted()
            val chatId = "${sorted[0]}_${sorted[1]}"

            val existing = chatDao.getChatSync(chatId)
            if (existing == null) {
                val newChat = ChatEntity(
                    chatId = chatId,
                    type = "direct",
                    user1Id = sorted[0],
                    user2Id = sorted[1],
                    lastMessageText = null,
                    lastMessageAt = System.currentTimeMillis(),
                    createdAt = System.currentTimeMillis()
                )
                chatDao.insertChat(newChat)
            }
            chatId
        }
    }

    suspend fun sendMessage(chatId: String, text: String) {
        withContext(Dispatchers.IO) {
            val myId = _currentUserId.value ?: return@withContext
            val chat = chatDao.getChatSync(chatId) ?: return@withContext
            val recipientId = if (chat.user1Id == myId) chat.user2Id else chat.user1Id
            val now = System.currentTimeMillis()

            val msgId = "msg_${UUID.randomUUID().toString().take(12)}"
            val message = MessageEntity(
                messageId = msgId,
                chatId = chatId,
                senderId = myId,
                recipientId = recipientId,
                text = text.trim(),
                createdAt = now,
                status = "sent"
            )
            messageDao.insertMessage(message)

            // Update chat meta
            val isUser1 = chat.user1Id == myId
            val updatedChat = chat.copy(
                lastMessageText = text.trim(),
                lastMessageSenderId = myId,
                lastMessageAt = now,
                unreadCountUser1 = if (isUser1) 0 else chat.unreadCountUser1 + 1,
                unreadCountUser2 = if (!isUser1) 0 else chat.unreadCountUser2 + 1,
                hiddenForUser1 = false,
                hiddenForUser2 = false
            )
            chatDao.updateChat(updatedChat)

            // Simulate auto-delivery after brief network propagation
            scope.launch(Dispatchers.IO) {
                kotlinx.coroutines.delay(800)
                messageDao.markMessagesDelivered(chatId, recipientId, System.currentTimeMillis())
            }
        }
    }

    suspend fun markChatAsSeen(chatId: String) {
        withContext(Dispatchers.IO) {
            val myId = _currentUserId.value ?: return@withContext
            val chat = chatDao.getChatSync(chatId) ?: return@withContext
            val now = System.currentTimeMillis()

            messageDao.markMessagesSeen(chatId, myId, now)
            val updatedChat = if (chat.user1Id == myId) {
                chat.copy(unreadCountUser1 = 0)
            } else {
                chat.copy(unreadCountUser2 = 0)
            }
            chatDao.updateChat(updatedChat)
        }
    }

    suspend fun deleteMessageForEveryone(messageId: String) {
        withContext(Dispatchers.IO) {
            // Find message and mark deleted
            val allMsgs = database.openHelper.writableDatabase
            allMsgs.execSQL("UPDATE messages SET deletedForEveryone = 1, text = 'এই মেসেজটি মুছে ফেলা হয়েছে' WHERE messageId = '$messageId'")
        }
    }

    suspend fun deleteMessageForMe(messageId: String, currentUid: String) {
        withContext(Dispatchers.IO) {
            val allMsgs = database.openHelper.writableDatabase
            allMsgs.execSQL("UPDATE messages SET deletedForSender = CASE WHEN senderId = '$currentUid' THEN 1 ELSE deletedForSender END, deletedForRecipient = CASE WHEN recipientId = '$currentUid' THEN 1 ELSE deletedForRecipient END WHERE messageId = '$messageId'")
        }
    }

    // Admin Operations
    fun loginAdmin(id: String, password: String):Result<Boolean> {
        val trimmedId = id.trim()
        if (trimmedId.equals("pakhi", ignoreCase = true) && password == adminPasswordCurrent) {
            _isAdminLoggedIn.value = true
            return Result.success(true)
        }
        return Result.failure(Exception("ইউজার আইডি বা পাসওয়ার্ড সঠিক নয়।"))
    }

    fun logoutAdmin() {
        _isAdminLoggedIn.value = false
    }

    fun changeAdminPassword(newPass: String): Result<Boolean> {
        if (newPass.length < 6) {
            return Result.failure(Exception("পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে।"))
        }
        adminPasswordCurrent = newPass
        _adminMustChangePassword.value = false
        logAdminAction("CHANGE_PASSWORD", "ADMIN", "pakhi", "Admin password changed successfully")
        return Result.success(true)
    }

    suspend fun getDashboardStats(): DashboardStats {
        return withContext(Dispatchers.IO) {
            DashboardStats(
                totalUsers = userDao.getUserCount(),
                activeUsers = userDao.getActiveUserCount(),
                totalChats = chatDao.getChatCount(),
                totalMessages = messageDao.getMessageCount()
            )
        }
    }

    suspend fun toggleUserStatus(targetUid: String, newStatus: String) {
        withContext(Dispatchers.IO) {
            val user = userDao.getUserSync(targetUid) ?: return@withContext
            userDao.updateUser(user.copy(status = newStatus))
            logAdminAction("SET_USER_STATUS", "USER", targetUid, "Status changed to $newStatus")
        }
    }

    suspend fun saveAnnouncement(title: String, body: String, pinned: Boolean, id: String? = null) {
        withContext(Dispatchers.IO) {
            val annId = id ?: "ann_${UUID.randomUUID().toString().take(8)}"
            val entity = AnnouncementEntity(
                id = annId,
                title = title,
                body = body,
                pinned = pinned,
                published = true,
                createdAt = System.currentTimeMillis()
            )
            announcementDao.insertAnnouncement(entity)
            logAdminAction("SAVE_ANNOUNCEMENT", "ANNOUNCEMENT", annId, "Saved: $title")
        }
    }

    suspend fun deleteAnnouncement(id: String) {
        withContext(Dispatchers.IO) {
            announcementDao.deleteAnnouncement(id)
            logAdminAction("DELETE_ANNOUNCEMENT", "ANNOUNCEMENT", id, "Deleted announcement $id")
        }
    }

    suspend fun saveBanner(title: String, description: String, active: Boolean, id: String? = null) {
        withContext(Dispatchers.IO) {
            val banId = id ?: "ban_${UUID.randomUUID().toString().take(8)}"
            val entity = BannerEntity(
                id = banId,
                title = title,
                description = description,
                active = active,
                orderIndex = 1,
                createdAt = System.currentTimeMillis()
            )
            bannerDao.insertBanner(entity)
            logAdminAction("SAVE_BANNER", "BANNER", banId, "Saved banner: $title")
        }
    }

    suspend fun deleteBanner(id: String) {
        withContext(Dispatchers.IO) {
            bannerDao.deleteBanner(id)
            logAdminAction("DELETE_BANNER", "BANNER", id, "Deleted banner $id")
        }
    }

    suspend fun updateAppConfig(maintenance: Boolean, maintenanceMessage: String, emergencyMsg: String?) {
        withContext(Dispatchers.IO) {
            val existing = appConfigDao.getConfigSync() ?: AppConfigEntity()
            val updated = existing.copy(
                maintenanceEnabled = maintenance,
                maintenanceMessage = maintenanceMessage,
                emergencyMessage = emergencyMsg,
                updatedAt = System.currentTimeMillis()
            )
            appConfigDao.insertOrUpdate(updated)
            logAdminAction("UPDATE_CONFIG", "SYSTEM", "main", "Maintenance: $maintenance")
        }
    }

    fun logAdminAction(action: String, targetType: String, targetId: String, meta: String) {
        scope.launch(Dispatchers.IO) {
            adminLogDao.insertLog(
                AdminLogEntity(
                    id = "log_${UUID.randomUUID().toString().take(10)}",
                    adminUid = "pakhi",
                    action = action,
                    targetType = targetType,
                    targetId = targetId,
                    meta = meta,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
}
