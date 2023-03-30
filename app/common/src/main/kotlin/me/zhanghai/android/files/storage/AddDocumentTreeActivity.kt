package me.zhanghai.android.files.storage

import android.os.Bundle
import android.view.View
import androidx.fragment.app.commit
import tech.nagual.app.BaseActivity
import me.zhanghai.android.files.util.args
import me.zhanghai.android.files.util.putArgs

class AddDocumentTreeActivity : BaseActivity() {
    private val args by args<me.zhanghai.android.files.storage.AddDocumentTreeFragment.Args>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Calls ensureSubDecor().
        findViewById<View>(android.R.id.content)
        if (savedInstanceState == null) {
            val fragment = AddDocumentTreeFragment()
                .putArgs(args)
            supportFragmentManager.commit {
                add(fragment, AddDocumentTreeFragment::class.java.name)
            }
        }
    }
}
