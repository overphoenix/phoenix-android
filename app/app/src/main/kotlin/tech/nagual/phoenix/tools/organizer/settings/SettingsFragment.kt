package tech.nagual.phoenix.tools.organizer.settings

import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import me.zhanghai.android.files.ui.OverlayToolbarActionMode
import me.zhanghai.android.files.ui.ScrollingViewOnApplyWindowInsetsListener
import me.zhanghai.android.files.ui.ThemedFastScroller
import tech.nagual.common.databinding.GenericAppBarLayoutBinding
import tech.nagual.common.preferences.Preference
import tech.nagual.common.preferences.PreferenceScreen
import tech.nagual.common.preferences.PreferencesAdapter
import tech.nagual.common.preferences.datastore.showOneChoicePreferenceDialog
import tech.nagual.common.preferences.helpers.categoryHeader
import tech.nagual.common.preferences.helpers.pref
import tech.nagual.common.preferences.helpers.screen
import tech.nagual.phoenix.R
import tech.nagual.phoenix.databinding.OrganizerSettingsFragmentBinding
import tech.nagual.phoenix.tools.organizer.OrganizerActivity
import tech.nagual.phoenix.tools.organizer.backup.ExportDialogFragment
import tech.nagual.phoenix.tools.organizer.common.BaseFragment
import tech.nagual.phoenix.tools.organizer.preferences.DateFormat
import tech.nagual.phoenix.tools.organizer.preferences.OrganizerPreferences
import tech.nagual.phoenix.tools.organizer.utils.RestoreOrganizerContract
import tech.nagual.phoenix.tools.organizer.utils.collect
import tech.nagual.phoenix.tools.organizer.utils.launch
import tech.nagual.phoenix.tools.organizer.utils.viewBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class SettingsFragment : BaseFragment(R.layout.organizer_settings_fragment),
    PreferencesAdapter.OnScreenChangeListener {
    private val binding by viewBinding(OrganizerSettingsFragmentBinding::bind)

    private val preferencesAdapter: PreferencesAdapter = PreferencesAdapter()
    private lateinit var preferencesView: RecyclerView

    private val model: SettingsViewModel by viewModels()

    private var organizerPreferences = OrganizerPreferences()

    private val loadBackupLauncher = registerForActivityResult(RestoreOrganizerContract) { uri ->
        if (uri == null) return@registerForActivityResult
        (activity as OrganizerActivity).restoreNotes(uri)
    }

    init {
        Preference.Config.dialogBuilderFactory = { context ->
            MaterialAlertDialogBuilder(context)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val appBarBinding = GenericAppBarLayoutBinding.bind(binding.root)
        appBarLayout = appBarBinding.appBarLayout
        toolbar = appBarBinding.toolbar
        toolbarTitle = getString(R.string.organizer_settings_title)
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

        model.preferences.collect(viewLifecycleOwner) {
            organizerPreferences = it

            with(organizerPreferences) {
                rootScreen[defaultNoteType.key]!!.summary =
                    getString(defaultNoteType.nameResource)
                rootScreen.requestRebind(defaultNoteType.key)

                rootScreen[dateFormat.key]!!.summary =
                    DateTimeFormatter.ofPattern(getString(dateFormat.patternResource)).format(LocalDate.now())
                rootScreen.requestRebind(dateFormat.key)

                rootScreen[groupUnassignedNotes.key]!!.summary =
                    getString(groupUnassignedNotes.nameResource)
                rootScreen.requestRebind(groupUnassignedNotes.key)

                rootScreen[noteDeletionTime.key]!!.summary =
                    getString(noteDeletionTime.nameResource)
                rootScreen.requestRebind(noteDeletionTime.key)

//                rootScreen[backupStrategy.key]!!.summary = getString(backupStrategy.nameResource)
//                rootScreen.requestRebind(backupStrategy.key)
            }
        }

        // Restore adapter state from saved state
        savedInstanceState?.getParcelable<PreferencesAdapter.SavedState>("adapter")
            ?.let(preferencesAdapter::loadSavedState)
        preferencesAdapter.onScreenChangeListener = this
    }

    fun createRootScreen() = screen(context) {
        collapseIcon = true
        categoryHeader("organizer_header_common") {
            title = getString(R.string.organizer_pref_header_common)
        }
        pref(organizerPreferences.defaultNoteType.key) {
            title = getString(R.string.organizer_pref_default_note_type)
            persistent = false
            clickListener = Preference.OnClickListener { preference, holder ->
                showOneChoicePreferenceDialog(
                    R.string.organizer_pref_default_note_type,
                    organizerPreferences.defaultNoteType
                ) { selected ->
                    model.setPreference(selected)
                }
                false
            }
        }
        pref(organizerPreferences.dateFormat.key) {
            title = getString(tech.nagual.common.R.string.pref_date_format)
            persistent = false
            clickListener = Preference.OnClickListener { preference, holder ->
                val localDate = LocalDate.now()
                val items = DateFormat.values()
                    .map {
                        DateTimeFormatter.ofPattern(getString(it.patternResource)).format(localDate)
                    }
                    .toTypedArray()

                showOneChoicePreferenceDialog(
                    tech.nagual.common.R.string.pref_date_format,
                    organizerPreferences.dateFormat,
                    items = items
                ) { selected ->
                    model.setPreference(selected)
                }
                false
            }
        }
        pref(organizerPreferences.groupUnassignedNotes.key) {
            title = getString(R.string.organizer_pref_group_unassigned_notes)
            summary = getString(R.string.organizer_pref_group_unassigned_notes_summary)
            persistent = false
            clickListener = Preference.OnClickListener { preference, holder ->
                showOneChoicePreferenceDialog(
                    R.string.organizer_pref_group_unassigned_notes,
                    organizerPreferences.groupUnassignedNotes
                ) { selected ->
                    model.setPreference(selected)
                }
                false
            }
        }
        pref(organizerPreferences.noteDeletionTime.key) {
            title = getString(R.string.organizer_pref_note_deletion_time)
            persistent = false
            clickListener = Preference.OnClickListener { _, holder ->
                showOneChoicePreferenceDialog(
                    R.string.organizer_pref_note_deletion_time,
                    organizerPreferences.noteDeletionTime
                ) { selected ->
                    model.setPreference(selected)
                }
                false
            }
        }
        categoryHeader("organizer_header_backup") {
            title = getString(R.string.organizer_pref_header_extra)
        }
        pref("organizer_create_backup") {
            title = getString(R.string.organizer_pref_organizer_export)
            summary = getString(R.string.organizer_pref_create_backup_summary)
            persistent = false
            clickListener = Preference.OnClickListener { _, holder ->
                activityModel.notesToProcess = null
                ExportDialogFragment.show(this@SettingsFragment)
                false
            }
        }
        pref("organizer_restore") {
            title = getString(R.string.organizer_pref_organizer_import)
            summary = getString(R.string.organizer_pref_restore_summary)
            persistent = false
            clickListener = Preference.OnClickListener { _, holder ->
                loadBackupLauncher.launch()
                false
            }
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
