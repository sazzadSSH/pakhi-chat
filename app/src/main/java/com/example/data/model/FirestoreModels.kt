package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val pakhiId: String = "",
    val displayName: String = "",
    val email: String = "",           // Visible only to user themselves and in Admin panel
    val photoUrl: String? = null,
    val status: String = "active",     // "active", "blocked", "deactivated"
    val isOnline: Boolean = true,
    @ServerTimestamp val lastSeen: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PublicUserProfile(
    @DocumentId val id: String = "",   // Document ID is the pakhiId (e.g. PK-583921)
    val pakhiId: String = "",
    val uid: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreChat(
    @DocumentId val id: String = "",
    val chatId: String = "",
    val members: List<String> = emptyList(),
    val memberPakhiIds: Map<String, String> = emptyMap(),
    val memberNames: Map<String, String> = emptyMap(),
    val lastMessageText: String = "",
    val lastMessageSenderId: String = "",
    @ServerTimestamp val lastMessageAt: Timestamp? = null,
    val unreadCount: Map<String, Long> = emptyMap(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreMessage(
    @DocumentId val id: String = "",
    val messageId: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val recipientId: String = "",
    val text: String = "",
    val status: String = "sent",       // "sent", "delivered", "seen"
    @ServerTimestamp val deliveredAt: Timestamp? = null,
    @ServerTimestamp val seenAt: Timestamp? = null,
    val deletedForEveryone: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreAnnouncement(
    @DocumentId val id: String = "",
    val title: String = "",
    val body: String = "",
    val imageUrl: String? = null,
    val pinned: Boolean = false,
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreBanner(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreAppConfig(
    @DocumentId val id: String = "global",
    val maintenanceEnabled: Boolean = false,
    val maintenanceMessage: String = "পাখির সার্ভার রক্ষণাবেক্ষণ চলছে। অনুগ্রহ করে কিছুক্ষণ পর আবার চেষ্টা করুন।",
    val emergencyMessage: String? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreAdminLog(
    @DocumentId val id: String = "",
    val action: String = "",
    val targetType: String = "",
    val targetId: String = "",
    val meta: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null
)
