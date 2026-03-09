package com.atixedu.haruplanner.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable
)


data class BlockAppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    val isBlocked: Boolean
)


object AppHelper {
    fun getUserInstalledApps(context: Context): List<AppInfo> {
        val packageManager = context.packageManager
        val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        return apps.map {
            AppInfo(
                name = it.loadLabel(packageManager).toString(),
                packageName = it.packageName,
                icon = it.loadIcon(packageManager)
            )
        }
    }

//    fun getBlocAppInfo() : MutableList<BlockAppInfo> {
//        val sampleApps = mutableListOf(
//            BlockAppInfo( "com.google.android.youtube", R.drawable.ic_launcher_foreground, false),
//            BlockAppInfo("com.instagram.android", R.drawable.ic_launcher_foreground, true),
//            BlockAppInfo( "com.ss.android.ugc.trill", R.drawable.ic_launcher_foreground, false),
//            BlockAppInfo( "com.facebook.katana", R.drawable.ic_launcher_foreground, true),
//            BlockAppInfo( "com.twitter.android", R.drawable.ic_launcher_foreground, false),
//            BlockAppInfo( "com.netflix.mediaclient", R.drawable.ic_launcher_foreground, false),
//            BlockAppInfo( "com.spotify.music", R.drawable.ic_launcher_foreground, false),
//            BlockAppInfo( "com.whatsapp", R.drawable.ic_launcher_foreground, false),
//            BlockAppInfo( "org.telegram.messenger", R.drawable.ic_launcher_foreground, false),
//            BlockAppInfo( "com.android.chrome", R.drawable.ic_launcher_foreground, false)
//        )
//
//        return sampleApps
//    }

    fun getBlockedAppInfo(context: Context): List<BlockAppInfo> {
        val packageManager = context.packageManager
        val allApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val launcherApps = packageManager.queryIntentActivities(launcherIntent, 0)
            .map { it.activityInfo.packageName }
            .toSet()

        val blockedApps = BlockedAppsManager.getBlockedApps()

        return allApps
            .map {
                BlockAppInfo(
                    name = it.loadLabel(packageManager).toString(),
                    packageName = it.packageName,
                    icon = it.loadIcon(packageManager),
                    isBlocked = blockedApps.contains(it.packageName)
                )
            }
    }

}
