package tech.nagual.phoenix.tools.organizer.workflows

import android.text.TextUtils
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import tech.nagual.app.application
import me.zhanghai.android.files.ui.AnimatedListAdapter
import me.zhanghai.android.files.ui.CheckableItemBackground
import me.zhanghai.android.fastscroll.PopupTextProvider
import me.zhanghai.android.foregroundcompat.ForegroundImageButton
import me.zhanghai.android.files.util.layoutInflater
import tech.nagual.phoenix.R
import tech.nagual.phoenix.databinding.OrganizerWorkflowItemBinding
import tech.nagual.phoenix.tools.organizer.data.model.Workflow
import java.util.*

class WorkflowsAdapter(
    var listener: Listener,
) : AnimatedListAdapter<Workflow, WorkflowsAdapter.ViewHolder>(DiffCallback()), PopupTextProvider {

    private val selectedWorkflows = workflowItemSetOf()

    private val workflowPositionMap = mutableMapOf<String, Int>()

    private lateinit var _nameEllipsize: TextUtils.TruncateAt
    var nameEllipsize: TextUtils.TruncateAt
        get() = _nameEllipsize
        set(value) {
            _nameEllipsize = value
            notifyItemRangeChanged(0, itemCount, PAYLOAD_STATE_CHANGED)
        }

    fun replaceSelectedNotebooks(workflows: WorkflowItemSet) {
        val changedWorkflows = workflowItemSetOf()
        val iterator = selectedWorkflows.iterator()
        while (iterator.hasNext()) {
            val workflow = iterator.next()
            if (workflow !in workflows) {
                iterator.remove()
                changedWorkflows.add(workflow)
            }
        }
        for (workflow in workflows) {
            if (workflow !in selectedWorkflows) {
                selectedWorkflows.add(workflow)
                changedWorkflows.add(workflow)
            }
        }
        for (workflow in changedWorkflows) {
            val position = workflowPositionMap[workflow.name]
            position?.let { notifyItemChanged(it, PAYLOAD_STATE_CHANGED) }
        }
    }

    fun replaceList(list: List<Workflow>) {
        super.replace(list, false)
        rebuildWorkflowPositionMap()
    }

    private fun rebuildWorkflowPositionMap() {
        workflowPositionMap.clear()
        for (index in 0 until itemCount) {
            val workflow = getItem(index)
            workflowPositionMap[workflow.name] = index
        }
    }

    override fun clear() {
        super.clear()
        rebuildWorkflowPositionMap()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            OrganizerWorkflowItemBinding.inflate(parent.context.layoutInflater, parent, false)
        ).apply {
            binding.itemLayout.background =
                CheckableItemBackground.create(binding.itemLayout.context)
            popupMenu = PopupMenu(binding.menuButton.context, binding.menuButton)
                .apply { inflate(R.menu.organizer_workflow_item) }
            binding.menuButton.setOnClickListener { popupMenu.show() }
            startButton = binding.startButton
        }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        throw UnsupportedOperationException()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        val workflow = getItem(position)
        val binding = holder.binding
        val checked = workflow in selectedWorkflows
        binding.itemLayout.isChecked = checked
        val nameEllipsize = nameEllipsize
        binding.nameText.ellipsize = nameEllipsize
        binding.nameText.isSelected = nameEllipsize == TextUtils.TruncateAt.MARQUEE
        if (payloads.isNotEmpty()) {
            return
        }
        bindViewHolderAnimation(holder)
        binding.itemLayout.setOnClickListener {
            if (selectedWorkflows.isEmpty()) {
                listener.openWorkflow(workflow)
            } else {
                selectWorkflow(workflow)
            }
        }
        binding.itemLayout.setOnLongClickListener {
            if (selectedWorkflows.isEmpty()) {
                selectWorkflow(workflow)
            } else {
                listener.openWorkflow(workflow)
            }
            true
        }
        binding.iconLayout.setOnClickListener { selectWorkflow(workflow) }
        binding.iconImage.setImageResource(R.drawable.organizer_workflow_icon_24dp)

        binding.nameText.isActivated = workflow.active
        binding.nameText.text = workflow.name
        binding.descriptionText.text = null

        holder.startButton.setImageDrawable(
            application.getDrawable(
                if (workflow.active) R.drawable.stop_icon_24dp
                else R.drawable.start_icon_24dp
            )
        )
        holder.startButton.setOnClickListener {
            listener.updateWorkflowState(workflow)
        }

        holder.popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_edit -> {
                    true
                }
                R.id.action_rename -> {
                    listener.renameWorkflow(workflow)
                    true
                }
                R.id.action_delete -> {
                    listener.deleteWorkflow(workflow)
                    true
                }
                else -> false
            }
        }
    }

    override fun getPopupText(position: Int): String {
        val workflow = getItem(position)
        return workflow.name.take(1).uppercase(Locale.getDefault())
    }

    private fun selectWorkflow(workflow: Workflow) {
        val selected = workflow in selectedWorkflows
        listener.selectWorkflow(workflow, !selected)
    }

    fun selectAllWorkflows() {
        val workflows = workflowItemSetOf()
        for (index in 0 until itemCount) {
            val workflow = getItem(index)
            workflows.add(workflow)
        }
        listener.selectWorkflows(workflows, true)
    }

    private class DiffCallback : DiffUtil.ItemCallback<Workflow>() {
        override fun areItemsTheSame(oldItem: Workflow, newItem: Workflow): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Workflow, newItem: Workflow): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private val PAYLOAD_STATE_CHANGED = Any()
    }

    class ViewHolder(val binding: OrganizerWorkflowItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        lateinit var popupMenu: PopupMenu
        lateinit var startButton: ForegroundImageButton
    }

    interface Listener {
        fun updateWorkflowState(workflow: Workflow)
        fun openWorkflow(workflow: Workflow)
        fun selectWorkflow(workflow: Workflow, selected: Boolean)
        fun selectWorkflows(workflow: WorkflowItemSet, selected: Boolean)
        fun renameWorkflow(workflow: Workflow)
        fun deleteWorkflow(workflow: Workflow)
    }
}
