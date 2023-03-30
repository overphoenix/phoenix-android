package tech.nagual.common.interfaces

import tech.nagual.common.activities.BaseSimpleActivity

interface RenameTab {
    fun initTab(activity: tech.nagual.common.activities.BaseSimpleActivity, paths: ArrayList<String>)

    fun dialogConfirmed(useMediaFileExtension: Boolean, callback: (success: Boolean) -> Unit)
}
