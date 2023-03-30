package tech.nagual.phoenix.tools.organizer.common

import androidx.annotation.LayoutRes
import androidx.fragment.app.activityViewModels
import tech.nagual.app.BaseNavigationFragment
import tech.nagual.phoenix.tools.organizer.ActivityViewModel
import tech.nagual.phoenix.tools.organizer.OrganizerActivity
import tech.nagual.phoenix.tools.organizer.utils.ExportNotesContract

const val FRAGMENT_MESSAGE = "FRAGMENT_MESSAGE"

open class BaseFragment(@LayoutRes resId: Int) : BaseNavigationFragment(resId) {

    val activityModel: ActivityViewModel by activityViewModels()

    protected val exportNotesLauncher = registerForActivityResult(ExportNotesContract) { uri ->
        if (uri == null) return@registerForActivityResult
        (activity as OrganizerActivity).startBackup(uri)
    }
}
