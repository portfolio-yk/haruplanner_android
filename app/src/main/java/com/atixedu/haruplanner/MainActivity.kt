package com.atixedu.haruplanner

import BlockedAppsManager
import OverlayPermissionChecker
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import android.provider.Settings
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.atixedu.haruplanner.service.LockServiceManager
import com.atixedu.haruplanner.service.ScheduleManager
import com.atixedu.haruplanner.service.WebviewInterface
import com.atixedu.haruplanner.ui.theme.ComposeLockScreenTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val TAG = this.javaClass.simpleName // 클래스 이름을 태그로 사용

    @Inject
    lateinit var lockServiceManager: LockServiceManager // LockServiceManager 주입

    private var webView: WebView? = null // 1. Add a nullable WebView reference

    private fun startLockService() {
        lockServiceManager.start() // 잠금 서비스 시작
    }

    // 오버레이 권한 요청용 런처
    private val overlayPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (isOverlayPermissionGranted(this)) {
            Log.e(TAG, "Overlay permission denied") // 권한 거부 시 로그 출력
        } else {
            Log.d(TAG, "Overlay permission granted") // 권한 허용 시 로그 출력
            startLockService() // 잠금 서비스 시작
        }
    }


    // 오버레이 권한이 필요한지 확인
    private fun isOverlayPermissionGranted(context: Context): Boolean {
        return !Settings.canDrawOverlays(context) // 권한 필요 여부 반환
    }



    private fun checkPermissions() {
        // 오버레이 권한 확인 및 요청
        if (isOverlayPermissionGranted(this)) {
            Log.e(TAG, "checkPermissions: Overlay permission needed")
            val overlayIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
        } else {
            startLockService() // 권한이 있으면 잠금 서비스 시작
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //알림으로 메인 진입 시 알림 제거 처리
//        val planId = intent.getIntExtra("planId", -1)
//        if (planId != -1) {
//            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            notificationManager.cancel(planId)
//        }

        //checkPermissions() // 권한 확인 및 요청 시작

        ScheduleManager.init(applicationContext)
        BlockedAppsManager.init(applicationContext)
        //BlockedAppsManager.clearBlockedApps()


        //ScheduleManager.clearAll()
        //ScheduleManager.setPlan(Plan.fromJson(Dummy.getScheduleData()))
        //ScheduleManager.setPlan(Plan.fromJson(Dummy.getScheduleData3()))




        //TEST
//        this.startActivity(
//            Intent(this, LockActivity::class.java).apply {
//                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // 새로운 태스크에서 Activity 시작
//                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP) // 이미 존재하는 태스크의 최상단에서 시작
//                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS) // 최근 앱 목록에서 제외
//            }
//        )

        setContent {
            ComposeLockScreenTheme {
                val REQUEST_CODE_NOTIFICATION_PERMISSION = 1
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            this@MainActivity,
                            arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                            REQUEST_CODE_NOTIFICATION_PERMISSION
                        )
                    }
                }
                OverlayPermissionChecker(
                    onPermissionGranted = { startLockService() },
                    onPermissionRequest = { intent -> overlayPermissionLauncher.launch(intent) },
                    onCancel = { finish() }
                )
                WebViewScreen(this) { webViewInstance ->
                    webView = webViewInstance
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 3. Call the JavaScript function when the activity resumes
        webView?.evaluateJavascript("onAndroidResume()", null)
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ComposeLockScreenTheme {
        Greeting("Android")
    }
}


@Composable
fun WebViewScreen(context: Context, onWebViewReady: (WebView) -> Unit) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {

                // 1. 먼저 자바스크립트 인터페이스 추가
                addJavascriptInterface(WebviewInterface(context), "AndroidBridge")

                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                settings.javaScriptEnabled = true

                loadUrl("file:///android_asset/webview_page/main.html")

                onWebViewReady(this)
            }
        }
    )
}