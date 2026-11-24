package io.github.zdyoyoyo.backendquiz.ui.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.zdyoyoyo.backendquiz.AppContainer
import io.github.zdyoyoyo.backendquiz.BuildConfig
import io.github.zdyoyoyo.backendquiz.notify.Notifications

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(container: AppContainer, onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SettingsViewModel(container.settingsStore, container.reminderScheduler) }
        },
    )
    val reminder by vm.reminder.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 通知權限狀態；從系統設定回來時重新檢查
    var canNotify by remember { mutableStateOf(Notifications.canPost(context)) }
    LifecycleResumeEffect(Unit) {
        canNotify = Notifications.canPost(context)
        onPauseOrDispose { }
    }

    // Android 13+ 的執行期權限請求：結果回來才真的開啟提醒
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        canNotify = Notifications.canPost(context)
        if (granted) vm.setEnabled(true)
    }

    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
            )
        },
    ) { padding ->
        val r = reminder ?: return@Scaffold
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("每日提醒", style = MaterialTheme.typography.titleMedium)
                    Text("提醒今天該複習的題數，也會告訴你題庫更新", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = r.enabled,
                    onCheckedChange = { checked ->
                        if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !canNotify) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            vm.setEnabled(checked)
                        }
                    },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().clickable(enabled = r.enabled) { showTimePicker = true },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("提醒時間", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(
                    "%02d:%02d".format(r.hour, r.minute),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (r.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                )
            }

            if (r.enabled && !canNotify) {
                Text(
                    "通知權限被關閉了，提醒不會出現。",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    )
                }) { Text("開啟系統通知設定") }
            }

            if (r.enabled && canNotify) {
                OutlinedButton(onClick = vm::testNow) { Text("立即測試通知") }
            }

            Text(
                "Android 為了省電會調整背景工作的時間，實際提醒可能比設定時間晚幾分鐘到十幾分鐘。",
                style = MaterialTheme.typography.bodySmall,
            )

            HorizontalDivider()
            Text("版本 ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall)
        }

        if (showTimePicker) {
            val state = rememberTimePickerState(initialHour = r.hour, initialMinute = r.minute, is24Hour = true)
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        vm.setTime(state.hour, state.minute)
                        showTimePicker = false
                    }) { Text("確定") }
                },
                dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("取消") } },
                text = { TimePicker(state = state) },
            )
        }
    }
}
