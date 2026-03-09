package com.atixedu.haruplanner

import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.atixedu.haruplanner.data.LogEntry
import com.atixedu.haruplanner.data.Plan
import com.atixedu.haruplanner.service.MyForegroundService
import com.atixedu.haruplanner.service.ScheduleManager
import java.time.OffsetDateTime

class OverlayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val nowPlan : Plan? = ScheduleManager.getNowPlan()

        if (nowPlan == null) {
            val intent = Intent(this@OverlayActivity, MyForegroundService::class.java)
            stopService(intent)
            finish()
            return
        }

        val appIcon: Drawable = packageManager.getApplicationIcon(packageName)
        val appName: String = packageManager.getApplicationLabel(
            packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        ).toString()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(android.graphics.Color.parseColor("#99000000"))
            setPadding(24.dpToPx(), 24.dpToPx(), 24.dpToPx(), 24.dpToPx())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val iconView = ImageView(this).apply {
            setImageDrawable(appIcon)
            layoutParams = LinearLayout.LayoutParams(100.dpToPx(), 100.dpToPx()).apply {
                bottomMargin = 16.dpToPx()
            }
        }

        val appNameView = TextView(this).apply {
            text = appName
            setTextColor(android.graphics.Color.WHITE)
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8.dpToPx()
            }
        }

        val messageView = TextView(this).apply {
            text = "이 앱은 차단되었습니다."
            setTextColor(android.graphics.Color.WHITE)
            textSize = 18f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8.dpToPx()
            }
        }

        val planNameView = TextView(this).apply {
            text = "현재 계획: ${nowPlan.title}"
            setTextColor(android.graphics.Color.LTGRAY)
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 24.dpToPx()
            }
        }

        val pauseButton = Button(this).apply {
            text = "일시정지"
            setTextColor(Color.WHITE)
            backgroundTintList = ColorStateList.valueOf(0xFF9E9E9E.toInt()) // 회색
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12.dpToPx()
            }
            setOnClickListener {
                //일시정지 로직
                ScheduleManager.setLog(nowPlan.id, LogEntry("exit", OffsetDateTime.now().toString()))

                val intent = Intent(this@OverlayActivity, MyForegroundService::class.java)
                stopService(intent)
                finish()
            }
        }

        val giveUpButton = Button(this).apply {
            text = "포기하기"
            setTextColor(Color.WHITE)
            backgroundTintList = ColorStateList.valueOf(0xFFFF5722.toInt()) // 주황색
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                //포기 로직
                ScheduleManager.updateStatus(nowPlan.id, 1) // 포기한 상태로 변경

                val intent = Intent(this@OverlayActivity, MyForegroundService::class.java)
                stopService(intent)
                finish()
            }
        }

        layout.apply {
            addView(iconView)
            addView(appNameView)
            addView(messageView)
            addView(planNameView)
            addView(pauseButton)
            addView(giveUpButton)
        }

        setContentView(layout)
    }



    // dp를 px로 변환하는 확장함수 (Activity 또는 Context 안에 추가)
    private fun Int.dpToPx(): Int =
        (this * resources.displayMetrics.density).toInt()

    override fun onBackPressed() {
        super.onBackPressed()

        // 홈 화면으로 강제 이동
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)

        // 액티비티 종료
        finish()
    }
}
