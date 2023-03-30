package tech.nagual.phoenix.tools.organizer.editor

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.NinePatchDrawable
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.annotation.ColorInt
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.*
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialSharedAxis
import com.h6ah4i.android.widget.advrecyclerview.animator.DraggableItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import dagger.hilt.android.AndroidEntryPoint
import io.noties.markwon.Markwon
import io.noties.markwon.editor.MarkwonEditor
import io.noties.markwon.editor.MarkwonEditorTextWatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.files.util.getDrawable
import me.zhanghai.android.files.util.getDrawableCompat
import org.commonmark.node.Code
import tech.nagual.common.extensions.*
import tech.nagual.common.permissions.Permission
import tech.nagual.common.permissions.runWithPermissions
import tech.nagual.common.ui.BottomSheet
import tech.nagual.phoenix.R
import tech.nagual.phoenix.databinding.OrganizerLayoutAttachmentBinding
import tech.nagual.phoenix.databinding.OrganizerNoteEditorFragmentBinding
import tech.nagual.phoenix.tools.organizer.OrganizersManager
import tech.nagual.phoenix.tools.organizer.attachments.dialog.AttachmentEditDescriptionDialog
import tech.nagual.phoenix.tools.organizer.attachments.fromUri
import tech.nagual.phoenix.tools.organizer.attachments.recycler.AttachmentRecyclerListener
import tech.nagual.phoenix.tools.organizer.attachments.recycler.AttachmentsAdapter
import tech.nagual.phoenix.tools.organizer.attachments.recycler.AttachmentsGridManager
import tech.nagual.phoenix.tools.organizer.attachments.uri
import tech.nagual.phoenix.tools.organizer.camera.capturer.ImageCapturer
import tech.nagual.phoenix.tools.organizer.camera.capturer.VideoCapturer
import tech.nagual.phoenix.tools.organizer.common.BaseDialog
import tech.nagual.phoenix.tools.organizer.common.BaseFragment
import tech.nagual.phoenix.tools.organizer.common.showMoveToFolderDialog
import tech.nagual.phoenix.tools.organizer.components.MediaStorageManager
import tech.nagual.phoenix.tools.organizer.data.model.*
import tech.nagual.phoenix.tools.organizer.editor.markdown.*
import tech.nagual.phoenix.tools.organizer.gallery.GalleryActivity
import tech.nagual.phoenix.tools.organizer.media.MediaActivity
import tech.nagual.phoenix.tools.organizer.recorder.RECORDED_ATTACHMENT
import tech.nagual.phoenix.tools.organizer.recorder.RECORD_CODE
import tech.nagual.phoenix.tools.organizer.recorder.RecordAudioDialog
import tech.nagual.phoenix.tools.organizer.reminders.EditReminderDialog
import tech.nagual.phoenix.tools.organizer.tasks.TaskRecyclerListener
import tech.nagual.phoenix.tools.organizer.tasks.TaskViewHolder
import tech.nagual.phoenix.tools.organizer.tasks.TasksAdapter
import tech.nagual.phoenix.tools.organizer.utils.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import javax.inject.Inject

private typealias Data = EditorViewModel.Data

