package tech.nagual.phoenix.tools.organizer.common.recycler

import android.annotation.SuppressLint
import android.content.Context
import android.view.ContextThemeWrapper
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.noties.markwon.Markwon
import org.commonmark.node.Code
import tech.nagual.app.application
import tech.nagual.common.R
import tech.nagual.common.extensions.dp
import tech.nagual.phoenix.databinding.OrganizerNoteItemBinding
import tech.nagual.phoenix.tools.organizer.attachments.recycler.AttachmentViewHolder
import tech.nagual.phoenix.tools.organizer.attachments.recycler.AttachmentsAdapter
import tech.nagual.phoenix.tools.organizer.attachments.recycler.AttachmentsPreviewGridManager
import tech.nagual.phoenix.tools.organizer.data.model.*
import tech.nagual.phoenix.tools.organizer.data.repo.CategoriesRepository
import tech.nagual.phoenix.tools.organizer.editor.markdown.applyTo
import tech.nagual.phoenix.tools.organizer.tasks.TasksAdapter
import tech.nagual.phoenix.tools.organizer.utils.ellipsize
import tech.nagual.phoenix.tools.organizer.utils.resId

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CategoriesEntryPoint {
    var categoriesRepository: CategoriesRepository
}

class NoteViewHolder(
    private val binding: OrganizerNoteItemBinding,
    listener: NoteRecyclerListener?,
    private val context: Context,
    private val searchMode: Boolean,
    private val markwon: Markwon,
) : RecyclerView.ViewHolder(binding.root), SelectableViewHolder {

    private val tasksAdapter = TasksAdapter(true, null, markwon)
    private val attachmentsAdapter = AttachmentsAdapter(null, true)

    private val defaultStrokeWidth = 1.dp(context)
    private val selectedStrokeWidth = 2.dp(context)

    private val categoriesEntryPoint =
        EntryPointAccessors.fromApplication(
            application.applicationContext,
            CategoriesEntryPoint::class.java
        )
    private val categoriesRepository = categoriesEntryPoint.categoriesRepository

    init {
        binding.recyclerAttachments.apply {
            layoutManager = AttachmentsPreviewGridManager(context, 2)
            adapter = attachmentsAdapter
        }

        binding.recyclerTasks.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = tasksAdapter
        }

        if (listener != null) {
            itemView.setOnClickListener { listener.onItemClick(bindingAdapterPosition, binding) }
            itemView.setOnLongClickListener {
                listener.onLongClick(
                    bindingAdapterPosition,
                    binding
                )
            }
        }
    }

    private fun updateBackgroundColor(color: NoteColor) {
        color.resId(context)?.let { resId ->
            binding.root.setCardBackgroundColor(resId)
//            binding.linearLayout.setBackgroundColor(resId)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateTags(tags: List<Tag>) {
        binding.containerTags.removeAllViews()
        binding.containerTags.isVisible = tags.isNotEmpty()

        if (tags.isEmpty()) return

        for (tag in tags) {
            val tagView = TextView(ContextThemeWrapper(context, R.style.TagChip))
            if (binding.containerTags.childCount > 0) {
                tagView.text = "+${tags.size - binding.containerTags.childCount}"
                binding.containerTags.addView(tagView)
                break
            }
            tagView.text = "# ${tag.name}"
            binding.containerTags.addView(tagView)
        }
    }

    private fun updateIndicatorIcons(note: Note, hasReminders: Boolean) = with(binding) {
        indicatorNoteHidden.isVisible = note.isHidden && !searchMode
        indicatorPinned.isVisible = note.isPinned && !searchMode
        indicatorHasReminder.isVisible = hasReminders
        indicatorDeleted.isVisible = note.isDeleted && searchMode
        indicatorArchived.isVisible = note.isArchived && searchMode
        if (indicatorNoteHidden.isVisible ||
            indicatorPinned.isVisible ||
            indicatorHasReminder.isVisible ||
            indicatorDeleted.isVisible ||
            indicatorArchived.isVisible
        ) {
            if (note.title.isNullOrEmpty()) {
                textViewTitle.text = note.title
                textViewTitle.isVisible = true
            }
        } else if (note.title.isNullOrEmpty()) {
            textViewTitle.isVisible = false
        }
    }

    private fun setTitle(note: Note) {
        if (note.title.isNullOrEmpty()) {
            binding.textViewTitle.isVisible = false
        } else {
            binding.textViewTitle.isVisible = true
            binding.textViewTitle.text = note.title
        }
    }

    private fun setContent(note: Note) = with(binding) {
        recyclerTasks.isVisible = note.type == NoteViewType.TaskList && note.taskList.isNotEmpty()
        indicatorMoreTasks.isVisible = false

        if (note.type == NoteViewType.Categories) {
            textViewContent.isVisible = true
            textViewContent.text = note.categoriesToString()
        } else {
            textViewContent.isVisible = note.type == NoteViewType.Text && note.content.isNotEmpty()

            val taskList =
                note.taskList.takeIf { it.size <= 8 } ?: note.taskList.subList(0, 8).also {
                    val moreItems = note.taskList.size - 8

                    indicatorMoreTasks.isVisible = true
                    indicatorMoreTasks.text =
                        context.resources.getQuantityString(
                            R.plurals.more_items,
                            moreItems,
                            moreItems
                        )
                }

            tasksAdapter.submitList(taskList)
            textViewContent.ellipsize()

            if (note.isMarkdownEnabled && note.content.isNotBlank()) {
                try {
                    markwon.applyTo(textViewContent, note.content) {
                        maximumTableColumns = 4
                        tableReplacement = { Code(context.getString(R.string.message_cannot_preview_table)) }
                    }
                } catch(e: Throwable) {
                    textViewContent.text = ""
                }
            } else {
                textViewContent.text = note.content
            }
        }
    }

    private fun setupAttachments(attachments: List<Attachment>) {
        binding.recyclerAttachments.isVisible = attachments.isNotEmpty()
        if (attachments.isEmpty()) return

        val layoutManager =
            binding.recyclerAttachments.layoutManager as AttachmentsPreviewGridManager

        val list = attachments.take(attachments.size.coerceAtMost(4))
        val remaining = attachments.size - list.size
        layoutManager.allocateSpans(list.size)
        attachmentsAdapter.submitList(list)

        if (remaining > 0) {
            binding.recyclerAttachments.doOnPreDraw {
                (binding.recyclerAttachments.findViewHolderForAdapterPosition(3) as? AttachmentViewHolder)
                    ?.showMoreAttachmentsIndicator(remaining)
            }
        }
    }

    fun runPayloads(note: Note, payloads: List<NoteRecyclerAdapter.Payload>) {
        payloads.forEach {
            when (it) {
                NoteRecyclerAdapter.Payload.TitleChanged -> setTitle(note)
                NoteRecyclerAdapter.Payload.ContentChanged -> setContent(note)
                NoteRecyclerAdapter.Payload.PinChanged -> updateIndicatorIcons(
                    note,
                    note.reminders.isNotEmpty()
                )
                NoteRecyclerAdapter.Payload.MarkdownChanged -> setContent(note)
                NoteRecyclerAdapter.Payload.HiddenChanged -> updateIndicatorIcons(
                    note,
                    note.reminders.isNotEmpty()
                )
                NoteRecyclerAdapter.Payload.ColorChanged -> updateBackgroundColor(note.color)
                NoteRecyclerAdapter.Payload.ArchivedChanged -> updateIndicatorIcons(
                    note,
                    note.reminders.isNotEmpty()
                )
                NoteRecyclerAdapter.Payload.DeletedChanged -> updateIndicatorIcons(
                    note,
                    note.reminders.isNotEmpty()
                )
                NoteRecyclerAdapter.Payload.AttachmentsChanged -> setupAttachments(note.attachments)
                NoteRecyclerAdapter.Payload.TagsChanged -> updateTags(note.tags)
                NoteRecyclerAdapter.Payload.RemindersChanged -> updateIndicatorIcons(
                    note,
                    note.reminders.isNotEmpty()
                )
                NoteRecyclerAdapter.Payload.TasksChanged -> setContent(note)
                NoteRecyclerAdapter.Payload.CategoriesChanged -> setContent(note)
            }
        }
    }

    fun bind(note: Note) {
        setContent(note)
        setTitle(note)
        updateBackgroundColor(note.color)
        updateIndicatorIcons(note, note.reminders.isNotEmpty())
        updateTags(note.tags)
        setupAttachments(note.attachments)

        ViewCompat.setTransitionName(binding.root, "editor_${note.id}")
    }

    override fun onSelectedStatusChanged(isSelected: Boolean) {
        binding.root.isChecked = isSelected
        binding.root.strokeWidth = if (isSelected) selectedStrokeWidth else defaultStrokeWidth
    }
}
