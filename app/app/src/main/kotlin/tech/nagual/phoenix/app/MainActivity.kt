package tech.nagual.phoenix.app

import android.os.Bundle
import tech.nagual.settings.Settings
import tech.nagual.protection.SecurityDialog
import me.zhanghai.android.files.util.valueCompat
import me.zhanghai.android.files.filelist.FileListActivity
import tech.nagual.common.activities.BaseInitActivity
import me.zhanghai.android.files.util.createIntent
import tech.nagual.protection.WAS_PROTECTION_HANDLED

class MainActivity : tech.nagual.common.activities.BaseInitActivity() {
    private var mIsPasswordProtectionPending = false
    private var mWasProtectionHandled = false

    override fun initActivity() {
        mIsPasswordProtectionPending = Settings.APP_PASSWORD_PROTECTION.valueCompat
        //launchActivity()
    }

    override fun onResume() {
        super.onResume()

        if (mIsPasswordProtectionPending && !mWasProtectionHandled) {
            handlePasswordProtection {
                mWasProtectionHandled = it
                if (it) {
                    mIsPasswordProtectionPending = false
                    launchActivity()
                } else {
                    finish()
                }
            }
        } else {
            launchActivity()
        }
    }

    private fun handlePasswordProtection(callback: (success: Boolean) -> Unit) {
        if (Settings.APP_PASSWORD_PROTECTION.valueCompat) {
            SecurityDialog(
                this,
                Settings.APP_PASSWORD_HASH.valueCompat,
                Settings.APP_PROTECTION_TYPE.valueCompat
            ) { _, _, success ->
                callback(success)
            }
        } else {
            callback(true)
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

    private fun launchActivity() {
        startActivity(FileListActivity::class.createIntent())
        finish()
    }
}
