package tech.nagual.phoenix.tools.organizer.editor

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractDraggableItemViewHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.nagual.app.application
import me.zhanghai.android.files.compat.foregroundCompat
import me.zhanghai.android.files.compat.getDrawableCompat
import me.zhanghai.android.files.compat.isTransformedTouchPointInViewCompat
import me.zhanghai.android.files.ui.SimpleAdapter
import me.zhanghai.android.files.util.layoutInflater
import tech.nagual.phoenix.databinding.OrganizerNoteVariantItemBinding
import tech.nagual.phoenix.tools.organizer.data.model.RawVariant

class CategoriesAdapter(
    private val listener: Listener
) : SimpleAdapter<RawVariant, CategoriesAdapter.ViewHolder>(),
    DraggableItemAdapter<CategoriesAdapter.ViewHolder> {
    override val hasStableIds: Boolean
        get() = true

    override fun getItemId(position: Int): Long = getItem(position).id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            OrganizerNoteVariantItemBinding.inflate(parent.context.layoutInflater, parent, false)
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val rawVariant = getItem(position)
        val binding = holder.binding
        binding.root.setBackgroundColor(listener.backgroundColor)
        binding.root.foregroundCompat!!.mutate().setVisible(!holder.dragState.isActive, false)

//        binding.iconImage.setImageResource(OrganizersManager.getCategoryTypeIconRes(rawVariant.categoryType))
        binding.textLayout.hint = rawVariant.categoryName
        binding.textLayout.setDropDown(listener.inEditMode)
        if (listener.inEditMode) {
            listener.checkCategoryExists(rawVariant.categoryId) { exists ->
                if (!exists) {
                    withContext(Dispatchers.Main) {
                        binding.textLayout.endIconDrawable =
                            application.getDrawableCompat(tech.nagual.common.R.drawable.organizer_warning_icon_red_24dp)
                    }
                }
            }
        }

        binding.text.setText(rawVariant.getCompleteValue())
        binding.text.setTextIsSelectable(!listener.inEditMode)
        if (listener.inEditMode) {
            binding.text.setOnClickListener {
                listener.chooseVariant(rawVariant, position)
            }
            binding.text.setOnLongClickListener {
                listener.removeCategory(rawVariant.categoryId)
                true
            }
        } else {
            binding.text.setOnClickListener(null)
            binding.text.setOnLongClickListener(null)
        }
        binding.dragHandleView.isVisible = listener.inEditMode
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
        listener.moveCategory(fromPosition, toPosition)
    }

    class ViewHolder(val binding: OrganizerNoteVariantItemBinding) :
        AbstractDraggableItemViewHolder(
            binding.root
        )

    interface Listener {
        val inEditMode: Boolean
        var backgroundColor: Int
        fun checkCategoryExists(categoryId: Long, transform: suspend (Boolean) -> Unit)
        fun moveCategory(fromPosition: Int, toPosition: Int)
        fun removeCategory(categoryId: Long)
        fun chooseVariant(rawCariant: RawVariant, position: Int)
    }
}
