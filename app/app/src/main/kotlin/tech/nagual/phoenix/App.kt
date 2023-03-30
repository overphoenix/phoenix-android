package tech.nagual.phoenix

import android.app.Application
import android.os.StrictMode
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import tech.nagual.phoenix.app.appInitializers
import tech.nagual.app.application
import tech.nagual.app.navigationItemListLiveData
import tech.nagual.phoenix.navigation.NavigationItemListLiveData
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun getWorkManagerConfiguration() =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    
    override fun onCreate() {
//        if (BuildConfig.DEBUG) {
//            enableStrictMode()
//        }
        super.onCreate()

        application = this

        appInitializers.forEach { it() }

        navigationItemListLiveData = NavigationItemListLiveData()
    }

    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectNonSdkApiUsage()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build()
        )
    }

    companion object {
        val TAG = App::class.java.simpleName
    }
}
