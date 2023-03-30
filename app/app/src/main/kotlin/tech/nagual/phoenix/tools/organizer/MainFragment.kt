package tech.nagual.phoenix.tools.organizer

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.nagual.app.application
import tech.nagual.common.databinding.GenericAppBarLayoutBinding
import tech.nagual.common.permissions.Permission
import tech.nagual.common.permissions.runWithPermissions
import me.zhanghai.android.files.ui.OverlayToolbarActionMode
import tech.nagual.common.ui.speeddial.SpeedDialActionItem
import me.zhanghai.android.files.util.getDrawable
import tech.nagual.phoenix.R
import tech.nagual.phoenix.databinding.OrganizerMainFragmentBinding
import tech.nagual.phoenix.databinding.OrganizerNoteItemBinding
import tech.nagual.phoenix.tools.organizer.attachments.fromUri
import tech.nagual.phoenix.tools.organizer.common.AbstractNotesFragment
import tech.nagual.phoenix.tools.organizer.components.MediaStorageManager
import tech.nagual.phoenix.tools.organizer.data.model.Attachment
import tech.nagual.phoenix.tools.organizer.data.model.NoteViewType
import tech.nagual.phoenix.tools.organizer.data.model.Workflow
import tech.nagual.phoenix.tools.organizer.preferences.DefaultNoteType
import tech.nagual.phoenix.tools.organizer.preferences.LayoutMode
import tech.nagual.phoenix.tools.organizer.preferences.SortMethod
import tech.nagual.phoenix.tools.organizer.utils.*

@AndroidEntryPoint
open class MainFragment : AbstractNotesFragment(R.layout.organizer_main_fragment) {
    private val binding by viewBinding(OrganizerMainFragmentBinding::bind)

    override val currentDestinationId: Int = R.id.organizer_main_fragment
    override val model: OrganizerViewModel by viewModels()

    var folderId: Long? = null

    data class InvokeData(
        val workflow: Workflow? = null,
        var attachments: List<Attachment> = listOf(),
        val sharedElement: View,
        val transitionName: String = ""
    )

    private var invokeData: InvokeData? = null

    protected lateinit var defaultNoteViewType: NoteViewType

    override val recyclerView: RecyclerView
        get() = binding.recyclerView
    override val swipeRefreshLayout: SwipeRefreshLayout
        get() = binding.layoutSwipeRefresh
    override val snackbarLayout: View
        get() = binding.fabCreateNote
    override val snackbarAnchor: View
        get() = binding.fabCreateNote
    override val emptyIndicator: TextView
        get() = binding.emptyView
    override val overlayMenuRes: Int = R.menu.organizer_selected_notes

    private val chooseFileLauncher = registerForActivityResult(ChooseFilesContract) { uris ->
        if (uris.isEmpty()) return@registerForActivityResult

        val attachments = uris.map {
            requireContext().contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            Attachment.fromUri(requireContext(), it)
        }

        invokeData!!.attachments = attachments
        goToEditor()
    }

    private val takePhotoLauncher = registerForActivityResult(TakePhotoContract) { saved ->
        if (!saved) return@registerForActivityResult

        activityModel.tempMediaUri?.toString()?.let { path ->
            invokeData!!.attachments = listOf(Attachment(Attachment.Type.PHOTO, path = path))
            goToEditor()
        }
        activityModel.tempMediaUri = null
    }

    private val takePhotosLauncher = registerForActivityResult(TakePhotosContract) { uris ->
        if (uris == null) return@registerForActivityResult
        val attachments: MutableList<Attachment> = mutableListOf()
        for (photoUri in uris) {
            attachments.add(Attachment.fromUri(requireContext(), photoUri))
        }
        invokeData!!.attachments = attachments
        goToEditor()
    }

    private val takeVideoLauncher = registerForActivityResult(TakeVideoContract) { saved ->
        if (!saved) return@registerForActivityResult

        activityModel.tempMediaUri?.toString()?.let { path ->
            invokeData!!.attachments = listOf(Attachment(Attachment.Type.VIDEO, path = path))
            goToEditor()
        }
        activityModel.tempMediaUri = null
    }

    private fun getWorkflowResId(workflowId: Long): Int = resources.getIdentifier(
        "workflow${workflowId}",
        "id",
        application.packageName.toString()
    )

