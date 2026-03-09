package com.atixedu.haruplanner.receiver // 적절한 패키지로 변경하세요.

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.atixedu.haruplanner.LockActivity // 잠금 화면 액티비티
import com.atixedu.haruplanner.MainActivity
import com.atixedu.haruplanner.R

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val planId = intent.getIntExtra("PLAN_ID", -1)
        val planTitle = intent.getStringExtra("PLAN_TITLE") ?: "알 수 없는 계획"
        val planContent = intent.getStringExtra("PLAN_CONTENT") ?: "알 수 없는 내용"
        val startTime = intent.getStringExtra("PLAN_START_TIME") ?: "알 수 없음"

        Log.d("AlarmReceiver", "알람 수신: ID=$planId, 제목=$planTitle, 시간=$startTime")

        // TODO: LockActivity 대신 Notification을 띄우는 로직으로 변경
        showNotification(context, planId, planTitle, planContent)
    }

    /**
     * 알림을 생성하고 표시하는 함수
     */
    private fun showNotification(context: Context, planId: Int, planTitle: String, planContent: String) {
        val channelId = "haru_planner_alarm_channel"
        val channelName = context.getString(R.string.notification_channel_name) // 예시: "Haru Planner 알림"

        // 알림 클릭 시 실행될 Intent
        val notificationIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("planId", planId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            planId,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // NotificationManager를 가져옵니다.
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android O(API 26) 이상에서는 알림 채널이 필수
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH // 알림 중요도 설정
            ).apply {
                description = "Haru Planner의 알림입니다."
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Notification Builder를 사용하여 알림을 생성합니다.
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 알림 아이콘
            .setContentTitle(planTitle) // 알림 제목
            .setContentText(planContent) // 알림 내용
            .setPriority(NotificationCompat.PRIORITY_HIGH) // 알림 중요도
            .setContentIntent(pendingIntent) // 알림 클릭 시 실행될 PendingIntent
            .setAutoCancel(true) // 알림 클릭 시 자동으로 사라지도록 설정

        // 알림을 표시합니다.
        notificationManager.notify(planId, builder.build())
    }
}