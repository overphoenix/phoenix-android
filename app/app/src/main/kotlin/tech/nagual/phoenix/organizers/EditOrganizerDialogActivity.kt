package tech.nagual.phoenix.organizers

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.commit
import dagger.hilt.android.AndroidEntryPoint
import tech.nagual.app.BaseActivity
import tech.nagual.app.application
import me.zhanghai.android.files.util.args
import me.zhanghai.android.files.util.putArgs
import me.zhanghai.android.files.util.startActivitySafe
import tech.nagual.common.R
import tech.nagual.phoenix.tools.organizer.data.model.Organizer
import me.zhanghai.android.files.util.createIntent

@AndroidEntryPoint
class EditOrganizerDialogActivity : BaseActivity() {
    private val args by args<EditOrganizerDialogFragment.Args>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Calls ensureSubDecor().
        findViewById<View>(android.R.id.content)
        if (savedInstanceState == null) {
            val fragment = EditOrganizerDialogFragment().putArgs(args)
            supportFragmentManager.commit {
                add(fragment, EditOrganizerDialogFragment::class.java.name)
            }
        }
    }

    companion object {
        fun showCreateOrganizerDialog(context: Context) {
            val organizer = Organizer(
                name = application.getString(R.string.organizer_new_placeholder),
                description = ""
            )
            context.startActivitySafe(
                EditOrganizerDialogActivity::class.createIntent()
                    .putArgs(EditOrganizerDialogFragment.Args(organizer, true))
            )
        }
    }
}
