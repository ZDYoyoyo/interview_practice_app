package io.github.zdyoyoyo.backendquiz.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.zdyoyoyo.backendquiz.MainActivity
import io.github.zdyoyoyo.backendquiz.R
import io.github.zdyoyoyo.backendquiz.domain.reminder.ReminderMessage

object Notifications {
    const val CHANNEL_REMINDER = "daily_reminder"
    private const val ID_REMINDER = 1001

    /** Android 8+ 必須先建立通知頻道，使用者可以在系統設定裡針對頻道開關；重複呼叫沒有副作用 */
    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDER, "每日複習提醒", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "每天提醒你該複習的題目與題庫更新"
            }
        )
    }

    /** Android 13+ 需要使用者授權 POST_NOTIFICATIONS；舊版本預設允許 */
    fun canPost(context: Context): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun showReminder(context: Context, message: ReminderMessage) {
        if (!canPost(context)) return
        ensureChannels(context)

        // 點通知 → 打開 App
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(message.title)
            .setContentText(message.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(ID_REMINDER, notification)
        } catch (e: SecurityException) {
            // 權限在檢查後被撤銷：忽略即可
        }
    }
}
