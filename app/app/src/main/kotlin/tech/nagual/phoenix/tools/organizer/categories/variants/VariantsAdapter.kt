package tech.nagual.phoenix.tools.organizer.categories.variants

import android.text.TextUtils
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import me.zhanghai.android.fastscroll.PopupTextProvider
import tech.nagual.phoenix.R
import tech.nagual.phoenix.databinding.OrganizerVariantItemBinding
import tech.nagual.phoenix.tools.organizer.data.model.Variant
import me.zhanghai.android.files.ui.AnimatedListAdapter
import me.zhanghai.android.files.ui.CheckableItemBackground
import me.zhanghai.android.files.util.layoutInflater
import java.util.*

class VariantsAdapter(
    var listener: Listener,
) : AnimatedListAdapter<Variant, VariantsAdapter.ViewHolder>(DiffCallback()), PopupTextProvider {

    private val selectedVariants = variantItemSetOf()

    private val variantPositionMap = mutableMapOf<String, Int>()

    private lateinit var _nameEllipsize: TextUtils.TruncateAt
    var nameEllipsize: TextUtils.TruncateAt
        get() = _nameEllipsize
        set(value) {
            _nameEllipsize = value
            notifyItemRangeChanged(0, itemCount, PAYLOAD_STATE_CHANGED)
        }

    fun replaceSelectedVariants(variants: VariantItemSet) {
        val changedVariants = variantItemSetOf()
        val iterator = selectedVariants.iterator()
        while (iterator.hasNext()) {
            val variant = iterator.next()
            if (variant !in variants) {
                iterator.remove()
                changedVariants.add(variant)
            }
        }
        for (variant in variants) {
            if (variant !in selectedVariants) {
                selectedVariants.add(variant)
                changedVariants.add(variant)
            }
        }
        for (variant in changedVariants) {
            val position = variantPositionMap[variant.value]
            position?.let { notifyItemChanged(it, PAYLOAD_STATE_CHANGED) }
        }
    }

    fun replaceList(list: List<Variant>) {
        super.replace(list, false)
        rebuildVariantPositionMap()
    }

    private fun rebuildVariantPositionMap() {
        variantPositionMap.clear()
        for (index in 0 until itemCount) {
            val variant = getItem(index)
            variantPositionMap[variant.value] = index
        }
    }

    override fun clear() {
        super.clear()
        rebuildVariantPositionMap()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            OrganizerVariantItemBinding.inflate(parent.context.layoutInflater, parent, false)
        ).apply {
            binding.itemLayout.background =
                CheckableItemBackground.create(binding.itemLayout.context)
            popupMenu = PopupMenu(binding.menuButton.context, binding.menuButton)
                .apply { inflate(R.menu.organizer_variant_item) }
            binding.menuButton.setOnClickListener { popupMenu.show() }
        }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        throw UnsupportedOperationException()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        val variant = getItem(position)
        val binding = holder.binding
        val checked = variant in selectedVariants
        binding.itemLayout.isChecked = checked
        val nameEllipsize = nameEllipsize
        binding.nameText.ellipsize = nameEllipsize
        binding.nameText.isSelected = nameEllipsize == TextUtils.TruncateAt.MARQUEE

        if (payloads.isNotEmpty()) {
            return
        }
        bindViewHolderAnimation(holder)
        binding.itemLayout.setOnClickListener {
            if (selectedVariants.isEmpty()) {
                listener.openVariant(variant)
            } else {
                selectVariant(variant)
            }
        }
        binding.itemLayout.setOnLongClickListener {
            if (selectedVariants.isEmpty()) {
                selectVariant(variant)
            } else {
                listener.openVariant(variant)
            }
            true
        }
        binding.iconLayout.setOnClickListener { selectVariant(variant) }
        binding.iconImage.setImageResource(R.drawable.variant_icon_24dp)

        binding.nameText.text = variant.value
        binding.descriptionText.text = null

        holder.popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_edit -> {
                    listener.editVariant(variant)
                    true
                }
                R.id.action_delete -> {
                    listener.deleteVariant(variant)
                    true
                }
                else -> false
            }
        }
    }

    override fun getPopupText(position: Int): String {
        val variant = getItem(position)
        return variant.value.take(1).uppercase(Locale.getDefault())
    }

    private fun selectVariant(variant: Variant) {
        val selected = variant in selectedVariants
        listener.selectVariant(variant, !selected)
    }

    fun selectAllVariants() {
        val variants = variantItemSetOf()
        for (index in 0 until itemCount) {
            val file = getItem(index)
            variants.add(file)
        }
        listener.selectVariants(variants, true)
    }

    private class DiffCallback : DiffUtil.ItemCallback<Variant>() {
        override fun areItemsTheSame(oldItem: Variant, newItem: Variant): Boolean {
            return oldItem.value == newItem.value
        }

        override fun areContentsTheSame(oldItem: Variant, newItem: Variant): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private val PAYLOAD_STATE_CHANGED = Any()

    }

    class ViewHolder(val binding: OrganizerVariantItemBinding) : RecyclerView.ViewHolder(binding.root) {
        lateinit var popupMenu: PopupMenu
    }

    interface Listener {
        fun openVariant(variant: Variant)
        fun selectVariant(variant: Variant, selected: Boolean)
        fun selectVariants(variant: VariantItemSet, selected: Boolean)
        fun editVariant(variant: Variant)
        fun deleteVariant(variant: Variant)
    }
}
