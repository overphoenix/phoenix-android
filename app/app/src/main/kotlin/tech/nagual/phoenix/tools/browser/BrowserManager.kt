package tech.nagual.phoenix.tools.browser

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import com.google.android.material.color.DynamicColors
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import net.luminis.quic.QuicConnection
import tech.nagual.app.application
import tech.nagual.common.R
import tech.nagual.phoenix.tools.browser.core.Content
import tech.nagual.phoenix.tools.browser.core.DOCS
import tech.nagual.phoenix.tools.browser.core.pages.PAGES
import tech.nagual.phoenix.tools.browser.utils.AdBlocker
import threads.lite.IPFS
import threads.lite.LogUtils
import threads.lite.cid.PeerId
import java.util.*

class BrowserManager {
    private val applicationContext = application.applicationContext
    private val gson = Gson()
    private fun createStorageChannel(context: Context) {
        try {
            val name: CharSequence = context.getString(R.string.browser_storage_channel_name)
            val description = context.getString(R.string.browser_storage_channel_description)
            val mChannel = NotificationChannel(
                STORAGE_CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW
            )
            mChannel.description = description
            val notificationManager = context.getSystemService(
                Application.NOTIFICATION_SERVICE
            ) as NotificationManager
            notificationManager.createNotificationChannelGroup(
                NotificationChannelGroup(STORAGE_GROUP_ID, name)
            )
            notificationManager.createNotificationChannel(mChannel)
        } catch (throwable: Throwable) {
            LogUtils.error(TAG, throwable)
        }
    }

    fun onMessageReceived(conn: QuicConnection, content: String) {
        try {
            val hashMap = object : TypeToken<HashMap<String?, String?>?>() {}.type
            Objects.requireNonNull(conn)
            val ipfs = IPFS.getInstance(applicationContext)
            Objects.requireNonNull(content)
            val data = gson.fromJson<Map<String, String>>(content, hashMap)
            tech.nagual.phoenix.tools.browser.LogUtils.debug(TAG, "Push Message : $data")
            val ipns = data[Content.IPNS]
            Objects.requireNonNull(ipns)
            val pid = data[Content.PID]
            Objects.requireNonNull(pid)
            val seq = data[Content.SEQ]
            Objects.requireNonNull(seq)
            val peerId = PeerId.fromBase58(pid)
            val sequence = seq!!.toLong()
            if (sequence >= 0) {
                if (ipfs.isValidCID(ipns!!)) {
                    val pages = PAGES.getInstance(applicationContext)
                    val page = pages.createPage(peerId.toBase58())
                    page.setContent(ipns)
                    page.sequence = sequence
                    pages.storePage(page)
                }
            }
            DOCS.getInstance(applicationContext).addResolves(peerId, ipns)
        } catch (throwable: Throwable) {
            tech.nagual.phoenix.tools.browser.LogUtils.error(TAG, throwable)
        }
    }

    companion object {
        const val TIME_TAG = "BROWSER_TIME_TAG"
        const val STORAGE_CHANNEL_ID = "BROWSER_STORAGE_CHANNEL_ID"
        const val STORAGE_GROUP_ID = "BROWSER_STORAGE_GROUP_ID"
        private val TAG = BrowserManager::class.java.simpleName

        private lateinit var INSTANCE: BrowserManager
        fun getInstance(): BrowserManager {
            if (!this::INSTANCE.isInitialized) {
                synchronized(BrowserManager::class.java) {
                    if (!this::INSTANCE.isInitialized) {
                        INSTANCE = BrowserManager()
                    }
                }
            }
            return INSTANCE
        }
    }

    fun init() {
        DynamicColors.applyToActivitiesIfAvailable(application)
        val start = System.currentTimeMillis()
        createStorageChannel(applicationContext)
        AdBlocker.init(applicationContext)
        tech.nagual.phoenix.tools.browser.LogUtils.info(
            TIME_TAG, "InitApplication after add blocker [" +
                    (System.currentTimeMillis() - start) + "]..."
        )
        try {
            val ipfs = IPFS.getInstance(applicationContext)
            ipfs.setPusher(::onMessageReceived)
        } catch (throwable: Throwable) {
            tech.nagual.phoenix.tools.browser.LogUtils.error(TAG, throwable)
        }
        tech.nagual.phoenix.tools.browser.LogUtils.info(
            TIME_TAG, "InitApplication after starting ipfs [" +
                    (System.currentTimeMillis() - start) + "]..."
        )
    }
}