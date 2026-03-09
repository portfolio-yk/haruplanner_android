import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext


@Composable
fun UsagePermissionChecker(
    onPermissionGranted: () -> Unit,
    onPermissionRequest: (Intent) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!hasUsageStatsPermission(context)) {
            showDialog = true
        } else {
            onPermissionGranted()
        }
    }

    PermissionDialog(
        showDialog = showDialog,
        title = "사용 정보 접근 권한 설정",
        message = "앱 차단 기능을 위해 '사용 정보 접근' 권한이 필요합니다.\n\n설정 화면에서 [하루플래너]를 찾아 권한을 허용으로 설정해주세요.",
        onConfirm = {
            showDialog = false
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            onPermissionRequest(intent)
        },
        onDismiss = {
            onCancel()
            showDialog = false
            Log.d("UsagePermissionDialog", "사용자가 사용 정보 접근 권한 요청을 취소함")
        }
    )
}

@Composable
fun OverlayPermissionChecker(
    onPermissionGranted: () -> Unit,
    onPermissionRequest: (Intent) -> Unit,
    onCancel : () -> Unit,
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    //사용자 오버레이 권한
    LaunchedEffect(Unit) {
        if (!Settings.canDrawOverlays(context)) {
            showDialog = true
        } else {
            onPermissionGranted()
        }
    }

    PermissionDialog(
        showDialog = showDialog,
        title = "다른 앱 위에 표시 권한 설정",
        message = "정확한 시간 측정을 위해 '다른 앱 위에 표시' 권한이 필요합니다.\n\n화면 이동 후 [다른 앱 위에 표시] > [하루플래너] > [허용]으로 설정해주세요.",
        onConfirm = {
            showDialog = false
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            onPermissionRequest(intent)
        },
        onDismiss = {
            onCancel()
            showDialog = false
            Log.d("PermissionDialog", "사용자가 오버레이 권한 요청을 취소함")
        }
    )
}


@Composable
fun PermissionDialog(
    showDialog: Boolean,
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("취소")
                }
            }
        )
    }
}

// 사용 정보 접근 권한 체크 함수
private fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
    } else {
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}
// 알림 권한 체크 함수
private fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        // Android 13 미만에서는 알림 권한이 자동으로 허용됨
        true
    }
}