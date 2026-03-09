package com.atixedu.haruplanner.service

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.*
import com.atixedu.haruplanner.OverlayActivity


class MyForegroundService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var overlayView: View? = null
    private val NOTIFICATION_CHANNEL_ID = "overlay_channel"
    private val NOTIFICATION_ID = 1

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED)
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch {
            while (isActive) {

                val topApp = getForegroundApp(this@MyForegroundService)

                val blockedApps = BlockedAppsManager.getBlockedApps()

                Log.d("hi", topApp.toString())
                withContext(Dispatchers.Main) {
                    if (topApp != null && topApp in blockedApps) {
                        //showOverlay(applicationContext)
                        val overlayIntent = Intent(applicationContext, OverlayActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            putExtra("blockedApp", topApp)
                        }
                        applicationContext.startActivity(overlayIntent)
                    }
                }
                delay(2000)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        removeOverlay()

        // 알림 완전히 제거
        stopForeground(STOP_FOREGROUND_REMOVE)

        // 추가로 NotificationManager로도 제거
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)

        Log.d("Service", "Service destroyed and notification removed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("공부 중")
            .setContentText("공부 중에는 차단 된 앱은 사용할 수 없습니다")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true) // 스와이프로 삭제 방지
            .setAutoCancel(false) // 탭해도 삭제되지 않음
            .setPriority(Notification.PRIORITY_HIGH) // 높은 우선순위
            .apply {
                // Android 8.0 이상에서 추가 설정
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setCategory(Notification.CATEGORY_SERVICE)
                    setShowWhen(false)
                }
                // Android 5.0 이상에서 잠금화면에도 표시
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setVisibility(Notification.VISIBILITY_PUBLIC)
                }
            }
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "학습 모니터링",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "앱 차단 모니터링 서비스"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                // 사용자가 알림 채널을 끌 수 없게 하려면 (선택사항)
                // setImportance(NotificationManager.IMPORTANCE_HIGH)
            }

            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(chan)
        }
    }

    private fun showOverlay(context: Context) {
        Log.d("Overlay", "Trying to show overlay...")
        if (!Settings.canDrawOverlays(context)) {
            Log.e("Overlay", "No overlay permission!")
            return
        }

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        if (overlayView != null) return  // 중복 방지

        val appContext = context.applicationContext

        val container = LinearLayout(appContext).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(50, 50, 50, 50)
            setBackgroundResource(android.R.drawable.dialog_holo_light_frame) // 카드 느낌
            alpha = 0f // 애니메이션 위해 시작 투명
            animate().alpha(1f).setDuration(500).start()
        }

        val title = TextView(appContext).apply {
            text = "차단된 앱 실행 감지!"
            textSize = 20f
            setTextColor(android.graphics.Color.WHITE)
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
        }

        val message = TextView(appContext).apply {
            text = "해당 앱은 현재 사용이 제한되어 있습니다."
            textSize = 16f
            setTextColor(android.graphics.Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 0)
        }

        container.setBackgroundColor(0xCC000000.toInt()) // 반투명 배경
        container.addView(title)
        container.addView(message)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        try {
            wm.addView(container, params)
            overlayView = container
            Log.d("Overlay", "✅ addView 성공")
        } catch (e: Exception) {
            Log.e("Overlay", "❌ addView 실패: ${e.message}")
        }
    }

    private fun removeOverlay() {
        overlayView?.let { view ->
            try {
                val wm = getSystemService(WINDOW_SERVICE) as WindowManager
                wm.removeView(view)
                overlayView = null
                Log.d("Overlay", "✅ removeView 성공")
            } catch (e: Exception) {
                Log.e("Overlay", "❌ removeView 실패: ${e.message}")
            }
        }
    }

    fun getForegroundApp(context: Context): String? {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val begin = now - 1000 * 10 // 최근 10초간 이벤트 조회 범위

        val usageEvents = usm.queryEvents(begin, now)
        var lastForegroundPackage: String? = null

        val event = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                // 포그라운드로 진입한 앱 패키지명 갱신
                lastForegroundPackage = event.packageName
            }
        }
        return lastForegroundPackage
    }
}