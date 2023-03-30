package tech.nagual.common.dialogs

import android.view.LayoutInflater
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import tech.nagual.common.R
import tech.nagual.common.databinding.DialogRenameBinding
import tech.nagual.common.extensions.onPageChangeListener
import tech.nagual.common.extensions.onTabSelectionChanged
import tech.nagual.common.extensions.setupDialogStuff
import tech.nagual.common.helpers.RENAME_PATTERN
import tech.nagual.common.helpers.RENAME_SIMPLE
import tech.nagual.common.activities.BaseSimpleActivity
import tech.nagual.common.adapters.RenameAdapter
import tech.nagual.common.extensions.*
import tech.nagual.common.extensions.*
import tech.nagual.common.views.MyViewPager
import java.util.*

class RenameDialog(
    val activity: tech.nagual.common.activities.BaseSimpleActivity,
    val paths: ArrayList<String>,
    val useMediaFileExtension: Boolean,
    val callback: () -> Unit
) {
    var dialog: AlertDialog? = null
    private val binding = DialogRenameBinding.inflate(LayoutInflater.from(activity))
    val view = binding.root
    var tabsAdapter: RenameAdapter
    var viewPager: MyViewPager

    init {
        view.apply {
            viewPager = binding.dialogTabViewPager
            tabsAdapter = RenameAdapter(activity, paths)
            viewPager.adapter = tabsAdapter
            viewPager.onPageChangeListener {
                binding.dialogTabLayout.getTabAt(it)!!.select()
            }
            viewPager.currentItem = activity.baseConfig.lastRenameUsed

            binding.dialogTabLayout.onTabSelectionChanged(tabSelectedAction = {
                viewPager.currentItem = when {
                    it.text.toString().equals(
                        resources.getString(R.string.simple_renaming),
                        true
                    ) -> RENAME_SIMPLE
                    else -> RENAME_PATTERN
                }
            })
        }

        dialog = MaterialAlertDialogBuilder(activity)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel) { dialog, which -> dismissDialog() }
            .create().apply {
                activity.setupDialogStuff(view, this).apply {
                    window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        tabsAdapter.dialogConfirmed(useMediaFileExtension, viewPager.currentItem) {
                            dismissDialog()
                            if (it) {
                                activity.baseConfig.lastRenameUsed = viewPager.currentItem
                                callback()
                            }
                        }
                    }
                }
            }
    }

    private fun dismissDialog() {
        dialog!!.dismiss()
    }
}
