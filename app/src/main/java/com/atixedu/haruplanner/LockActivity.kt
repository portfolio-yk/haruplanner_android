package com.atixedu.haruplanner

import BlockAppDialog
import BlockedAppsManager
import Clock
import UsagePermissionChecker
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atixedu.haruplanner.ui.theme.ComposeLockScreenTheme
import com.atixedu.haruplanner.util.AppHelper
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.core.graphics.drawable.toBitmap
import android.graphics.Canvas
import android.net.Uri
import android.provider.Settings
import android.provider.Settings.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.TextUnit
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startForegroundService
import com.atixedu.haruplanner.data.LogEntry
import com.atixedu.haruplanner.data.Plan
import com.atixedu.haruplanner.data.ScheduleItem
import com.atixedu.haruplanner.service.MyForegroundService
import com.atixedu.haruplanner.service.ScheduleManager
import com.atixedu.haruplanner.util.BlockAppInfo
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale


@AndroidEntryPoint
class LockActivity : ComponentActivity() {
    private val TAG = this.javaClass.simpleName // 클래스 이름을 태그로 사용

    fun getTodayPlan() : List<ScheduleItem> {
        val todayPlanList: List<Plan>? = ScheduleManager.getTodayPlan()

        val scheduleItems = todayPlanList?.map { plan ->
            plan.toScheduleItem2()
        } ?: emptyList<ScheduleItem>()

        return scheduleItems
    }


