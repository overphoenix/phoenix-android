package tech.nagual.common.preferences

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import tech.nagual.common.R
import tech.nagual.common.databinding.PreferenceFragmentBinding

abstract class PreferenceFragment : Fragment(R.layout.preference_fragment),
    PreferencesAdapter.OnScreenChangeListener {
    protected lateinit var binding: PreferenceFragmentBinding

    protected lateinit var rootScreen: PreferenceScreen
    private val preferencesAdapter: PreferencesAdapter = PreferencesAdapter()
    private lateinit var preferencesView: RecyclerView

    init {
        Preference.Config.dialogBuilderFactory = { context ->
            MaterialAlertDialogBuilder(context)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        PreferenceFragmentBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(binding.toolbar)
        activity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        preferencesView = binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = preferencesAdapter
            layoutAnimation = AnimationUtils.loadLayoutAnimation(
                requireContext(),
                R.anim.preference_layout_fall_down
            )
        }
//        val fastScroller = ThemedFastScroller.create(preferencesView)
//        preferencesView.setOnApplyWindowInsetsListener(
//            ScrollingViewOnApplyWindowInsetsListener(preferencesView, fastScroller)
//        )

        rootScreen = createRootScreen()
        preferencesAdapter.setRootScreen(rootScreen)

        // Restore adapter state from saved state
        savedInstanceState?.getParcelable<PreferencesAdapter.SavedState>("adapter")
            ?.let(preferencesAdapter::loadSavedState)
        preferencesAdapter.onScreenChangeListener = this
    }

    override fun onDestroy() {
        preferencesAdapter.onScreenChangeListener = null
        preferencesView.adapter = null
        rootScreen.destroyScreen()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the adapter state as a parcelable into the Android-managed instance state
        outState.putParcelable("adapter", preferencesAdapter.getSavedState())
    }

    override fun onScreenChanged(screen: PreferenceScreen, subScreen: Boolean) {
        preferencesView.scheduleLayoutAnimation()
//        screen["25"]?.let { pref ->
//            val viewOffset =
//                ((preferencesView.height - 64 * resources.displayMetrics.density) / 2).toInt()
//            (preferencesView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
//                pref.screenPosition,
//                viewOffset
//            )
//            pref.requestRebindAndHighlight()
//        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val n = rootScreen.size()
        for (i in 0 until n) {
            val pref = rootScreen[i]
            if (pref is ActivityResultListener) {
                (pref as ActivityResultListener).onActivityResult(
                    requestCode,
                    resultCode,
                    data
                )
            }
        }
    }

    abstract fun createRootScreen(): PreferenceScreen

    interface ActivityResultListener {
        fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    }
}
