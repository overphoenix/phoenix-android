package tech.nagual.app

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Looper
import androidx.datastore.preferences.preferencesDataStore
import androidx.preference.PreferenceManager
import me.zhanghai.android.files.navigation.NavigationItemListLiveData
import kotlin.properties.Delegates

lateinit var application: Application
lateinit var appClassLoader: ClassLoader

lateinit var internalDataPath: String
lateinit var externalDataPath: String

lateinit var appVersionName: String
var appVersionCode by Delegates.notNull<Int>()

lateinit var navigationItemListLiveData: NavigationItemListLiveData

val defaultSharedPreferences: SharedPreferences by lazy {
    PreferenceManager.getDefaultSharedPreferences(application)
}

val Context.appDataStore by preferencesDataStore("app_prefs")

fun isOnMainThread() = Looper.myLooper() == Looper.getMainLooper()

fun ensureBackgroundThread(callback: () -> Unit) {
    if (isOnMainThread()) {
        Thread {
            callback()
        }.start()
    } else {
        callback()
    }
}

fun isRPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R