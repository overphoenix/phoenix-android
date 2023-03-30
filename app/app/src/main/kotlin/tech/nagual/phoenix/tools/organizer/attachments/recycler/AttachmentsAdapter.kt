package tech.nagual.phoenix.tools.organizer.attachments.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import tech.nagual.phoenix.tools.organizer.data.model.Attachment
import tech.nagual.phoenix.databinding.OrganizerLayoutAttachmentBinding
import tech.nagual.phoenix.tools.organizer.common.recycler.ExtendedListAdapter

class AttachmentsAdapter(
    var listener: AttachmentRecyclerListener? = null,
    private val inPreview: Boolean = false,
) : ExtendedListAdapter<Attachment, AttachmentViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttachmentViewHolder {
        val binding: OrganizerLayoutAttachmentBinding =
            OrganizerLayoutAttachmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AttachmentViewHolder(parent.context, binding, listener, inPreview)
    }

    override fun onBindViewHolder(holder: AttachmentViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            return super.onBindViewHolder(holder, position, payloads)
        }

        holder.runPayloads(getItem(position), payloads.map { it as Payload })
    }

    override fun onBindViewHolder(holder: AttachmentViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val attachment: Attachment = getItem(position)
        holder.bind(attachment)
    }

    override fun getItemId(position: Int): Long = getItem(position).path.hashCode().toLong()

    private class DiffCallback : DiffUtil.ItemCallback<Attachment>() {
        override fun areItemsTheSame(oldItem: Attachment, newItem: Attachment): Boolean {
            return oldItem.path == newItem.path
        }

        override fun areContentsTheSame(oldItem: Attachment, newItem: Attachment): Boolean {
            return oldItem == newItem
        }

        override fun getChangePayload(oldItem: Attachment, newItem: Attachment): Payload? {
            return if (oldItem.description != newItem.description) Payload.DescriptionChanged else null
        }
    }

    enum class Payload {
        DescriptionChanged
    }
}
