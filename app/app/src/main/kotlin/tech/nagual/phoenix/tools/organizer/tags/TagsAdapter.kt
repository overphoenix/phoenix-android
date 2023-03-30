package tech.nagual.phoenix.tools.organizer.tags

import android.text.TextUtils
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import me.zhanghai.android.fastscroll.PopupTextProvider
import tech.nagual.phoenix.R
import tech.nagual.phoenix.databinding.OrganizerTagItemBinding
import tech.nagual.phoenix.tools.organizer.data.model.Tag
import me.zhanghai.android.files.ui.AnimatedListAdapter
import me.zhanghai.android.files.ui.CheckableItemBackground
import me.zhanghai.android.files.util.layoutInflater
import java.util.*

class TagsAdapter(
    var listener: Listener,
) : AnimatedListAdapter<TagData, TagsAdapter.ViewHolder>(DiffCallback()), PopupTextProvider {

    private val selectedTags = tagItemSetOf()

    private val tagPositionMap = mutableMapOf<String, Int>()

    private lateinit var _nameEllipsize: TextUtils.TruncateAt
    var nameEllipsize: TextUtils.TruncateAt
        get() = _nameEllipsize
        set(value) {
            _nameEllipsize = value
            notifyItemRangeChanged(0, itemCount, PAYLOAD_STATE_CHANGED)
        }

    fun replaceSelectedTags(tagItemSet: TagItemSet) {
        val changedTags = tagItemSetOf()
        val iterator = selectedTags.iterator()
        while (iterator.hasNext()) {
            val tag = iterator.next()
            if (tag !in tagItemSet) {
                iterator.remove()
                changedTags.add(tag)
            }
        }
        for (tag in tagItemSet) {
            if (tag !in selectedTags) {
                selectedTags.add(tag)
                changedTags.add(tag)
            }
        }
        for (tagData in changedTags) {
            val position = tagPositionMap[tagData.tag.name]
            position?.let { notifyItemChanged(it, PAYLOAD_STATE_CHANGED) }
        }
    }

    fun replaceList(list: List<TagData>) {
        super.replace(list, false)
        rebuildTagPositionMap()
    }

    private fun rebuildTagPositionMap() {
        tagPositionMap.clear()
        for (index in 0 until itemCount) {
            val tagData = getItem(index)
            tagPositionMap[tagData.tag.name] = index
        }
    }

    override fun clear() {
        super.clear()
        rebuildTagPositionMap()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            OrganizerTagItemBinding.inflate(parent.context.layoutInflater, parent, false)
        ).apply {
            binding.itemLayout.background =
                CheckableItemBackground.create(binding.itemLayout.context)
            popupMenu = PopupMenu(binding.menuButton.context, binding.menuButton)
                .apply { inflate(R.menu.organizer_tag_item) }
            binding.menuButton.setOnClickListener { popupMenu.show() }
        }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        throw UnsupportedOperationException()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        val tagData = getItem(position)
        val binding = holder.binding
        val checked = tagData in selectedTags
        binding.itemLayout.isChecked = checked
        val nameEllipsize = nameEllipsize
        binding.nameText.ellipsize = nameEllipsize
        binding.nameText.isSelected = nameEllipsize == TextUtils.TruncateAt.MARQUEE
        if (payloads.isNotEmpty()) {
            return
        }
        bindViewHolderAnimation(holder)
        binding.itemLayout.setOnClickListener {
            listener.itemClicked(tagData)
            if (listener.isNoteAttached()) binding.checkBox.isChecked = !binding.checkBox.isChecked
        }
        binding.itemLayout.setOnLongClickListener {
            listener.itemLongClicked(tagData)
            true
        }
        binding.iconLayout.setOnClickListener { selectTag(tagData) }
        binding.iconImage.setImageResource(R.drawable.tag_icon_white_24dp)

        binding.nameText.text = tagData.tag.name
        binding.descriptionText.text = null
        binding.checkBox.isClickable = false
        binding.checkBox.isVisible = listener.isNoteAttached()
        binding.checkBox.isChecked = tagData.inNote

        holder.popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_rename -> {
                    listener.renameTag(tagData.tag)
                    true
                }
                R.id.action_delete -> {
                    listener.deleteTag(tagData.tag)
                    true
                }
                else -> false
            }
        }
    }

    override fun getPopupText(position: Int): String {
        val tagData = getItem(position)
        return tagData.tag.name.take(1).uppercase(Locale.getDefault())
    }

    fun selectTag(tagData: TagData) {
        val selected = tagData in selectedTags
        listener.selectTag(tagData, !selected)
    }

    fun selectAllTags() {
        val tags = tagItemSetOf()
        for (index in 0 until itemCount) {
            val tagData = getItem(index)
            tags.add(tagData)
        }
        listener.selectTags(tags, true)
    }

    private class DiffCallback : DiffUtil.ItemCallback<TagData>() {
        override fun areItemsTheSame(oldItem: TagData, newItem: TagData): Boolean {
            return oldItem.tag.name == newItem.tag.name
        }

        override fun areContentsTheSame(oldItem: TagData, newItem: TagData): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private val PAYLOAD_STATE_CHANGED = Any()

    }

    class ViewHolder(val binding: OrganizerTagItemBinding) : RecyclerView.ViewHolder(binding.root) {
        lateinit var popupMenu: PopupMenu
    }

    interface Listener {
        fun isNoteAttached(): Boolean
        fun itemClicked(tagData: TagData)
        fun itemLongClicked(tagData: TagData)
        fun selectTag(tagData: TagData, selected: Boolean)
        fun selectTags(tagItemSet: TagItemSet, selected: Boolean)
        fun renameTag(tag: Tag)
        fun deleteTag(tag: Tag)
    }
}
