package me.zhanghai.android.files.features.ftpserver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.view.ContextMenu
import android.view.Menu
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import tech.nagual.common.preferences.Preference
import tech.nagual.common.preferences.PreferencesAdapter
import me.zhanghai.android.files.util.valueCompat
import tech.nagual.common.R
import tech.nagual.app.clipboardManager
import tech.nagual.settings.Settings
import me.zhanghai.android.files.util.copyText
import me.zhanghai.android.files.util.getLocalAddress
import java.net.InetAddress

class FtpServerUrlPreference(private val fragment: Fragment) : Preference("pref_ftp_server_url") {
    private val observer = Observer<Any> { updateSummary() }
    private val connectivityReceiver = ConnectivityReceiver()

    private val contextMenuListener = ContextMenuListener()
    private var hasUrl = false

    init {
        persistent = false
        updateSummary()
    }

    override fun onAttach() {
        super.onAttach()
        Settings.FTP_SERVER_ANONYMOUS_LOGIN.observeForever(observer)
        Settings.FTP_SERVER_USERNAME.observeForever(observer)
        Settings.FTP_SERVER_PORT.observeForever(observer)
        fragment.requireContext().registerReceiver(
            connectivityReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )
    }

    override fun onDetach() {
        super.onDetach()
        Settings.FTP_SERVER_ANONYMOUS_LOGIN.removeObserver(observer)
        Settings.FTP_SERVER_USERNAME.removeObserver(observer)
        Settings.FTP_SERVER_PORT.removeObserver(observer)
        fragment.requireContext().unregisterReceiver(connectivityReceiver)
    }

    private fun updateSummary() {
        val localAddress = InetAddress::class.getLocalAddress()
        val summary: String
        if (localAddress != null) {
            val username = if (!Settings.FTP_SERVER_ANONYMOUS_LOGIN.valueCompat) {
                Settings.FTP_SERVER_USERNAME.valueCompat
            } else {
                null
            }
            val host = localAddress.hostAddress
            val port = Settings.FTP_SERVER_PORT.valueCompat
            summary = "ftp://${if (username != null) "$username@" else ""}$host:$port/"
            hasUrl = true
        } else {
            summary = fragment.getString(R.string.ftp_server_url_summary_no_local_inet_address)
            hasUrl = false
        }
        this.summary = summary
        requestRebind()
    }

    override fun bindViews(holder: PreferencesAdapter.ViewHolder) {
        super.bindViews(holder)
        holder.itemView.setOnCreateContextMenuListener(contextMenuListener)
    }

    private inner class ConnectivityReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (val action = intent.action) {
                ConnectivityManager.CONNECTIVITY_ACTION -> updateSummary()
                else -> throw IllegalArgumentException(action)
            }
        }
    }

    private inner class ContextMenuListener : View.OnCreateContextMenuListener {
        override fun onCreateContextMenu(
            menu: ContextMenu, view: View, menuInfo: ContextMenu.ContextMenuInfo?
        ) {
            if (!hasUrl) {
                return
            }
            val url = summary!!
            menu
                .setHeaderTitle(url)
                .apply {
                    add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.ftp_server_url_menu_copy_url)
                        .setOnMenuItemClickListener {
                            clipboardManager.copyText(url, fragment.requireContext())
                            true
                        }
                }
                .apply {
                    if (!Settings.FTP_SERVER_ANONYMOUS_LOGIN.valueCompat) {
                        val password = Settings.FTP_SERVER_PASSWORD.valueCompat
                        if (password.isNotEmpty()) {
                            add(
                                Menu.NONE, Menu.NONE, Menu.NONE,
                                R.string.ftp_server_url_menu_copy_password
                            )
                                .setOnMenuItemClickListener {
                                    clipboardManager.copyText(
                                        password,
                                        fragment.requireContext()
                                    )
                                    true
                                }
                        }
                    }
                }
        }
    }
}