package tech.nagual.phoenix.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.webkit.WebView
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.jakewharton.threetenabp.AndroidThreeTen
import jcifs.context.SingletonContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.zhanghai.android.files.app.createNotificationChannels
import me.zhanghai.android.files.app.upgradeApp
import me.zhanghai.android.files.coil.initializeCoil
import me.zhanghai.android.files.features.ftpserver.FtpServerActivity
import me.zhanghai.android.files.hiddenapi.HiddenApi
import me.zhanghai.android.files.storage.FtpServerAuthenticator
import me.zhanghai.android.files.storage.SftpServerAuthenticator
import me.zhanghai.android.files.storage.SmbServerAuthenticator
import me.zhanghai.android.files.storage.StorageVolumeListLiveData
import me.zhanghai.android.files.tools.Tool
import me.zhanghai.android.files.tools.Tools
import me.zhanghai.android.files.util.valueCompat
import tech.nagual.app.*
import tech.nagual.common.R
import tech.nagual.phoenix.App
import tech.nagual.phoenix.BuildConfig
import tech.nagual.phoenix.tools.browser.BrowserActivity
import tech.nagual.phoenix.tools.browser.BrowserManager
import tech.nagual.phoenix.tools.calculator.activities.CalculatorActivity
import tech.nagual.phoenix.tools.camera.ui.activities.CameraActivity
import tech.nagual.phoenix.tools.gps.GpsActivity
import tech.nagual.phoenix.tools.gps.GpsManager
import tech.nagual.phoenix.tools.organizer.OrganizersManager
import tech.nagual.phoenix.tools.organizer.components.BinCleaningWorker
import tech.nagual.settings.Settings
import tech.nagual.theme.custom.CustomThemeHelper
import tech.nagual.theme.night.NightModeHelper
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import me.zhanghai.android.files.provider.ftp.client.Client as FtpClient
import me.zhanghai.android.files.provider.sftp.client.Client as SftpClient
import me.zhanghai.android.files.provider.smb.client.Client as SmbClient

val appInitializers = listOf(
    ::initializeCommon,
    ::initializeThreeTen,
    ::disableHiddenApiChecks,
    ::initializeWebViewDebugging,
    ::initializeCoil,
    ::initializeFileSystemProviders,
    ::upgradeApp,
    ::initializeLiveDataObjects,
    ::initializeCustomTheme,
    ::initializeNightMode,
    ::createNotificationChannels,
    ::createOrganizerNotificationChannels,
    ::enqueueWorkers,
    ::initGPS,
    ::initBrowser,
    ::initOrganizers,
    ::initTools
)

private fun initializeCommon() {
    appVersionName = BuildConfig.VERSION_NAME
    appVersionCode = BuildConfig.VERSION_CODE

    appClassLoader = App::class.java.classLoader!!

    internalDataPath = application.dataDir.absolutePath

    // create external data dir
    var path: String? = application.getExternalFilesDir(null)?.absolutePath
    path = if (path == null)
        "/storage/emulated/0/Android/data/${BuildConfig.APPLICATION_ID}"
    else
        Path(path).parent.absolutePathString()
    externalDataPath = path
}

private fun disableHiddenApiChecks() {
    HiddenApi.disableHiddenApiChecks()
}

private fun initializeThreeTen() {
    AndroidThreeTen.init(application)
}

private fun initializeWebViewDebugging() {
    if (BuildConfig.DEBUG) {
        WebView.setWebContentsDebuggingEnabled(true)
    }
}

private fun initializeFileSystemProviders() {
    me.zhanghai.android.files.provider.FileSystemProviders.install()
    me.zhanghai.android.files.provider.FileSystemProviders.overflowWatchEvents = true

    GlobalScope.launch(Dispatchers.Default) {
        // SingletonContext.init() calls NameServiceClientImpl.initCache() which connects to network.
        SingletonContext.init(
            Properties().apply {
                setProperty("jcifs.netbios.cachePolicy", "0")
                setProperty("jcifs.smb.client.maxVersion", "SMB1")
            }
        )
    }
    FtpClient.authenticator = FtpServerAuthenticator
    SftpClient.authenticator = SftpServerAuthenticator
    SmbClient.authenticator = SmbServerAuthenticator
}

