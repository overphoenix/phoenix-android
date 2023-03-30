package tech.nagual.phoenix.tools.organizer.workflows

import android.graphics.drawable.NinePatchDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h6ah4i.android.widget.advrecyclerview.animator.DraggableItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.nagual.common.databinding.GenericAppBarLayoutBinding
import me.zhanghai.android.files.ui.OverlayToolbarActionMode
import me.zhanghai.android.files.ui.UnfilteredArrayAdapter
import me.zhanghai.android.files.util.getDrawable
import me.zhanghai.android.files.util.getTextArray
import me.zhanghai.android.files.util.viewModels
import tech.nagual.phoenix.R
import tech.nagual.phoenix.databinding.OrganizerWorkflowFragmentBinding
import tech.nagual.phoenix.tools.organizer.categories.CategoriesViewModel
import tech.nagual.phoenix.tools.organizer.common.BaseFragment
import tech.nagual.phoenix.tools.organizer.data.RawCategories
import tech.nagual.phoenix.tools.organizer.data.model.Attachment
import tech.nagual.phoenix.tools.organizer.data.model.Folder
import tech.nagual.phoenix.tools.organizer.data.model.NoteViewType
import tech.nagual.phoenix.tools.organizer.data.model.RawCategory
import tech.nagual.phoenix.tools.organizer.utils.collect
import tech.nagual.phoenix.tools.organizer.utils.viewBinding

private typealias Data = WorkflowViewModel.Data

enum class AttachmentType {
    None,
    Generic,
    Photo,
    Video,
    Audio
}

