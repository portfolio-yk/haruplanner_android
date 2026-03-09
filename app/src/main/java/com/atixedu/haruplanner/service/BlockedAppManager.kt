import android.content.Context
import android.content.SharedPreferences

object BlockedAppsManager {

    private lateinit var prefs: SharedPreferences
    private const val PREF_NAME = "BlockedAppsPrefs"
    private const val KEY_BLOCKED_APPS = "blocked_apps"

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getBlockedApps(): Set<String> {
        checkInit()
        return prefs.getStringSet(KEY_BLOCKED_APPS, emptySet()) ?: emptySet()
    }

    fun addBlockedApp(packageName: String) {
        checkInit()
        val currentSet = getBlockedApps().toMutableSet()
        currentSet.add(packageName)
        prefs.edit().putStringSet(KEY_BLOCKED_APPS, currentSet).apply()
    }

    fun removeBlockedApp(packageName: String) {
        checkInit()
        val currentSet = getBlockedApps().toMutableSet()
        currentSet.remove(packageName)
        prefs.edit().putStringSet(KEY_BLOCKED_APPS, currentSet).apply()
    }

    fun clearBlockedApps() {
        checkInit()
        prefs.edit().remove(KEY_BLOCKED_APPS).apply()
    }

    fun isBlocked(packageName: String): Boolean {
        checkInit()
        return getBlockedApps().contains(packageName)
    }

    private fun checkInit() {
        if (!::prefs.isInitialized) {
            throw IllegalStateException("BlockedAppsManager is not initialized. Call init(context) first.")
        }
    }
}