private fun initializeLiveDataObjects() {
    // Force initialization of LiveData objects so that it won't happen on a background thread.
    StorageVolumeListLiveData.value
    Settings.FILE_LIST_DEFAULT_DIRECTORY.value
}

private fun initializeCustomTheme() {
    CustomThemeHelper.initialize(application)
}

private fun initializeNightMode() {
    NightModeHelper.initialize(application)
}

private fun enqueueWorkers() {
    val workManager = WorkManager.getInstance(application)

    val periodicRequests = listOf(
        "BIN_CLEAN" to PeriodicWorkRequestBuilder<BinCleaningWorker>(5, TimeUnit.HOURS)
            .build(),
    )

    periodicRequests.forEach { (name, request) ->
        workManager.enqueueUniquePeriodicWork(name, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}

private fun createOrganizerNotificationChannels() {
    val notificationManager =
        ContextCompat.getSystemService(application, NotificationManager::class.java) ?: return

    listOf(
        NotificationChannel(
            OrganizersManager.REMINDERS_CHANNEL_ID,
            application.getString(R.string.notifications_channel_reminders),
            NotificationManager.IMPORTANCE_HIGH
        ),
        NotificationChannel(
            OrganizersManager.BACKUPS_CHANNEL_ID,
            application.getString(R.string.notifications_channel_backups),
            NotificationManager.IMPORTANCE_DEFAULT
        ),
        NotificationChannel(
            OrganizersManager.PLAYBACK_CHANNEL_ID,
            application.getString(R.string.notifications_channel_playback),
            NotificationManager.IMPORTANCE_DEFAULT
        )
    ).forEach { notificationManager.createNotificationChannel(it) }
}

private fun initBrowser() {
    BrowserManager.getInstance().init()
}

private fun initGPS() {
    GpsManager.getInstance().init()
}

private fun initOrganizers() {
    OrganizersManager.getInstance().init()
}

private fun initTools() {
    Tools.infos = mapOf(
        Pair(
            application.getString(R.string.gps_title), Tools.ToolInfo(
                GpsActivity::class.java.name,
                R.drawable.gps_icon_white_24dp
            )
        ),
        Pair(
            application.getString(R.string.navigation_camera), Tools.ToolInfo(
                CameraActivity::class.java.name,
                R.drawable.camera_icon_white_24dp
            )
        ),
        Pair(
            application.getString(R.string.navigation_browser), Tools.ToolInfo(
                BrowserActivity::class.java.name,
                R.drawable.web_icon_white_24dp
            )
        ),
//        Pair(
//            application.getString(R.string.paint_title), Tools.ToolInfo(
//                PainterActivity::class.java.name,
//                R.drawable.paint_icon_white_24dp
//            )
//        ),
//        Pair(
//            application.getString(R.string.navigation_calc), Tools.ToolInfo(
//                CalculatorActivity::class.java.name,
//                R.drawable.calc_icon_white_24dp
//            )
//        ),
        Pair(
            application.getString(R.string.navigation_ftp_server), Tools.ToolInfo(
                FtpServerActivity::class.java.name,
                R.drawable.shared_directory_icon_white_24dp
            )
        )
    )

    Tools.list = listOf(
        Tool(
            application.getString(R.string.gps_title),
            true
        ),
        Tool(
            application.getString(R.string.navigation_camera),
            true
        ),
        Tool(
            application.getString(R.string.navigation_browser),
            true
        ),
//        Tool(
//            application.getString(R.string.paint_title),
//            true
//        ),
//        Tool(
//            application.getString(R.string.navigation_calc),
//            false
//        ),
        Tool(
            application.getString(R.string.navigation_ftp_server),
            true
        )
    )

    // Looking for new tools
    for (tool in Tools.list) {
        val savedList = Settings.TOOLS.valueCompat.toMutableList()
        if (savedList.find { it.origName == tool.origName } == null) {
            savedList.add(tool)
            Settings.TOOLS.putValue(savedList)
        }
    }

    // Looking for removed tools
    for (tool in Settings.TOOLS.valueCompat) {
        val savedList = Settings.TOOLS.valueCompat.toMutableList()
        if (Tools.list.find { it.origName == tool.origName } == null) {
            savedList.remove(tool)
            Settings.TOOLS.putValue(savedList)
        }
    }
}