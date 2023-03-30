package tech.nagual.phoenix.tools.organizer.workflows

import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import tech.nagual.common.databinding.GenericAppBarLayoutBinding
import me.zhanghai.android.files.ui.OverlayToolbarActionMode
import me.zhanghai.android.files.ui.ScrollingViewOnApplyWindowInsetsListener
import me.zhanghai.android.files.ui.ThemedFastScroller
import me.zhanghai.android.files.ui.ToolbarActionMode
import tech.nagual.common.ui.simpledialogs.SimpleDialog
import me.zhanghai.android.files.util.fadeToVisibilityUnsafe
import tech.nagual.phoenix.R
import tech.nagual.phoenix.databinding.OrganizerWorkflowsFragmentBinding
import tech.nagual.phoenix.tools.organizer.common.BaseFragment
import tech.nagual.phoenix.tools.organizer.data.model.Workflow
import tech.nagual.phoenix.tools.organizer.utils.collect
import tech.nagual.phoenix.tools.organizer.utils.navigateSafely
import tech.nagual.phoenix.tools.organizer.utils.viewBinding
import tech.nagual.settings.Settings

@AndroidEntryPoint
class WorkflowsFragment : BaseFragment(R.layout.organizer_workflows_fragment),
    WorkflowsAdapter.Listener,
    tech.nagual.common.ui.simpledialogs.SimpleDialog.OnDialogResultListener,
    WorkflowEditDialogFragment.Listener {
    private val binding by viewBinding(OrganizerWorkflowsFragmentBinding::bind)

    private val viewModel: WorkflowsViewModel by viewModels()
    private lateinit var adapter: WorkflowsAdapter

    private val DELETE_WORKFLOW = "deleteNotebook"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val appBarBinding = GenericAppBarLayoutBinding.bind(binding.root)
        appBarLayout = appBarBinding.appBarLayout
        toolbar = appBarBinding.toolbar
        toolbarTitle = getString(R.string.organizer_workflows_title)
        super.onViewCreated(view, savedInstanceState)

        overlayActionMode = OverlayToolbarActionMode(appBarBinding.overlayToolbar)

        binding.recyclerView.layoutManager = GridLayoutManager(activity, /* TODO */ 1)
        adapter = WorkflowsAdapter(this)
        binding.recyclerView.adapter = adapter
        val fastScroller = ThemedFastScroller.create(binding.recyclerView)
        binding.recyclerView.setOnApplyWindowInsetsListener(
            ScrollingViewOnApplyWindowInsetsListener(binding.recyclerView, fastScroller)
        )

        Settings.NAME_ELLIPSIZE.observe(viewLifecycleOwner) { onNameEllipsizeChanged(it) }
        viewModel.selectedWorkflowsLiveData.observe(viewLifecycleOwner) {
            onSelectedNotebooksChanged(
                it
            )
        }

        activityModel.workflows.collect(viewLifecycleOwner) { workflows ->
            adapter.replaceList(workflows)
            binding.emptyView.fadeToVisibilityUnsafe(workflows.isEmpty())
        }

        binding.fab.setOnClickListener {
            WorkflowEditDialogFragment.show(null, this)
        }

        liftAppBarOnScrollFor(binding.recyclerView)
    }

    fun onBackPressed(): Boolean {
        if (overlayActionMode.isActive) {
            overlayActionMode.finish()
            return true
        }
        return false
    }

    override fun selectWorkflow(workflow: Workflow, selected: Boolean) {
        viewModel.selectWorkflow(workflow, selected)
    }

    override fun selectWorkflows(workflows: WorkflowItemSet, selected: Boolean) {
        viewModel.selectWorkflows(workflows, selected)
    }

    private fun selectAllWorkflows() {
        adapter.selectAllWorkflows()
    }

    override fun updateWorkflowState(workflow: Workflow) {
        viewModel.updateWorkflowState(workflow)
    }

    override fun openWorkflow(workflow: Workflow) {
        findNavController().navigateSafely(
            R.id.organizer_workflow_fragment,
            bundleOf(
                "workflowId" to workflow.id,
                "workflowName" to workflow.name,
            )
        )
    }

    override fun createWorkflow(workflowName: String) {
        findNavController().navigateSafely(
            R.id.organizer_workflow_fragment,
            bundleOf(
                "workflowName" to workflowName,
            )
        )
    }

    override fun renameWorkflow(workflow: Workflow) {
        WorkflowEditDialogFragment.show(workflow, this)
    }

    override fun deleteWorkflow(workflow: Workflow) {
        val extras = Bundle()
        extras.putParcelable("workflow", workflow)
        tech.nagual.common.ui.simpledialogs.SimpleDialog.build()
            .msg(getString(R.string.organizer_delete_workflow_ask_message, workflow.name))
            .pos(R.string.yes)
            .neg(R.string.cancel)
            .extra(extras)
            .show(this, DELETE_WORKFLOW)
    }

    private fun onNameEllipsizeChanged(nameEllipsize: TextUtils.TruncateAt) {
        adapter.nameEllipsize = nameEllipsize
    }

    private fun onOverlayActionModeFinished(toolbarActionMode: ToolbarActionMode) {
        viewModel.clearSelectedWorkflows()
    }

    private fun onSelectedNotebooksChanged(workflows: WorkflowItemSet) {
        updateOverlayToolbar()
        adapter.replaceSelectedNotebooks(workflows)
    }

    private fun onOverlayActionModeItemClicked(
        toolbarActionMode: ToolbarActionMode,
        item: MenuItem
    ): Boolean =
        when (item.itemId) {
            R.id.action_delete -> {
                viewModel.deleteWorkflows(viewModel.selectedWorkflows)
                true
            }
            R.id.action_select_all -> {
                selectAllWorkflows()
                true
            }
            else -> false
        }

    private fun updateOverlayToolbar() {
        val workflows = viewModel.selectedWorkflows
        if (workflows.isEmpty()) {
            if (overlayActionMode.isActive) {
                overlayActionMode.finish()
            }
            return
        }
        overlayActionMode.title = getString(R.string.list_select_title_format, workflows.size)
        overlayActionMode.setMenuResource(R.menu.organizer_default_menu)

        if (!overlayActionMode.isActive) {
            appBarLayout.setExpanded(true)
            overlayActionMode.start(object : ToolbarActionMode.Callback {
                override fun onToolbarActionModeStarted(toolbarActionMode: ToolbarActionMode) {}

                override fun onToolbarActionModeItemClicked(
                    toolbarActionMode: ToolbarActionMode,
                    item: MenuItem
                ): Boolean = onOverlayActionModeItemClicked(toolbarActionMode, item)

                override fun onToolbarActionModeFinished(toolbarActionMode: ToolbarActionMode) {
                    onOverlayActionModeFinished(toolbarActionMode)
                }
            })
        }
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if (DELETE_WORKFLOW == dialogTag) {
            when (which) {
                tech.nagual.common.ui.simpledialogs.SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE -> {
                    viewModel.deleteWorkflows(extras.getParcelable<Workflow>("workflow")!!)
                    return true
                }
            }
        }

        return false
    }
}
