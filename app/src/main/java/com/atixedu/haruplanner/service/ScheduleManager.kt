package com.atixedu.haruplanner.service

import android.content.Context
import android.content.SharedPreferences
import com.atixedu.haruplanner.data.LogEntry
import com.atixedu.haruplanner.data.Plan
import com.atixedu.haruplanner.data.StudentPlan
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object ScheduleManager {
    private const val PREF_NAME = "schedule_prefs"
    private const val KEY_STUDENT_PLAN = "student_plan"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        //clearAll()
    }

    fun saveStudentPlan(studentPlan: StudentPlan) : Boolean {
        val json = studentPlan.toJson()
        prefs.edit().putString(KEY_STUDENT_PLAN, json).apply()
        return true
    }

    fun getStudentPlan(): StudentPlan? {
        val json = prefs.getString(KEY_STUDENT_PLAN, null) ?: return StudentPlan(1, emptyList())
        val studentPlan = StudentPlan.fromJson(json)
        return studentPlan
    }


    //계획 추가하기
    fun setPlan(plan : Plan):Boolean {
        // 기존 데이터 불러오기
        val json = prefs.getString(KEY_STUDENT_PLAN, null)
        val updatedStudentPlan = if (json != null) {
            val existing = StudentPlan.fromJson(json)
            existing.copy(planList = existing.planList + plan)
        } else {
            StudentPlan(
                studentId = 1, // 또는 기본값/동적으로 지정
                planList = listOf(plan)
            )
        }

        saveStudentPlan(updatedStudentPlan)
        return true
    }


    //
    // 계획 수정하기
    fun modifyPlan(modifiedPlan: Plan): Boolean {
        val json = prefs.getString(KEY_STUDENT_PLAN, null) ?: return false
        val studentPlan = StudentPlan.fromJson(json)

        val updatedPlanList = studentPlan.planList.map { plan ->
            if (plan.id == modifiedPlan.id) modifiedPlan else plan
        }

        val updatedStudentPlan = studentPlan.copy(planList = updatedPlanList)
        saveStudentPlan(updatedStudentPlan)
        return true
    }

    // 계획 삭제하기
    fun removePlan(planId: Int): Boolean {
        val json = prefs.getString(KEY_STUDENT_PLAN, null) ?: return false
        val studentPlan = StudentPlan.fromJson(json)

        val updatedPlanList = studentPlan.planList.filter { plan ->
            plan.id != planId
        }

        val updatedStudentPlan = studentPlan.copy(planList = updatedPlanList)
        saveStudentPlan(updatedStudentPlan)
        return true
    }



    //로그 insert
    fun setLog(planId: Int, log: LogEntry) {
        val json = prefs.getString(KEY_STUDENT_PLAN, null) ?: return
        val studentPlan = StudentPlan.fromJson(json)

        // planList에서 해당 planId를 찾아 logs에 log 추가
        val updatedPlans = studentPlan.planList.map { plan ->
            if (plan.id == planId) {
                try {
                    // 로그 시간과 계획 종료시간을 파싱
                    val logDateTime = ZonedDateTime.parse(log.timestamp, DateTimeFormatter.ISO_DATE_TIME)
                        .withZoneSameInstant(ZoneId.systemDefault())
                    val planEndDateTime = ZonedDateTime.parse(plan.endTime, DateTimeFormatter.ISO_DATE_TIME)
                        .withZoneSameInstant(ZoneId.systemDefault())

                    // 로그 시간이 계획 종료시간보다 크면 종료시간으로 대체
                    val adjustedLog = if (logDateTime.isAfter(planEndDateTime)) {
                        log.copy(timestamp = plan.endTime)
                    } else {
                        log
                    }

                    plan.copy(logs = plan.logs + adjustedLog)
                } catch (e: Exception) {
                    // 파싱 오류 시 원본 로그 사용
                    plan.copy(logs = plan.logs + log)
                }
            } else {
                plan
            }
        }

        val updatedStudentPlan = studentPlan.copy(planList = updatedPlans)

        saveStudentPlan(updatedStudentPlan)
    }

    //오늘 계획 가져오기
    fun getTodayPlan(): List<Plan>? {
        val json = prefs.getString(KEY_STUDENT_PLAN, null) ?: return null
        val studentPlan = StudentPlan.fromJson(json)
        val planList = studentPlan.planList

        val today = LocalDate.now() // 디바이스 로컬 기준

        return planList.filterNotNull()
            .filter { plan ->
                try {
                    val zoned = ZonedDateTime.parse(plan.startTime, DateTimeFormatter.ISO_DATE_TIME)
                        .withZoneSameInstant(ZoneId.systemDefault()) // 로컬 시간대 변환
                    zoned.toLocalDate() == today
                } catch (e: Exception) {
                    false
                }
            }.sortedBy { plan ->
                try {
                    ZonedDateTime.parse(plan.startTime, DateTimeFormatter.ISO_DATE_TIME)
                        .withZoneSameInstant(ZoneId.systemDefault())
                        .toLocalDateTime()
                } catch (e: Exception) {
                    LocalDateTime.MAX // 에러 시 뒤로 정렬
                }
            }
    }



    //계획 포기 선언 (1)
    fun updateStatus(planId: Int, newStatus: Int) {
        val json = prefs.getString(KEY_STUDENT_PLAN, null) ?: return

        val studentPlan = Json.decodeFromString<StudentPlan>(json)
        val updatedPlanList = studentPlan.planList.map { plan ->
            if (plan.id == planId) plan.copy(status = newStatus) else plan
        }

        val updatedStudentPlan = studentPlan.copy(planList = updatedPlanList)
        val updatedJson = Json.encodeToString(updatedStudentPlan)

        prefs.edit().putString(KEY_STUDENT_PLAN, updatedJson).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    //현재 진행중인 계획 가져오기
    fun getNowPlan(): Plan? {
        val json = prefs.getString(KEY_STUDENT_PLAN, null) ?: return null
        val studentPlan = StudentPlan.fromJson(json)
        val planList = studentPlan.planList

        val now = LocalDateTime.now() // 현재 시간

        return planList.filterNotNull()
            .filter { plan ->
                try {
                    // startTime과 endTime을 LocalDateTime으로 변환
                    val startDateTime = ZonedDateTime.parse(plan.startTime, DateTimeFormatter.ISO_DATE_TIME)
                        .withZoneSameInstant(ZoneId.systemDefault())
                        .toLocalDateTime()
                    val endDateTime = ZonedDateTime.parse(plan.endTime, DateTimeFormatter.ISO_DATE_TIME)
                        .withZoneSameInstant(ZoneId.systemDefault())
                        .toLocalDateTime()

                    // 현재 시간이 시작시간과 종료시간 사이에 있는지 확인
                    val isInTimeRange = now.isAfter(startDateTime) && now.isBefore(endDateTime)

                    // 마지막 로그 이벤트가 "enter"로 끝나는지 확인
                    val lastLogEndsWithEnter = plan.logs.isNotEmpty() &&
                            plan.logs.last().event.endsWith("enter")

                    isInTimeRange && lastLogEndsWithEnter
                } catch (e: Exception) {
                    false
                }
            }
            .firstOrNull() // 조건을 만족하는 첫 번째 계획 반환
    }
}
