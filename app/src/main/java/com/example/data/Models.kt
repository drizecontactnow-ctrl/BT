package com.example.data

import androidx.annotation.Keep

@Keep
data class User(
    val name: String = "",
    val role: String = "user", // "user" or "admin"
    val status: String = "idle" // "idle", "on_break", "completed", "late"
)

@Keep
data class BreakLog(
    val id: String = "",
    val userId: String = "", // Matches User.name
    val startTime: Long = 0,
    val endTime: Long = 0,
    val status: String = "on_break" // "on_break", "completed", "late"
)

@Keep
data class LateRequest(
    val id: String = "",
    val userId: String = "", // Matches User.name
    val reason: String = "",
    val status: String = "pending" // "pending", "approved", "rejected"
)
