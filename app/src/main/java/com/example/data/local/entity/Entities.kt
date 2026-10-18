package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val uid: String,
    val pakhiId: String,          // e.g. "PK-583921"
    val displayName: String,
    val email: String,
    val photoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "active", // "active" | "disabled"
    val isOnline: Boolean = true,
    val lastSeen: Long = System.currentTimeMillis(),
    val themePreference: String = "system",
    val notificationsEnabled: Boolean = true,
    val notificationSound: Boolean = true,
    val showLastSeen: Boolean = true
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey
    val chatId: String,           // sorted: "uidA_uidB"
    val type: String = "direct",
    val user1Id: String,
    val user2Id: String,
    val lastMessageText: String? = null,
    val lastMessageSenderId: String? = null,
    val lastMessageAt: Long = System.currentTimeMillis(),
    val unreadCountUser1: Int = 0,
    val unreadCountUser2: Int = 0,
    val hiddenForUser1: Boolean = false,
    val hiddenForUser2: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val messageId: String,
    val chatId: String,
    val senderId: String,
    val recipientId: String,
    val text: String,
    val type: String = "text",
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "sent",  // "sent" | "delivered" | "seen"
    val deliveredAt: Long? = null,
    val seenAt: Long? = null,
    val deletedForEveryone: Boolean = false,
    val deletedForSender: Boolean = false,
    val deletedForRecipient: Boolean = false
)

@Entity(tableName = "announcements")
data class AnnouncementEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val body: String,
    val pinned: Boolean = false,
    val published: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "Admin"
)

@Entity(tableName = "banners")
data class BannerEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String? = null,
    val orderIndex: Int = 0,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "admin_logs")
data class AdminLogEntity(
    @PrimaryKey
    val id: String,
    val adminUid: String,
    val action: String,
    val targetType: String,
    val targetId: String,
    val meta: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_config")
data class AppConfigEntity(
    @PrimaryKey
    val id: String = "main",
    val appName: String = "পাখি",
    val tagline: String = "কথা উড়ে যায়, মন ছুঁয়ে যায়",
    val maintenanceEnabled: Boolean = false,
    val maintenanceMessage: String = "পাখির সার্ভার রক্ষণাবেক্ষণ চলছে। শীঘ্রই ফিরে আসছি।",
    val emergencyMessage: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
