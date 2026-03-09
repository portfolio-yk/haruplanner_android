package com.atixedu.haruplanner.data

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Serializable
data class LogEntry(val event: String, val timestamp: String)

@Serializable
data class Plan(
    val id: Int,
    val startTime: String,
    val endTime: String,
    val title: String,
    val description: String,
    val logs: List<LogEntry>,
    val category: String,
    val warningCount: Int,
    val isWarnedAdmin: Boolean,
    val status: Int
) {
    companion object {
        fun fromJson(json : String):Plan {
            return Json.decodeFromString<Plan>(json)
        }
    }

    fun toScheduleItem2(): ScheduleItem {
        // UTC 시간을 로컬 시간으로 변환하는 헬퍼 함수
        fun parseToLocalDateTime(timeString: String): LocalDateTime {
            val utcInstant = Instant.parse(timeString)
            return LocalDateTime.ofInstant(utcInstant, ZoneId.systemDefault())
        }

        val start = parseToLocalDateTime(startTime)
        val end = parseToLocalDateTime(endTime)
        val totalDuration = Duration.between(start, end).seconds.coerceAtLeast(1)
        val now = LocalDateTime.now()

        // 누적 체류 시간 계산
        var completeDuration: Long = 0
        val enterStack = ArrayDeque<LocalDateTime>()

        for (log in logs) {
            val time = parseToLocalDateTime(log.timestamp)
            when (log.event) {
                "enter" -> enterStack.add(time)
                "exit" -> {
                    val enterTime = enterStack.removeLastOrNull()
                    if (enterTime != null) {
                        completeDuration += Duration.between(enterTime, time).seconds
                    }
                }
            }
        }

        // 아직 종료되지 않은 체류 시간 처리 (마지막 enter 이후 아직 exit 안 한 경우)
        if (enterStack.isNotEmpty()) {
            val lastEnterTime = enterStack.removeLast()
            completeDuration += Duration.between(lastEnterTime, now).seconds
        }

        // 종료되지 않은 enter → 현재 시간까지 누적
        for (unclosedEnter in enterStack) {
            completeDuration += Duration.between(unclosedEnter, now).seconds
        }

        // 진행률과 색상
        val progress = (completeDuration.toDouble() / totalDuration).coerceIn(0.0, 1.0)
        val color = when {
            progress >= 0.75 -> Color(0xFF52B652)
            progress >= 0.4 -> Color(0xFFFFAA00)
            progress > 0.0 -> Color(0xFFE57373)
            else -> Color.Gray
        }

        // 포맷된 시간 문자열
        var formattedTime = "00:00:00"

        if (completeDuration > 0) {
            val hours = completeDuration / 3600
            val minutes = (completeDuration % 3600) / 60
            val seconds = completeDuration % 60
            formattedTime = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }

        Log.d("Lock", now.isBefore(end).toString() + now.toString() + start.toString() + end.toString())

        // 현재 시간이 시작 시간과 종료시간 사이여야하고, status 0(미종료) 이여야함
        val isGoing = now.isAfter(start) && now.isBefore(end) && status == 0
        val lastEvent = logs.lastOrNull()?.event
        val isEnter = lastEvent == "enter"

        val timeStr = start.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
        val endTimeStr = end.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))

        return ScheduleItem(
            id = id,
            time = timeStr,
            endTime = endTimeStr,
            title = title,
            completeTime = formattedTime,
            color = color,
            isGoing = mutableStateOf(isGoing),
            duration = completeDuration,
            isEnter = isEnter,
            status = mutableIntStateOf(status),
        )
    }


}

@Serializable
data class StudentPlan(
    val studentId: Int,
    val planList: List<Plan>,
) {
    companion object {
        fun fromJson(json : String): StudentPlan {
            return Json.decodeFromString<StudentPlan>(json)
        }
    }

    fun toJson(): String {
        return Json.encodeToString(this)
    }
}



data class ScheduleItem(
    val id : Int,
    val time: String,
    val endTime: String,
    val title: String,
    val completeTime: String = "00:00:00",
    val color: Color = Color.Gray,
    val isGoing: MutableState<Boolean> = mutableStateOf(false),
    val duration: Long,
    val isEnter: Boolean,
    val status : MutableState<Int> = mutableIntStateOf(0),
)
