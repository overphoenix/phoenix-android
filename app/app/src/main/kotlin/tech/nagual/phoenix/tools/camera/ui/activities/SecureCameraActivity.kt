package tech.nagual.phoenix.tools.camera.ui.activities

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import me.zhanghai.android.files.util.createIntent
import me.zhanghai.android.files.util.startActivitySafe

class SecureCameraActivity : CameraActivity() {

    var openedActivityAt = DEFAULT_OPENED_AT_TIMESTAMP

    val capturedFilePaths = arrayListOf<String>()

    private lateinit var fileSP: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openedActivityAt = System.currentTimeMillis()
        fileSP = getSharedPreferences(getSPName(), Context.MODE_PRIVATE)
    }

    private fun getSPName(): String {
        return "files-$openedActivityAt"
    }

    override fun openGallery() {
    }

    override fun onDestroy() {
        super.onDestroy()
        fileSP.edit().clear().apply()
    }

    companion object {
        const val DEFAULT_OPENED_AT_TIMESTAMP = 0L
    }
}