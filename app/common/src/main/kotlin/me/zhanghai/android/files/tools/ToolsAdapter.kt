package me.zhanghai.android.files.tools

import android.content.Context
import android.content.Intent
import android.view.ViewGroup
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractDraggableItemViewHolder
import me.zhanghai.android.files.compat.foregroundCompat
import me.zhanghai.android.files.compat.isTransformedTouchPointInViewCompat
import me.zhanghai.android.files.ui.SimpleAdapter
import me.zhanghai.android.files.util.layoutInflater
import me.zhanghai.android.files.util.startActivitySafe
import tech.nagual.common.databinding.ToolItemBinding

class ToolsAdapter(
    private val listener: Listener
) : SimpleAdapter<Tool, ToolsAdapter.ViewHolder>(),
    DraggableItemAdapter<ToolsAdapter.ViewHolder> {
    override val hasStableIds: Boolean
        get() = true

    override fun getItemId(position: Int): Long = getItem(position).id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            ToolItemBinding.inflate(parent.context.layoutInflater, parent, false)
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tool = getItem(position)
        val binding = holder.binding
        // Need to remove the ripple before it's drawn onto the bitmap for dragging.
        binding.root.foregroundCompat!!.mutate().setVisible(!holder.dragState.isActive, false)
        binding.root.setOnClickListener {
            listener.currentContext.startActivitySafe(
                Intent(
                    listener.currentContext,
                    Class.forName(Tools.infos[tool.origName]!!.packageName)
                )
            )
        }
        binding.root.setOnLongClickListener {
            listener.editTool(tool)
            true
        }
        binding.iconImage.setImageResource(Tools.infos[tool.origName]!!.iconRes)
        binding.nameText.isActivated = tool.isVisible
        binding.nameText.text = tool.getName()
//        binding.descriptionText.text = null
    }

    override fun onCheckCanStartDrag(holder: ViewHolder, position: Int, x: Int, y: Int): Boolean =
        (holder.binding.root as ViewGroup).isTransformedTouchPointInViewCompat(
            x.toFloat(), y.toFloat(), holder.binding.dragHandleView, null
        )

    override fun onGetItemDraggableRange(holder: ViewHolder, position: Int): ItemDraggableRange? =
        null

    override fun onCheckCanDrop(draggingPosition: Int, dropPosition: Int): Boolean = true

    override fun onItemDragStarted(position: Int) {
        notifyDataSetChanged()
    }

    override fun onItemDragFinished(fromPosition: Int, toPosition: Int, result: Boolean) {
        notifyDataSetChanged()
    }

    override fun onMoveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition == toPosition) {
            return
        }
        listener.moveTool(fromPosition, toPosition)
    }

    class ViewHolder(val binding: ToolItemBinding) : AbstractDraggableItemViewHolder(
        binding.root
    )

    interface Listener {
        val currentContext: Context
        fun editTool(tool: Tool)
        fun moveTool(fromPosition: Int, toPosition: Int)
    }
}
