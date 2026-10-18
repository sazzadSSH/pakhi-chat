package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.FirestoreAdminLog
import com.example.data.model.FirestoreAnnouncement
import com.example.data.model.FirestoreAppConfig
import com.example.data.model.FirestoreBanner
import com.example.data.model.FirestoreChat
import com.example.data.model.FirestoreMessage
import com.example.data.model.PublicUserProfile
import com.example.data.model.UserProfile
import com.example.data.repository.DashboardStats
import com.example.data.repository.FirestorePakhiRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(
    private val firestoreRepository: FirestorePakhiRepository
) : ViewModel() {

    val firebaseUser: StateFlow<FirebaseUser?> = firestoreRepository.authUserFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    val currentUserProfile: StateFlow<UserProfile?> = firestoreRepository.currentUserProfile

    val appConfig: StateFlow<FirestoreAppConfig?> = firestoreRepository.observeAppConfig().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        FirestoreAppConfig()
    )

    fun signOut() {
        viewModelScope.launch {
            firestoreRepository.signOut()
        }
    }
}

class ChatViewModel(
    private val firestoreRepository: FirestorePakhiRepository
) : ViewModel() {

    val chats: StateFlow<List<FirestoreChat>> = firestoreRepository.observeUserChats().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val announcements: StateFlow<List<FirestoreAnnouncement>> = firestoreRepository.observeAnnouncements().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Search by Pakhi ID state (RETURNS PublicUserProfile WITHOUT EMAIL)
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _searchResultUser = MutableStateFlow<PublicUserProfile?>(null)
    val searchResultUser = _searchResultUser.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError = _searchError.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _searchResultUser.value = null
        _searchError.value = null
    }

    fun searchUserByPakhiId(pakhiId: String) {
        viewModelScope.launch {
            _isSearching.value = true
            _searchError.value = null
            _searchResultUser.value = null
            val result = firestoreRepository.searchUserByPakhiId(pakhiId)
            _isSearching.value = false
            if (result.isSuccess) {
                _searchResultUser.value = result.getOrNull()
            } else {
                _searchError.value = result.exceptionOrNull()?.message ?: "ব্যবহারকারী পাওয়া যায়নি।"
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResultUser.value = null
        _searchError.value = null
    }

    suspend fun startChat(otherPublicUser: PublicUserProfile): String {
        return firestoreRepository.getOrCreateChat(otherPublicUser)
    }

    fun getMessages(chatId: String): StateFlow<List<FirestoreMessage>> {
        return firestoreRepository.observeMessages(chatId).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
    }

    fun sendMessage(chatId: String, text: String, recipientId: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            try {
                firestoreRepository.sendMessage(chatId, text, recipientId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun markSeen(chatId: String) {
        viewModelScope.launch {
            try {
                firestoreRepository.markChatSeen(chatId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteMessageForEveryone(chatId: String, messageId: String) {
        viewModelScope.launch {
            try {
                firestoreRepository.deleteMessageForEveryone(chatId, messageId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class AdminViewModel(
    private val repository: FirestorePakhiRepository
) : ViewModel() {

    val isAdminLoggedIn = repository.isAdminLoggedIn
    val mustChangePassword = repository.adminMustChangePassword

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError = _loginError.asStateFlow()

    private val _stats = MutableStateFlow(DashboardStats())
    val stats = _stats.asStateFlow()

    val allUsers: StateFlow<List<UserProfile>> = repository.observeAllUsers().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val allAnnouncements: StateFlow<List<FirestoreAnnouncement>> = repository.observeAllAnnouncements().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val allBanners: StateFlow<List<FirestoreBanner>> = repository.observeAllBanners().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val adminLogs: StateFlow<List<FirestoreAdminLog>> = repository.observeAdminLogs().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val appConfig: StateFlow<FirestoreAppConfig?> = repository.observeAppConfig().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        FirestoreAppConfig()
    )

    fun login(id: String, pass: String) {
        _loginError.value = null
        val result = repository.loginAdmin(id, pass)
        if (result.isFailure) {
            _loginError.value = result.exceptionOrNull()?.message ?: "ভুল আইডি বা পাসওয়ার্ড"
        } else {
            refreshStats()
        }
    }

    fun logout() {
        repository.logoutAdmin()
    }

    fun changePassword(newPass: String): Result<Boolean> {
        val res = repository.changeAdminPassword(newPass)
        if (res.isFailure) {
            _loginError.value = res.exceptionOrNull()?.message
        }
        return res
    }

    fun refreshStats() {
        viewModelScope.launch {
            _stats.value = repository.getDashboardStats()
        }
    }

    fun toggleUserStatus(targetUid: String, newStatus: String) {
        viewModelScope.launch {
            repository.toggleUserStatus(targetUid, newStatus)
            refreshStats()
        }
    }

    fun saveBanner(title: String, description: String, imageUrl: String? = null, active: Boolean = true, id: String? = null) {
        viewModelScope.launch {
            repository.saveBanner(title, description, imageUrl, active, id)
        }
    }

    fun deleteBanner(id: String) {
        viewModelScope.launch {
            repository.deleteBanner(id)
        }
    }

    fun saveAnnouncement(title: String, body: String, imageUrl: String? = null, pinned: Boolean = false, id: String? = null) {
        viewModelScope.launch {
            repository.saveAnnouncement(title, body, imageUrl, pinned, id)
        }
    }

    fun deleteAnnouncement(id: String) {
        viewModelScope.launch {
            repository.deleteAnnouncement(id)
        }
    }

    fun updateConfig(maintenance: Boolean, message: String, emergencyMsg: String?) {
        viewModelScope.launch {
            repository.updateAppConfig(maintenance, message, emergencyMsg)
        }
    }
}
