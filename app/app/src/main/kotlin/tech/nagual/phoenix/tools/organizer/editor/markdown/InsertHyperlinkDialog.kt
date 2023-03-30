package tech.nagual.phoenix.tools.organizer.editor.markdown

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import dagger.hilt.android.AndroidEntryPoint
import tech.nagual.common.R
import tech.nagual.phoenix.databinding.OrganizerDialogInsertLinkBinding
import tech.nagual.phoenix.tools.organizer.common.BaseDialog
import tech.nagual.phoenix.tools.organizer.editor.NoteEditorFragment
import tech.nagual.phoenix.tools.organizer.utils.requestFocusAndKeyboard

@AndroidEntryPoint
class InsertHyperlinkDialog : BaseDialog<OrganizerDialogInsertLinkBinding>() {

    private var text: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        text = arguments?.getString(TEXT)
    }

    override fun createBinding(inflater: LayoutInflater) = OrganizerDialogInsertLinkBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.editTextHyperlinkText.setText(text.toString())

        dialog.apply {
            setTitle(getString(R.string.action_insert_link))
            setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.action_cancel)) { _, _ -> }
            setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.action_insert)) { _, _ ->
                val markdown = hyperlinkMarkdown(
                    url = binding.editTextHyperlink.text.toString(),
                    content = binding.editTextHyperlinkText.text.toString(),
                )
                setFragmentResult(
                    NoteEditorFragment.MARKDOWN_DIALOG_RESULT,
                    bundleOf(
                        NoteEditorFragment.MARKDOWN_DIALOG_RESULT to markdown
                    )
                )
            }
        }

        if (binding.editTextHyperlinkText.text?.isEmpty() == true) {
            binding.editTextHyperlinkText.requestFocusAndKeyboard()
        } else {
            binding.editTextHyperlink.requestFocusAndKeyboard()
        }
    }

    companion object {
        private const val TEXT = "TEXT"

        fun build(text: String): InsertHyperlinkDialog {
            return InsertHyperlinkDialog().apply {
                arguments = bundleOf(
                    TEXT to text,
                )
            }
        }
    }
}
