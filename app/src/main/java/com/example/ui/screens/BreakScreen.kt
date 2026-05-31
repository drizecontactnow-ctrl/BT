package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.BreakTimerViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BreakScreen(
    username: String,
    viewModel: BreakTimerViewModel,
    onLogout: () -> Unit
) {
    val currentLog by viewModel.currentLog.collectAsState()
    val timeLeftMs by viewModel.timeLeftMs.collectAsState()
    val timerProgress by viewModel.timerProgress.collectAsState()
    val isLate by viewModel.isLate.collectAsState()
    val lateReasonSubmitted by viewModel.lateReasonSubmitted.collectAsState()

    var reasonText by remember { mutableStateOf("") }
    
    val timeFormat = remember { SimpleDateFormat("hh:mm:ss a", Locale.getDefault()) }
    
    val formattedStart = remember(currentLog) {
        currentLog?.startTime?.let { timeFormat.format(Date(it)) } ?: "--:--:--"
    }
    val formattedEnd = remember(currentLog) {
        currentLog?.endTime?.let { timeFormat.format(Date(it)) } ?: "--:--:--"
    }

    // Dynamic timer color based on how much time is left < 2 minutes (120000 ms)
    val ringColor = when {
        isLate -> MaterialTheme.colorScheme.error
        timeLeftMs < 120000L -> MaterialTheme.colorScheme.tertiary // Coral Red warning
        else -> MaterialTheme.colorScheme.primary // Emerald
    }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        )
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "BREAK TIMER",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (!isLate) {
                                viewModel.completeBreakEarly()
                            }
                            onLogout()
                        },
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent,
        modifier = Modifier.background(gradientBrush)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Greeting user details
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "CURRENT SESSION",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = username,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isLate) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isLate) "⚠️ LATE BUDGET" else "⏱️ ON BREAK",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isLate) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // CIRCULAR COUNTDOWN OR LATE BLOCK STATE
            Crossfade(targetState = isLate) { late ->
                if (!late) {
                    // TIMER NORMAL VIEW
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(260.dp)
                                .testTag("circular_timer")
                        ) {
                            // Dial background track
                            Canvas(modifier = Modifier.size(240.dp)) {
                                drawCircle(
                                    color = Color.LightGray.copy(alpha = 0.2f),
                                    radius = size.minDimension / 2,
                                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }

                            // Dial current progress arc
                            Canvas(modifier = Modifier.size(240.dp)) {
                                val arcSize = size.minDimension
                                drawArc(
                                    color = ringColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f * timerProgress,
                                    useCenter = false,
                                    topLeft = androidx.compose.ui.geometry.Offset(
                                        (size.width - arcSize) / 2,
                                        (size.height - arcSize) / 2
                                    ),
                                    size = Size(arcSize, arcSize),
                                    style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }

                            // Middle Countdown text
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val minutes = (timeLeftMs / 1000) / 60
                                val seconds = (timeLeftMs / 1000) % 60
                                val timeText = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

                                Text(
                                    text = timeText,
                                    fontSize = 46.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = ringColor
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (timeLeftMs < 120000L) "Warning!" else "Minutes Left",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = ringColor.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Start & End Timestamps info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Started At", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formattedStart, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Release Target", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formattedEnd, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Informational System Control Status Card
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "SYSTEM REGULATED TIMING",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "This break is fully system-controlled and runs on background scheduling controls. No manual end triggers are permitted. Simply tap the exit/logout button above to check-back in.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                } else {
                    // TIMER PROGRESS COMPLETED / LATE VIEW
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("late_submission_screen"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Late warning",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(72.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "LATE SUBMISSION REQUIREMENT",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = "You exceeded the permitted 15-minute break budget. MONZ security controls require registering a reason to release your session to the Admin.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                        )

                        Crossfade(targetState = lateReasonSubmitted) { submitted ->
                            if (!submitted) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = reasonText,
                                        onValueChange = { reasonText = it },
                                        label = { Text("What caused the delay?") },
                                        placeholder = { Text("e.g. Discussing layout with creative director / Machine rebooting") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                            .testTag("reason_input"),
                                        shape = RoundedCornerShape(12.dp),
                                        maxLines = 4
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = { viewModel.submitLateReason(reasonText) },
                                        enabled = reasonText.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("submit_reason_button")
                                    ) {
                                        Text(
                                            "SUBMIT DELAY REASON",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            } else {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Reason Filed Successfully",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Your reason has been registered and is pending Suruthi (Admin) review. You may sign out or await real-time approval.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
