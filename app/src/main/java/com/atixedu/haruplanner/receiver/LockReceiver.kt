package com.atixedu.haruplanner.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.atixedu.haruplanner.LockActivity

/**
 * 화면 잠금 이벤트를 처리하는 BroadcastReceiver
 */
object LockReceiver : BroadcastReceiver() {

    /**
     * 수신된 인텐트에 따라 적절한 작업을 수행
     *
     * @param context: 애플리케이션 컨텍스트
     * @param intent: 수신된 인텐트
     */
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_ON -> {
                navigateToLock(context) // 화면이 켜졌을 때 LockActivity로 이동
            }
        }
    }

    /**
     * LockActivity로 이동하는 메서드
     *
     * @param context: 애플리케이션 컨텍스트
     */
    private fun navigateToLock(context: Context) {
        context.startActivity(
            Intent(context, LockActivity::class.java).apply {
//                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // 새로운 태스크에서 Activity 시작
//                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)

                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) // 기존 태스크 제거
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            }
        )
    }
}