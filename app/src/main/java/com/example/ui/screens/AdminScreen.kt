package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.FirestoreAdminLog
import com.example.data.model.FirestoreAnnouncement
import com.example.data.model.FirestoreAppConfig
import com.example.data.model.FirestoreBanner
import com.example.data.model.UserProfile
import com.example.ui.components.UserAvatar
import com.example.ui.theme.PakhiDanger
import com.example.ui.theme.PakhiPrimaryLight
import com.example.ui.theme.PakhiSuccess
import com.example.ui.theme.PakhiWarning
import com.example.ui.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    adminViewModel: AdminViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLoggedIn by adminViewModel.isAdminLoggedIn.collectAsState()
    val mustChangePassword by adminViewModel.mustChangePassword.collectAsState()
    val loginError by adminViewModel.loginError.collectAsState()

    BackHandler {
        onBack()
    }

    if (!isLoggedIn) {
        AdminLoginView(
            onLogin = { id, pass -> adminViewModel.login(id, pass) },
            error = loginError,
            onBack = onBack
        )
    } else if (mustChangePassword) {
        AdminChangePasswordView(
            onChangePassword = { newPass -> adminViewModel.changePassword(newPass) },
            error = loginError
        )
    } else {
        AdminDashboardView(
            adminViewModel = adminViewModel,
            onBack = onBack
        )
    }
}