    @SuppressLint("UnrememberedMutableState")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
        }

        //내 설치된 앱 목록
        val appList = AppHelper.getBlockedAppInfo(applicationContext).toMutableList()

        val scheduleItems2: List<ScheduleItem> = getTodayPlan()
        //Log.d(TAG, scheduleItems.toString())

        setContent {

            var showMenu by remember { mutableStateOf(false) }

            var showBlockAppDialog by remember { mutableStateOf(false) }

            var showUsagePermissionChecker by remember { mutableStateOf(false) } // 추가

            var isButtonStart by remember { mutableStateOf(false) } // 추가

            // 현재 시간 계산 (분 단위만 사용)
            var currentTime by remember { mutableStateOf("00:00") }


            val scheduleItems = remember { mutableStateListOf<ScheduleItem>() }
            scheduleItems.addAll(scheduleItems2)

            LaunchedEffect(Unit) {
                while (true) {
                    val now = LocalTime.now().withSecond(0).withNano(0)

                    if (currentTime != now.format(DateTimeFormatter.ofPattern("HH:mm"))) {
                        currentTime = now.format(DateTimeFormatter.ofPattern("HH:mm"))
                    }

                    // scheduleItems 업데이트 (방법 1을 사용하는 경우)
                    for (i in scheduleItems.indices) {
                        val item = scheduleItems[i]
                        val start = LocalTime.parse(item.time, DateTimeFormatter.ofPattern("HH:mm"))
                        val end = LocalTime.parse(item.endTime, DateTimeFormatter.ofPattern("HH:mm"))
                        val isGoing = (now.isAfter(start) || now.equals(start)) &&
                                (now.isBefore(end) || now.equals(end)) &&
                                item.status.value == 0
                        Log.d("WHY", scheduleItems[i].toString() + ":::: " + isGoing.toString())
                        if (item.isGoing.value != isGoing) {
                            item.isGoing.value = isGoing
                        }
                    }


                    delay(1000)
                }
            }


            val mutableAppList = remember { mutableStateListOf<BlockAppInfo>() }
            mutableAppList.addAll(appList)
            val blockAppList by remember {
                derivedStateOf {
                    mutableAppList.filter { it.isBlocked }
                }
            }

            ComposeLockScreenTheme {
                Box(modifier = Modifier.fillMaxSize()) {

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFe8d5de))
                    )

                    // UsagePermissionChecker를 조건부로 표시
                    if (showUsagePermissionChecker) {
                        UsagePermissionChecker(
                            onPermissionGranted = {
                                val intent = Intent(this@LockActivity, MyForegroundService::class.java)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    startForegroundService(intent)
                                } else {
                                    startService(intent)
                                }
                                if(!isButtonStart) {
                                    showBlockAppDialog = true
                                }
                                showUsagePermissionChecker = false
                                isButtonStart = false
                            },
                            onPermissionRequest = { intent ->
                                // 여기서 intent를 사용해서 설정 화면으로 이동
                                startActivity(intent)
                                if(!isButtonStart) {
                                    showBlockAppDialog = true
                                }
                                showUsagePermissionChecker = false
                                isButtonStart = false
                            },
                            onCancel = {

                            }
                        )
                    }

                    // 상단 버튼들 Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // 좌측 상단 홈 버튼
                        IconButton(
                            onClick = {
                                // 모든 스택을 버리고 MainActivity로 이동
                                val intent = Intent(this@LockActivity, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                startActivity(intent)
                                finish()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "홈",
                                tint = Color.DarkGray
                            )
                        }

                        // 우측 상단 설정 버튼 & 메뉴
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "설정"
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                offset = DpOffset(0.dp, 0.dp) // 위치 조정
                            ) {
                                DropdownMenuItem(
                                    text = { Text("차단 앱 설정") },
                                    onClick = {
                                        showUsagePermissionChecker = true // 권한 체크 시작
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // 차단 앱 설정 다이얼로그
                    if (showBlockAppDialog) {
                        BlockAppDialog(
                            apps = mutableAppList,
                            onDismiss = { showBlockAppDialog = false },
                            block = { packageName -> BlockedAppsManager.addBlockedApp(packageName = packageName) },
                            unblock = { packageName -> BlockedAppsManager.removeBlockedApp(packageName = packageName) }
                        ) {
                            // TODO: 차단 설정 저장
                            showBlockAppDialog = false
                        }
                    }


                    Column(
                        modifier = Modifier
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(5.dp))

                        Clock(currentTime = currentTime)

                        if (scheduleItems.isEmpty()) {
                            Text(
                                text = "오늘 일정 없음",
                                fontSize=20.sp,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.DarkGray,
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            ScheduleList(scheduleItems,
                                //사용정보권한 있을 시, 포그라운드 서비스 실행
                                onStart = {
                                    showUsagePermissionChecker = true
                                    isButtonStart = true
                                },
                                onEnd = {
                                    val intent = Intent(this@LockActivity, MyForegroundService::class.java)
                                    stopService(intent)
                                },
                            )
                        }

                        BottomAppDock(appList = blockAppList)
                    }
                }
            }
        }
    }

    @Composable
    fun ScheduleList(scheduleItems: MutableList<ScheduleItem>, onStart: () -> Unit, onEnd: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            scheduleItems.forEachIndexed { index, item ->
                if (item.isGoing.value) {
                    CurrentScheduleItem(
                        item = item,
                        onGiveUp = {
                            // 포기 버튼 누르면 isGoing false로 바꾸고 다시 리스트 갱신
                            item.isGoing.value = false
                            item.status.value = 1 // 1 포기 상태
                            val intent = Intent(this@LockActivity, MyForegroundService::class.java)
                            stopService(intent)
                        },
                        onStartTimer = {
                            onStart()
                        },
                        onEndTimer = {
                            onEnd()
                        }
                    )
                } else {
                    ScheduleItem(item)
                }
            }
        }
    }
}

@Composable
fun ScheduleItem(item: ScheduleItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(35.dp))

        //상태
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(item.color, CircleShape)
        )

        Spacer(modifier = Modifier.width(10.dp))

        // 10:00 | 영어 수행 끝내기
        ScheduleItemText(
            timeText = item.time,
            title = item.title,
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.width(5.dp))

        // 00:56:21
        Column(
            verticalArrangement = Arrangement.Bottom,
            modifier = Modifier.height(24.dp) // 높이 주는 게 중요!
        ) {
            Text(
                text = item.completeTime,
                style = TextStyle(fontSize = 12.sp, color = Color.Gray)
            )
        }
    }
}


@Composable
fun ScheduleItemText(timeText:String, title: String, fontSize: TextUnit) {
    //시간
    Text(
        text = timeText,
        fontWeight = FontWeight.Normal,
        color = Color.DarkGray,
        fontSize = fontSize
    )
    Text(text = " | ", color = Color.DarkGray, fontSize = fontSize)
    //계획명
    Text(
        text = title,
        fontWeight = FontWeight.Normal,
        color = Color.DarkGray,
        fontSize = fontSize
    )
}



