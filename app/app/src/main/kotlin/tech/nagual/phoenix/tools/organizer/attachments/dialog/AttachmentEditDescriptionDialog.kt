package tech.nagual.phoenix.tools.organizer.attachments.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import tech.nagual.common.R
import tech.nagual.phoenix.databinding.OrganizerDialogEditAttachmentBinding
import tech.nagual.phoenix.tools.organizer.common.BaseDialog
import tech.nagual.phoenix.tools.organizer.utils.collect
import tech.nagual.phoenix.tools.organizer.utils.requestFocusAndKeyboard

class AttachmentEditDescriptionDialog : BaseDialog<OrganizerDialogEditAttachmentBinding>() {
    private val model: AttachmentDialogViewModel by activityViewModels()

    private var path: String? = null
    private var noteId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        path = arguments?.getString(ATTACHMENT_PATH)
        noteId = arguments?.getLong(NOTE_ID)
    }

    override fun createBinding(inflater: LayoutInflater) =
        OrganizerDialogEditAttachmentBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val noteId = noteId ?: return
        val path = path ?: return

        dialog.apply {
            setTitle(getString(R.string.attachments_edit_description))
            setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.save)) { _, _ ->
                model.updateAttachmentDescription(
                    noteId,
                    path,
                    binding.valueEdit.text.toString()
                )
                dismiss()
            }
        }

        model.getAttachment(noteId, path).collect(this) {
            if (it == null) return@collect
            binding.valueEdit.setText(it.description)
            if (it.description.isEmpty()) binding.valueEdit.requestFocusAndKeyboard()
        }
    }

    companion object {
        private const val ATTACHMENT_PATH = "ATTACHMENT_PATH"
        private const val NOTE_ID = "NOTE_ID"

        fun build(noteId: Long, attachmentPath: String): AttachmentEditDescriptionDialog {
            return AttachmentEditDescriptionDialog().apply {
                arguments = bundleOf(
                    ATTACHMENT_PATH to attachmentPath,
                    NOTE_ID to noteId
                )
            }
        }
    }
}
