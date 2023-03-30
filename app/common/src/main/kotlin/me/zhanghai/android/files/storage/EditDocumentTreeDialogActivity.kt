package me.zhanghai.android.files.storage

import android.os.Bundle
import android.view.View
import androidx.fragment.app.commit
import tech.nagual.app.BaseActivity
import me.zhanghai.android.files.util.args
import me.zhanghai.android.files.util.putArgs

class EditDocumentTreeDialogActivity : BaseActivity() {
    private val args by args<EditDocumentTreeDialogFragment.Args>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Calls ensureSubDecor().
        findViewById<View>(android.R.id.content)
        if (savedInstanceState == null) {
            val fragment = EditDocumentTreeDialogFragment().putArgs(args)
            supportFragmentManager.commit {
                add(fragment, EditDocumentTreeDialogFragment::class.java.name)
            }
        }
    }
}
