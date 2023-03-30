package tech.nagual.app

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.transition.MaterialSharedAxis
import tech.nagual.common.R
import me.zhanghai.android.files.ui.CoordinatorAppBarLayout
import me.zhanghai.android.files.ui.ToolbarActionMode
import me.zhanghai.android.files.util.getBoolean
import tech.nagual.common.extensions.liftAppBarOnScroll

const val FRAGMENT_MESSAGE = "FRAGMENT_MESSAGE"

open class BaseNavigationFragment(@LayoutRes resId: Int) : Fragment(resId) {
    protected lateinit var appBarLayout: CoordinatorAppBarLayout
    protected lateinit var overlayActionMode: ToolbarActionMode

    protected open val hasMenu: Boolean = true
    protected open val hasDefaultAnimation: Boolean = true

    protected lateinit var toolbar: Toolbar
    protected lateinit var toolbarTitle: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(hasMenu)
        if (hasDefaultAnimation) {
            enterTransition =
                MaterialSharedAxis(MaterialSharedAxis.Z, true).apply { duration = 150L }
            reenterTransition =
                MaterialSharedAxis(MaterialSharedAxis.Z, true).apply { duration = 150L }
            exitTransition =
                MaterialSharedAxis(MaterialSharedAxis.Z, false).apply { duration = 150L }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
    }

    protected open fun setupToolbar() {
        val hasToolbarTitle = this::toolbarTitle.isInitialized
        (activity as BaseNavigationActivity).apply {
            setSupportActionBar(toolbar)
            setupActionBarWithNavController(navController, appBarConfiguration)
            if (hasToolbarTitle) {
                supportActionBar?.title = toolbarTitle
            }
        }
    }

    protected fun updateToolbarTitle(title: String) {
        (activity as BaseNavigationActivity).apply {
            supportActionBar?.title = title
        }
    }

    protected fun sendMessage(message: String) {
        setFragmentResult(FRAGMENT_MESSAGE, bundleOf(FRAGMENT_MESSAGE to message))
    }

    protected fun liftAppBarOnScrollFor(view: View) {
        if (getBoolean(R.bool.generic_app_bar_lift_on_scroll)) {
            // Lift app bar during scrolling
            appBarLayout.let {
                view.liftAppBarOnScroll(
                    it,
                    requireContext().resources.getDimension(R.dimen.app_bar_elevation)
                )
            }
        }
    }
}
