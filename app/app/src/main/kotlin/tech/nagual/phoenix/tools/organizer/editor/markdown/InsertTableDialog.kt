package tech.nagual.phoenix.tools.organizer.editor.markdown

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.setFragmentResult
import dagger.hilt.android.AndroidEntryPoint
import tech.nagual.common.R
import tech.nagual.phoenix.databinding.OrganizerDialogInsertTableBinding
import tech.nagual.phoenix.tools.organizer.common.BaseDialog
import tech.nagual.phoenix.tools.organizer.common.setButton
import tech.nagual.phoenix.tools.organizer.editor.NoteEditorFragment
import tech.nagual.phoenix.tools.organizer.utils.requestFocusAndKeyboard

@AndroidEntryPoint
class InsertTableDialog : BaseDialog<OrganizerDialogInsertTableBinding>() {
    override fun createBinding(inflater: LayoutInflater) = OrganizerDialogInsertTableBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dialog.apply {
            setTitle(getString(R.string.action_insert_table))
            setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.action_cancel)) { _, _ -> }
            setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.action_insert), this@InsertTableDialog) {
                val rows = binding.editTextRows.text.toString()
                val columns = binding.editTextColumns.text.toString()

                if (rows.isBlank() || columns.isBlank() || !rows.isDigitsOnly() || !columns.isDigitsOnly()) {
                    Toast.makeText(requireContext(), getString(R.string.message_invalid_number_rows_columns), Toast.LENGTH_SHORT).show()
                    return@setButton
                }

                val markdown = tableMarkdown(
                    rows = rows.toInt(),
                    columns = columns.toInt(),
                )
                setFragmentResult(
                    NoteEditorFragment.MARKDOWN_DIALOG_RESULT,
                    bundleOf(
                        NoteEditorFragment.MARKDOWN_DIALOG_RESULT to markdown
                    )
                )
                dismiss()
            }
        }

        if (binding.editTextColumns.text?.isEmpty() == true) {
            binding.editTextColumns.requestFocusAndKeyboard()
        } else {
            binding.editTextRows.requestFocusAndKeyboard()
        }
    }
}
