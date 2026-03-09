package com.atixedu.haruplanner.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.atixedu.haruplanner.LockActivity
import com.atixedu.haruplanner.data.LogEntry
import com.atixedu.haruplanner.data.Plan
import com.atixedu.haruplanner.data.StudentPlan
import com.atixedu.haruplanner.util.AlarmScheduleManager
import java.time.OffsetDateTime

class WebviewInterface(private val context: Context) {

    @JavascriptInterface
    fun addPlan(json: String) : Boolean {
        fun getAlarmRequestCode(planId: Int): Int {
            // Plan ID를 기반으로 고유한 요청 코드를 생성합니다.
            // 예를 들어, 큰 숫자를 더하거나 비트 연산을 사용하여 다른 PendingIntent와 충돌을 피할 수 있습니다.
            return planId * 1000 + 1 // 예시: planId 1 -> 1001, planId 2 -> 2001
        }

        try {
            val plan = Plan.fromJson(json)
            ScheduleManager.setPlan(plan) // DB 또는 로컬 스케줄에 계획 저장

            // Android 12+ (API 31+) 에서 정확한 알람 권한 확인 및 요청
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                if (!alarmManager.canScheduleExactAlarms()) {
                    // 권한이 없으면 설정 화면으로 이동하여 요청
                    try {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = android.net.Uri.parse("package:${context.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                        Log.w("WebAppInterface", "정확한 알람 권한이 필요합니다. 설정 화면으로 이동합니다.")
                    } catch (e: Exception) {
                        Log.e("WebAppInterface", "권한 요청 화면 열기 실패: ${e.message}")
                    }
                    //TODO Response 웹에서 잘 처리해주도록 해여함.
                    //return false // 권한이 없으므로 알람 설정 실패로 간주
                }
            }

            // 알람 예약
            AlarmScheduleManager.scheduleAlarm(
                context,
                planId = getAlarmRequestCode(plan.id),
                planTitle = plan.title,
                startTime = plan.startTime
            )

            return true
        } catch (e: Exception) {
            Log.e("WebAppInterface", "addPlan 오류: ${e.message}")
            return false
        }
    }

    @JavascriptInterface
    fun getStudentPlan() : String {
        try {
            //WEB에서 DB에 저장되어있는 것을 구조에 맞게 넘겨줘야함.
            val result = ScheduleManager.getStudentPlan()
            return result!!.toJson()
        } catch (e: Exception) {
            Log.e("WebAppInterface", "JSON 파싱 실패: ${e.message}")
            return StudentPlan(1, emptyList()).toJson()
        }
    }

    @JavascriptInterface
    fun removePlan(id: Int): Boolean {
        return try {
            ScheduleManager.removePlan(id)
            // 해당 계획에 대한 알람 취소
            AlarmScheduleManager.cancelAlarm(context, id)
            true
        } catch (e: Exception) {
            Log.e("WebAppInterface", "계획 삭제 실패: ${e.message}")
            false
        }
    }

    @JavascriptInterface
    fun modifyPlan(json: String): Boolean {
        return try {
            val plan = Plan.fromJson(json)
            // 기존 알람 취소 (혹시 모를 중복 방지 및 시간 변경 대응)
            AlarmScheduleManager.cancelAlarm(context, plan.id)
            // DB 또는 로컬 스케줄에서 계획 수정
            ScheduleManager.modifyPlan(plan)
            // 수정된 정보로 알람 재예약
            AlarmScheduleManager.scheduleAlarm(
                context,
                planId = plan.id,
                planTitle = plan.title,
                startTime = plan.startTime
            )
            true
        } catch (e: Exception) {
            Log.e("WebAppInterface", "계획 수정 실패: ${e.message}")
            false
        }
    }


    @JavascriptInterface
    fun moveLockActivity() : Boolean {
        try {
            //WEB에서 DB에 저장되어있는 것을 구조에 맞게 넘겨줘야함.
            context.startActivity(
                Intent(context, LockActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // 새로운 태스크에서 Activity 시작
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP) // 이미 존재하는 태스크의 최상단에서 시작

                }
            )
            return true
        } catch (e: Exception) {
            return false
        }
    }

    @JavascriptInterface
    fun giveUpNowPlan(id: Int) : Boolean {
        try {
            ScheduleManager.updateStatus(id, 1) // 포기한 상태로 변경
            ScheduleManager.setLog(id, LogEntry("exit", OffsetDateTime.now().toString()))
            // 만약 포기하면 더 이상 알람이 울릴 필요가 없다
            AlarmScheduleManager.cancelAlarm(context, id)
            return true
        } catch (e: Exception) {
            Log.e("WebAppInterface", "JSON 파싱 실패: ${e.message}")
            return false
        }
    }
}