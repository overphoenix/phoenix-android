package tech.nagual.phoenix.organizers

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractDraggableItemViewHolder
import me.zhanghai.android.files.compat.foregroundCompat
import me.zhanghai.android.files.compat.isTransformedTouchPointInViewCompat
import me.zhanghai.android.files.ui.SimpleAdapter
import me.zhanghai.android.files.util.layoutInflater
import tech.nagual.common.R
import tech.nagual.phoenix.databinding.OrganizerItemBinding
import tech.nagual.phoenix.tools.organizer.data.model.Organizer
import androidx.core.view.isVisible
import tech.nagual.phoenix.tools.organizer.OrganizersManager

class OrganizersAdapter(
    private val listener: Listener
) : SimpleAdapter<Organizer, OrganizersAdapter.ViewHolder>(),
    DraggableItemAdapter<OrganizersAdapter.ViewHolder> {
    override val hasStableIds: Boolean
        get() = true

    override fun getItemId(position: Int): Long = getItem(position).id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            OrganizerItemBinding.inflate(parent.context.layoutInflater, parent, false)
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val organizer = getItem(position)
        val binding = holder.binding
        // Need to remove the ripple before it's drawn onto the bitmap for dragging.
        binding.root.foregroundCompat!!.mutate().setVisible(!holder.dragState.isActive, false)
        binding.root.setOnClickListener { OrganizersManager.getInstance().open(organizer, listener.currentContext) }
        binding.root.setOnLongClickListener {
            OrganizersManager.getInstance().edit(organizer, listener.currentContext)
            true
        }
        binding.iconImage.setImageResource(R.drawable.organizer_icon)
        binding.nameText.isActivated = true
        binding.nameText.text = organizer.name
        if (organizer.description.isNotEmpty()) {
            binding.descriptionText.isVisible = true
            binding.descriptionText.text = organizer.description
        } else {
            binding.descriptionText.isVisible = false
        }
    }

    override fun onCheckCanStartDrag(holder: ViewHolder, position: Int, x: Int, y: Int): Boolean =
        (holder.binding.root as ViewGroup).isTransformedTouchPointInViewCompat(
            x.toFloat(), y.toFloat(), holder.binding.dragHandleView, null
        )

    override fun onGetItemDraggableRange(holder: ViewHolder, position: Int): ItemDraggableRange? =
        null

    override fun onCheckCanDrop(draggingPosition: Int, dropPosition: Int): Boolean = true

    @SuppressLint("NotifyDataSetChanged")
    override fun onItemDragStarted(position: Int) {
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onItemDragFinished(fromPosition: Int, toPosition: Int, result: Boolean) {
        notifyDataSetChanged()
    }

    override fun onMoveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition == toPosition) {
            return
        }
        listener.moveOrganizer(fromPosition, toPosition)
    }

    class ViewHolder(val binding: OrganizerItemBinding) : AbstractDraggableItemViewHolder(
        binding.root
    )

    interface Listener {
        val currentContext: Context
        fun moveOrganizer(fromPosition: Int, toPosition: Int)
    }
}