@AndroidEntryPoint
class NoteEditorFragment : BaseFragment(R.layout.organizer_note_editor_fragment),
    tech.nagual.common.ui.simpledialogs.SimpleDialog.OnDialogResultListener,
    CategoriesAdapter.Listener {
    private val binding by viewBinding(OrganizerNoteEditorFragmentBinding::bind)
    val model: EditorViewModel by viewModels()

    override val inEditMode: Boolean
        get() = model.inEditMode

    private val args: NoteEditorFragmentArgs by navArgs()
    private var snackbar: Snackbar? = null
    private var mainMenu: Menu? = null
    private var contentHasFocus: Boolean = false
    private var isNoteDeleted: Boolean = false
    private var markwonTextWatcher: TextWatcher? = null
    private var isMarkwonAttachedToEditText: Boolean = false
    private var onBackPressHandled: Boolean = false

    private var attachmentsItem: MenuItem? = null
    private var noteTypeItem: MenuItem? = null

    @ColorInt
    override var backgroundColor: Int = Color.TRANSPARENT
    private var data = Data()

    private var nextTaskId: Long = 0L
    private var noteViewType: NoteViewType = NoteViewType.Text
    private var isFirstLoad: Boolean = true
    private var formatter: DateTimeFormatter? = null

    private lateinit var attachmentsAdapter: AttachmentsAdapter
    private lateinit var tasksAdapter: TasksAdapter

    private lateinit var categoriesAdapter: CategoriesAdapter
    private lateinit var categoriesDragDropManager: RecyclerViewDragDropManager
    private lateinit var categoriesWrappedAdapter: RecyclerView.Adapter<*>

    private var newExVariants = mutableListOf<Variant>()
    private var exId = 0L

    @Inject
    lateinit var markwon: Markwon

    @Inject
    lateinit var markwonEditor: MarkwonEditor

    override val hasDefaultAnimation = false

    private val requestFilesLauncher = registerForActivityResult(ChooseFilesContract) { uris ->
        if (uris.isEmpty()) return@registerForActivityResult

        val attachments = uris.map {
            requireContext().contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            Attachment.fromUri(requireContext(), it)
        }

        model.insertAttachments(*attachments.toTypedArray())
    }

    private val takePhotoLauncher = registerForActivityResult(TakePhotoContract) { saved ->
        if (!saved) return@registerForActivityResult
        val uri = activityModel.tempMediaUri ?: return@registerForActivityResult

        model.insertAttachments(Attachment.fromUri(requireContext(), uri))
        activityModel.tempMediaUri = null
        activityModel.tempMediaFile = null
    }

    private val takePhotosLauncher = registerForActivityResult(TakePhotosContract) { uris ->
        if (uris == null) return@registerForActivityResult
        val attachments: MutableList<Attachment> = mutableListOf()
        for (photoUri in uris) {
            attachments.add(Attachment.fromUri(requireContext(), photoUri))
        }
        model.insertAttachments(*attachments.toTypedArray())
    }

    private val takeVideoLauncher = registerForActivityResult(TakeVideoContract) { saved ->
        if (!saved) return@registerForActivityResult
        val uri = activityModel.tempMediaUri ?: return@registerForActivityResult

        model.insertAttachments(Attachment.fromUri(requireContext(), uri))
        activityModel.tempMediaUri = null
        activityModel.tempMediaFile = null
    }

    private val drawImageLauncher = registerForActivityResult(DrawImageContract) { saved ->
        if (!saved) return@registerForActivityResult
        val uri = activityModel.tempMediaUri ?: return@registerForActivityResult

        model.insertAttachments(Attachment.fromUri(requireContext(), uri))
        activityModel.tempMediaUri = null
        activityModel.tempMediaFile = null
    }

    private val itemTouchHelper =
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(UP or DOWN, LEFT or RIGHT) {
            override fun isLongPressDragEnabled() = false

            override fun isItemViewSwipeEnabled() = model.inEditMode

            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder) = 0.5F

            override fun getSwipeEscapeVelocity(defaultValue: Float) = 3 * defaultValue

            override fun getSwipeVelocityThreshold(defaultValue: Float) = defaultValue / 3

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                tasksAdapter.tasks.removeAt(viewHolder.bindingAdapterPosition)
                model.updateTaskList(tasksAdapter.tasks)
                tasksAdapter.notifyItemRemoved(viewHolder.bindingAdapterPosition)
                tasksAdapter.notifyItemRangeChanged(
                    viewHolder.bindingAdapterPosition,
                    tasksAdapter.tasks.size - 1
                )
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
            ): Boolean {
                tasksAdapter.moveItem(
                    viewHolder.bindingAdapterPosition,
                    target.bindingAdapterPosition
                )
                return true
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean,
            ) {
                when (actionState) {
                    ACTION_STATE_DRAG -> {
                        val top = viewHolder.itemView.top + dY
                        val bottom = top + viewHolder.itemView.height
                        if (top > 0 && bottom < recyclerView.height) {
                            super.onChildDraw(
                                c,
                                recyclerView,
                                viewHolder,
                                dX,
                                dY,
                                actionState,
                                isCurrentlyActive
                            )
                        }
                    }
                    ACTION_STATE_SWIPE -> {
                        val newDx = dX / 3
                        val p = Paint().apply {
                            color = context?.resolveAttribute(R.attr.colorTaskSwipe) ?: Color.RED
                        }
                        val itemView = viewHolder.itemView
                        val icon = context?.getDrawableCompat(R.drawable.ic_indicator_delete_task)
                            ?.toBitmap()
                        val height = itemView.bottom - itemView.top
                        val size = (24).dp(requireContext())

                        if (dX < 0) {
                            val background = RectF(
                                itemView.right.toFloat() + newDx,
                                itemView.top.toFloat(),
                                itemView.right.toFloat(),
                                itemView.bottom.toFloat()
                            )
                            c.drawRect(background, p)

                            val iconRect = RectF(
                                background.right - size - 16.dp(requireContext()),
                                background.top + (height - size) / 2,
                                background.right - 16.dp(requireContext()),
                                background.bottom - (height - size) / 2,
                            )
                            if (icon != null) c.drawBitmap(icon, null, iconRect, p)
                        } else if (dX > 0) {
                            val background = RectF(
                                itemView.left.toFloat(),
                                itemView.top.toFloat(),
                                newDx,
                                itemView.bottom.toFloat()
                            )
                            c.drawRect(background, p)
                            val iconRect = RectF(
                                background.left + 16.dp(requireContext()),
                                background.top + (height - size) / 2,
                                background.left + size + 16.dp(requireContext()),
                                background.bottom - (height - size) / 2,
                            )
                            if (icon != null) c.drawBitmap(icon, null, iconRect, p)
                        }
                        return super.onChildDraw(
                            c,
                            recyclerView,
                            viewHolder,
                            newDx,
                            dY,
                            actionState,
                            isCurrentlyActive
                        )
                    }
                }
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)

                (viewHolder as TaskViewHolder?)?.let { vh ->
                    vh.taskBackgroundColor = backgroundColor
                    vh.isBeingMoved = true
                }
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)
                (viewHolder as TaskViewHolder?)?.let {
                    if (it.isBeingMoved) it.isBeingMoved = false
                }
                model.updateTaskList(tasksAdapter.tasks)
            }
        })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.nav_host_fragment
            duration = 300L
            scrimColor = Color.TRANSPARENT

            requireContext().resolveAttribute(R.attr.colorBackground)
                ?.let { setAllContainerColors(it) }
        }
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply { duration = 300L }

        postponeEnterTransition()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        appBarLayout = binding.appBarLayout
        toolbar = binding.toolbar

        super.onViewCreated(view, savedInstanceState)

        data = Data()
        isFirstLoad = true

        if (model.isNotInitialized) {
            model.initialize(
                noteId = args.noteId,
                newNoteTitle = args.newNoteTitle,
                newNoteContent = args.newNoteContent,
                newNoteHidden = args.newNoteHidden,
                newNoteAttachments = args.newNoteAttachments?.toList() ?: emptyList(),
                newNoteRawCategories = args.newNoteRawCategories?.toList() ?: emptyList(),
                newNoteViewType = args.newNoteViewType,
                newNoteFolderId = args.newNoteFolderId.takeIf { it > 0L },
                workflowId = args.newNoteWorkflowId
            )
        }

        setupAttachmentsRecycler()
        setupTasksRecycler()
        setupCategoriesRecycler()
        observeData()
        setupEditTexts()
        setupMarkdown()
        setupListeners()

        toolbar.setTitleTextColor(Color.TRANSPARENT)
        ViewCompat.setTransitionName(binding.root, args.transitionName)
        binding.scrollView.liftAppBarOnScroll(
            binding.appBarLayout,
            requireContext().resources.getDimension(R.dimen.app_bar_elevation)
        )

        setFragmentResultListener(RECORD_CODE) { s, bundle ->
            val attachment = bundle.getParcelable<Attachment>(RECORDED_ATTACHMENT)
                ?: return@setFragmentResultListener
            model.insertAttachments(attachment)
        }

        setFragmentResultListener(MARKDOWN_DIALOG_RESULT) { s, bundle ->
            val markdown =
                bundle.getString(MARKDOWN_DIALOG_RESULT) ?: return@setFragmentResultListener
            binding.editTextContent.apply {
                if (selectedText?.isNotEmpty() == true) {
                    text?.replace(selectionStart, selectionEnd, "")
                }
                text?.insert(selectionStart, markdown)
            }
        }

        binding.fabChangeMode.setOnClickListener {
            updateEditMode(!model.inEditMode)
            if (model.inEditMode) requestFocusForFields(true) else view.hideKeyboard()
        }


    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.organizer_note, menu)

        mainMenu = menu

        lifecycleScope.launch {
            model.data.first().note?.let { setupMenuItems(it, it.reminders.isNotEmpty()) }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        data.note?.let { note ->
            when (item.itemId) {
                R.id.action_note_simple -> model.changeViewType(NoteViewType.Text)
                R.id.action_note_task_list -> model.changeViewType(NoteViewType.TaskList)
                R.id.action_note_categories -> model.changeViewType(NoteViewType.Categories)
                R.id.action_archive_note -> {
                    if (note.isArchived) activityModel.unarchiveNotes(note) else activityModel.archiveNotes(
                        note
                    )
                    sendMessage(getString(R.string.indicator_archive_note))
                    activity?.onBackPressed()
                }
                R.id.action_delete_note -> {
                    activityModel.deleteNotes(note)
                    sendMessage(getString(R.string.indicator_moved_note_to_bin))
                    activity?.onBackPressed()
                }
                R.id.action_restore_note -> {
                    activityModel.restoreNotes(note)
                    activity?.onBackPressed()
                }
                R.id.action_delete_permanently_note -> {
                    activityModel.deleteNotesPermanently(note)
                    sendMessage(getString(R.string.indicator_deleted_note_permanently))
                    activity?.onBackPressed()
                }
                R.id.action_view_tags -> findNavController().navigateSafely(
                    NoteEditorFragmentDirections.actionEditorToTags()
                        .setNoteId(note.id)
                )
                R.id.action_view_reminders -> showRemindersDialog(note)
                R.id.action_pin_note -> activityModel.pinNotes(note)
                R.id.action_hide_note -> if (note.isHidden)
                    activityModel.showNotes(note)
                else
                    activityModel.hideNotes(note)
                R.id.action_change_color -> showColorChangeDialog()
                R.id.action_export_note -> {
                    activityModel.notesToProcess = setOf(note)
                    exportNotesLauncher.launch()
                }
                R.id.action_share -> shareNote(requireContext(), note)
                R.id.action_attach_file -> requestFilesLauncher.launch()
                R.id.action_take_photo -> lifecycleScope.launch {
                    // Single mode photo
//                    val mediaFile =
//                        activityModel.createMediaFile(MediaStorageManager.MediaType.IMAGE)
//                    runWithPermissions(Permission.CAMERA) {
//                        takePhotoLauncher.launch(mediaFile)
//                    }

                    // Multi mode photo
                    runWithPermissions(Permission.CAMERA) {
                        takePhotosLauncher.launch(null)
                    }
                }
                R.id.action_take_video -> lifecycleScope.launch {
                    val mediaFile =
                        activityModel.createMediaFile(MediaStorageManager.MediaType.VIDEO)
                    runWithPermissions(Permission.CAMERA) {
                        takeVideoLauncher.launch(mediaFile)
                    }
                }
                R.id.action_record_audio -> {
                    clearFragmentResult(RECORD_CODE)
                    RecordAudioDialog().show(parentFragmentManager, null)
                }
                R.id.action_drawing -> lifecycleScope.launch {
                    val mediaFile =
                        activityModel.createMediaFile(MediaStorageManager.MediaType.IMAGE)
                    runWithPermissions(Permission.CAMERA) {
                        drawImageLauncher.launch(mediaFile)
                    }
                }
                R.id.action_enable_disable_markdown -> {
                    if (note.isMarkdownEnabled) {
                        activityModel.disableMarkdown(note)
                    } else {
                        activityModel.enableMarkdown(note)
                    }
                }
                else -> false
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        model.selectedRange = with(binding.editTextContent) { selectionStart to selectionEnd }
        super.onPause()

        categoriesDragDropManager.cancelDrag()
    }

    override fun onDestroyView() {
        // Dismiss the snackbar which is shown for deleted notes
        snackbar?.dismiss()
        itemTouchHelper.attachToRecyclerView(null)
        attachmentsAdapter.listener = null
        tasksAdapter.listener = null
        super.onDestroyView()

        categoriesDragDropManager.release()
        WrapperAdapterUtils.releaseAll(categoriesWrappedAdapter)
    }

    private fun selectNoteTypeItem() {
        mainMenu?.findItem(
            when (data.note!!.type) {
                NoteViewType.Text -> R.id.action_note_simple
                NoteViewType.TaskList -> R.id.action_note_task_list
                NoteViewType.Categories -> R.id.action_note_categories
            }
        )?.isChecked = true
    }

    private fun jumpToNextTaskOrAdd(fromPosition: Int) {
        val next = tasksAdapter.tasks.getOrNull(fromPosition + 1)
        if (next == null || next.content.isNotEmpty()) {
            addTask(fromPosition + 1)
            return
        }
        (binding.recyclerTasks.findViewHolderForAdapterPosition(fromPosition + 1) as TaskViewHolder).requestFocus()
    }

    private fun setupTasksRecycler() {
        tasksAdapter = TasksAdapter(
            false,
            object : TaskRecyclerListener {
                override fun onDrag(viewHolder: TaskViewHolder) {
                    itemTouchHelper.startDrag(viewHolder)
                }

                override fun onTaskStatusChanged(position: Int, isDone: Boolean) {
                    updateTask(position = position, isDone = isDone)
                }

                override fun onTaskContentChanged(position: Int, content: String) {
                    updateTask(position = position, content = content)
                }

                override fun onNext(position: Int) {
                    jumpToNextTaskOrAdd(position)
                }
            },
            markwon = markwon,
        )

        binding.recyclerTasks.apply {
            isVisible = true
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = tasksAdapter
            itemTouchHelper.attachToRecyclerView(this)
        }
    }

    private fun setupCategoriesRecycler() {
        categoriesAdapter = CategoriesAdapter(this)
        categoriesDragDropManager = RecyclerViewDragDropManager().apply {
            setDraggingItemShadowDrawable(
                getDrawable(R.drawable.ms9_composite_shadow_z2) as NinePatchDrawable
            )
        }
        categoriesDragDropManager.attachRecyclerView(binding.recyclerCategories)
        categoriesWrappedAdapter = categoriesDragDropManager.createWrappedAdapter(categoriesAdapter)

        binding.recyclerCategories.layoutManager = LinearLayoutManager(
            activity, RecyclerView.VERTICAL, false
        )
        binding.recyclerCategories.adapter = categoriesWrappedAdapter
        binding.recyclerCategories.itemAnimator = DraggableItemAnimator()
    }

    private fun setupAttachmentsRecycler() = with(binding) {
        // Create the adapter
        val listener = object : AttachmentRecyclerListener {
            @SuppressLint("SuspiciousIndentation")
            override fun onItemClick(position: Int, viewBinding: OrganizerLayoutAttachmentBinding) {
                val attachment = attachmentsAdapter.getItemAtPosition(position)

//                if (data.openMediaInternally) {
                    startActivity(
                        Intent(requireContext(), MediaActivity::class.java).apply {
                            putExtra(MediaActivity.ATTACHMENT, attachment)
                        }
                    )
//                } else {
//                    Intent(Intent.ACTION_VIEW).apply {
//                        data = attachment.uri(requireContext()) ?: return@apply
//                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
//                        startActivity(this)
//                    }
//                }

//                val aUri = attachment.uri(requireContext()) ?: return
//                if (VideoCapturer.isVideo(aUri) || ImageCapturer.isImage(aUri)) {
//                    val uris = arrayListOf<Uri>()
//                    val attachments = arrayListOf<Attachment>()
//                    for (a in data.note!!.attachments) {
//                        val uri = a.uri(requireContext()) ?: continue
//                        if (VideoCapturer.isVideo(uri) || ImageCapturer.isImage(uri)) {
//                            attachments.add(a)
//                            uris.add(uri)
//                        }
//                    }
//
//                    if (uris.isNotEmpty()) {
//                        val intent = Intent(requireContext(), GalleryActivity::class.java)
//                        intent.putExtra("videosOnly", false)
//                        intent.putParcelableArrayListExtra("attachments", attachments)
//                        intent.putExtra("position", position)
//                        intent.putParcelableArrayListExtra("mediaUris", uris)
//                        startActivity(intent)
//                    }
//                }
            }

            override fun onLongClick(
                position: Int,
                viewBinding: OrganizerLayoutAttachmentBinding
            ): Boolean {
                if (data.note?.isDeleted == true) return false

                data.note?.id?.let { noteId ->
                    val attachment = attachmentsAdapter.getItemAtPosition(position)

                    BottomSheet.show(attachment.description, parentFragmentManager) {
                        action(R.string.attachments_edit_description, R.drawable.ic_pencil) {
                            AttachmentEditDescriptionDialog.build(noteId, attachment.path)
                                .show(parentFragmentManager, null)
                        }
                        action(R.string.delete_action, R.drawable.ic_bin) {
                            model.deleteAttachment(attachment)
                        }
                        action(R.string.share_action, R.drawable.ic_share) {
                            shareAttachment(requireContext(), attachment)
                        }
                    }
                }
                return true
            }
        }

        attachmentsAdapter = AttachmentsAdapter(listener)
        // Configure the recycler view
        recyclerAttachments.apply {
            layoutManager = AttachmentsGridManager(requireContext())
            adapter = attachmentsAdapter
        }
    }

    private fun setMarkdownToolbarVisibility(note: Note? = data.note) = with(binding) {
        if (note == null) return@with

        containerBottomToolbar.isVisible =
            noteViewType == NoteViewType.Text && note.isMarkdownEnabled && model.inEditMode && contentHasFocus

        scrollView.updateLayoutParams<CoordinatorLayout.LayoutParams> {
            val actionBarSize = requireContext().getDimensionAttribute(R.attr.actionBarSize) ?: 0
            bottomMargin = when {
                containerBottomToolbar.isVisible -> actionBarSize
                else -> 0
            }
        }
    }

    private fun setupEditTexts() = with(binding) {
        editTextTitle.apply {
            imeOptions = EditorInfo.IME_ACTION_NEXT
            setRawInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)

            setOnEditorActionListener { v, actionId, event ->
                when {
                    actionId == EditorInfo.IME_ACTION_NEXT && data.note?.type == NoteViewType.TaskList -> {
                        jumpToNextTaskOrAdd(-1)
                        true
                    }
                    else -> false
                }
            }

            doOnTextChanged { text, start, before, count ->
                // Only listen for meaningful changes
                if (data.note == null) {
                    return@doOnTextChanged
                }

                model.setNoteTitle(text.toString().trim())
            }
        }

        editTextContent.apply {
            enableUndoRedo(this@NoteEditorFragment)
            setRawInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
            doOnTextChanged { text, start, before, count ->
                // Only listen for meaningful changes, we do not care about empty text
                if (data.note == null) {
                    return@doOnTextChanged
                }

                model.setNoteContent(text.toString().trim())
            }
            setOnFocusChangeListener { v, hasFocus ->
                contentHasFocus = hasFocus
                setMarkdownToolbarVisibility()
            }

            setOnEditorActionListener(addListItemListener)

            setOnCanUndoRedoListener { canUndo, canRedo ->
                binding.bottomToolbar.menu?.run {
                    findItem(R.id.action_undo).isEnabled = canUndo
                    findItem(R.id.action_redo).isEnabled = canRedo
                }
            }
        }

        // Used to clear focus and hide the keyboard when touching outside of the edit texts
        linearLayout.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) root.hideKeyboard()
        }
    }

    private fun setupMenuItems(note: Note, hasReminders: Boolean) = mainMenu?.run {
        findItem(R.id.action_restore_note)?.isVisible = note.isDeleted
        findItem(R.id.action_delete_permanently_note)?.isVisible = note.isDeleted
        findItem(R.id.action_delete_note)?.isVisible = !note.isDeleted
        findItem(R.id.action_view_tags)?.isVisible = !note.isDeleted
        findItem(R.id.action_change_color)?.isVisible = !note.isDeleted
        attachmentsItem = findItem(R.id.action_attachments)
        attachmentsItem?.isVisible = !note.isDeleted && model.inEditMode

        noteTypeItem = findItem(R.id.action_note_type)
        noteTypeItem?.isVisible = !note.isDeleted
        selectNoteTypeItem()

        findItem(R.id.action_pin_note)?.apply {
            isChecked = note.isPinned
            isVisible = !note.isDeleted
            title =
                if (note.isPinned) getString(R.string.unpin_title) else getString(R.string.pin_title)
        }

        findItem(R.id.action_hide_note)?.apply {
            isChecked = note.isHidden
            isVisible = !note.isDeleted
        }

        findItem(R.id.action_view_reminders)?.apply {
            setIcon(if (hasReminders) R.drawable.bell_filled_icon_24dp else R.drawable.bell_icon_24dp)
            isVisible = !note.isDeleted
        }

        findItem(R.id.action_archive_note)?.apply {
            title =
                if (note.isArchived) getString(R.string.action_unarchive) else getString(R.string.archive_action)
            isVisible = !note.isDeleted
        }

        findItem(R.id.action_enable_disable_markdown)?.apply {
            title =
                if (note.isMarkdownEnabled) getString(R.string.disable_markdown_action) else getString(
                    R.string.enable_markdown_action
                )
            isVisible = !note.isDeleted
        }

    }

    private fun observeData() = with(binding) {
        model.data.collect(viewLifecycleOwner) { data ->
            if (data.note == null && data.isInitialized) {
                return@collect run { findNavController().navigateUp() }
            }

            if (!data.isInitialized || data.note == null) return@collect

            this@NoteEditorFragment.data = data

            val isConverted = data.note.type != noteViewType
            val isMarkdownEnabled = data.note.isMarkdownEnabled
            val (dateFormat, timeFormat) = data.dateTimeFormats

            noteViewType = data.note.type
            isNoteDeleted = data.note.isDeleted

            if (isMarkdownEnabled) {
                enableMarkdownTextWatcher()
            } else {
                disableMarkdownTextWatcher()
            }

            // Update Title and Content only the first the since they are EditTexts
            if (isFirstLoad) {

                editTextTitle.withoutTextWatchers {
                    setText(data.note.title)
                }

                when (noteViewType) {
                    NoteViewType.TaskList -> tasksAdapter.submitList(data.note.taskList)
                    NoteViewType.Text -> {
                        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
                            editTextContent.withOnlyTextWatcher<MarkwonEditorTextWatcher> {
                                setText(data.note.content)
                            }
                            val (selStart, selEnd) = model.selectedRange
                            if (selStart >= 0 && selEnd <= editTextContent.length()) {
                                editTextContent.setSelection(selStart, selEnd)
                            }
                        }
                    }
                    NoteViewType.Categories -> {}
                }

                nextTaskId = data.note.taskList.map { it.id }.maxOrNull()?.plus(1) ?: 0L
            }

            // We only want to update the task list when the user converts the note from text to list
            if (isConverted) {
                tasksAdapter.tasks.clear()
                tasksAdapter.notifyDataSetChanged()
                tasksAdapter.submitList(data.note.taskList)
                editTextContent.withOnlyTextWatcher<MarkwonEditorTextWatcher> {
                    setText(data.note.content)
                }
            }
            recyclerTasks.isVisible = noteViewType == NoteViewType.TaskList

            binding.recyclerCategories.isVisible = data.note.type == NoteViewType.Categories

            updateEditMode(note = data.note)

            // Must be called after updateEditMode since that method changes the visibility of the inputs
            if (isFirstLoad) requestFocusForFields()

            // Also set text of preview textviews
            if (data.note.title.isNotEmpty())
                textViewTitlePreview.text = data.note.title

            if (isMarkdownEnabled) {
                // Seems to be crashing often without wrapping it in a post { } call
                textViewContentPreview.post {
                    markwon.applyTo(textViewContentPreview, data.note.content) {
                        tableReplacement =
                            { Code(getString(R.string.message_cannot_preview_table)) }
                        maximumTableColumns = 15
                    }
                }
            } else {
                textViewContentPreview.text = data.note.content
            }

            setupMenuItems(data.note, data.note.reminders.isNotEmpty())

            // Update folder indicator
            notebookView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                requireContext().getDrawableCompat(R.drawable.organizer_folder_icon_24dp),
                null,
                requireContext().getDrawableCompat(if (data.folder == null) R.drawable.add_icon_24dp else R.drawable.swap_icon_24dp),
                null
            )
            notebookView.text = data.folder?.name ?: getString(R.string.organizer_without_folder)

            // Update fragment background colour
            data.note.color.resId(requireContext())?.let { color ->
                backgroundColor = color
                root.setBackgroundColor(color)
                containerBottomToolbar.setBackgroundColor(color)
                toolbar.setBackgroundColor(color)
            }

            // Update date
            val offset = ZoneId.systemDefault().rules.getOffset(Instant.now())
            val creationDate = LocalDateTime.ofEpochSecond(data.note.creationDate, 0, offset)
            val modifiedDate = LocalDateTime.ofEpochSecond(data.note.modifiedDate, 0, offset)

            formatter =
                DateTimeFormatter.ofPattern(
                    "${getString(dateFormat.patternResource)}, ${
                        getString(
                            timeFormat.patternResource
                        )
                    }"
                )

            textViewDate.isVisible = data.showDates
            if (formatter != null && data.showDates) {
                textViewDate.text =
                    getString(
                        R.string.note_dates,
                        creationDate.format(formatter),
                        modifiedDate.format(formatter)
                    )
            }

            // We want to start the transition only when everything is loaded
            binding.root.doOnPreDraw {
                startPostponedEnterTransition()
            }

            if (isNoteDeleted) {
                snackbar = Snackbar.make(binding.root, "", Snackbar.LENGTH_INDEFINITE)
                    .setText(getString(R.string.indicator_deleted_note_cannot_be_edited))
                    .setAction(getString(R.string.action_restore)) { view ->
                        activityModel.restoreNotes(data.note)
                        activity?.onBackPressed()
                    }
                snackbar?.show()
                snackbar?.addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                    override fun onShown(transientBottomBar: Snackbar?) {
                        super.onShown(transientBottomBar)
                        scrollView.apply {
                            setPadding(
                                paddingLeft,
                                paddingTop,
                                paddingRight,
                                snackbar?.view?.height ?: paddingBottom
                            )
                        }
                    }
                })
            }

            // Update attachments
            attachmentsAdapter.submitList(data.note.attachments)

            // Update tags
            containerTags.removeAllViews()
            data.note.tags.forEach { tag ->
                containerTags.addView(
                    TextView(ContextThemeWrapper(requireContext(), R.style.TagChip)).apply {
                        text = "# ${tag.name}"
                    }
                )
            }

            isFirstLoad = false
        }
    }

    private fun setupListeners() = with(binding) {
        bottomToolbar.setOnMenuItemClickListener {

            val span = when (it.itemId) {
                R.id.action_insert_bold -> MarkdownSpan.BOLD
                R.id.action_insert_italics -> MarkdownSpan.ITALICS
                R.id.action_insert_strikethrough -> MarkdownSpan.STRIKETHROUGH
                R.id.action_insert_code -> MarkdownSpan.CODE
                R.id.action_insert_quote -> MarkdownSpan.QUOTE
                R.id.action_insert_heading -> MarkdownSpan.HEADING
                R.id.action_insert_link -> {
                    clearFragmentResult(MARKDOWN_DIALOG_RESULT)
                    InsertHyperlinkDialog
                        .build(editTextContent.selectedText ?: "")
                        .show(parentFragmentManager, null)
                    null
                }
                R.id.action_insert_image -> {
                    clearFragmentResult(MARKDOWN_DIALOG_RESULT)
                    InsertImageDialog
                        .build(editTextContent.selectedText ?: "")
                        .show(parentFragmentManager, null)
                    null
                }
                R.id.action_insert_table -> {
                    clearFragmentResult(MARKDOWN_DIALOG_RESULT)
                    InsertTableDialog().show(parentFragmentManager, null)
                    null
                }
                R.id.action_toggle_check_line -> {
                    editTextContent.toggleCheckmarkCurrentLine()
                    null
                }
                R.id.action_scroll_to_top -> {
                    scrollView.smoothScrollTo(0, 0)
                    editTextContent.setSelection(0)
                    null
                }
                R.id.action_scroll_to_bottom -> {
                    scrollView.smoothScrollTo(
                        0,
                        editTextContent.bottom + editTextContent.paddingBottom + editTextContent.marginBottom
                    )
                    editTextContent.setSelection(editTextContent.length())
                    null
                }
                R.id.action_undo -> {
                    editTextContent.undo()
                    null
                }
                R.id.action_redo -> {
                    editTextContent.redo()
                    null
                }
                else -> return@setOnMenuItemClickListener false
            }
            editTextContent.insertMarkdown(span ?: return@setOnMenuItemClickListener false)
            true
        }

        notebookView.setOnClickListener {
            data.note?.let { showMoveToFolderDialog(it) }
        }

        actionAddItem.setOnClickListener {
            when (data.note!!.type) {
                NoteViewType.TaskList -> addTask()
                NoteViewType.Categories -> addCategory()
                else -> {}
            }
        }
    }

    private fun setupMarkdown() {
        markwonTextWatcher = MarkwonEditorTextWatcher.withPreRender(
            markwonEditor, Executors.newCachedThreadPool(),
            binding.editTextContent
        )
    }

    private fun enableMarkdownTextWatcher() = with(binding) {
        if (markwonTextWatcher != null && !editTextContent.isMarkdownEnabled) {
            // TextWatcher is created and currently not attached to the EditText, we attach it
            editTextContent.addTextChangedListener(markwonTextWatcher)

            // Re-set text to notify the listener
            editTextContent.withOnlyTextWatcher<MarkwonEditorTextWatcher> {
                setText(text)
            }

            editTextContent.isMarkdownEnabled = true
            setMarkdownToolbarVisibility()
        }
    }

    private fun disableMarkdownTextWatcher() = with(binding) {
        if (markwonTextWatcher != null && editTextContent.isMarkdownEnabled) {
            // TextWatcher is created and currently attached to the EditText, we detach it
            editTextContent.removeTextChangedListener(markwonTextWatcher)
            val text = editTextContent.text.toString()

            editTextContent.text?.clearSpans()
            editTextContent.withoutTextWatchers {
                setText(text)
            }

            editTextContent.isMarkdownEnabled = false
            setMarkdownToolbarVisibility()
        }
    }

    override fun setupToolbar(): Unit = with(binding) {
        super.setupToolbar()
        val onBackPressedHandler = {
            if (findNavController().navigateUp()) {
                // This is needed because "Notes" label briefly appears
                // during the shared element transition when returning.
                // Todo: Needs a better fix
                toolbar.setTitleTextColor(Color.TRANSPARENT)

                // This is needed because the view jumps around
                // during the shared element transition when returning.
                // Todo: Needs a better fix
                notebookView.isVisible = false
            }
        }

        toolbar.setNavigationOnClickListener { onBackPressedHandler() }
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner) {
            if (!onBackPressHandled) {
                onBackPressedHandler()
                onBackPressHandled = true
            }
        }
    }

    private fun addCategory() {
        lifecycleScope.launch(Dispatchers.IO) {
            val essentialCategories = model.getAllEssentialCategories()
            val rawCategories = OrganizersManager.activeOrganizer.categories
            val items = mutableListOf<String>()
            val ids = mutableListOf<Long>()

            for (rawCategory in rawCategories) {
                if (essentialCategories.find { it.id == rawCategory.id } == null)
                    continue
                if (data.note!!.variants.find { variant -> variant.categoryId == rawCategory.id } == null) {
                    items += rawCategory.name
                    ids += rawCategory.id
                }
            }
            withContext(Dispatchers.Main) {
                if (items.isNotEmpty()) {
                    val extras = Bundle()
                    extras.putLongArray("ids", ids.toLongArray())
                    tech.nagual.common.ui.simpledialogs.list.SimpleListDialog.build()
                        .title(R.string.organizer_category_choose_one)
                        .choiceMode(tech.nagual.common.ui.simpledialogs.list.SimpleListDialog.SINGLE_CHOICE_DIRECT)
                        .items(items.toTypedArray())
                        .extra(extras)
                        .show(this@NoteEditorFragment, CATEGORY_CHOICE)
                } else {
                    Toast
                        .makeText(
                            requireContext(),
                            getString(R.string.category_empty_list),
                            Toast.LENGTH_SHORT
                        )
                        .show()
                }
            }
        }
    }

    private fun addTask(position: Int = tasksAdapter.tasks.size) {
        tasksAdapter.tasks.add(position, NoteTask(nextTaskId, "", false))
        tasksAdapter.notifyItemInserted(tasksAdapter.tasks.size - 1)

        if (position < tasksAdapter.tasks.size - 1) {
            tasksAdapter.notifyItemRangeChanged(position, tasksAdapter.tasks.size - position + 1)
        }

        binding.recyclerTasks.doOnNextLayout {
            (binding.recyclerTasks.findViewHolderForAdapterPosition(position) as TaskViewHolder).requestFocus()
        }

        nextTaskId += 1
        model.updateTaskList(tasksAdapter.tasks)
    }

    private fun updateTask(position: Int, content: String? = null, isDone: Boolean? = null) {
        tasksAdapter.tasks = tasksAdapter.tasks
            .mapIndexed { index, task ->
                when (index) {
                    position -> task.copy(
                        content = content ?: task.content,
                        isDone = isDone ?: task.isDone
                    )
                    else -> task
                }
            }
            .toMutableList()
        model.updateTaskList(tasksAdapter.tasks)
    }

    private fun showColorChangeDialog() {
        val selected = NoteColor.values().indexOf(data.note?.color).coerceAtLeast(0)
        val dialog = BaseDialog.build(requireContext()) {
            setTitle(getString(R.string.change_color_action))
            setSingleChoiceItems(
                NoteColor.values().map { it.localizedName }.toTypedArray(),
                selected
            ) { dialog, which ->
                model.setColor(NoteColor.values()[which])
            }
            setPositiveButton(getString(R.string.ok)) { dialog, which -> }
        }

        dialog.show()
    }

    private fun showRemindersDialog(note: Note) {
        BottomSheet.show(getString(R.string.reminders), parentFragmentManager) {
            data.note?.reminders?.forEach { reminder ->
                val offset = ZoneId.systemDefault().rules.getOffset(Instant.now())
                val reminderDate = LocalDateTime.ofEpochSecond(reminder.date, 0, offset)

                action(
                    reminder.name + " (${reminderDate.format(formatter)})",
                    R.drawable.bell_icon_24dp
                ) {
                    EditReminderDialog.build(note.id, reminder).show(parentFragmentManager, null)
                }
            }
            action(R.string.new_reminder, R.drawable.add_icon_24dp) {
                EditReminderDialog.build(note.id, null).show(parentFragmentManager, null)
            }
        }
    }

    /** Gives the focus to the editor fields if they are empty */
    private fun requestFocusForFields(forceFocus: Boolean = false) = with(binding) {
        if (editTextTitle.text.isNullOrEmpty()) {
            editTextTitle.requestFocusAndKeyboard()
        } else {
            if (editTextContent.text.isNullOrEmpty() || forceFocus) {
                editTextContent.requestFocusAndKeyboard()
            }
        }
    }

    private fun updateEditMode(inEditMode: Boolean = model.inEditMode, note: Note? = data.note) {
        val self = this

        with(binding) {
            // If the note is empty the fragment should open in edit mode by default
            val noteHasEmptyContent = when (note?.type) {
                NoteViewType.Text -> note.content.isBlank()
                NoteViewType.TaskList -> note.taskList.isEmpty()
                NoteViewType.Categories -> note.variants.isEmpty()
                else -> true
            }

            model.inEditMode = (inEditMode || noteHasEmptyContent) && !isNoteDeleted

            attachmentsItem?.isVisible = model.inEditMode
            noteTypeItem?.isVisible = !isNoteDeleted

            textViewTitlePreview.isVisible = !model.inEditMode && !note!!.title.isNullOrEmpty()
            editTextTitle.isVisible = model.inEditMode

            textViewContentPreview.isVisible =
                !model.inEditMode && noteViewType == NoteViewType.Text
            editTextContent.isVisible = model.inEditMode && noteViewType == NoteViewType.Text

            actionAddItem.isVisible = noteViewType != NoteViewType.Text && model.inEditMode
            actionAddItem.text =
                if (noteViewType == NoteViewType.TaskList) getString(R.string.action_add_task) else getString(
                    R.string.action_add_category
                )
            recyclerTasks.doOnPreDraw {
                for (pos in 0 until tasksAdapter.tasks.size) {
                    (recyclerTasks.findViewHolderForAdapterPosition(pos) as? TaskViewHolder)?.isEnabled =
                        model.inEditMode
                }
            }

            val shouldDisplayFAB = !isNoteDeleted && !noteHasEmptyContent
            when {
                fabChangeMode.isVisible == shouldDisplayFAB -> { /* FAB is already like it should be, no reason to animate */
                }
                fabChangeMode.isVisible && !shouldDisplayFAB -> fabChangeMode.hide()
                else -> fabChangeMode.show()
            }

            fabChangeMode.setImageResource(if (model.inEditMode) R.drawable.ic_show else R.drawable.ic_pencil)
            setMarkdownToolbarVisibility(note)

            // categories
            if (binding.recyclerCategories.isVisible) {
                categoriesAdapter.replace(note!!.variants)
            }
        }
    }

    override fun moveCategory(fromPosition: Int, toPosition: Int) {
        model.moveCategory(fromPosition, toPosition)
    }

    override fun removeCategory(categoryId: Long) {
        model.removeCategory(categoryId)
    }

    override fun checkCategoryExists(categoryId: Long, transform: suspend (Boolean) -> Unit) {
        model.viewModelScope.launch(Dispatchers.IO) {
            val category = model.getCategoryById(categoryId).first()
            transform(category != null)
        }
    }

    override fun chooseVariant(rawVariant: RawVariant, position: Int) {
        model.viewModelScope.launch(Dispatchers.IO) {
            val category = model.getCategoryById(rawVariant.categoryId).first()
            if (category == null) {
                withContext(Dispatchers.Main) {
                    Toast
                        .makeText(
                            requireContext(),
                            getString(R.string.category_not_exists, rawVariant.categoryName),
                            Toast.LENGTH_SHORT
                        )
                        .show()
                }
                return@launch
            }
            val variants = model.getVariantsForCategory(rawVariant.categoryId)
            if (variants.isNotEmpty()) {
                showVariantsSelectionDialog(variants, position)
            }
        }
    }

    private fun showVariantsSelectionDialog(variants: List<Variant>, position: Int) {
        val extras = Bundle()
        extras.putInt("position", position)
        tech.nagual.common.ui.simpledialogs.list.SimpleListDialog.build()
            .title(R.string.organizer_category_choose_one)
            .choiceMode(tech.nagual.common.ui.simpledialogs.list.SimpleListDialog.SINGLE_CHOICE_DIRECT)
            .items(variants.map { it.value }.toTypedArray())
            .extra(extras)
            .show(this, VARIANT_CHOICE)
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        when (which) {
            tech.nagual.common.ui.simpledialogs.SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE -> {
                if (VARIANT_CHOICE == dialogTag) {
                    val position = extras.getInt("position")
                    val variantValue = extras.getString(tech.nagual.common.ui.simpledialogs.list.SimpleListDialog.SELECTED_SINGLE_LABEL)!!
                    lifecycleScope.launch(Dispatchers.IO) {
                        val rawVariant = data.note!!.variants[position]
                        if (rawVariant.categoryType == CategoryType.ExVariants) {
                            val subVariant =
                                model.getVariantByValue(rawVariant.categoryId, variantValue)
                            newExVariants.add(subVariant!!)
                            exId = subVariant.id
                            val variants = model.getVariantsForCategory(rawVariant.categoryId, exId)
                            if (variants.isNotEmpty()) {
                                withContext(Dispatchers.Main) {
                                    showVariantsSelectionDialog(variants, position)
                                }
                            } else {
                                var child: RawChildVariant? = null
                                var prevRawChildVariant: RawChildVariant? = null
                                for (i in (1 until newExVariants.size).reversed()) {
                                    prevRawChildVariant = if (i == newExVariants.size - 1) {
                                        child = RawChildVariant(
                                            id = newExVariants[i].id,
                                            value = newExVariants[i].value
                                        )
                                        child
                                    } else
                                        RawChildVariant(
                                            id = newExVariants[i].id,
                                            value = newExVariants[i].value,
                                            child = prevRawChildVariant
                                        )
                                }

                                model.replaceVariant(
                                    rawVariant.copy(
                                        id = newExVariants[0].id,
                                        value = newExVariants[0].value,
                                        child = child
                                    )
                                )
                                newExVariants.clear()
                            }
                        } else {
                            val variant =
                                model.getVariantByValue(rawVariant.categoryId, variantValue)!!
                            model.replaceVariant(
                                rawVariant.copy(
                                    id = variant.id,
                                    value = variant.value
                                )
                            )
                        }

                    }
                    return true
                } else if (CATEGORY_CHOICE == dialogTag) {
                    val pos = extras.getInt(tech.nagual.common.ui.simpledialogs.list.SimpleListDialog.SELECTED_SINGLE_POSITION)
                    val categoryIds = extras.getLongArray("ids")!!
                    val categoryId = categoryIds[pos]
                    lifecycleScope.launch(Dispatchers.IO) {
                        model.addCategoryToNote(categoryId)
                    }
                    return true
                }
                return false
            }
        }
        return false
    }

    private val NoteColor.localizedName
        get() = getString(
            when (this) {
                NoteColor.Default -> R.string.notes_color_default
                NoteColor.Green -> R.string.notes_color_green
                NoteColor.Pink -> R.string.notes_color_pink
                NoteColor.Blue -> R.string.notes_color_blue
                NoteColor.Red -> R.string.notes_color_red
                NoteColor.Orange -> R.string.notes_color_orange
                NoteColor.Brown -> R.string.notes_color_brown
                NoteColor.Purple -> R.string.notes_color_purple
            }
        )

    companion object {
        const val MARKDOWN_DIALOG_RESULT = "MARKDOWN_DIALOG_RESULT"
        private const val CATEGORY_CHOICE = "categoryChoice"
        private const val VARIANT_CHOICE = "variantChoice"
    }
}
