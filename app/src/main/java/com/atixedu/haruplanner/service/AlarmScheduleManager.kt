package com.atixedu.haruplanner.util // 적절한 패키지로 변경하세요.

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.atixedu.haruplanner.LockActivity // 알람 발생 시 실행될 액티비티
import com.atixedu.haruplanner.receiver.AlarmReceiver // 알람을 수신할 BroadcastReceiver
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Calendar

object AlarmScheduleManager {

    private const val TAG = "AlarmScheduleManager"

    // 알람 요청 코드를 생성하는 함수 (Plan ID를 기반으로 고유하게)


    /**
     * 특정 계획에 대한 알람을 예약합니다.
     * @param context 애플리케이션 컨텍스트
     * @param planId 계획의 고유 ID
     * @param planTitle 알람에 표시될 계획 제목
     * @param startTime 알람이 울릴 시간 (HH:mm 형식)
     */
    fun scheduleAlarm(context: Context, planId: Int, planTitle: String, startTime: String) {
        fun parseToLocalDateTime(timeString: String): LocalDateTime {
            val utcInstant = Instant.parse(timeString)
            return LocalDateTime.ofInstant(utcInstant, ZoneId.systemDefault())
        }

        // startTime 문자열을 ZonedDateTime으로 파싱하여 LocalDateTime으로 변환
        val localDateTime = try {
            parseToLocalDateTime(startTime)
        } catch (e: DateTimeParseException) {
            Log.e(TAG, "startTime 형식 오류: 올바른 ZonedDateTime 형식이 아닙니다. $startTime", e)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            // 알람 수신기에 전달할 추가 데이터
            putExtra("PLAN_ID", planId)
            putExtra("PLAN_TITLE", "${planTitle} 계획 시간")
            putExtra("PLAN_CONTENT", "계획을 시작할 시간이에요.")
            putExtra("PLAN_START_TIME", localDateTime.toString())
        }

        // PendingIntent의 요청 코드를 planId 기반으로 고유하게 설정
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            planId, // 고유한 요청 코드
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE // 기존 PendingIntent가 있다면 업데이트
        )

        // Calendar 객체에 localDateTime의 모든 날짜와 시간 정보 설정
        val calendar = java.util.Calendar.getInstance().apply {
            set(Calendar.YEAR, localDateTime.year)
            set(Calendar.MONTH, localDateTime.monthValue - 1) // Calendar의 MONTH는 0부터 시작 (0:1월, 11:12월)
            set(Calendar.DAY_OF_MONTH, localDateTime.dayOfMonth)
            set(Calendar.HOUR_OF_DAY, localDateTime.hour)
            set(Calendar.MINUTE, localDateTime.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 현재 시간보다 과거이면 다음 날로 설정 (선택 사항)
//        if (calendar.timeInMillis <= System.currentTimeMillis()) {
//            calendar.add(java.util.Calendar.DATE, 1)
//        }

        try {
            // Android 12+ (API 31+) 정확한 알람 권한 확인 및 처리 필요
            // 이 부분은 addPlan에서 처리하는 것이 더 적절할 수 있습니다.
            // 여기서는 이미 권한이 있다고 가정하고 진행합니다.
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle( // 또는 setExact
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "알람 예약됨 (API 31+): ID=$planId, 시간=${localDateTime}")
                } else {
                    Log.w(TAG, "정확한 알람 권한이 없어 ID=$planId 알람 예약 실패.")
                    // 사용자에게 권한 요청 UI를 띄우도록 유도할 수 있습니다.
                }
            } else {
                alarmManager.setExact( // 이전 버전에서는 setExact 사용
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                Log.d(TAG, "알람 예약됨 (API < 31): ID=$planId, 시간=${localDateTime}")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "알람 예약 실패: 정확한 알람 권한이 없습니다. ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "알람 예약 중 오류 발생: ${e.message}")
        }
    }

    /**
     * 특정 계획에 대한 알람을 취소합니다.
     * @param context 애플리케이션 컨텍스트
     * @param planId 취소할 계획의 고유 ID
     */
    fun cancelAlarm(context: Context, planId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            planId, // 예약 시 사용했던 것과 동일한 요청 코드 사용
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE // 기존 PendingIntent가 없으면 생성하지 않음
        )

        // PendingIntent가 존재하는 경우에만 취소
        pendingIntent?.let {
            alarmManager.cancel(it)
            Log.d(TAG, "알람 취소됨: ID=$planId")
            it.cancel() // PendingIntent 자원 해제 (선택 사항이지만 권장)
        } ?: Log.d(TAG, "취소할 알람이 존재하지 않음: ID=$planId")
    }

    /**
     * 모든 예약된 알람을 취소합니다. (선택 사항)
     * 앱을 완전히 종료하거나 데이터가 초기화될 때 유용할 수 있습니다.
     * 주의: 이 함수는 모든 계획을 순회하며 개별적으로 취소해야 합니다.
     * 현재 스케줄된 모든 계획의 ID를 알아야 합니다.
     */
    fun cancelAllAlarms(context: Context) {
        // 이 함수를 사용하려면 ScheduleManager.getStudentPlan() 등을 통해
        // 현재 저장된 모든 Plan의 ID 목록을 가져와야 합니다.
        Log.w(TAG, "모든 알람 취소 기능은 모든 plan ID에 대한 반복 처리가 필요합니다.")
        // 예시 (실제 구현 시 ScheduleManager에서 모든 plan ID를 가져와야 함):
        /*
        val allPlans = ScheduleManager.getStudentPlan()?.plans
        allPlans?.forEach { plan ->
            cancelAlarm(context, plan.id)
        }
        */
    }
}