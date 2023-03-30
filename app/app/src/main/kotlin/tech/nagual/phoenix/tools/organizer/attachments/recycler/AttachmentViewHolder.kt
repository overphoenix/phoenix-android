package tech.nagual.phoenix.tools.organizer.attachments.recycler

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
//import coil.fetch.VideoFrameUriFetcher
import coil.load
import coil.request.ImageRequest
import tech.nagual.common.R
import tech.nagual.phoenix.tools.organizer.data.model.Attachment
import tech.nagual.phoenix.databinding.OrganizerLayoutAttachmentBinding
import tech.nagual.phoenix.tools.organizer.attachments.uri

class AttachmentViewHolder(
    private val context: Context,
    private val binding: OrganizerLayoutAttachmentBinding,
    listener: AttachmentRecyclerListener? = null,
    private val inPreview: Boolean,
) : RecyclerView.ViewHolder(binding.root) {

    init {
        if (!inPreview) binding.root.cardElevation = 16F

        if (listener != null) {
            itemView.setOnClickListener { listener.onItemClick(bindingAdapterPosition, binding) }
            itemView.setOnLongClickListener { listener.onLongClick(bindingAdapterPosition, binding) }
        }
    }

    private inline fun ImageView.loadThumbnail(
        uri: Uri?,
        builder: ImageRequest.Builder.() -> Unit = {}
    ) {
        load(uri) {
            if (inPreview) size(480, 480) else size(860, 860)
            builder()
        }
    }

    private fun setIndicator(@DrawableRes id: Int) = with(binding) {
        indicatorAttachmentType.isVisible = true
        indicatorAttachmentType.setImageDrawable(ContextCompat.getDrawable(context, id))
    }

    private fun setDescription(description: String) = with(binding) {
        textView.text = description
        textView.isVisible = description.isNotEmpty() && !inPreview
    }

    fun bind(attachment: Attachment) = with(binding) {
        setDescription(attachment.description)

        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        indicatorAttachmentType.isVisible = false

        when (attachment.type) {
            Attachment.Type.PHOTO -> {
                imageView.loadThumbnail(attachment.uri(context))
            }
            Attachment.Type.VIDEO -> {
                imageView.apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    loadThumbnail(attachment.uri(context)) {
//                        fetcher(VideoFrameUriFetcher(context))
                    }
                }
                setIndicator(R.drawable.ic_movie)
            }
            Attachment.Type.AUDIO -> {
                imageView.loadThumbnail(attachment.uri(context)) {
//                    fetcher(AlbumArtFetcher(context))
                }

                setIndicator(R.drawable.ic_music)
            }
            Attachment.Type.GENERIC -> {
                imageView.setColorFilter(Color.WHITE)
                imageView.load(R.drawable.ic_file)
            }
        }
    }

    fun showMoreAttachmentsIndicator(count: Int) = with(binding) {
        indicatorMoreAttachments.visibility = View.VISIBLE
        indicatorMoreAttachments.text = "+$count"
    }

    fun runPayloads(attachment: Attachment, payloads: List<AttachmentsAdapter.Payload>) {
        payloads.forEach {
            when (it) {
                AttachmentsAdapter.Payload.DescriptionChanged -> setDescription(attachment.description)
            }
        }
    }
}