@Composable
fun AdminLoginView(
    onLogin: (String, String) -> Unit,
    error: String?,
    onBack: () -> Unit
) {
    var adminId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                }
                Text("অ্যাডমিন লগইন", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "পাখি অ্যাডমিন প্যানেল",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "নিরাপদ প্রশাসনিক নিয়ন্ত্রণ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            OutlinedTextField(
                value = adminId,
                onValueChange = { adminId = it },
                label = { Text("ইউজার আইডি (User ID)") },
                placeholder = { Text("যেমন: pakhi") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_id_input"),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("পাসওয়ার্ড") },
                placeholder = { Text("গোপন পাসওয়ার্ড লিখুন") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "পাসওয়ার্ড লুকান" else "পাসওয়ার্ড দেখুন"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_password_input"),
                shape = RoundedCornerShape(14.dp)
            )

            AnimatedVisibility(visible = error != null) {
                error?.let { err ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = PakhiDanger.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = err,
                            color = PakhiDanger,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onLogin(adminId, password) },
                enabled = adminId.isNotBlank() && password.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_login_submit_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("লগইন করুন", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AdminChangePasswordView(
    onChangePassword: (String) -> Result<Boolean>,
    error: String?
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    var showPassword by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            modifier = Modifier.size(54.dp),
            tint = PakhiWarning
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "পাসওয়ার্ড পরিবর্তন",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "সিস্টেমের নিরাপত্তার জন্য নতুন শক্তিশালী পাসওয়ার্ড নির্ধারণ করুন।",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text("নতুন পাসওয়ার্ড (কমপক্ষে ৬ অক্ষর)") },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("পাসওয়ার্ড নিশ্চিত করুন") },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )

        val displayError = localError ?: error
        if (displayError != null) {
            Text(
                text = displayError,
                color = PakhiDanger,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (newPassword.length < 6) {
                    localError = "পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে।"
                } else if (newPassword != confirmPassword) {
                    localError = "উভয় পাসওয়ার্ড এক হতে হবে।"
                } else {
                    localError = null
                    onChangePassword(newPassword)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardView(
    adminViewModel: AdminViewModel,
    onBack: () -> Unit
) {
    val stats by adminViewModel.stats.collectAsState()
    val allUsers by adminViewModel.allUsers.collectAsState()
    val allAnnouncements by adminViewModel.allAnnouncements.collectAsState()
    val allBanners by adminViewModel.allBanners.collectAsState()
    val appConfig by adminViewModel.appConfig.collectAsState()
    val adminLogs by adminViewModel.adminLogs.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("মেট্রিক্স", "ইউজার তালিকা", "নোটিশ ও ব্যানার", "অ্যাপ সেটিংস", "লগ")

    LaunchedEffect(Unit) {
        adminViewModel.refreshStats()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("পাখি অ্যাডমিন ড্যাশবোর্ড", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                actions = {
                    IconButton(onClick = { adminViewModel.logout() }) {
                        Icon(imageVector = Icons.Default.Logout, contentDescription = "লগআউট", tint = PakhiDanger)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 12.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    // Dashboard Overview & Real Cloud Stats
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "ক্লাউড ফায়ারবেস লাইভ কানেক্টেড: রিয়েল-টাইমে সমস্ত ডাটাবেস পরিচালিত হচ্ছে।",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("রিয়েল-টাইম মেট্রিক্স (Firebase Firestore)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            AdminStatCard(title = "মোট নিবন্ধিত ইউজার", value = allUsers.size.toString(), icon = Icons.Default.People, modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(10.dp))
                            val activeCount = allUsers.count { it.status == "active" }
                            AdminStatCard(title = "সক্রিয় ইউজার", value = activeCount.toString(), icon = Icons.Default.Check, color = PakhiSuccess, modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            AdminStatCard(title = "মোট চ্যাট কথোপকথন", value = stats.totalChats.toString(), icon = Icons.Default.Campaign, modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(10.dp))
                            val blockedCount = allUsers.count { it.status == "blocked" }
                            AdminStatCard(title = "ব্লকড ইউজার", value = blockedCount.toString(), icon = Icons.Default.Block, color = PakhiDanger, modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text("সর্বশেষ প্রশাসনিক কার্যক্রম", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (adminLogs.isEmpty()) {
                            Text(
                                text = "এখনো কোনো প্রশাসনিক কার্যক্রম রেকর্ড হয়নি।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            adminLogs.take(5).forEach { log ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = log.action, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Text(text = log.meta, style = MaterialTheme.typography.bodySmall)
                                        }
                                        val time = remember(log.timestamp) {
                                            log.timestamp?.toDate()?.let {
                                                SimpleDateFormat("h:mm a", Locale.getDefault()).format(it)
                                            } ?: ""
                                        }
                                        Text(text = time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Real User Management Tab (Connected to Firestore)
                    RealUserManagementView(
                        users = allUsers,
                        onUpdateStatus = { uid, newStatus -> adminViewModel.toggleUserStatus(uid, newStatus) }
                    )
                }
                2 -> {
                    // Announcements & Push Notices Tab with Image Support
                    AnnouncementsAndBannersView(
                        announcements = allAnnouncements,
                        banners = allBanners,
                        onAddAnnouncement = { title, body, imageUrl, pinned ->
                            adminViewModel.saveAnnouncement(title, body, imageUrl, pinned)
                        },
                        onDeleteAnnouncement = { adminViewModel.deleteAnnouncement(it) },
                        onAddBanner = { title, desc, img ->
                            adminViewModel.saveBanner(title, desc, img, active = true)
                        },
                        onDeleteBanner = { adminViewModel.deleteBanner(it) }
                    )
                }
                3 -> {
                    // App Settings Tab (Maintenance Mode, Emergency broadcast)
                    AdminSettingsView(
                        appConfig = appConfig,
                        onSave = { maintenance, msg, emergency -> adminViewModel.updateConfig(maintenance, msg, emergency) }
                    )
                }
                4 -> {
                    // Audit Log Tab
                    AuditLogView(logs = adminLogs)
                }
            }
        }
    }
}

@Composable
fun AdminStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color = PakhiPrimaryLight,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun RealUserManagementView(
    users: List<UserProfile>,
    onUpdateStatus: (String, String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = users.filter {
        it.displayName.contains(searchQuery, ignoreCase = true) ||
        it.pakhiId.contains(searchQuery, ignoreCase = true) ||
        it.email.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "রিয়েল ব্যবহারকারী (${users.size} জন)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("নাম, ইমেইল বা @Pakhi ID দিয়ে খুঁজুন...") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (users.isEmpty()) "এখনো কোনো ব্যবহারকারী Google দিয়ে সাইন ইন করেননি।" else "কোনো ফলাফল পাওয়া যায়নি।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.uid }) { user ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(name = user.displayName, size = 46, isOnline = user.isOnline)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = user.displayName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val statusColor = when (user.status) {
                                            "active" -> PakhiSuccess
                                            "blocked" -> PakhiDanger
                                            else -> PakhiWarning
                                        }
                                        val statusLabel = when (user.status) {
                                            "active" -> "সক্রিয়"
                                            "blocked" -> "ব্লকড"
                                            else -> "ডিএক্টিভ"
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(statusColor.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = statusLabel,
                                                color = statusColor,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Text(
                                        text = "@${user.pakhiId}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (user.email.isNotBlank()) {
                                        Text(
                                            text = user.email,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action buttons: Active, Block, Deactivate
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                if (user.status != "active") {
                                    Button(
                                        onClick = { onUpdateStatus(user.uid, "active") },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PakhiSuccess.copy(alpha = 0.15f),
                                            contentColor = PakhiSuccess
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("সক্রিয় করুন", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                if (user.status != "blocked") {
                                    Button(
                                        onClick = { onUpdateStatus(user.uid, "blocked") },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PakhiDanger.copy(alpha = 0.15f),
                                            contentColor = PakhiDanger
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("ব্লক করুন", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                if (user.status != "deactivated") {
                                    Button(
                                        onClick = { onUpdateStatus(user.uid, "deactivated") },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("ডিএক্টিভ", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnnouncementsAndBannersView(
    announcements: List<FirestoreAnnouncement>,
    banners: List<FirestoreBanner>,
    onAddAnnouncement: (String, String, String?, Boolean) -> Unit,
    onDeleteAnnouncement: (String) -> Unit,
    onAddBanner: (String, String, String?) -> Unit,
    onDeleteBanner: (String) -> Unit
) {
    var showAnnDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("পুশ নোটিফিকেশন ও ঘোষণা", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("অ্যাপে পিন হয়ে থাকবে এবং নোটিফিকেশন যাবে", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = { showAnnDialog = true },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("নতুন নোটিশ")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (announcements.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Text(
                    text = "এখনো কোনো ঘোষণা প্রকাশিত হয়নি। নতুন ঘোষণা দিলে তা ব্যবহারকারীদের অ্যাপে পিন হবে।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            announcements.forEach { ann ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            if (ann.pinned) {
                                Icon(Icons.Default.PushPin, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = ann.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = ann.body, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { onDeleteAnnouncement(ann.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "মুছুন", tint = PakhiDanger)
                            }
                        }

                        if (!ann.imageUrl.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            AsyncImage(
                                model = ann.imageUrl,
                                contentDescription = "ঘোষণার ছবি",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("ব্যানার সমূহ (Interactive Banners)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        banners.forEach { ban ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = ban.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(text = ban.description, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { onDeleteBanner(ban.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "মুছুন", tint = PakhiDanger)
                    }
                }
            }
        }
    }

    if (showAnnDialog) {
        var title by remember { mutableStateOf("") }
        var body by remember { mutableStateOf("") }
        var imageUrl by remember { mutableStateOf("") }
        var pinned by remember { mutableStateOf(true) }

        androidx.compose.ui.window.Dialog(onDismissRequest = { showAnnDialog = false }) {
            androidx.compose.material3.Surface(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("নতুন পুশ নোটিশ তৈরি করুন", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("নোটিশের শিরোনাম") },
                        placeholder = { Text("যেমন: নতুন আপডেট বা বিশেষ বার্তা") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        label = { Text("বিস্তারিত বিবরণ") },
                        placeholder = { Text("বার্তার বিস্তারিত লিখুন...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("ছবির লিঙ্ক (ঐচ্ছিক URL)") },
                        placeholder = { Text("https://example.com/banner.png") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = pinned, onCheckedChange = { pinned = it })
                        Text("হোম স্ক্রিনের শীর্ষে পিন করে রাখুন", fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(onClick = { showAnnDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurface)) {
                            Text("বাতিল")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    onAddAnnouncement(title, body, imageUrl.ifBlank { null }, pinned)
                                    showAnnDialog = false
                                }
                            }
                        ) {
                            Text("প্রকাশ ও পুশ করুন")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminSettingsView(
    appConfig: FirestoreAppConfig?,
    onSave: (Boolean, String, String?) -> Unit
) {
    var maintenance by remember(appConfig) { mutableStateOf(appConfig?.maintenanceEnabled ?: false) }
    var maintenanceMessage by remember(appConfig) { mutableStateOf(appConfig?.maintenanceMessage ?: "পাখির সার্ভার রক্ষণাবেক্ষণ চলছে।") }
    var emergencyMsg by remember(appConfig) { mutableStateOf(appConfig?.emergencyMessage ?: "") }
    var isSaved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("অ্যাপ সেটিংস ও ক্লাউড নিয়ন্ত্রণ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("মেইনটেন্যান্স মোড (Maintenance Mode)", fontWeight = FontWeight.Bold)
                        Text("চালু করলে সাধারণ ব্যবহারকারীরা রক্ষণাবেক্ষণ স্ক্রিন দেখতে পাবেন", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = maintenance, onCheckedChange = { maintenance = it })
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = maintenanceMessage,
                    onValueChange = { maintenanceMessage = it },
                    label = { Text("রক্ষণাবেক্ষণ বার্তা") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("জরুরি বার্তা ব্যানার (Emergency Alert)", fontWeight = FontWeight.Bold)
                Text("হোম স্ক্রিনের শীর্ষে লাল সতর্কবার্তা হিসেবে প্রদর্শিত হবে", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = emergencyMsg,
                    onValueChange = { emergencyMsg = it },
                    placeholder = { Text("যেমন: কারিগরি ত্রুটির কারণে নোটিফিকেশন সাময়িক বিলম্বিত হতে পারে...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                onSave(maintenance, maintenanceMessage, emergencyMsg.ifBlank { null })
                isSaved = true
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(if (isSaved) "ক্লাউডে সংরক্ষণ সম্পন্ন হয়েছে ✓" else "সেটিংস সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AuditLogView(logs: List<FirestoreAdminLog>) {
    if (logs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("কোনো অডিট লগ রেকর্ড নেই।", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(logs, key = { it.id }) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = log.action, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            val date = remember(log.timestamp) {
                                log.timestamp?.toDate()?.let {
                                    SimpleDateFormat("dd/MM/yyyy h:mm a", Locale.getDefault()).format(it)
                                } ?: ""
                            }
                            Text(text = date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = log.meta, style = MaterialTheme.typography.bodySmall)
                        if (log.targetId.isNotBlank()) {
                            Text(text = "লক্ষ্য: ${log.targetType} / ${log.targetId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