@Composable
fun CurrentScheduleItem(item: ScheduleItem, onGiveUp: () -> Unit, onStartTimer: () -> Unit, onEndTimer: () -> Unit) {
    var isStarted by remember { mutableStateOf(item.isEnter) }
    var isPaused by remember { mutableStateOf(false) }
    var duration by remember { mutableStateOf(item.duration) }


    // TODO 타이머 효과 (일시정지 아니고 시작한 경우만 증가)
//    LaunchedEffect(key1 = isStarted && !isPaused) {
//        while (isStarted && !isPaused) {
//            delay(1000)
//            duration += 1
//        }
//    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 35.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color.Black, CircleShape)
            )

            Spacer(modifier = Modifier.width(10.dp))

            ScheduleItemText(
                timeText = item.time,
                title = item.title,
                fontSize = 28.sp
            )
        }

        //TODO 나중에
//        Row(verticalAlignment = Alignment.CenterVertically) {
//            Spacer(modifier = Modifier.width(20.dp))
//            val hours = duration / 3600
//            val minutes = (duration % 3600) / 60
//            val secs = duration % 60
//            val goingTime = String.format("%d시간 %02d분 %02d초", hours, minutes, secs)
//            Text(
//                text = goingTime,
//                fontWeight = FontWeight.Bold,
//                color = Color.Black,
//                fontSize = 23.sp
//            )
//            Text(
//                text = "동안 하는중",
//                color = Color.Black,
//                fontSize = 23.sp
//            )
//        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Spacer(modifier = Modifier.width(10.dp))

            when {
                !isStarted -> {
                    Button(
                        onClick = {
                            onStartTimer()

                            isStarted = true
                            isPaused = false
                            ScheduleManager.setLog(item.id, LogEntry("enter", OffsetDateTime.now(
                                ZoneOffset.UTC).toString()))
                        },
                        modifier = Modifier.height(35.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("시작하기", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                }
                else -> {
                    Button(
                        onClick = {
                            onEndTimer()

                            isStarted = false
                            isPaused = true
                            ScheduleManager.setLog(item.id, LogEntry("exit", OffsetDateTime.now().toString()))
                        },
                        modifier = Modifier.height(35.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
                    ) {
                        Text("일시정지", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }


            Button(
                    onClick = {
                        ScheduleManager.updateStatus(item.id, 1) // 포기한 상태로 변경
                        ScheduleManager.setLog(item.id, LogEntry("exit", OffsetDateTime.now().toString()))
                        isStarted = false
                        onGiveUp() // 상태 변경 콜백
                    },
                    modifier = Modifier.height(35.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                ) {
                    Text("포기", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
                }

        }
    }
}


@Composable
fun BottomAppDock(
    appList: List<BlockAppInfo>,
    pageCount: Int = 2,
    currentPage: Int = 0,
    ) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp, max = 300.dp)
            .clip(
                RoundedCornerShape(
                    topStart = 24.dp,
                    topEnd = 24.dp,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp
                )
            ) // 둥근 네모 박스
            .background(Color.White)
            .padding(20.dp)
    ) {
        Column{
            Text(
                text = "차단된 앱 목록",
                fontSize = 19.sp,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 구분선
            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp),
                color = Color(0xFFE0E0E0),
                thickness = 1.dp
            )


            // 앱 아이콘 그리드 (예시)
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp) // 예시 높이
            ) {
                items(appList) { app ->
                    AppIcon(app)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 하단 점 세 개
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth(),
//                horizontalArrangement = Arrangement.Center
//            ) {
//                repeat(pageCount) { index ->
//                    Box(
//                        modifier = Modifier
//                            .size(8.dp)
//                            .clip(CircleShape)
//                            .background(
//                                if (index == currentPage) Color.Black else Color.LightGray
//                            )
//                            .padding(horizontal = 4.dp)
//                    )
//                    if (index != pageCount - 1) Spacer(modifier = Modifier.width(8.dp))
//                }
//            }
        }
    }
}

@Composable
fun AppIcon(appInfo : BlockAppInfo) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = drawableToPainter(appInfo.icon),
            contentDescription = appInfo.packageName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
fun drawableToPainter(drawable: Drawable): Painter {
    val bitmap = remember(drawable) {
        when (drawable) {
            is BitmapDrawable -> drawable.bitmap
            is AdaptiveIconDrawable -> {
                // AdaptiveIconDrawable은 직접 bitmap으로 못 쓰므로 조합해서 그리기
                val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 108
                val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 108
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            }
            else -> drawable.toBitmap() // fallback
        }
    }

    return remember(bitmap) { BitmapPainter(bitmap.asImageBitmap()) }
}