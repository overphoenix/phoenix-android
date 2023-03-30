package me.zhanghai.android.files.tools

import android.content.Intent
import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import me.zhanghai.android.files.util.takeIfNotEmpty
import me.zhanghai.android.files.util.createIntent
import me.zhanghai.android.files.util.putArgs

@Parcelize
data class Tool(
    val origName: String,
    val isVisible: Boolean,
    val customName: String? = null
) : Parcelable {
    @IgnoredOnParcel
    val id: Long = origName.hashCode().toLong()

    fun getName(): String = customName?.takeIfNotEmpty() ?: origName

    fun createEditIntent(): Intent =
        EditToolDialogActivity::class.createIntent()
            .putArgs(EditToolDialogFragment.Args(this))
}
