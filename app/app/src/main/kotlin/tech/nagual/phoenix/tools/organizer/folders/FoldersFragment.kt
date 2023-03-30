package tech.nagual.phoenix.tools.organizer.folders

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
import tech.nagual.phoenix.databinding.OrganizerFoldersFragmentBinding
import tech.nagual.phoenix.tools.organizer.common.BaseFragment
import tech.nagual.phoenix.tools.organizer.data.model.Folder
import tech.nagual.phoenix.tools.organizer.utils.collect
import tech.nagual.phoenix.tools.organizer.utils.navigateSafely
import tech.nagual.phoenix.tools.organizer.utils.viewBinding
import tech.nagual.settings.Settings

@AndroidEntryPoint
class FoldersFragment : BaseFragment(R.layout.organizer_folders_fragment),
    FoldersAdapter.Listener,
    tech.nagual.common.ui.simpledialogs.SimpleDialog.OnDialogResultListener {
    private val binding by viewBinding(OrganizerFoldersFragmentBinding::bind)

    private val viewModel: FoldersViewModel by viewModels()
    private lateinit var adapter: FoldersAdapter

    private val DELETE_NOTEBOOK = "deleteNotebook"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val appBarBinding = GenericAppBarLayoutBinding.bind(binding.root)
        appBarLayout = appBarBinding.appBarLayout
        toolbar = appBarBinding.toolbar
        toolbarTitle = getString(R.string.organizer_folders_title)
        super.onViewCreated(view, savedInstanceState)

        overlayActionMode = OverlayToolbarActionMode(appBarBinding.overlayToolbar)

        binding.recyclerView.layoutManager = GridLayoutManager(activity, /* TODO */ 1)
        adapter = FoldersAdapter(this)
        binding.recyclerView.adapter = adapter
        val fastScroller = ThemedFastScroller.create(binding.recyclerView)
        binding.recyclerView.setOnApplyWindowInsetsListener(
            ScrollingViewOnApplyWindowInsetsListener(binding.recyclerView, fastScroller)
        )

        Settings.NAME_ELLIPSIZE.observe(viewLifecycleOwner) { onNameEllipsizeChanged(it) }
        viewModel.selectedNotebooksLiveData.observe(viewLifecycleOwner) {
            onSelectedNotebooksChanged(
                it
            )
        }

        activityModel.folders.collect(viewLifecycleOwner) { (_, notebooks) ->
            adapter.replaceList(notebooks)
            binding.emptyView.fadeToVisibilityUnsafe(notebooks.isEmpty())
        }

        binding.fab.setOnClickListener {
            FolderRenameDialogFragment.show(null, this)
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

    override fun selectFolder(folder: Folder, selected: Boolean) {
        viewModel.selectNotebook(folder, selected)
    }

    override fun selectFolders(notebooks: FolderItemSet, selected: Boolean) {
        viewModel.selectNotebooks(notebooks, selected)
    }

    private fun selectAllNotebooks() {
        adapter.selectAllFolders()
    }

    override fun openFolder(folder: Folder) {
        findNavController().navigateSafely(
            R.id.organizer_folder_fragment,
            bundleOf(
                "folderId" to folder.id,
                "folderName" to folder.name,
            )
        )
    }

    override fun renameFolder(folder: Folder) {
        FolderRenameDialogFragment.show(folder, this)
    }

    override fun deleteFolder(folder: Folder) {
        val extras = Bundle()
        extras.putParcelable("folder", folder)
        tech.nagual.common.ui.simpledialogs.SimpleDialog.build()
            .msg(getString(R.string.organizer_delete_notebook_ask_message, folder.name))
            .pos(R.string.yes)
            .neg(R.string.cancel)
            .extra(extras)
            .show(this, DELETE_NOTEBOOK)
    }

    private fun onNameEllipsizeChanged(nameEllipsize: TextUtils.TruncateAt) {
        adapter.nameEllipsize = nameEllipsize
    }

    private fun onOverlayActionModeFinished(toolbarActionMode: ToolbarActionMode) {
        viewModel.clearSelectedNotebooks()
    }

    private fun onSelectedNotebooksChanged(notebooks: FolderItemSet) {
        updateOverlayToolbar()
        adapter.replaceSelectedNotebooks(notebooks)
    }

    private fun onOverlayActionModeItemClicked(
        toolbarActionMode: ToolbarActionMode,
        item: MenuItem
    ): Boolean =
        when (item.itemId) {
            R.id.action_delete -> {
                viewModel.deleteFolders(viewModel.selectedNotebooks)
                true
            }
            R.id.action_select_all -> {
                selectAllNotebooks()
                true
            }
            else -> false
        }

    private fun updateOverlayToolbar() {
        val notebooks = viewModel.selectedNotebooks
        if (notebooks.isEmpty()) {
            if (overlayActionMode.isActive) {
                overlayActionMode.finish()
            }
            return
        }
        overlayActionMode.title = getString(R.string.list_select_title_format, notebooks.size)
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
        if (DELETE_NOTEBOOK == dialogTag) {
            when (which) {
                tech.nagual.common.ui.simpledialogs.SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE -> {
                    viewModel.deleteFolders(extras.getParcelable<Folder>("folder")!!)
                    return true
                }
            }
        }

        return false
    }
}
