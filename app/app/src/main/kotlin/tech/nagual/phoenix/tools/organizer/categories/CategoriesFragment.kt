package tech.nagual.phoenix.tools.organizer.categories

import android.graphics.drawable.NinePatchDrawable
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h6ah4i.android.widget.advrecyclerview.animator.DraggableItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tech.nagual.common.databinding.GenericAppBarLayoutBinding
import me.zhanghai.android.files.ui.OverlayToolbarActionMode
import me.zhanghai.android.files.util.fadeToVisibilityUnsafe
import me.zhanghai.android.files.util.getDrawable
import tech.nagual.phoenix.R
import tech.nagual.phoenix.databinding.OrganizerCategoriesFragmentBinding
import tech.nagual.phoenix.tools.organizer.OrganizersManager
import tech.nagual.phoenix.tools.organizer.common.BaseFragment
import tech.nagual.phoenix.tools.organizer.data.RawCategories
import tech.nagual.phoenix.tools.organizer.data.model.CategoryType
import tech.nagual.phoenix.tools.organizer.data.model.RawCategory
import tech.nagual.phoenix.tools.organizer.utils.collect
import tech.nagual.phoenix.tools.organizer.utils.viewBinding

@AndroidEntryPoint
class CategoriesFragment : BaseFragment(R.layout.organizer_categories_fragment),
    CategoriesAdapter.Listener {
    private val binding by viewBinding(OrganizerCategoriesFragmentBinding::bind)

    private lateinit var adapter: CategoriesAdapter
    private lateinit var dragDropManager: RecyclerViewDragDropManager
    private lateinit var wrappedAdapter: RecyclerView.Adapter<*>

    private val viewModel: CategoriesViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val appBarBinding = GenericAppBarLayoutBinding.bind(binding.root)
        appBarLayout = appBarBinding.appBarLayout
        toolbar = appBarBinding.toolbar
        toolbarTitle = getString(R.string.categories_title)
        super.onViewCreated(view, savedInstanceState)

        overlayActionMode = OverlayToolbarActionMode(appBarBinding.overlayToolbar)

        liftAppBarOnScrollFor(binding.recyclerView)

        adapter = CategoriesAdapter(this)
        dragDropManager = RecyclerViewDragDropManager().apply {
            setDraggingItemShadowDrawable(
                getDrawable(R.drawable.ms9_composite_shadow_z2) as NinePatchDrawable
            )
        }
        dragDropManager.attachRecyclerView(binding.recyclerView)
        wrappedAdapter = dragDropManager.createWrappedAdapter(adapter)

        binding.recyclerView.layoutManager = LinearLayoutManager(
            activity, RecyclerView.VERTICAL, false
        )
        binding.recyclerView.adapter = wrappedAdapter
        binding.recyclerView.itemAnimator = DraggableItemAnimator()

        OrganizersManager.getInstance().organizerFlow.collect(viewLifecycleOwner) { organizer ->
            if (organizer == null) return@collect

            binding.emptyView.fadeToVisibilityUnsafe(organizer.categories.isEmpty())
            adapter.replace(organizer.categories)
        }

        binding.categoriesDialView.inflate(R.menu.categories_speed_dial)
        binding.categoriesDialView.setOnActionSelectedListener {
            when (it.id) {
                R.id.action_variants -> CategoryVariantsEditDialogFragment.show(
                    null,
                    CategoryType.Variants,
                    this
                )
                R.id.action_complex_variants -> CategoryVariantsEditDialogFragment.show(
                    null,
                    CategoryType.ExVariants,
                    this
                )
                R.id.action_autoincrement -> CategoryAutoincrementEditDialogFragment.show(
                    null,
                    CategoryType.AutoIncrement,
                    this
                )
                R.id.action_geo -> CategoryGeoEditDialogFragment.show(
                    null,
                    CategoryType.Geo,
                    this
                )
//                R.id.action_datetime -> CategoryDatetimeEditDialogFragment.show(
//                    null,
//                    CategoryType.DateTime,
//                    this
//                )
//                R.id.action_password -> CategoryPasswordEditDialogFragment.show(
//                    null,
//                    CategoryType.Password,
//                    this
//                )
            }
            binding.categoriesDialView.close()
            true
        }
    }

    override fun onPause() {
        super.onPause()

        dragDropManager.cancelDrag()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        dragDropManager.release()
        WrapperAdapterUtils.releaseAll(wrappedAdapter)
    }

    override fun openCategory(rawCategory: RawCategory) {
        viewModel.openCategory(rawCategory, this)
    }

    override fun editCategory(rawCategory: RawCategory) {
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.editCategory(rawCategory, this@CategoriesFragment)
        }
    }

    override fun moveCategory(fromPosition: Int, toPosition: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            OrganizersManager.getInstance().organizerRepository.update(
                OrganizersManager.activeOrganizer.copy(
                    categories = RawCategories.moveCategory(
                        OrganizersManager.activeOrganizer.categories,
                        fromPosition,
                        toPosition
                    )
                )
            )
        }
    }
}
