package me.zhanghai.android.files.features.ftpserver

import android.text.InputType
import androidx.fragment.app.Fragment
import tech.nagual.common.preferences.PreferenceFragment
import tech.nagual.common.preferences.PreferenceScreen
import tech.nagual.common.preferences.helpers.categoryHeader
import tech.nagual.common.preferences.helpers.editText
import tech.nagual.common.preferences.helpers.screen
import tech.nagual.common.preferences.helpers.switch
import tech.nagual.common.preferences.preferences.EditTextPreference
import me.zhanghai.android.files.util.getBoolean
import me.zhanghai.android.files.util.getInteger
import tech.nagual.common.R
import tech.nagual.common.extensions.toInt

inline fun PreferenceScreen.Appendable.ftpServerState(
    fragment: Fragment,
    block: FtpServerStatePreference.() -> Unit
): FtpServerStatePreference {
    return FtpServerStatePreference(fragment).apply(block).also(::addPreferenceItem)
}

inline fun PreferenceScreen.Appendable.ftpServerUrl(
    fragment: Fragment,
    block: FtpServerUrlPreference.() -> Unit
): FtpServerUrlPreference {
    return FtpServerUrlPreference(fragment).apply(block).also(::addPreferenceItem)
}

inline fun PreferenceScreen.Appendable.ftpHomeDirectory(
    fragment: Fragment,
    block: FtpServerHomeDirectoryPreference.() -> Unit
): FtpServerHomeDirectoryPreference {
    return FtpServerHomeDirectoryPreference(fragment).apply(block).also(::addPreferenceItem)
}

class FtpServerFragment : PreferenceFragment() {
    override fun createRootScreen() = screen(context) {
        collapseIcon = true
        ftpServerState(this@FtpServerFragment) {
            titleRes = R.string.ftp_server_state_title
        }
        ftpServerUrl(this@FtpServerFragment) {
            titleRes = R.string.ftp_server_url_title
        }
        categoryHeader("ftp_header_config") {
            titleRes = R.string.ftp_server_configuration_title
        }
        switch(getString(R.string.pref_key_ftp_server_anonymous_login)) {
            titleRes = R.string.ftp_server_anonymous_login_title
            defaultValue = getBoolean(R.bool.pref_default_value_ftp_server_anonymous_login)
            dependency = getString(R.string.pref_key_ftp_server_state)
            dependencyInverse = true
        }
        editText(getString(R.string.pref_key_ftp_server_username)) {
            titleRes = R.string.ftp_server_username_title
            summaryProvider = { it }
            textChangeListener = EditTextPreference.OnTextChangeListener { pref, input ->
                if (input.isEmpty()) {
                    currentInput = getString(R.string.pref_default_value_ftp_server_username)
                    commitString(currentInput.toString())
                    return@OnTextChangeListener false
                }
                true
            }
            dependency = getString(R.string.pref_key_ftp_server_state)
            dependencyInverse = true
        }
        editText(getString(R.string.pref_key_ftp_server_password)) {
            titleRes = R.string.ftp_server_password_title
            textInputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            summaryProvider =
                { if (it.isNullOrEmpty()) getString(R.string.not_specified) else "•".repeat(it.length) }
            dependency = getString(R.string.pref_key_ftp_server_state)
            dependencyInverse = true
        }
        editText(getString(R.string.pref_key_ftp_server_port)) {
            titleRes = R.string.ftp_server_port_title
            textInputType = InputType.TYPE_CLASS_NUMBER
            summaryProvider = { it }
            valueReader = {
                currentInput =
                    getInt(this@FtpServerFragment.getInteger(R.integer.pref_default_value_ftp_server_port)).toString()
            }
            textChangeListener = EditTextPreference.OnTextChangeListener { pref, input ->
                var port = if (input.isEmpty()) {
                    this@FtpServerFragment.getInteger(R.integer.pref_default_value_ftp_server_port)
                } else {
                    input.toInt()
                }
                commitInt(port)
                currentInput = port.toString()
                false
            }
            dependency = getString(R.string.pref_key_ftp_server_state)
            dependencyInverse = true
        }
        ftpHomeDirectory(
            this@FtpServerFragment
        ) {
            titleRes = R.string.ftp_server_home_directory_title
            dependency = getString(R.string.pref_key_ftp_server_state)
            dependencyInverse = true
        }
        switch(getString(R.string.pref_key_ftp_server_writable)) {
            titleRes = R.string.ftp_server_writable_title
            defaultValue = getBoolean(R.bool.pref_default_value_ftp_server_writable)
            dependency = getString(R.string.pref_key_ftp_server_state)
            dependencyInverse = true
        }
    }
}
