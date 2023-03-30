package tech.nagual.phoenix.tools.organizer.categories

import android.view.ViewGroup
import androidx.core.view.isVisible
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractDraggableItemViewHolder
import me.zhanghai.android.files.compat.foregroundCompat
import me.zhanghai.android.files.compat.isTransformedTouchPointInViewCompat
import me.zhanghai.android.files.ui.SimpleAdapter
import me.zhanghai.android.files.util.layoutInflater
import tech.nagual.phoenix.databinding.OrganizerCategoryItemBinding
import tech.nagual.phoenix.tools.organizer.OrganizersManager
import tech.nagual.phoenix.tools.organizer.data.model.RawCategory

class CategoriesAdapter(
    private val listener: Listener
) : SimpleAdapter<RawCategory, CategoriesAdapter.ViewHolder>(),
    DraggableItemAdapter<CategoriesAdapter.ViewHolder> {
    override val hasStableIds: Boolean
        get() = true

    override fun getItemId(position: Int): Long = getItem(position).id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            OrganizerCategoryItemBinding.inflate(parent.context.layoutInflater, parent, false)
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val rawCategory = getItem(position)
        val binding = holder.binding
        // Need to remove the ripple before it's drawn onto the bitmap for dragging.
        binding.root.foregroundCompat!!.mutate().setVisible(!holder.dragState.isActive, false)
        binding.root.setOnClickListener {
            listener.openCategory(rawCategory)
        }
        binding.root.setOnLongClickListener {
            listener.editCategory(rawCategory)
            true
        }
        binding.iconImage.setImageResource(OrganizersManager.getCategoryTypeIconRes(rawCategory.type))
        binding.nameText.isActivated = true
        binding.nameText.text = rawCategory.name
//        if (rawCategory.categoryName.isNotEmpty()) {
//            binding.descriptionText.isVisible = true
//            binding.descriptionText.text = rawCategory.categoryName
//        } else {
            binding.descriptionText.isVisible = false
//        }
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
        listener.moveCategory(fromPosition, toPosition)
    }

    class ViewHolder(val binding: OrganizerCategoryItemBinding) : AbstractDraggableItemViewHolder(
        binding.root
    )

    interface Listener {
        fun openCategory(rawCategory: RawCategory)
        fun editCategory(rawCategory: RawCategory)
        fun moveCategory(fromPosition: Int, toPosition: Int)
    }
}
