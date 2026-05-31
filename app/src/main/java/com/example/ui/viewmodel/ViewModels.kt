package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.BreakLog
import com.example.data.BreakTimeRepository
import com.example.data.LateRequest
import com.example.data.User
import com.example.worker.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: BreakTimeRepository) : ViewModel() {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    fun login(username: String, passwordToCheck: String): Boolean {
        _loginError.value = null
        if (username.isBlank() || passwordToCheck.isBlank()) {
            _loginError.value = "Username and password cannot be empty."
            return false
        }

        val cleanName = username.trim()
        val foundUser = repository.loginUser(cleanName)

        if (foundUser != null) {
            // Predefined rules state: password is same as username
            if (passwordToCheck == foundUser.name) {
                _currentUser.value = foundUser
                return true
            } else {
                _loginError.value = "Incorrect password. Standard credentials match username."
                return false
            }
        } else {
            _loginError.value = "User not registered in MONZ School records."
            return false
        }
    }

    fun logout() {
        _currentUser.value = null
    }
}

class BreakTimerViewModel(
    application: Application,
    private val repository: BreakTimeRepository,
    private val username: String
) : AndroidViewModel(application) {
    
    private val context = application.applicationContext
    private var timerJob: Job? = null
    
    private val _currentLog = MutableStateFlow<BreakLog?>(null)
    val currentLog: StateFlow<BreakLog?> = _currentLog.asStateFlow()

    private val _timeLeftMs = MutableStateFlow<Long>(15 * 60 * 1000L) // 15 Minutes
    val timeLeftMs: StateFlow<Long> = _timeLeftMs.asStateFlow()

    private val _timerProgress = MutableStateFlow(1.0f) // 1.0 down to 0.0
    val timerProgress: StateFlow<Float> = _timerProgress.asStateFlow()

    private val _isLate = MutableStateFlow(false)
    val isLate: StateFlow<Boolean> = _isLate.asStateFlow()

    private val _lateReasonSubmitted = MutableStateFlow(false)
    val lateReasonSubmitted: StateFlow<Boolean> = _lateReasonSubmitted.asStateFlow()

    init {
        startOrResumeTimer()
    }

    private fun startOrResumeTimer() {
        viewModelScope.launch {
            // Find if there is an active break log for this user in repository
            val activeLogs = repository.breakLogs.value.filter { 
                it.userId.equals(username, ignoreCase = true) 
            }
            
            val ongoingLog = activeLogs.find { it.status == "on_break" }
            
            val finalLog = if (ongoingLog != null) {
                ongoingLog
            } else {
                // Start a brand new 15-minute break
                repository.startBreak(username)
            }
            
            _currentLog.value = finalLog
            
            // Schedule the alarm warnings at 13 and 15 mins
            NotificationHelper.scheduleBreakNotifications(context, username, finalLog.startTime)
            
            // Begin counting down locally based on actual clock timestamps
            runTimerJob(finalLog.endTime)
        }
    }

    private fun runTimerJob(endTime: Long) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val totalDurationMs = 15 * 60 * 1000L
            while (true) {
                val now = System.currentTimeMillis()
                val diff = endTime - now
                
                if (diff <= 0) {
                    _timeLeftMs.value = 0L
                    _timerProgress.value = 0.0f
                    _isLate.value = true
                    
                    // Mark as LATE in repository db
                    val logId = _currentLog.value?.id ?: ""
                    repository.markAsLate(username, logId)
                    break
                } else {
                    _timeLeftMs.value = diff
                    _timerProgress.value = diff.toFloat() / totalDurationMs.toFloat()
                }
                delay(500L) // Refresh frequently for exceptionally smooth dial animations
            }
        }
    }

    fun completeBreakEarly() {
        timerJob?.cancel()
        val logId = _currentLog.value?.id ?: ""
        repository.completeBreak(username, logId)
        NotificationHelper.cancelNotifications(context)
        _timeLeftMs.value = 0L
        _timerProgress.value = 0.0f
    }

    fun submitLateReason(reason: String) {
        if (reason.isNotBlank()) {
            repository.submitLateRequest(username, reason.trim())
            _lateReasonSubmitted.value = true
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

class AdminViewModel(private val repository: BreakTimeRepository) : ViewModel() {
    val users: StateFlow<List<User>> = repository.users
    val breakLogs: StateFlow<List<BreakLog>> = repository.breakLogs
    val lateRequests: StateFlow<List<LateRequest>> = repository.lateRequests

    fun approveRequest(requestId: String) {
        repository.approveLateRequest(requestId)
    }

    fun rejectRequest(requestId: String) {
        repository.rejectLateRequest(requestId)
    }
}

// ViewModel Factory to cleanly support custom constructors
class ViewModelFactory(
    private val application: Application,
    private val repository: BreakTimeRepository,
    private val username: String = ""
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(repository) as T
            }
            modelClass.isAssignableFrom(BreakTimerViewModel::class.java) -> {
                BreakTimerViewModel(application, repository, username) as T
            }
            modelClass.isAssignableFrom(AdminViewModel::class.java) -> {
                AdminViewModel(repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
