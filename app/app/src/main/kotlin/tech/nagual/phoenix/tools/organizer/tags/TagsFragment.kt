package tech.nagual.phoenix.tools.organizer.tags

import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import tech.nagual.common.databinding.GenericAppBarLayoutBinding
import me.zhanghai.android.files.ui.OverlayToolbarActionMode
import me.zhanghai.android.files.ui.ScrollingViewOnApplyWindowInsetsListener
import me.zhanghai.android.files.ui.ThemedFastScroller
import me.zhanghai.android.files.ui.ToolbarActionMode
import me.zhanghai.android.files.util.fadeToVisibilityUnsafe
import tech.nagual.phoenix.R
import tech.nagual.phoenix.databinding.OrganizerTagsFragmentBinding
import tech.nagual.phoenix.tools.organizer.common.BaseFragment
import tech.nagual.phoenix.tools.organizer.data.model.Tag
import tech.nagual.phoenix.tools.organizer.utils.collect
import tech.nagual.phoenix.tools.organizer.utils.navigateSafely
import tech.nagual.phoenix.tools.organizer.utils.viewBinding
import tech.nagual.settings.Settings

@AndroidEntryPoint
class TagsFragment : BaseFragment(R.layout.organizer_tags_fragment), TagsAdapter.Listener {
    private val binding by viewBinding(OrganizerTagsFragmentBinding::bind)

    private val args: TagsFragmentArgs by navArgs()
    private val viewModel: TagsViewModel by viewModels()
    private lateinit var adapter: TagsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val appBarBinding = GenericAppBarLayoutBinding.bind(binding.root)
        appBarLayout = appBarBinding.appBarLayout
        toolbar = appBarBinding.toolbar
        toolbarTitle = getString(R.string.tags_title)
        super.onViewCreated(view, savedInstanceState)

        overlayActionMode = OverlayToolbarActionMode(appBarBinding.overlayToolbar)

        binding.recyclerView.layoutManager = GridLayoutManager(activity, /* TODO */ 1)
        adapter = TagsAdapter(this)
        binding.recyclerView.adapter = adapter
        val fastScroller = ThemedFastScroller.create(binding.recyclerView)
        binding.recyclerView.setOnApplyWindowInsetsListener(
            ScrollingViewOnApplyWindowInsetsListener(binding.recyclerView, fastScroller)
        )

        Settings.NAME_ELLIPSIZE.observe(viewLifecycleOwner) { onNameEllipsizeChanged(it) }
        viewModel.selectedTagsLiveData.observe(viewLifecycleOwner) {
            onSelectedTagsChanged(
                it
            )
        }

        viewModel
            .getData(args.noteId.takeIf { it >= 0L })
            .collect(viewLifecycleOwner) { tags ->
                adapter.replaceList(tags)
                binding.emptyView.fadeToVisibilityUnsafe(tags.isEmpty())
            }

        binding.fab.setOnClickListener {
            TagRenameDialogFragment.show(null, this)
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

    override fun selectTag(tagData: TagData, selected: Boolean) {
        viewModel.selectTag(tagData, selected)
    }

    override fun selectTags(tags: TagItemSet, selected: Boolean) {
        viewModel.selectTags(tags, selected)
    }

    private fun selectAllTags() {
        adapter.selectAllTags()
    }

    override fun isNoteAttached() = args.noteId > 0L

    override fun itemClicked(tagData: TagData) {
        if (args.noteId > 0L) {
            if (tagData.inNote) {
                viewModel.deleteTagFromNote(tagData.tag.id, args.noteId)
            } else {
                viewModel.addTagToNote(tagData.tag.id, args.noteId)
            }
        } else {
            if (viewModel.selectedTags.isEmpty()) {
                openTag(tagData.tag)
            } else {
                adapter.selectTag(tagData)
            }
        }
    }

    override fun itemLongClicked(tagData: TagData) {
        if (viewModel.selectedTags.isEmpty()) {
            adapter.selectTag(tagData)
        } else {
            openTag(tagData.tag)
        }
    }

    private fun openTag(tag: Tag) {
        findNavController()
            .navigateSafely(
                TagsFragmentDirections.actionTagsToSearch().setSearchQuery(tag.name)
            )
    }

    override fun renameTag(tag: Tag) {
        TagRenameDialogFragment.show(tag, this)
    }

    override fun deleteTag(tag: Tag) {
        viewModel.deleteTags(tag)
    }

    private fun onNameEllipsizeChanged(nameEllipsize: TextUtils.TruncateAt) {
        adapter.nameEllipsize = nameEllipsize
    }

    private fun onOverlayActionModeFinished(toolbarActionMode: ToolbarActionMode) {
        viewModel.clearSelectedTags()
    }

    private fun onSelectedTagsChanged(tags: TagItemSet) {
        updateOverlayToolbar()
        adapter.replaceSelectedTags(tags)
    }

    private fun onOverlayActionModeItemClicked(
        toolbarActionMode: ToolbarActionMode,
        item: MenuItem
    ): Boolean =
        when (item.itemId) {
            R.id.action_delete -> {
                viewModel.deleteTags(viewModel.selectedTags)
                true
            }
            R.id.action_select_all -> {
                selectAllTags()
                true
            }
            else -> false
        }

    private fun updateOverlayToolbar() {
        val tags = viewModel.selectedTags
        if (tags.isEmpty()) {
            if (overlayActionMode.isActive) {
                overlayActionMode.finish()
            }
            return
        }
        overlayActionMode.title = getString(R.string.list_select_title_format, tags.size)
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
}
