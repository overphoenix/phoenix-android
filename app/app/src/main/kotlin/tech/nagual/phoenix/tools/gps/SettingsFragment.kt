package tech.nagual.phoenix.tools.gps

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import tech.nagual.app.BaseNavigationFragment
import tech.nagual.common.databinding.GenericAppBarLayoutBinding
import tech.nagual.common.preferences.Preference
import tech.nagual.common.preferences.PreferenceScreen
import tech.nagual.common.preferences.PreferencesAdapter
import tech.nagual.common.preferences.helpers.categoryHeader
import tech.nagual.common.preferences.helpers.editText
import tech.nagual.common.preferences.helpers.screen
import tech.nagual.common.preferences.helpers.switch
import me.zhanghai.android.files.ui.OverlayToolbarActionMode
import me.zhanghai.android.files.ui.ScrollingViewOnApplyWindowInsetsListener
import me.zhanghai.android.files.ui.ThemedFastScroller
import me.zhanghai.android.files.util.getQuantityString
import tech.nagual.phoenix.R
import tech.nagual.phoenix.databinding.GpsSettingsFragmentBinding
import tech.nagual.phoenix.tools.gps.preferences.GpsPreferences
import tech.nagual.phoenix.tools.gps.preferences.PreferenceKeys
import tech.nagual.phoenix.tools.organizer.utils.viewBinding

