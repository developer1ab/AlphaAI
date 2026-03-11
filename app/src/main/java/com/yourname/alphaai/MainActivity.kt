package com.yourname.alphaai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import com.example.alphaai.BuildConfig
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.yourname.alphaai.core.AppResolver
import com.yourname.alphaai.core.ExecutionEvent
import com.yourname.alphaai.core.SimpleScheduler
import com.yourname.alphaai.data.RecommendationLog
import com.yourname.alphaai.data.UserAction
import com.yourname.alphaai.accessibility.AccessibilitySkill
import com.yourname.alphaai.recommendation.RecommendationScheduler
import com.yourname.alphaai.skills.CameraSkill
import com.yourname.alphaai.skills.CloudApiSkill
import com.yourname.alphaai.skills.IntentSkill
import com.yourname.alphaai.skills.LocationSkill
import com.yourname.alphaai.skills.NotificationSkill
import com.yourname.alphaai.skills.ToastSkill
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    private val appResolver by lazy { AppResolver(this) }
    private val intentSkill by lazy { IntentSkill(this) }
    private val accessibilitySkill by lazy {
        AccessibilitySkill(this, BuildConfig.ACCESSIBILITY_SIMULATION_ENABLED)
    }
    private val cloudApiSkill by lazy {
        CloudApiSkill(
            gatewayBaseUrl = BuildConfig.CLOUD_GATEWAY_BASE_URL,
            userApiKey = BuildConfig.CLOUD_GATEWAY_API_KEY
        )
    }
    private val database by lazy { (application as AlphaAIApplication).database }
    private val historyViewModel: HistoryViewModel by viewModels {
        HistoryViewModelFactory(database)
    }
    private val recommendationLogViewModel: RecommendationLogViewModel by viewModels {
        RecommendationLogViewModelFactory(database)
    }

    private val scheduler by lazy {
        val skills = mapOf(
            "system.toast" to ToastSkill(this),
            "camera.take_photo" to CameraSkill(this, this),
            "location.get" to LocationSkill(this),
            "notification.show" to NotificationSkill(this),
            "intent.execute" to intentSkill,
            "intent.launch" to intentSkill,
            "accessibility.control" to accessibilitySkill,
            "cloud.api" to cloudApiSkill
        )
        SimpleScheduler(skills, appResolver, database)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureCameraPermission()
        ensureLocationPermission()
        handleRecommendationIntent(intent)

        setContent {
            val recentActions by historyViewModel.recentActions.observeAsState(emptyList())
            val recentRecommendationLogs by recommendationLogViewModel.recentLogs.observeAsState(emptyList())
            AlphaAITheme {
                MainScreen(
                    scheduler = scheduler,
                    accessibilityFeatureEnabled = BuildConfig.ACCESSIBILITY_SIMULATION_ENABLED,
                    hasLocationPermission = ::hasLocationPermission,
                    requestLocationPermission = ::ensureLocationPermission,
                    clearLearningData = ::clearLearningData,
                    recentActions = recentActions,
                    triggerRecommendationNow = ::triggerRecommendationNow,
                    recentRecommendationLogs = recentRecommendationLogs
                )
            }
        }
    }

    private fun triggerRecommendationNow() {
        RecommendationScheduler.triggerNow(this)
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleRecommendationIntent(intent)
    }

    private fun handleRecommendationIntent(intent: android.content.Intent?) {
        val recommendation = intent?.getStringExtra("recommendation_intent") ?: return
        lifecycleScope.launch {
            scheduler.submit(recommendation).collect { }
        }
    }

    private suspend fun clearLearningData() {
        withContext(Dispatchers.IO) {
            database.userActionDao().clearAll()
            database.appUsageDao().clearAll()
            database.userProfileDao().clearAll()
            database.recommendationLogDao().clearAll()
        }
    }

    private fun ensureCameraPermission() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun ensureLocationPermission() {
        if (!hasLocationPermission()) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }
}

