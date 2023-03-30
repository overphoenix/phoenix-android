package tech.nagual.phoenix.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.annotation.StyleRes
import androidx.fragment.app.add
import androidx.fragment.app.commit
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import me.zhanghai.android.files.util.startActivitySafe
import tech.nagual.app.BaseActivity
import tech.nagual.theme.custom.CustomThemeHelper.OnThemeChangedListener
import tech.nagual.theme.night.NightModeHelper.OnNightModeChangedListener
import tech.nagual.protection.SecurityDialog
import me.zhanghai.android.files.util.*
import me.zhanghai.android.files.util.createIntent
import tech.nagual.protection.WAS_PROTECTION_HANDLED
import tech.nagual.settings.Settings

class SettingsActivity : BaseActivity(), OnThemeChangedListener, OnNightModeChangedListener {
    private var isRestarting = false

    private var mIsPasswordProtectionPending = false
    private var mWasProtectionHandled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val args = intent.extras?.getArgsOrNull<Args>()
        val savedInstanceState = savedInstanceState ?: args?.savedInstanceState
        super.onCreate(savedInstanceState)

        // Calls ensureSubDecor().
        findViewById<View>(android.R.id.content)
        if (savedInstanceState == null) {
            supportFragmentManager.commit { add<SettingsFragment>(android.R.id.content) }
        }

        mIsPasswordProtectionPending = Settings.SETTINGS_PASSWORD_PROTECTION.valueCompat && !Settings.SETTINGS_PASSWORD_PROTECTION_RESTARTING.valueCompat
    }

    override fun onResume() {
        super.onResume()

        Settings.SETTINGS_PASSWORD_PROTECTION_RESTARTING.putValue(false)

        if (mIsPasswordProtectionPending && !mWasProtectionHandled) {
            handlePasswordProtection {
                mWasProtectionHandled = it
                if (it) {
                    mIsPasswordProtectionPending = false
                } else {
                    finish()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(WAS_PROTECTION_HANDLED, mWasProtectionHandled)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mWasProtectionHandled = savedInstanceState.getBoolean(WAS_PROTECTION_HANDLED, false)
    }

    override fun onThemeChanged(@StyleRes theme: Int) {
        // ActivityCompat.recreate() may call ActivityRecreator.recreate() without calling
        // Activity.recreate(), so we cannot simply override it. To work around this, we just
        // manually call restart().
        restart()
    }

    override fun onNightModeChangedFromHelper(nightMode: Int) {
        // ActivityCompat.recreate() may call ActivityRecreator.recreate() without calling
        // Activity.recreate(), so we cannot simply override it. To work around this, we just
        // manually call restart().
        restart()
    }

    private fun restart() {
        val savedInstanceState = Bundle().apply {
            onSaveInstanceState(this)
        }
        finish()
        val intent = SettingsActivity::class.createIntent().putArgs(Args(savedInstanceState))
        startActivitySafe(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        isRestarting = true
        Settings.SETTINGS_PASSWORD_PROTECTION_RESTARTING.putValue(true)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return isRestarting || super.dispatchKeyEvent(event)
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyShortcutEvent(event: KeyEvent): Boolean {
        return isRestarting || super.dispatchKeyShortcutEvent(event)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        return isRestarting || super.dispatchTouchEvent(event)
    }

    override fun dispatchTrackballEvent(event: MotionEvent): Boolean {
        return isRestarting || super.dispatchTrackballEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        return isRestarting || super.dispatchGenericMotionEvent(event)
    }

    private fun handlePasswordProtection(callback: (success: Boolean) -> Unit) {
        if (Settings.SETTINGS_PASSWORD_PROTECTION.valueCompat) {
            SecurityDialog(
                this,
                Settings.SETTINGS_PASSWORD_HASH.valueCompat,
                Settings.SETTINGS_PROTECTION_TYPE.valueCompat
            ) { _, _, success ->
                callback(success)
            }
        } else {
            callback(true)
        }
    }

    @Parcelize
    class Args(val savedInstanceState: @WriteWith<BundleParceler> Bundle?) : ParcelableArgs
}
