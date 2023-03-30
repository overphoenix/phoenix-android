package tech.nagual.phoenix.tools.organizer.attachments.recycler

import tech.nagual.phoenix.databinding.OrganizerLayoutAttachmentBinding

interface AttachmentRecyclerListener {
    fun onItemClick(position: Int, viewBinding: OrganizerLayoutAttachmentBinding)
    fun onLongClick(position: Int, viewBinding: OrganizerLayoutAttachmentBinding): Boolean
}