@AndroidEntryPoint
class WorkflowFragment : BaseFragment(R.layout.organizer_workflow_fragment),
    CategoriesAdapter.Listener {
    private val binding by viewBinding(OrganizerWorkflowFragmentBinding::bind)
    private val args: WorkflowFragmentArgs by navArgs()

    private val workflowViewModel: WorkflowViewModel by viewModels()
    private val categoriesViewModel: CategoriesViewModel by viewModels()

    private var data = Data()
    private var isFirstLoad: Boolean = true

    private var folders: List<Folder>? = null

    private var workflowId = -1L

    private var noteType: NoteViewType
        get() {
            val adapter = binding.noteTypeEdit.adapter
            val items = List(adapter.count) { adapter.getItem(it) as CharSequence }
            val selectedItem = binding.noteTypeEdit.text
            val selectedIndex = items.indexOfFirst { TextUtils.equals(it, selectedItem) }
            return NoteViewType.values()[selectedIndex]
        }
        set(value) {
            val adapter = binding.noteTypeEdit.adapter
            val item = adapter.getItem(value.ordinal) as CharSequence
            binding.noteTypeEdit.setText(item, false)
        }

    private var attachmentType: AttachmentType
        get() {
            val adapter = binding.attachmentsEdit.adapter
            val items = List(adapter.count) { adapter.getItem(it) as CharSequence }
            val selectedItem = binding.attachmentsEdit.text
            val selectedIndex = items.indexOfFirst { TextUtils.equals(it, selectedItem) }
            return AttachmentType.values()[selectedIndex]
        }
        set(value) {
            val adapter = binding.attachmentsEdit.adapter
            val item = adapter.getItem(value.ordinal) as CharSequence
            binding.attachmentsEdit.setText(item, false)
        }

    private lateinit var categoriesAdapter: CategoriesAdapter
    private lateinit var categoriesDragDropManager: RecyclerViewDragDropManager
    private lateinit var categoriesWrappedAdapter: RecyclerView.Adapter<*>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val appBarBinding = GenericAppBarLayoutBinding.bind(binding.root)
        appBarLayout = appBarBinding.appBarLayout
        toolbar = appBarBinding.toolbar
        toolbarTitle = args.workflowName
        super.onViewCreated(view, savedInstanceState)

        overlayActionMode = OverlayToolbarActionMode(appBarBinding.overlayToolbar)

        liftAppBarOnScrollFor(binding.scrollView)

        binding.noteTypeEdit.setAdapter(
            UnfilteredArrayAdapter(
                binding.noteTypeEdit.context, R.layout.dropdown_item,
                objects = getTextArray(R.array.organizer_note_view_types)
            )
        )
        binding.noteTypeEdit.doAfterTextChanged {
            workflowViewModel.setNoteType(noteType)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val folders = activityModel.folderRepository.getAll().first()
            this@WorkflowFragment.folders = folders
            val foldersList: MutableList<String> =
                mutableListOf(requireContext().getString(R.string.organizer_without_folder))

            folders.forEachIndexed { _, folder ->
                foldersList.add(folder.name)
            }

            withContext(Dispatchers.Main) {
                binding.folderEdit.setAdapter(
                    UnfilteredArrayAdapter(
                        binding.folderEdit.context, R.layout.dropdown_item,
                        objects = foldersList.toTypedArray()
                    )
                )
            }
        }

        binding.folderEdit.doAfterTextChanged {
            val adapter = binding.folderEdit.adapter
            val items = List(adapter.count) { adapter.getItem(it) as CharSequence }
            val selectedItem = binding.folderEdit.text
            val selectedIndex = items.indexOfFirst { TextUtils.equals(it, selectedItem) }
            workflowViewModel.setFolderId(if (selectedIndex == 0) null else folders!![selectedIndex - 1].id)
        }

        binding.attachmentsEdit.setAdapter(
            UnfilteredArrayAdapter(
                binding.noteTypeEdit.context, R.layout.dropdown_item,
                objects = getTextArray(R.array.organizer_attachment_types)
            )
        )
        binding.attachmentsEdit.doAfterTextChanged {
            workflowViewModel.setAttachmentType(
                if (attachmentType == AttachmentType.None)
                    null
                else
                    Attachment.Type.values()[attachmentType.ordinal - 1]
            )
        }

        categoriesAdapter = CategoriesAdapter(this)
        categoriesDragDropManager = RecyclerViewDragDropManager().apply {
            setDraggingItemShadowDrawable(
                getDrawable(R.drawable.ms9_composite_shadow_z2) as NinePatchDrawable
            )
        }
        categoriesDragDropManager.attachRecyclerView(binding.recyclerCategories)
        categoriesWrappedAdapter = categoriesDragDropManager.createWrappedAdapter(categoriesAdapter)

        binding.recyclerCategories.layoutManager = LinearLayoutManager(
            activity,
            RecyclerView.VERTICAL,
            false
        )
        binding.recyclerCategories.adapter = categoriesWrappedAdapter
        binding.recyclerCategories.itemAnimator = DraggableItemAnimator()

        data = Data()
        isFirstLoad = true

        if (workflowViewModel.isNotInitialized) {
            lifecycleScope.launch(Dispatchers.IO) {
                workflowId = workflowViewModel.initialize(
                    workflowId = args.workflowId,
                    workflowName = args.workflowName,
                )
            }
        }

        observeData()
    }

    override fun onPause() {
        super.onPause()

        categoriesDragDropManager.cancelDrag()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        categoriesDragDropManager.release()
        WrapperAdapterUtils.releaseAll(categoriesWrappedAdapter)
    }

    private fun observeData() = with(binding) {
        workflowViewModel.data.collect(viewLifecycleOwner) { data ->
            if (data.workflow == null && data.isInitialized) {
                return@collect run { findNavController().navigateUp() }
            }

            if (!data.isInitialized || data.workflow == null) return@collect

            this@WorkflowFragment.data = data

            noteType = data.workflow.noteViewType

            folderEdit.setText(data.folder?.name ?: getString(R.string.organizer_without_folder))

            attachmentType =
                AttachmentType.values()[if (data.workflow.attachmentType != null) data.workflow.attachmentType.ordinal + 1 else 0]

            hiddenCheck.isChecked = data.workflow.isHiddenNote
            hiddenCheck.setOnCheckedChangeListener { _, isChecked ->
                workflowViewModel.setNoteHidden(isChecked)
            }


            if (data.workflow.noteViewType == NoteViewType.Categories) {
                categoriesCardView.isVisible = true
                categoriesAdapter.replace(data.workflow.categories)
            } else {
                categoriesCardView.isVisible = false
            }

            isFirstLoad = false
        }
    }

    fun onBackPressed(): Boolean {
        if (overlayActionMode.isActive) {
            overlayActionMode.finish()
            return true
        }
        return false
    }

    override fun openCategory(rawCategory: RawCategory) {
        categoriesViewModel.openCategory(rawCategory, this)
    }

    override fun editCategory(rawCategory: RawCategory) {
        lifecycleScope.launch(Dispatchers.Main) {
            categoriesViewModel.editCategory(rawCategory, this@WorkflowFragment, workflowId)
        }
    }

    override fun moveCategory(fromPosition: Int, toPosition: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            workflowViewModel.workflowRepository.update(
                data.workflow!!.copy(
                    categories = RawCategories.moveCategory(
                        data.workflow!!.categories,
                        fromPosition,
                        toPosition
                    )
                )
            )
        }
    }

    override fun updateCategoryFlags(categoryId: Long, flags: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            workflowViewModel.workflowRepository.update(
                data.workflow!!.copy(
                    categories = RawCategories.updateFlags(
                        data.workflow!!.categories,
                        categoryId,
                        flags
                    )
                )
            )
        }
    }
}