@Composable
fun MainScreen(
    scheduler: SimpleScheduler,
    accessibilityFeatureEnabled: Boolean,
    hasLocationPermission: () -> Boolean,
    requestLocationPermission: () -> Unit,
    clearLearningData: suspend () -> Unit,
    recentActions: List<UserAction>,
    triggerRecommendationNow: () -> Unit,
    recentRecommendationLogs: List<RecommendationLog>
) {
    var inputText by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("Waiting for command...") }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showRecommendationDialog by remember { mutableStateOf(false) }
    var showAccessibilityRiskDialog by remember { mutableStateOf(false) }
    var pendingCommand by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    fun executeCommand(command: String) {
        coroutineScope.launch {
            scheduler.submit(command).collect { event ->
                outputText = when (event) {
                    is ExecutionEvent.Started -> "Running: ${event.skillId}"
                    is ExecutionEvent.Completed -> "Completed: ${formatExecutionResult(event.result)}"
                    is ExecutionEvent.Failed -> "Failed: ${event.error}"
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Enter command") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (isLocationIntent(inputText) && !hasLocationPermission()) {
                    requestLocationPermission()
                    outputText = "Location permission is required. Please grant permission and retry."
                    return@Button
                }

                if (isAccessibilityIntent(inputText)) {
                    if (!accessibilityFeatureEnabled) {
                        outputText = "Accessibility simulation is disabled in this build."
                        return@Button
                    }
                    pendingCommand = inputText
                    showAccessibilityRiskDialog = true
                    return@Button
                }

                executeCommand(inputText)
            }
        ) {
            Text("Run")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    clearLearningData()
                    outputText = "Learning data cleared."
                }
            }
        ) {
            Text("Clear learning data")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { showHistoryDialog = true }) {
            Text("View recent actions")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                triggerRecommendationNow()
                outputText = "Recommendation check triggered (debug)."
            }
        ) {
            Text("Trigger recommendation now")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { showRecommendationDialog = true }) {
            Text("View recommendation logs")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(outputText)

        if (showHistoryDialog) {
            HistoryDialog(
                actions = recentActions,
                onDismiss = { showHistoryDialog = false }
            )
        }

        if (showRecommendationDialog) {
            RecommendationLogDialog(
                logs = recentRecommendationLogs,
                onDismiss = { showRecommendationDialog = false }
            )
        }

        if (showAccessibilityRiskDialog) {
            AlertDialog(
                onDismissRequest = { showAccessibilityRiskDialog = false },
                title = { Text("High-Risk Capability") },
                text = {
                    Text(
                        "Screen simulation can trigger unintended operations. Continue only if you understand the risk and accept full responsibility."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showAccessibilityRiskDialog = false
                        executeCommand(pendingCommand)
                    }) {
                        Text("I Understand")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAccessibilityRiskDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun HistoryDialog(actions: List<UserAction>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Recent Actions") },
        text = {
            LazyColumn {
                items(actions) { action ->
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Text("Intent: ${action.intent}")
                        Text("Skill: ${action.skillId ?: "N/A"}")
                        val status = if (action.success) "OK" else "ERROR"
                        Text("Result: $status ${action.resultMessage ?: ""}")
                        Text("Time: ${formatTime(action.timestamp)}", fontSize = 12.sp)
                        Divider(modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

@Composable
fun RecommendationLogDialog(logs: List<RecommendationLog>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Recommendation Logs") },
        text = {
            LazyColumn {
                items(logs) { log ->
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Text("Rule: ${log.ruleName} (${log.ruleId})")
                        Text("Content: ${log.content}")
                        Text("Status: ${log.status}")
                        Text("Time: ${formatTime(log.triggerTime)}", fontSize = 12.sp)
                        Divider(modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private fun isLocationIntent(text: String): Boolean {
    return text.contains("location", ignoreCase = true) ||
        text.contains("where", ignoreCase = true)
}

private fun isAccessibilityIntent(text: String): Boolean {
    return text.trim().startsWith("access", ignoreCase = true)
}

private fun formatExecutionResult(result: Map<String, Any>): String {
    val summary = result["summary"] as? String
    if (!summary.isNullOrBlank()) {
        return summary
    }
    return result.toString()
}

@Composable
fun AlphaAITheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
