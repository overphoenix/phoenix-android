package me.zhanghai.android.files.tools

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.parcelize.Parcelize
import me.zhanghai.android.files.util.ParcelableArgs
import me.zhanghai.android.files.util.finish
import me.zhanghai.android.files.util.layoutInflater
import tech.nagual.common.R
import tech.nagual.common.databinding.EditToolDialogBinding
import me.zhanghai.android.files.util.*
import me.zhanghai.android.files.util.setTextWithSelection

class EditToolDialogFragment : AppCompatDialogFragment() {
    private val args by args<Args>()

    private lateinit var binding: EditToolDialogBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val tool = args.tool
        val dialogBuilder = MaterialAlertDialogBuilder(requireContext(), theme)
        dialogBuilder.setTitle(R.string.edit_tool_title)
            .apply {
                binding = EditToolDialogBinding.inflate(context.layoutInflater)

                binding.nameLayout.placeholderText = "My tool"
                if (savedInstanceState == null) {
                    binding.nameEdit.setTextWithSelection(
                        tool.getName()
                    )
                }
                setView(binding.root)
            }
            .setPositiveButton(android.R.string.ok) { _, _ -> save() }
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
            .setNeutralButton(
                if (tool.isVisible) R.string.hide else R.string.show
            ) { _, _ -> toggleVisibility() }

        return dialogBuilder.create()
            .apply {
                window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            }
    }

    private fun save() {
        val name =
            if (binding.nameEdit.text.isNullOrEmpty()) binding.nameLayout.placeholderText.toString()
            else binding.nameEdit.text.toString()
        val tool = args.tool.copy(customName = name)
        Tools.replace(tool)

        finish()
    }

    private fun toggleVisibility() {
        val tool = args.tool.let { it.copy(isVisible = !it.isVisible) }
        Tools.replace(tool)
        finish()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)

        finish()
    }

    @Parcelize
    class Args(val tool: Tool) : ParcelableArgs
}
