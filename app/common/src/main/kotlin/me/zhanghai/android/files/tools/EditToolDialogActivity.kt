package me.zhanghai.android.files.tools

import android.os.Bundle
import android.view.View
import androidx.fragment.app.commit
import tech.nagual.app.BaseActivity
import me.zhanghai.android.files.util.args
import me.zhanghai.android.files.util.putArgs

class EditToolDialogActivity : BaseActivity() {
    private val args by args<EditToolDialogFragment.Args>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Calls ensureSubDecor().
        findViewById<View>(android.R.id.content)
        if (savedInstanceState == null) {
            val fragment = EditToolDialogFragment().putArgs(args)
            supportFragmentManager.commit {
                add(fragment, EditToolDialogFragment::class.java.name)
            }
        }
    }
}
