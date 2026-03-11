package com.yourname.alphaai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.yourname.alphaai.core.AppResolver
import com.yourname.alphaai.core.ExecutionEvent
import com.yourname.alphaai.core.SimpleScheduler
import com.yourname.alphaai.data.AppHubDatabase
import com.yourname.alphaai.data.RecommendationLog
import com.yourname.alphaai.data.UserAction
import com.yourname.alphaai.recommendation.RecommendationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.yourname.alphaai.skills.CameraSkill
import com.yourname.alphaai.skills.IntentSkill
import com.yourname.alphaai.skills.LocationSkill
import com.yourname.alphaai.skills.NotificationSkill
import com.yourname.alphaai.skills.ToastSkill
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
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
    private val database by lazy { (application as AppHubApplication).database }
    private val historyViewModel: HistoryViewModel by viewModels {
        HistoryViewModelFactory(database)
    }
    private val recommendationLogViewModel: RecommendationLogViewModel by viewModels {
        RecommendationLogViewModelFactory(database)
    }

    // 初始化调度器并注册技能
    private val scheduler by lazy {
        val skills = mapOf(
            "system.toast" to ToastSkill(this),
            "camera.take_photo" to CameraSkill(this, this),
            "location.get" to LocationSkill(this),
            "notification.show" to NotificationSkill(this),
            "intent.execute" to intentSkill,
            "intent.launch" to intentSkill
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
            AppHubTheme {
                MainScreen(
                    scheduler = scheduler,
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
    hasLocationPermission: () -> Boolean,
    requestLocationPermission: () -> Unit,
    clearLearningData: suspend () -> Unit,
    recentActions: List<UserAction>,
    triggerRecommendationNow: () -> Unit,
    recentRecommendationLogs: List<RecommendationLog>
) {
    var inputText by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("等待指令...") }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showRecommendationDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

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
            label = { Text("输入指令") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (isLocationIntent(inputText) && !hasLocationPermission()) {
                    requestLocationPermission()
                    outputText = "请先授予位置权限后重试"
                    return@Button
                }

                coroutineScope.launch {
                    scheduler.submit(inputText).collect { event ->
                        outputText = when (event) {
                            is ExecutionEvent.Started -> "执行中: ${event.skillId}"
                            is ExecutionEvent.Completed -> "完成: ${event.result}"
                            is ExecutionEvent.Failed -> "失败: ${event.error}"
                        }
                    }
                }
            }
        ) {
            Text("执行")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    clearLearningData()
                    outputText = "学习数据已清除"
                }
            }
        ) {
            Text("清除学习数据")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                showHistoryDialog = true
            }
        ) {
            Text("查看最近操作")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                triggerRecommendationNow()
                outputText = "已触发推荐检查（调试）"
            }
        ) {
            Text("立即触发推荐")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                showRecommendationDialog = true
            }
        ) {
            Text("查看推荐记录")
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
    }
}

@Composable
fun HistoryDialog(actions: List<UserAction>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("最近操作") },
        text = {
            LazyColumn {
                items(actions) { action ->
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Text("指令: ${action.intent}")
                        Text("技能: ${action.skillId ?: "无"}")
                        val status = if (action.success) "✓" else "✗"
                        Text("结果: $status ${action.resultMessage ?: ""}")
                        Text("时间: ${formatTime(action.timestamp)}", fontSize = 12.sp)
                        Divider(modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
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
        title = { Text("推荐记录") },
        text = {
            LazyColumn {
                items(logs) { log ->
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Text("规则: ${log.ruleName} (${log.ruleId})")
                        Text("内容: ${log.content}")
                        Text("状态: ${log.status}")
                        Text("时间: ${formatTime(log.triggerTime)}", fontSize = 12.sp)
                        Divider(modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

private fun isLocationIntent(text: String): Boolean {
    return text.contains("location", ignoreCase = true) ||
        text.contains("where", ignoreCase = true) ||
        text.contains("定位") ||
        text.contains("位置")
}

// 临时主题
@Composable
fun AppHubTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
