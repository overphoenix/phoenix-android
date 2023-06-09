/*
 * Copyright (c) 2019 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.filejob

import android.os.Bundle
import android.view.View
import androidx.fragment.app.commit
import me.zhanghai.android.files.util.args
import me.zhanghai.android.files.util.putArgs
import tech.nagual.app.BaseActivity

class FileJobActionDialogActivity : BaseActivity() {
    private val args by args<FileJobActionDialogFragment.Args>()

    private lateinit var fragment: FileJobActionDialogFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Calls ensureSubDecor().
        findViewById<View>(android.R.id.content)
        if (savedInstanceState == null) {
            fragment = FileJobActionDialogFragment().putArgs(args)
            supportFragmentManager.commit {
                add(fragment, FileJobActionDialogFragment::class.java.name)
            }
        } else {
            fragment = supportFragmentManager.findFragmentByTag(
                FileJobActionDialogFragment::class.java.name
            ) as FileJobActionDialogFragment
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isFinishing) {
            fragment.onFinish()
        }
    }
}
