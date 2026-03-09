import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import com.atixedu.haruplanner.util.BlockAppInfo



// 필요한 import들
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.atixedu.haruplanner.drawableToPainter

@Composable
fun BlockAppDialog(
    apps: MutableList<BlockAppInfo>,
    onDismiss: () -> Unit,
    block: (packageName: String) -> Unit,
    unblock : (packageName: String) -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "차단 앱 설정",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                Text(
                    text = "차단할 앱을 선택하세요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(apps.size) { index ->
                        AppListItem(
                            app = apps[index],
                            onToggleBlock = { isBlocked ->
                                apps[index] = apps[index].copy(isBlocked = isBlocked)
                                if (isBlocked) block(apps[index].packageName)
                                else unblock(apps[index].packageName)

                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
//            TextButton(
//                onClick = onSave
//            ) {
//                Text(
//                    text = "저장",
//                    color = MaterialTheme.colorScheme.primary,
//                    fontWeight = FontWeight.Medium
//                )
//            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "취소",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        modifier = Modifier.fillMaxWidth(0.95f)
    )
}

@Composable
fun AppListItem(
    app: BlockAppInfo,
    onToggleBlock: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleBlock(!app.isBlocked) },
        colors = CardDefaults.cardColors(
            containerColor = if (app.isBlocked)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 앱 아이콘
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = drawableToPainter(app.icon),
                    contentDescription = app.packageName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 앱 정보
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 차단 상태 표시
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (app.isBlocked) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "차단",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Switch(
                    checked = app.isBlocked,
                    onCheckedChange = onToggleBlock,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.error,
                        checkedTrackColor = MaterialTheme.colorScheme.errorContainer
                    )
                )
            }
        }
    }
}

@Composable
fun drawableToPainter(drawable: Drawable): Painter {
    val context = LocalContext.current
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
