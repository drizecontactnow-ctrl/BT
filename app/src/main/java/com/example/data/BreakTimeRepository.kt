package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.User
import com.example.data.BreakLog
import com.example.data.LateRequest
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class BreakTimeRepository(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("break_time_prefs", Context.MODE_PRIVATE)
    
    // Check if real Firebase is available and configured
    private var isFirebaseAvailable = false
    private var firestore: FirebaseFirestore? = null
    
    // Local In-Memory & SharedPrefs State for Simulator Mode
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _breakLogs = MutableStateFlow<List<BreakLog>>(emptyList())
    val breakLogs: StateFlow<List<BreakLog>> = _breakLogs.asStateFlow()

    private val _lateRequests = MutableStateFlow<List<LateRequest>>(emptyList())
    val lateRequests: StateFlow<List<LateRequest>> = _lateRequests.asStateFlow()

    // Listeners for Real-time database updates
    private var usersListener: ListenerRegistration? = null
    private var logsListener: ListenerRegistration? = null
    private var requestsListener: ListenerRegistration? = null

    // Predefined Users Lists
    val predefinedUsers = listOf(
        "Priyadharshini.k", "Gowtham", "Anitharuth", "Vinotha", "Selvamani",
        "Sathyapriya", "Rahul", "Shivasankar", "Hariprasanth", "Mugila",
        "Praveena", "Priyadharshini.s", "hariprasath.M"
    )
    val predefinedAdmin = "Suruthi"

    init {
        try {
            // Attempt to initialize Firebase and Firestore safely
            val apps = FirebaseApp.getApps(context)
            if (apps.isNotEmpty()) {
                firestore = FirebaseFirestore.getInstance()
                isFirebaseAvailable = true
                Log.d("BreakTimeRepository", "Firebase Firestore is available and active.")
            } else {
                Log.d("BreakTimeRepository", "Firebase not initialized. Operating in local simulator mode.")
            }
        } catch (e: Exception) {
            Log.e("BreakTimeRepository", "Firebase initialization failed, falling back to local simulator mode", e)
            isFirebaseAvailable = false
        }

        if (isFirebaseAvailable) {
            setupFirebaseListeners()
        } else {
            loadLocalState()
        }
    }

    private fun setupFirebaseListeners() {
        val db = firestore ?: return
        
        // Seed users to Firestore if they don't exist yet
        db.collection("users").get().addOnSuccessListener { snapshot ->
            if (snapshot.isEmpty) {
                scope.launch {
                    seedFirebaseData()
                }
            }
        }

        // Live Listener for Users
        usersListener = db.collection("users").addSnapshotListener { querySnapshot, error ->
            if (error != null) {
                Log.e("BreakTimeRepository", "Firebase Users listener error", error)
                return@addSnapshotListener
            }
            if (querySnapshot != null) {
                val list = querySnapshot.toObjects(User::class.java)
                _users.value = list.sortedBy { it.name }
            }
        }

        // Live Listener for Break Logs
        logsListener = db.collection("break_logs").addSnapshotListener { querySnapshot, error ->
            if (error != null) {
                Log.e("BreakTimeRepository", "Firebase Logs listener error", error)
                return@addSnapshotListener
            }
            if (querySnapshot != null) {
                val list = querySnapshot.toObjects(BreakLog::class.java)
                _breakLogs.value = list.sortedByDescending { it.startTime }
            }
        }

        // Live Listener for Late Requests
        requestsListener = db.collection("late_requests").addSnapshotListener { querySnapshot, error ->
            if (error != null) {
                Log.e("BreakTimeRepository", "Firebase Requests listener error", error)
                return@addSnapshotListener
            }
            if (querySnapshot != null) {
                val list = querySnapshot.toObjects(LateRequest::class.java)
                _lateRequests.value = list
            }
        }
    }

    private fun seedFirebaseData() {
        val db = firestore ?: return
        // Create Admin user
        val adminUser = User(name = predefinedAdmin, role = "admin", status = "idle")
        db.collection("users").document(predefinedAdmin).set(adminUser)

        // Create standard users
        predefinedUsers.forEach { userName ->
            val usr = User(name = userName, role = "user", status = "idle")
            db.collection("users").document(userName).set(usr)
        }
    }

    // --- LOCAL STATE SIMULATOR IMPLEMENTATION ---

    private fun loadLocalState() {
        // Read Users from SharedPrefs or seed initial ones
        val usersJson = sharedPrefs.getString("simulator_users", null)
        if (usersJson == null) {
            val initialUsers = ArrayList<User>()
            initialUsers.add(User(name = predefinedAdmin, role = "admin", status = "idle"))
            predefinedUsers.forEach {
                initialUsers.add(User(name = it, role = "user", status = "idle"))
            }
            _users.value = initialUsers
            saveUsersState()
        } else {
            try {
                val type = Types.newParameterizedType(List::class.java, User::class.java)
                val adapter = moshi.adapter<List<User>>(type)
                _users.value = adapter.fromJson(usersJson) ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Read Logs
        val logsJson = sharedPrefs.getString("simulator_logs", null)
        if (logsJson != null) {
            try {
                val type = Types.newParameterizedType(List::class.java, BreakLog::class.java)
                val adapter = moshi.adapter<List<BreakLog>>(type)
                _breakLogs.value = adapter.fromJson(logsJson) ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Read Late Requests
        val reqsJson = sharedPrefs.getString("simulator_requests", null)
        if (reqsJson != null) {
            try {
                val type = Types.newParameterizedType(List::class.java, LateRequest::class.java)
                val adapter = moshi.adapter<List<LateRequest>>(type)
                _lateRequests.value = adapter.fromJson(reqsJson) ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveUsersState() {
        val type = Types.newParameterizedType(List::class.java, User::class.java)
        val adapter = moshi.adapter<List<User>>(type)
        sharedPrefs.edit().putString("simulator_users", adapter.toJson(_users.value)).apply()
    }

    private fun saveLogsState() {
        val type = Types.newParameterizedType(List::class.java, BreakLog::class.java)
        val adapter = moshi.adapter<List<BreakLog>>(type)
        sharedPrefs.edit().putString("simulator_logs", adapter.toJson(_breakLogs.value)).apply()
    }

    private fun saveRequestsState() {
        val type = Types.newParameterizedType(List::class.java, LateRequest::class.java)
        val adapter = moshi.adapter<List<LateRequest>>(type)
        sharedPrefs.edit().putString("simulator_requests", adapter.toJson(_lateRequests.value)).apply()
    }

    // --- REPOSITORY SERVICES INTERFACE ---

    fun loginUser(username: String): User? {
        val cleaned = username.trim()
        val foundUser = _users.value.find { it.name.equals(cleaned, ignoreCase = true) }
        
        // If Firebase is disabled and we want to allow instant login
        if (!isFirebaseAvailable && foundUser == null) {
            if (cleaned.equals(predefinedAdmin, ignoreCase = true)) {
                return User(name = predefinedAdmin, role = "admin", status = "idle")
            }
            if (predefinedUsers.any { it.equals(cleaned, ignoreCase = true) }) {
                // Find actual mixed-case username in list
                val exactName = predefinedUsers.first { it.equals(cleaned, ignoreCase = true) }
                return User(name = exactName, role = "user", status = "idle")
            }
        }
        return foundUser
    }

    fun updateUserStatus(username: String, status: String) {
        if (isFirebaseAvailable) {
            val db = firestore ?: return
            db.collection("users").document(username).update("status", status)
        } else {
            _users.value = _users.value.map {
                if (it.name.equals(username, ignoreCase = true)) it.copy(status = status) else it
            }
            saveUsersState()
        }
    }

    fun startBreak(username: String): BreakLog {
        val startTime = System.currentTimeMillis()
        val durationMs = 15 * 60 * 1000 // 15 Minutes
        val endTime = startTime + durationMs
        val logId = UUID.randomUUID().toString()

        val log = BreakLog(
            id = logId,
            userId = username,
            startTime = startTime,
            endTime = endTime,
            status = "on_break"
        )

        updateUserStatus(username, "on_break")

        if (isFirebaseAvailable) {
            val db = firestore ?: return log
            db.collection("break_logs").document(logId).set(log)
        } else {
            _breakLogs.value = (listOf(log) + _breakLogs.value)
            saveLogsState()
        }
        return log
    }

    fun completeBreak(username: String, logId: String) {
        updateUserStatus(username, "completed")
        
        if (isFirebaseAvailable) {
            val db = firestore ?: return
            db.collection("break_logs").document(logId).update("status", "completed")
        } else {
            _breakLogs.value = _breakLogs.value.map {
                if (it.id == logId) it.copy(status = "completed") else it
            }
            saveLogsState()
        }
    }

    fun markAsLate(username: String, logId: String) {
        updateUserStatus(username, "late")

        if (isFirebaseAvailable) {
            val db = firestore ?: return
            db.collection("break_logs").document(logId).update("status", "late")
        } else {
            _breakLogs.value = _breakLogs.value.map {
                if (it.id == logId) it.copy(status = "late") else it
            }
            saveLogsState()
        }
    }

    fun submitLateRequest(username: String, reason: String) {
        val requestId = UUID.randomUUID().toString()
        val request = LateRequest(
            id = requestId,
            userId = username,
            reason = reason,
            status = "pending"
        )

        if (isFirebaseAvailable) {
            val db = firestore ?: return
            db.collection("late_requests").document(requestId).set(request)
        } else {
            _lateRequests.value = _lateRequests.value + request
            saveRequestsState()
        }
    }

    fun approveLateRequest(requestId: String) {
        val req = _lateRequests.value.find { it.id == requestId } ?: return
        val username = req.userId

        // Approve logical status in database and reset user to completed break
        updateUserStatus(username, "completed")

        if (isFirebaseAvailable) {
            val db = firestore ?: return
            db.collection("late_requests").document(requestId).update("status", "approved")
        } else {
            _lateRequests.value = _lateRequests.value.map {
                if (it.id == requestId) it.copy(status = "approved") else it
            }
            saveRequestsState()
        }
    }

    fun rejectLateRequest(requestId: String) {
        if (isFirebaseAvailable) {
            val db = firestore ?: return
            db.collection("late_requests").document(requestId).update("status", "rejected")
        } else {
            _lateRequests.value = _lateRequests.value.map {
                if (it.id == requestId) it.copy(status = "rejected") else it
            }
            saveRequestsState()
        }
    }

    fun clearListeners() {
        usersListener?.remove()
        logsListener?.remove()
        requestsListener?.remove()
    }
}
