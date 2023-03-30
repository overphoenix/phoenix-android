package tech.nagual.phoenix.tools.gps.dashboard

import android.view.ViewGroup
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractDraggableItemViewHolder
import me.zhanghai.android.files.compat.foregroundCompat
import me.zhanghai.android.files.compat.isTransformedTouchPointInViewCompat
import me.zhanghai.android.files.ui.SimpleAdapter
import me.zhanghai.android.files.util.layoutInflater
import tech.nagual.phoenix.databinding.GpsIndicatorItemBinding

class GpsIndicatorsAdapter(
    private val listener: Listener
) : SimpleAdapter<GpsIndicator, GpsIndicatorsAdapter.ViewHolder>(),
    DraggableItemAdapter<GpsIndicatorsAdapter.ViewHolder> {
    override val hasStableIds: Boolean
        get() = true

    override fun getItemId(position: Int): Long = getItem(position).id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            GpsIndicatorItemBinding.inflate(parent.context.layoutInflater, parent, false)
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val indicator = getItem(position)
        val binding = holder.binding
        // Need to remove the ripple before it's drawn onto the bitmap for dragging.
        binding.root.foregroundCompat!!.mutate().setVisible(!holder.dragState.isActive, false)
        binding.root.setOnClickListener { listener.clickIndicator(indicator) }
        binding.iconImage.setImageResource(indicator.iconRes)
        binding.nameText.isActivated = indicator.isVisible
        binding.nameText.text = indicator.name
        binding.valueText.text = indicator.value
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
        listener.moveIndicator(fromPosition, toPosition)
    }

    class ViewHolder(val binding: GpsIndicatorItemBinding) : AbstractDraggableItemViewHolder(
        binding.root
    )

    interface Listener {
        fun clickIndicator(indicator: GpsIndicator)
        fun moveIndicator(fromPosition: Int, toPosition: Int)
    }
}