    private fun runWorkflow() {
        when (invokeData!!.workflow!!.attachmentType) {
            null -> goToEditor()
            Attachment.Type.GENERIC -> chooseFileLauncher.launch()
            Attachment.Type.PHOTO -> {
                runWithPermissions(Permission.CAMERA) {
                    takePhotosLauncher.launch(null)
                }
            }
            Attachment.Type.VIDEO -> {
                runWithPermissions(Permission.CAMERA) {
                    lifecycleScope.launch {
                        takeVideoLauncher.launch(
                            activityModel.createMediaFile(MediaStorageManager.MediaType.VIDEO)
                        )
                    }
                }
            }
            else -> {}
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val appBarBinding = GenericAppBarLayoutBinding.bind(binding.root)
        appBarLayout = appBarBinding.appBarLayout
        toolbar = appBarBinding.toolbar
        toolbarTitle = OrganizersManager.activeOrganizer.name

        super.onViewCreated(view, savedInstanceState)
        overlayActionMode = OverlayToolbarActionMode(appBarBinding.overlayToolbar)

        model.initialize(folderId)

        lifecycleScope.launch(Dispatchers.IO) {
            defaultNoteViewType = when (model.preferenceRepository.get<DefaultNoteType>().first()) {
                DefaultNoteType.SIMPLE -> NoteViewType.Text
                DefaultNoteType.TASK_LIST -> NoteViewType.TaskList
                DefaultNoteType.CATEGORIES -> NoteViewType.Categories
            }
        }

        if (folderId == null) {
            activityModel.workflows.collect(viewLifecycleOwner) { workflows ->
                val activeWorkflows = mutableListOf<Workflow>()
                var i = 0
                for (workflow in workflows) {
                    if (workflow.active) {
                        activeWorkflows.add(workflow)
                        if (i++ == 10)
                            break
                    }
                }

                withContext(Dispatchers.Main) {
                    if (activeWorkflows.isEmpty()) {
                        binding.fabCreateNote.setOnClickListener {
                            invokeData = InvokeData(
                                workflow = null,
                                sharedElement = binding.fabCreateNote,
                                transitionName = TRANSITION_FAB
                            )
                            goToEditor()
                        }
                    } else if (activeWorkflows.size == 1) {
                        binding.fabCreateNote.setOnClickListener {
                            invokeData = InvokeData(
                                workflow = activeWorkflows[0],
                                sharedElement = binding.fabCreateNote,
                                transitionName = TRANSITION_FAB
                            )
                            runWorkflow()
                        }
                    } else {
                        binding.fabCreateNote.isVisible = false
                        binding.workflowsDialView.isVisible = true

                        for (workflow in activeWorkflows) {
                            binding.workflowsDialView.addActionItem(
                                tech.nagual.common.ui.speeddial.SpeedDialActionItem.Builder(
                                    getWorkflowResId(workflow.id),
                                    getDrawable(R.drawable.organizer_workflow_icon_24dp)
                                )
                                    .setLabel(workflow.name)
                                    .create()
                            )
                        }
                        binding.workflowsDialView.setOnActionSelectedListener {
                            for (workflow in activeWorkflows) {
                                if (it.id == getWorkflowResId(workflow.id)) {
                                    invokeData = InvokeData(
                                        workflow = workflow,
                                        sharedElement = binding.workflowsDialView,
                                        transitionName = TRANSITION_SPEEDDIAL
                                    )
                                    runWorkflow()
                                    break
                                }
                            }
                            binding.workflowsDialView.close()
                            true
                        }
                    }
                }
            }
        } else {
            binding.fabCreateNote.setOnClickListener {
                invokeData = InvokeData(
                    workflow = null,
                    sharedElement = binding.fabCreateNote,
                    transitionName = TRANSITION_FAB
                )
                goToEditor()
            }
        }

//        setFragmentResultListener(RECORD_CODE) { _, bundle ->
//            val attachment = bundle.getParcelable<Attachment>(RECORDED_ATTACHMENT)
//                ?: return@setFragmentResultListener
//            goToEditor(
//                attachments = listOf(attachment),
//                sharedElement = binding.fabCreateNote
//            )
//        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.organizer, menu)
        mainMenu = menu
        setHiddenNotesItemActionText()
        setLayoutChangeActionIcon()
        selectSortMethodItem()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> findNavController().navigateSafely(actionToSearch())
            R.id.action_layout_mode -> toggleLayoutMode()
            R.id.action_sort_name_asc -> activityModel.setSortMethod(SortMethod.TITLE_ASC)
            R.id.action_sort_name_desc -> activityModel.setSortMethod(SortMethod.TITLE_DESC)
            R.id.action_sort_created_asc -> activityModel.setSortMethod(SortMethod.CREATION_ASC)
            R.id.action_sort_created_desc -> activityModel.setSortMethod(SortMethod.CREATION_DESC)
            R.id.action_sort_modified_asc -> activityModel.setSortMethod(SortMethod.MODIFIED_ASC)
            R.id.action_sort_modified_desc -> activityModel.setSortMethod(SortMethod.MODIFIED_DESC)
            R.id.action_show_hidden_notes -> toggleHiddenNotes()
            R.id.action_select_all -> selectAllNotes()
            R.id.action_archived -> findNavController().navigateSafely(R.id.ogranizer_archive_fragment)
            R.id.action_deleted -> findNavController().navigateSafely(R.id.organizer_deleted_fragment)
            R.id.action_workflows -> findNavController().navigateSafely(R.id.organizer_workflows_fragment)
            R.id.action_notebooks -> findNavController().navigateSafely(R.id.organizer_manage_notebooks_fragment)
            R.id.action_categories -> findNavController().navigateSafely(R.id.organizer_categories_fragment)
            R.id.action_tags -> findNavController().navigateSafely(R.id.organizer_tags_fragment)
            R.id.action_database_settings -> findNavController().navigateSafely(R.id.organizer_settings_fragment)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setSortMethod() {

    }

    open fun actionToEditor(
        transitionName: String,
        noteId: Long
    ): NavDirections =
        MainFragmentDirections.actionMainToEditor(transitionName)
            .setNoteId(noteId)
            .setNewNoteFolderId(invokeData!!.workflow?.folderId ?: 0L)
            .setNewNoteHidden(invokeData!!.workflow?.isHiddenNote ?: false)
            .setNewNoteViewType(invokeData!!.workflow?.noteViewType ?: defaultNoteViewType)
            .setNewNoteAttachments(invokeData!!.attachments.toTypedArray())
            .setNewNoteRawCategories(invokeData!!.workflow?.categories?.toTypedArray())
            .setNewNoteWorkflowId(invokeData!!.workflow?.id ?: 0L)

    open fun actionToSearch(searchQuery: String = ""): NavDirections =
        MainFragmentDirections.actionMainToSearch()
            .setSearchQuery(searchQuery)

    override fun onNoteClick(noteId: Long, position: Int, viewBinding: OrganizerNoteItemBinding) {
        invokeData = InvokeData(
            sharedElement = viewBinding.root,
            transitionName = TRANSITION_ROOT
        )
        goToEditor(noteId, fromPosition = position)
    }

    override fun onNoteLongClick(
        noteId: Long,
        position: Int,
        viewBinding: OrganizerNoteItemBinding
    ): Boolean {
        showMenuForNote(position)
        return true
    }

    override fun onSelectionChanged(selectedIds: List<Long>) {
        super.onSelectionChanged(selectedIds)

        val inSelectionMode = selectedIds.isNotEmpty()
        binding.fabCreateNote.isVisible = !inSelectionMode
    }

    override fun onLayoutModeChanged() {
        super.onLayoutModeChanged()
        setLayoutChangeActionIcon()
    }

    override fun onSortMethodChanged() {
        super.onSortMethodChanged()
        selectSortMethodItem()
    }

    private fun goToEditor(
        noteId: Long? = null,
        fromPosition: Int? = null
    ) {
        ViewCompat.setTransitionName(invokeData!!.sharedElement, invokeData!!.transitionName)
        applyNavToEditorAnimation(fromPosition)
        when (noteId) {
            null -> {
                findNavController().navigateSafely(
                    actionToEditor(invokeData!!.transitionName, noteId = 0L),
                    FragmentNavigatorExtras(invokeData!!.sharedElement to invokeData!!.transitionName)
                )
            }
            else -> {
                findNavController().navigateSafely(
                    actionToEditor("editor_$noteId", noteId),
                    FragmentNavigatorExtras(invokeData!!.sharedElement to "editor_$noteId")
                )
            }
        }
    }

    private fun setLayoutChangeActionIcon() {
        mainMenu?.findItem(R.id.action_layout_mode)?.apply {
            isVisible = true
            setIcon(if (data.layoutMode == LayoutMode.GRID) R.drawable.ic_list else R.drawable.ic_grid)
        }
    }

    private fun selectSortMethodItem() {
        mainMenu?.findItem(
            when (data.sortMethod) {
                SortMethod.TITLE_ASC -> R.id.action_sort_name_asc
                SortMethod.TITLE_DESC -> R.id.action_sort_name_desc
                SortMethod.CREATION_ASC -> R.id.action_sort_created_asc
                SortMethod.CREATION_DESC -> R.id.action_sort_created_desc
                SortMethod.MODIFIED_ASC -> R.id.action_sort_modified_asc
                SortMethod.MODIFIED_DESC -> R.id.action_sort_modified_desc
            }
        )?.isChecked = true
    }

    companion object {
        private const val TRANSITION_ROOT = "editor_from_root"
        private const val TRANSITION_FAB = "editor_from_fab"
        private const val TRANSITION_SPEEDDIAL = "editor_from_speeddial"
    }
}
