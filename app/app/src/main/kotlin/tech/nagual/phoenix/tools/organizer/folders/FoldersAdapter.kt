package tech.nagual.phoenix.tools.organizer.folders

import android.text.TextUtils
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import me.zhanghai.android.files.ui.AnimatedListAdapter
import me.zhanghai.android.files.ui.CheckableItemBackground
import me.zhanghai.android.fastscroll.PopupTextProvider
import me.zhanghai.android.files.util.layoutInflater
import tech.nagual.phoenix.R
import tech.nagual.phoenix.databinding.OrganizerFolderItemBinding
import tech.nagual.phoenix.tools.organizer.data.model.Folder
import java.util.*

class FoldersAdapter(
    var listener: Listener,
) : AnimatedListAdapter<Folder, FoldersAdapter.ViewHolder>(DiffCallback()), PopupTextProvider {

    private val selectedFolders = folderItemSetOf()

    private val folderPositionMap = mutableMapOf<String, Int>()

    private lateinit var _nameEllipsize: TextUtils.TruncateAt
    var nameEllipsize: TextUtils.TruncateAt
        get() = _nameEllipsize
        set(value) {
            _nameEllipsize = value
            notifyItemRangeChanged(0, itemCount, PAYLOAD_STATE_CHANGED)
        }

    fun replaceSelectedNotebooks(folders: FolderItemSet) {
        val changedFolders = folderItemSetOf()
        val iterator = selectedFolders.iterator()
        while (iterator.hasNext()) {
            val folder = iterator.next()
            if (folder !in folders) {
                iterator.remove()
                changedFolders.add(folder)
            }
        }
        for (folder in folders) {
            if (folder !in selectedFolders) {
                selectedFolders.add(folder)
                changedFolders.add(folder)
            }
        }
        for (folder in changedFolders) {
            val position = folderPositionMap[folder.name]
            position?.let { notifyItemChanged(it, PAYLOAD_STATE_CHANGED) }
        }
    }

    fun replaceList(list: List<Folder>) {
        super.replace(list, false)
        rebuildFolderPositionMap()
    }

    private fun rebuildFolderPositionMap() {
        folderPositionMap.clear()
        for (index in 0 until itemCount) {
            val folder = getItem(index)
            folderPositionMap[folder.name] = index
        }
    }

    override fun clear() {
        super.clear()
        rebuildFolderPositionMap()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            OrganizerFolderItemBinding.inflate(parent.context.layoutInflater, parent, false)
        ).apply {
            binding.itemLayout.background =
                CheckableItemBackground.create(binding.itemLayout.context)
            popupMenu = PopupMenu(binding.menuButton.context, binding.menuButton)
                .apply { inflate(R.menu.organizer_folder_item) }
            binding.menuButton.setOnClickListener { popupMenu.show() }
        }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        throw UnsupportedOperationException()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        val folder = getItem(position)
        val binding = holder.binding
        val checked = folder in selectedFolders
        binding.itemLayout.isChecked = checked
        val nameEllipsize = nameEllipsize
        binding.nameText.ellipsize = nameEllipsize
        binding.nameText.isSelected = nameEllipsize == TextUtils.TruncateAt.MARQUEE
        if (payloads.isNotEmpty()) {
            return
        }
        bindViewHolderAnimation(holder)
        binding.itemLayout.setOnClickListener {
            if (selectedFolders.isEmpty()) {
                listener.openFolder(folder)
            } else {
                selectFolder(folder)
            }
        }
        binding.itemLayout.setOnLongClickListener {
            if (selectedFolders.isEmpty()) {
                selectFolder(folder)
            } else {
                listener.openFolder(folder)
            }
            true
        }
        binding.iconLayout.setOnClickListener { selectFolder(folder) }
        binding.iconImage.setImageResource(R.drawable.organizer_folder_icon_24dp)

        binding.nameText.text = folder.name
        binding.descriptionText.text = null

        holder.popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
//                R.id.action_edit -> {
//                    true
//                }
                R.id.action_rename -> {
                    listener.renameFolder(folder)
                    true
                }
                R.id.action_delete -> {
                    listener.deleteFolder(folder)
                    true
                }
                else -> false
            }
        }
    }

    override fun getPopupText(position: Int): String {
        val folder = getItem(position)
        return folder.name.take(1).uppercase(Locale.getDefault())
    }

    private fun selectFolder(folder: Folder) {
        val selected = folder in selectedFolders
        listener.selectFolder(folder, !selected)
    }

    fun selectAllFolders() {
        val folders = folderItemSetOf()
        for (index in 0 until itemCount) {
            val folder = getItem(index)
            folders.add(folder)
        }
        listener.selectFolders(folders, true)
    }

    private class DiffCallback : DiffUtil.ItemCallback<Folder>() {
        override fun areItemsTheSame(oldItem: Folder, newItem: Folder): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: Folder, newItem: Folder): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private val PAYLOAD_STATE_CHANGED = Any()

    }

    class ViewHolder(val binding: OrganizerFolderItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        lateinit var popupMenu: PopupMenu
    }

    interface Listener {
        fun openFolder(folder: Folder)
        fun selectFolder(folder: Folder, selected: Boolean)
        fun selectFolders(folders: FolderItemSet, selected: Boolean)
        fun renameFolder(folder: Folder)
        fun deleteFolder(folder: Folder)
    }
}
