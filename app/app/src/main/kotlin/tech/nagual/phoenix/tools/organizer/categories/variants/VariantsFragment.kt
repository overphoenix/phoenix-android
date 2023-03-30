package tech.nagual.phoenix.tools.organizer.categories.variants

import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
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
import tech.nagual.phoenix.databinding.OrganizerVariantsFragmentBinding
import tech.nagual.phoenix.tools.gps.GpsManager
import tech.nagual.phoenix.tools.organizer.common.BaseFragment
import tech.nagual.phoenix.tools.organizer.data.model.CategoryType
import tech.nagual.phoenix.tools.organizer.data.model.Variant
import tech.nagual.phoenix.tools.organizer.utils.collect
import tech.nagual.phoenix.tools.organizer.utils.viewBinding
import tech.nagual.phoenix.tools.organizer.OrganizersManager
import tech.nagual.phoenix.tools.organizer.utils.navigateSafely
import tech.nagual.settings.Settings

@AndroidEntryPoint
class VariantsFragment : BaseFragment(R.layout.organizer_variants_fragment),
    VariantsAdapter.Listener {
    private val binding by viewBinding(OrganizerVariantsFragmentBinding::bind)

    private val args: VariantsFragmentArgs by navArgs()

    private val viewModel: VariantsViewModel by viewModels()
    private lateinit var adapter: VariantsAdapter

    private val variationsTitle: String
        get() = if (args.parentName.isNullOrEmpty()) args.categoryName else args.parentName

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val appBarBinding = GenericAppBarLayoutBinding.bind(binding.root)
        appBarLayout = appBarBinding.appBarLayout
        toolbar = appBarBinding.toolbar
        toolbarTitle = getString(R.string.category_variants_title, variationsTitle)
        super.onViewCreated(view, savedInstanceState)

        overlayActionMode = OverlayToolbarActionMode(appBarBinding.overlayToolbar)

        binding.recyclerView.layoutManager = GridLayoutManager(activity, /* TODO */ 1)
        adapter = VariantsAdapter(this)
        binding.recyclerView.adapter = adapter
        val fastScroller = ThemedFastScroller.create(binding.recyclerView)
        binding.recyclerView.setOnApplyWindowInsetsListener(
            ScrollingViewOnApplyWindowInsetsListener(binding.recyclerView, fastScroller)
        )

        Settings.NAME_ELLIPSIZE.observe(viewLifecycleOwner) { onNameEllipsizeChanged(it) }
        viewModel.selectedVariantsLiveData.observe(viewLifecycleOwner) {
            onSelectedVariantsChanged(
                it
            )
        }

        binding.emptyView.setCompoundDrawablesWithIntrinsicBounds(
            0,
            OrganizersManager.getCategoryTypeLargeIconRes(args.categoryType),
            0,
            0
        )

        viewModel.variants(args.categoryId, args.parentId).collect(viewLifecycleOwner) { variants ->
            adapter.replaceList(variants)
            binding.emptyView.fadeToVisibilityUnsafe(variants.isEmpty())
        }

        if (args.categoryType == CategoryType.Geo && !GpsManager.getInstance().isServiceStarted)
            binding.fab.isVisible = false

        binding.fab.setOnClickListener {
            when (args.categoryType) {
                CategoryType.Variants,
                CategoryType.ExVariants -> VariantEditDialogFragment.show(
                    null,
                    args.categoryId,
                    args.parentId,
                    this
                )
                CategoryType.AutoIncrement -> viewModel.createAutoIncrementVariant(args.categoryId)
                CategoryType.Geo -> {
                    val gpsManager = GpsManager.getInstance()
                    if (gpsManager.isServiceStarted) {
                        viewModel.createGeoVariant(args.categoryId, gpsManager.currentBestLocation)
                    }
                }
                else -> {}
            }
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

    override fun selectVariant(variant: Variant, selected: Boolean) {
        viewModel.selectVariant(variant, selected)
    }

    override fun selectVariants(variants: VariantItemSet, selected: Boolean) {
        viewModel.selectVariants(variants, selected)
    }

    private fun selectAllVariants() {
        adapter.selectAllVariants()
    }

    override fun openVariant(variant: Variant) {
        if (args.categoryType == CategoryType.ExVariants) {
            findNavController().navigateSafely(
                R.id.organizer_manage_variants_fragment,
                bundleOf(
                    "categoryId" to args.categoryId,
                    "categoryType" to args.categoryType,
                    "categoryName" to variationsTitle,
                    "parentId" to variant.id,
                    "parentName" to variant.value
                )
            )
        }
    }

    override fun editVariant(variant: Variant) {
        VariantEditDialogFragment.show(variant, args.categoryId, args.parentId, this)
    }

    override fun deleteVariant(variant: Variant) {
        viewModel.deleteVariants(variant)
    }

    private fun onNameEllipsizeChanged(nameEllipsize: TextUtils.TruncateAt) {
        adapter.nameEllipsize = nameEllipsize
    }

    private fun onOverlayActionModeFinished(toolbarActionMode: ToolbarActionMode) {
        viewModel.clearSelectedVariants()
    }

    private fun onSelectedVariantsChanged(variantItemSet: VariantItemSet) {
        updateOverlayToolbar()
        adapter.replaceSelectedVariants(variantItemSet)
    }

    private fun onOverlayActionModeItemClicked(
        toolbarActionMode: ToolbarActionMode,
        item: MenuItem
    ): Boolean =
        when (item.itemId) {
            R.id.action_delete -> {
                viewModel.deleteVariants(viewModel.selectedVariants)
                true
            }
            R.id.action_select_all -> {
                selectAllVariants()
                true
            }
            else -> false
        }

    private fun updateOverlayToolbar() {
        val variants = viewModel.selectedVariants
        if (variants.isEmpty()) {
            if (overlayActionMode.isActive) {
                overlayActionMode.finish()
            }
            return
        }
        overlayActionMode.title = getString(R.string.list_select_title_format, variants.size)
        overlayActionMode.setMenuResource(R.menu.organizer_variant_select)

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
