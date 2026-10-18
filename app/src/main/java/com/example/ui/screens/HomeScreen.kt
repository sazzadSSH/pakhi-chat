package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.FirestoreChat
import com.example.ui.components.NewChatModal
import com.example.ui.components.PakhiBirdLogo
import com.example.ui.components.PakhiIdCard
import com.example.ui.components.UserAvatar
import com.example.ui.theme.PakhiDanger
import com.example.ui.theme.PakhiWarning
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    chatViewModel: ChatViewModel,
    onOpenChat: (chatId: String, otherUserId: String, otherName: String, otherPakhiId: String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUserProfile.collectAsState()
    val appConfig by authViewModel.appConfig.collectAsState()
    val chats by chatViewModel.chats.collectAsState()
    val announcements by chatViewModel.announcements.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    var showNewChatModal by remember { mutableStateOf(false) }
    var chatFilterQuery by remember { mutableStateOf("") }

    val searchQuery by chatViewModel.searchQuery.collectAsState()
    val isSearching by chatViewModel.isSearching.collectAsState()
    val searchResult by chatViewModel.searchResultUser.collectAsState()
    val searchError by chatViewModel.searchError.collectAsState()

    val myUid = currentUser?.uid ?: ""
    val isUserBlocked = currentUser?.status == "blocked"

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("home_screen"),
        floatingActionButton = {
            if (!isUserBlocked) {
                FloatingActionButton(
                    onClick = {
                        chatViewModel.clearSearch()
                        showNewChatModal = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("new_chat_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "নতুন চ্যাট")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "নতুন চ্যাট",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PakhiBirdLogo(size = 40)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "পাখি",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "কথা উড়ে যায়, মন ছুঁয়ে যায়",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    currentUser?.let { user ->
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onNavigateToProfile() }
                                .padding(4.dp)
                        ) {
                            UserAvatar(
                                name = user.displayName,
                                size = 36,
                                isOnline = user.isOnline
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.testTag("home_menu_button")
                        ) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "মেনু")
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("আমার প্রোফাইল") },
                                onClick = {
                                    showMenu = false
                                    onNavigateToProfile()
                                },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("সেটিংস") },
                                onClick = {
                                    showMenu = false
                                    onNavigateToSettings()
                                },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("অ্যাডমিন প্যানেল") },
                                onClick = {
                                    showMenu = false
                                    onNavigateToAdmin()
                                },
                                leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) }
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                // Emergency Alert Banner if broadcasted by Admin
                val emergencyMsg = appConfig?.emergencyMessage
                if (!emergencyMsg.isNullOrBlank()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = PakhiDanger.copy(alpha = 0.12f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "জরুরি বার্তা",
                                    tint = PakhiDanger,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = emergencyMsg,
                                    color = PakhiDanger,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Blocked Account Warning
                if (isUserBlocked) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = PakhiDanger.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Block, contentDescription = null, tint = PakhiDanger, modifier = Modifier.size(26.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("অ্যাকাউন্ট স্থগিত করা হয়েছে", fontWeight = FontWeight.Bold, color = PakhiDanger)
                                    Text("অ্যাডমিন কর্তৃক আপনার অ্যাকাউন্টটি ব্লক করা হয়েছে। বার্তা প্রেরণ সাময়িক বন্ধ আছে।", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                // User Pakhi ID Card with instant Copy Button
                currentUser?.let { user ->
                    item {
                        PakhiIdCard(
                            pakhiId = user.pakhiId,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }

                // Announcements & Push Notices from Cloud Firestore (Pinned on Home)
                val pinnedAnnouncements = announcements.filter { it.pinned }
                if (pinnedAnnouncements.isNotEmpty()) {
                    items(pinnedAnnouncements, key = { it.id }) { ann ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = "পিন করা",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = ann.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = ann.body,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f)
                                )

                                if (!ann.imageUrl.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    AsyncImage(
                                        model = ann.imageUrl,
                                        contentDescription = "বিজ্ঞপ্তি ছবি",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }

                // Search Filter for Existing Chats
                item {
                    OutlinedTextField(
                        value = chatFilterQuery,
                        onValueChange = { chatFilterQuery = it },
                        placeholder = { Text("চ্যাট বা Pakhi ID খুঁজুন...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("chat_search_input"),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    )
                }

                // Chat List Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "কথোপকথন সমূহ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${chats.size} টি চ্যাট",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Filtered chats (Privacy: Display Name & Pakhi ID only, never email)
                val filteredChats = chats.filter { chat ->
                    val otherUid = chat.members.firstOrNull { it != myUid } ?: ""
                    val otherName = chat.memberNames[otherUid] ?: ""
                    val otherPakhiId = chat.memberPakhiIds[otherUid] ?: ""
                    val lastText = chat.lastMessageText

                    if (chatFilterQuery.isBlank()) true
                    else {
                        otherName.contains(chatFilterQuery, ignoreCase = true) ||
                        otherPakhiId.contains(chatFilterQuery, ignoreCase = true) ||
                        lastText.contains(chatFilterQuery, ignoreCase = true)
                    }
                }

                if (filteredChats.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(52.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "এখনো কোনো কথোপকথন নেই",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "আপনার @Pakhi ID শেয়ার করে বা নিচে '+' বাটনে ট্যাপ করে যেকোনো Pakhi ID দিয়ে রিয়েল-টাইম চ্যাট শুরু করুন।",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(filteredChats, key = { it.chatId }) { chat ->
                        val otherUid = chat.members.firstOrNull { it != myUid } ?: ""
                        val otherName = chat.memberNames[otherUid] ?: "পাখি ব্যবহারকারী"
                        val otherPakhiId = chat.memberPakhiIds[otherUid] ?: ""
                        val unreadCount = (chat.unreadCount[myUid] ?: 0L).toInt()

                        FirestoreChatItemRow(
                            chat = chat,
                            otherName = otherName,
                            otherPakhiId = otherPakhiId,
                            unreadCount = unreadCount,
                            onClick = {
                                onOpenChat(chat.chatId, otherUid, otherName, otherPakhiId)
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal for New Chat by Pakhi ID (Guaranteed No-Crash)
    if (showNewChatModal) {
        NewChatModal(
            onDismiss = { showNewChatModal = false },
            onStartChat = { publicUser ->
                showNewChatModal = false
                coroutineScope.launch {
                    try {
                        val chatId = chatViewModel.startChat(publicUser)
                        onOpenChat(chatId, publicUser.uid, publicUser.displayName, publicUser.pakhiId)
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "চ্যাট শুরু করতে সমস্যা হয়েছে: ${e.localizedMessage ?: "অনুগ্রহ করে আবার চেষ্টা করুন"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            },
            searchQuery = searchQuery,
            onQueryChange = { chatViewModel.onSearchQueryChange(it) },
            onSearch = { chatViewModel.searchUserByPakhiId(it) },
            isSearching = isSearching,
            searchResult = searchResult,
            searchError = searchError
        )
    }
}

@Composable
fun FirestoreChatItemRow(
    chat: FirestoreChat,
    otherName: String,
    otherPakhiId: String,
    unreadCount: Int,
    onClick: () -> Unit
) {
    val timeFormatted = remember(chat.lastMessageAt) {
        chat.lastMessageAt?.toDate()?.let {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(it)
        } ?: ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("chat_item_${chat.chatId}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            name = otherName,
            size = 52,
            isOnline = true
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = otherName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (otherPakhiId.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "@$otherPakhiId",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val lastText = if (chat.lastMessageText.isNotBlank()) chat.lastMessageText else "নতুন বার্তা শুরু করুন..."
                Text(
                    text = lastText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (unreadCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = unreadCount.toString(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
