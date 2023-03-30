package tech.nagual.common.ui.simplemenu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckedTextView
import androidx.recyclerview.widget.RecyclerView
import tech.nagual.common.R

internal class SimpleMenuListAdapter(private val mWindow: SimpleMenuPopupWindow) :
    RecyclerView.Adapter<SimpleMenuListAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.simple_menu_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(mWindow, position)
    }

    override fun getItemCount(): Int {
        return if (mWindow.entries == null) 0 else mWindow.entries!!.size
    }

    internal class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        var mCheckedTextView: CheckedTextView
        private var mWindow: SimpleMenuPopupWindow? = null
        fun bind(window: SimpleMenuPopupWindow?, position: Int) {
            mWindow = window
            if (window != null) {
                mCheckedTextView.text = window.entries!![position]
                mCheckedTextView.isChecked = position == window.selectedIndex
                mCheckedTextView.maxLines =
                    if (window.mode == SimpleMenuPopupWindow.DIALOG) Int.MAX_VALUE else 1
                val padding =
                    window.listPadding[window.mode]!![SimpleMenuPopupWindow.HORIZONTAL]

                val paddingVertical = mCheckedTextView.paddingTop
                mCheckedTextView.setPadding(padding, paddingVertical, padding, paddingVertical)
            }
        }

        override fun onClick(view: View) {
            mWindow!!.onItemClickListener?.onItemClick(adapterPosition)
            if (mWindow!!.isShowing) {
                mWindow!!.dismiss()
            }
        }

        init {
            mCheckedTextView = itemView.findViewById(android.R.id.text1)
            itemView.setOnClickListener(this)
        }
    }
}