class SettingsFragment : BaseNavigationFragment(R.layout.gps_settings_fragment),
    PreferencesAdapter.OnScreenChangeListener {
    private val binding by viewBinding(GpsSettingsFragmentBinding::bind)

    private val preferencesAdapter: PreferencesAdapter = PreferencesAdapter()
    private lateinit var preferencesView: RecyclerView

    init {
        Preference.Config.dialogBuilderFactory = { context ->
            MaterialAlertDialogBuilder(context)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val appBarBinding = GenericAppBarLayoutBinding.bind(binding.root)
        appBarLayout = appBarBinding.appBarLayout
        toolbar = appBarBinding.toolbar
        toolbarTitle = getString(R.string.gps_settings)
        super.onViewCreated(view, savedInstanceState)
        overlayActionMode = OverlayToolbarActionMode(appBarBinding.overlayToolbar)

        preferencesView = binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = preferencesAdapter
            layoutAnimation = AnimationUtils.loadLayoutAnimation(
                requireContext(),
                R.anim.preference_layout_fall_down
            )
        }
        val fastScroller = ThemedFastScroller.create(preferencesView)
        binding.recyclerView.setOnApplyWindowInsetsListener(
            ScrollingViewOnApplyWindowInsetsListener(preferencesView, fastScroller)
        )

        liftAppBarOnScrollFor(binding.recyclerView)

        val rootScreen = createRootScreen()
        preferencesAdapter.setRootScreen(rootScreen)

        // Restore adapter state from saved state
        savedInstanceState?.getParcelable<PreferencesAdapter.SavedState>("adapter")
            ?.let(preferencesAdapter::loadSavedState)
        preferencesAdapter.onScreenChangeListener = this
    }

    fun createRootScreen() = screen(context) {
        collapseIcon = true
        categoryHeader("gps_header_common") {
            title = getString(R.string.gps_pref_header_common)
        }
        switch(PreferenceKeys.GPS_SATELLITE_LOCATIONS) {
            title = getString(R.string.gps_listeners_gps_title)
            summary = getString(R.string.gps_listeners_gps_summary)
            defaultValue = true
        }
        switch(PreferenceKeys.GPS_NETWORK_LOCATIONS) {
            title = getString(R.string.gps_listeners_cell_title)
            summary = getString(R.string.gps_listeners_cell_summary)
            defaultValue = true
        }
        switch(PreferenceKeys.GPS_PASSIVE_LOCATIONS) {
            title = getString(R.string.gps_listeners_passive_title)
            summary = getString(R.string.gps_listeners_passive_summary)
            defaultValue = false
        }
        editText(PreferenceKeys.GPS_MINIMUM_INTERVAL) {
            title = getString(R.string.gps_minimum_interval_title)
            summary = getString(R.string.gps_minimum_interval_summary)
            summaryProvider = {
                getQuantityString(
                    R.plurals.gps_seconds_plural,
                    GpsPreferences.minimumInterval,
                    GpsPreferences.minimumInterval
                )
            }
            textInputType = InputType.TYPE_CLASS_NUMBER
            textInputHintRes = R.string.gps_minimum_interval_hint
        }
        editText(PreferenceKeys.GPS_MINIMUM_DISTANCE) {
            title = getString(R.string.gps_minimum_distance_title)
            summary = getString(R.string.gps_minimum_distance_summary)
            summaryProvider = {
                getQuantityString(
                    R.plurals.gps_meters_plural,
                    GpsPreferences.minimumDistance,
                    GpsPreferences.minimumDistance
                )
            }
            textInputType = InputType.TYPE_CLASS_NUMBER
            textInputHintRes = R.string.gps_settings_enter_meters
        }
        editText(PreferenceKeys.GPS_MINIMUM_ACCURACY) {
            title = getString(R.string.gps_minimum_accuracy_title)
            summary = getString(R.string.gps_minimum_accuracy_summary)
            summaryProvider = {
                getQuantityString(
                    R.plurals.gps_meters_plural,
                    GpsPreferences.minimumAccuracy,
                    GpsPreferences.minimumAccuracy
                )
            }
            textInputType = InputType.TYPE_CLASS_NUMBER
            textInputHintRes = R.string.gps_settings_enter_meters
        }
        editText(PreferenceKeys.GPS_DETECTING_RETRY_TIME) {
            title = getString(R.string.gps_detection_retry_time_title)
            summary = getString(R.string.gps_detection_retry_time_summary)
            summaryProvider = {
                getQuantityString(
                    R.plurals.gps_seconds_plural,
                    GpsPreferences.detectingRetryPeriod,
                    GpsPreferences.detectingRetryPeriod
                )
            }
            textInputType = InputType.TYPE_CLASS_NUMBER
            textInputHintRes = R.string.gps_time_hint
        }
        editText(PreferenceKeys.GPS_ABSOLUTE_TIMEOUT) {
            title = getString(R.string.gps_absolute_timeout_title)
            summary = getString(R.string.gps_absolute_timeout_summary)
            summaryProvider = {
                getQuantityString(
                    R.plurals.gps_seconds_plural,
                    GpsPreferences.absoluteTimeoutForAcquiringPosition,
                    GpsPreferences.absoluteTimeoutForAcquiringPosition
                )
            }
            textInputType = InputType.TYPE_CLASS_NUMBER
            textInputHintRes = R.string.gps_time_hint
        }
        switch(PreferenceKeys.GPS_ALTITUDE_SHOULD_ADJUST) {
            title = getString(R.string.gps_altitude_subtractgeoidheight_title)
            summary = getString(R.string.gps_altitude_subtractoffset_summary)
            defaultValue = false
        }
        editText(PreferenceKeys.GPS_ALTITUDE_SUBTRACT_OFFSET) {
            title = getString(R.string.gps_altitude_subtractoffset_title)
            summary = getString(R.string.gps_altitude_subtractoffset_summary)
            summaryProvider = {
                getQuantityString(
                    R.plurals.gps_meters_plural,
                    GpsPreferences.subtractAltitudeOffset,
                    GpsPreferences.subtractAltitudeOffset
                )
            }
            textInputType = InputType.TYPE_CLASS_NUMBER
            textInputHintRes = R.string.gps_settings_enter_meters
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the adapter state as a parcelable into the Android-managed instance state
        outState.putParcelable("adapter", preferencesAdapter.getSavedState())
    }

    override fun onScreenChanged(screen: PreferenceScreen, subScreen: Boolean) {
        preferencesView.scheduleLayoutAnimation()
        screen["25"]?.let { pref ->
            val viewOffset =
                ((preferencesView.height - 64 * resources.displayMetrics.density) / 2).toInt()
            (preferencesView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                pref.screenPosition,
                viewOffset
            )
            pref.requestRebindAndHighlight()
        }
    }

    override fun onDestroy() {
        preferencesAdapter.onScreenChangeListener = null
        preferencesView.adapter = null
        super.onDestroy()
    }

    fun onBackPressed(): Boolean {
        if (overlayActionMode.isActive) {
            overlayActionMode.finish()
            return true
        }
        if (!preferencesAdapter.goBack()) {
            return true
        }
        return false
    }

}
