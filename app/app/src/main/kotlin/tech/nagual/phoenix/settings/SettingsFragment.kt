package tech.nagual.phoenix.settings

import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import me.zhanghai.android.files.compat.ListFormatterCompat
import tech.nagual.common.preferences.Preference
import tech.nagual.common.preferences.PreferenceFragment
import tech.nagual.common.preferences.PreferenceScreen
import tech.nagual.common.preferences.helpers.*
import tech.nagual.common.preferences.preferences.choice.SelectionItem
import tech.nagual.theme.custom.CustomThemeHelper
import tech.nagual.theme.custom.ThemeColor
import tech.nagual.theme.night.NightMode
import tech.nagual.theme.night.NightModeHelper
import tech.nagual.protection.SecurityDialog
import me.zhanghai.android.files.util.getBoolean
import me.zhanghai.android.files.util.getTextArray
import me.zhanghai.android.files.util.startActivitySafe
import me.zhanghai.android.files.util.valueCompat
import tech.nagual.phoenix.BuildConfig
import tech.nagual.common.R
import tech.nagual.phoenix.tools.organizer.OrganizersManager
import tech.nagual.phoenix.tools.organizer.data.model.Organizer
import me.zhanghai.android.files.bookmarks.BookmarkDirectory
import tech.nagual.phoenix.organizers.OrganizersActivity
import me.zhanghai.android.files.preferences.DefaultDirectoryPreference
import me.zhanghai.android.files.standarddirectories.StandardDirectoriesLiveData
import me.zhanghai.android.files.standarddirectories.StandardDirectory
import me.zhanghai.android.files.storage.Storage
import me.zhanghai.android.files.storage.StoragesActivity
import tech.nagual.common.dialogs.ChangeDateTimeFormatDialog
import tech.nagual.common.dialogs.ConfirmationDialog
import tech.nagual.common.extensions.baseConfig
import me.zhanghai.android.files.bookmarks.BookmarkDirectoryListActivity
import me.zhanghai.android.files.standarddirectories.StandardDirectoryListActivity
import me.zhanghai.android.files.tools.Tool
import me.zhanghai.android.files.tools.ToolsActivity
import me.zhanghai.android.files.util.createIntent
import tech.nagual.protection.PROTECTION_FINGERPRINT
import tech.nagual.protection.SHOW_ALL_TABS
import tech.nagual.settings.Settings
import java.nio.charset.Charset

inline fun PreferenceScreen.Appendable.defaultDirectory(
    key: String,
    fragment: Fragment,
    block: DefaultDirectoryPreference.() -> Unit
): DefaultDirectoryPreference {
    return DefaultDirectoryPreference(key, fragment).apply(block).also(::addPreferenceItem)
}

class SettingsFragment : PreferenceFragment(),
    OrganizersManager.OrganizersListener {

    private val storagesObserver = Observer<List<Storage>> { onStorageListChanged(it) }

    private val standardDirectoriesObserver =
        Observer<List<StandardDirectory>> { onStandardDirectoriesChanged(it) }

    private val bookmarkDirectoriesObserver =
        Observer<List<BookmarkDirectory>> { onBookmarkDirectoryListChanged(it) }

    private val toolsObserver = Observer<List<Tool>> { onToolListChanged(it) }

    override fun onDestroy() {
        Settings.STORAGES.removeObserver(storagesObserver)
        StandardDirectoriesLiveData.removeObserver(standardDirectoriesObserver)
        Settings.BOOKMARK_DIRECTORIES.removeObserver(bookmarkDirectoriesObserver)
        OrganizersManager.getInstance().removeOrganizersListener(this)
        Settings.TOOLS.removeObserver(toolsObserver)
        super.onDestroy()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        Settings.STORAGES.observeForever(storagesObserver)
        StandardDirectoriesLiveData.observeForever(standardDirectoriesObserver)
        Settings.BOOKMARK_DIRECTORIES.observeForever(bookmarkDirectoriesObserver)
        OrganizersManager.getInstance().addOrganizersListener(this)
        Settings.TOOLS.observeForever(toolsObserver)

        val viewLifecycleOwner = viewLifecycleOwner
        // The following may end up passing the same lambda instance to the observer because it has
        // no capture, and result in an IllegalArgumentException "Cannot add the same observer with
        // different lifecycles" if activity is finished and instantly started again. To work around
        // this, always use an instance method reference.
        // https://stackoverflow.com/a/27524543
        //Settings.THEME_COLOR.observe(viewLifecycleOwner) { CustomThemeHelper.sync() }
        //Settings.MATERIAL_DESIGN_3.observe(viewLifecycleOwner) { CustomThemeHelper.sync() }
        //Settings.NIGHT_MODE.observe(viewLifecycleOwner) { NightModeHelper.sync() }
        //Settings.BLACK_NIGHT_MODE.observe(viewLifecycleOwner) { CustomThemeHelper.sync() }
        Settings.THEME_COLOR.observe(
            viewLifecycleOwner,
            this::onThemeColorChanged
        )
        Settings.MATERIAL_DESIGN_3.observe(
            viewLifecycleOwner,
            this::onMaterialDesign3Changed
        )
        Settings.NIGHT_MODE.observe(
            viewLifecycleOwner,
            this::onNightModeChanged
        )
        Settings.BLACK_NIGHT_MODE.observe(
            viewLifecycleOwner,
            this::onBlackNightModeChanged
        )
    }

    override fun createRootScreen() = screen(context) {
        collapseIcon = true
        categoryHeader("main_header_interface") {
            titleRes = R.string.settings_interface_title
        }
        themeColor(getString(R.string.pref_key_theme_color)) {
            titleRes = R.string.settings_theme_color_title
            summaryRes = R.string.settings_theme_color_summary
            enabled = !Settings.MATERIAL_DESIGN_3.valueCompat
        }
        switch(getString(R.string.pref_key_material_design_3)) {
            titleRes = R.string.settings_material_design_3_title
            defaultValue = getBoolean(R.bool.pref_default_value_material_design_3)
        }
        list(getString(R.string.pref_key_night_mode)) {
            titleRes = R.string.settings_night_mode_title
            defaultValue = getString(R.string.pref_default_value_night_mode)
            setEntries(getTextArray(R.array.settings_night_mode_entries))
            entryValues = getTextArray(R.array.pref_entry_values_night_mode)
        }
        switch(getString(R.string.pref_key_black_night_mode)) {
            titleRes = R.string.settings_black_night_mode
            defaultValue = getBoolean(R.bool.pref_default_value_black_night_mode)
        }
        switch(getString(R.string.settings_lists_animation_title)) {
            titleRes = R.string.settings_lists_animation_title
            defaultValue = getBoolean(R.bool.pref_default_value_file_list_animation)
        }
        list(getString(R.string.pref_key_file_name_ellipsize)) {
            titleRes = R.string.settings_name_ellipsize_title
            defaultValue = getString(R.string.pref_default_value_file_name_ellipsize)
            setEntries(getTextArray(R.array.settings_file_name_ellipsize_entries))
            entryValues = getTextArray(R.array.pref_entry_values_file_name_ellipsize)
        }
        pref(getString(R.string.pref_key_date_format)) {
            titleRes = tech.nagual.common.R.string.pref_date_format
            summary = ChangeDateTimeFormatDialog.formatDateSample(requireContext().baseConfig.dateFormat)
            persistent = false
            clickListener = Preference.OnClickListener { _, holder ->
                ChangeDateTimeFormatDialog(requireActivity()) {
                    summary = ChangeDateTimeFormatDialog.formatDateSample(requireContext().baseConfig.dateFormat)
                    requestRebind()
                }
                false
            }
        }
        categoryHeader("main_header_files") {
            titleRes = R.string.settings_files_title
        }
        defaultDirectory(
            getString(R.string.pref_key_file_list_default_directory),
            this@SettingsFragment
        ) {
            titleRes = R.string.settings_default_directory_title
        }
        switch(getString(R.string.pref_key_adding_storages_from_navigation)) {
            titleRes = R.string.settings_adding_storages_from_menu
            defaultValue = getBoolean(R.bool.pref_default_value_adding_storages_from_navigation)
        }
        pref(getString(R.string.pref_key_storages)) {
            titleRes = R.string.settings_storages_title
            persistent = false
            clickListener = Preference.OnClickListener { _, holder ->
                requireContext().startActivitySafe(StoragesActivity::class.createIntent())
                false
            }
            preBindListener = Preference.OnPreBindListener { preference, holder ->
                val summaryText = holder.summary
                summaryText!!.ellipsize = TextUtils.TruncateAt.END
                summaryText.isSingleLine = true
            }
        }
        pref(getString(R.string.pref_key_standard_directory_settings)) {
            titleRes = R.string.settings_standard_directories_title
            persistent = false
            clickListener = Preference.OnClickListener { _, holder ->
                requireContext().startActivity(StandardDirectoryListActivity::class.createIntent())
                false
            }
            preBindListener = Preference.OnPreBindListener { preference, holder ->
                val summaryText = holder.summary
                summaryText!!.ellipsize = TextUtils.TruncateAt.END
                summaryText.isSingleLine = true
            }
        }
        pref(getString(R.string.pref_key_bookmark_directories)) {
            titleRes = R.string.settings_bookmark_directories_title
            persistent = false
            clickListener = Preference.OnClickListener { _, holder ->
                requireContext().startActivitySafe(BookmarkDirectoryListActivity::class.createIntent())
                false
            }
            preBindListener = Preference.OnPreBindListener { preference, holder ->
                val summaryText = holder.summary
                summaryText!!.ellipsize = TextUtils.TruncateAt.END
                summaryText.isSingleLine = true
            }
        }
        list(getString(R.string.pref_key_root_strategy)) {
            titleRes = R.string.settings_root_strategy_title
            defaultValue = getString(R.string.pref_default_value_root_strategy)
            setEntries(getTextArray(R.array.settings_root_strategy_entries))
            entryValues = getTextArray(R.array.pref_entry_values_root_strategy)
        }
        val charsets = Charset.availableCharsets()
        val selectableItems = charsets.values.mapIndexed { index, charset ->
            SelectionItem(
                charsets.keys.elementAt(index), charset.displayName()
            )
        }
        singleChoice(getString(R.string.pref_key_archive_file_name_encoding), selectableItems) {
            titleRes = R.string.settings_archive_file_name_encoding_title
//            initialSelection = getString(R.string.pref_default_value_archive_file_name_encoding)
        }
        list(getString(R.string.pref_key_open_apk_default_action)) {
            titleRes = R.string.settings_open_apk_default_action_title
            defaultValue = getString(R.string.pref_default_value_open_apk_default_action)
            setEntries(getTextArray(R.array.settings_open_apk_default_action_entries))
            entryValues = getTextArray(R.array.pref_entry_values_open_apk_default_action)
        }
        switch(getString(R.string.pref_key_read_remote_files_for_thumbnail)) {
            titleRes = R.string.settings_read_remote_files_for_thumbnail_title
            defaultValue = getBoolean(R.bool.pref_default_value_read_remote_files_for_thumbnail)
        }
        categoryHeader("main_header_notes") {
            titleRes = R.string.notes_title
        }
        switch(getString(R.string.pref_key_adding_organizers_from_navigation)) {
            titleRes = R.string.settings_creating_organizers_from_menu
            defaultValue = getBoolean(R.bool.pref_default_value_adding_organizers_from_navigation)
        }
        switch(getString(R.string.pref_key_opening_organizers_from_navigation)) {
            titleRes = R.string.settings_starting_organizers_from_menu
            defaultValue = getBoolean(R.bool.pref_default_value_opening_organizers_from_navigation)
        }
        pref("pref_key_organizers") {
            titleRes = R.string.organizers_title
            persistent = false
            clickListener = Preference.OnClickListener { _, holder ->
                requireContext().startActivitySafe(OrganizersActivity::class.createIntent())
                false
            }
            preBindListener = Preference.OnPreBindListener { preference, holder ->
                val summaryText = holder.summary
                summaryText!!.ellipsize = TextUtils.TruncateAt.END
                summaryText.isSingleLine = true
            }
        }
        categoryHeader("main_header_extra") {
            titleRes = R.string.extra_title
        }
        pref(getString(R.string.pref_key_tools)) {
            titleRes = R.string.tools_title
            persistent = false
            clickListener = Preference.OnClickListener { _, holder ->
                requireContext().startActivitySafe(ToolsActivity::class.createIntent())
                false
            }
            preBindListener = Preference.OnPreBindListener { preference, holder ->
                val summaryText = holder.summary
                summaryText!!.ellipsize = TextUtils.TruncateAt.END
                summaryText.isSingleLine = true
            }
        }
        categoryHeader("main_header_security") {
            titleRes = R.string.security_title
        }
        switch(getString(R.string.pref_key_app_password_protection)) {
            titleRes = tech.nagual.common.R.string.pref_password_protect_whole_app
            defaultValue = Settings.APP_PASSWORD_PROTECTION.valueCompat
            persistent = false
            clickListener =
                Preference.OnClickListener { preference, holder ->
                    val tabToShow =
                        if (Settings.APP_PASSWORD_PROTECTION.valueCompat) Settings.APP_PROTECTION_TYPE.valueCompat else SHOW_ALL_TABS
                    SecurityDialog(
                        requireActivity(),
                        Settings.APP_PASSWORD_HASH.valueCompat,
                        tabToShow
                    ) { hash, type, success ->
                        if (success) {
                            val hasPasswordProtection =
                                Settings.APP_PASSWORD_PROTECTION.valueCompat
                            Settings.APP_PASSWORD_PROTECTION.putValue(!hasPasswordProtection)
                            Settings.APP_PASSWORD_HASH.putValue(
                                if (hasPasswordProtection) getString(
                                    R.string.pref_default_value_app_password_hash
                                ) else hash
                            )
                            Settings.APP_PROTECTION_TYPE.putValue(type)

                            if (Settings.APP_PASSWORD_PROTECTION.valueCompat) {
                                val confirmationTextId =
                                    if (Settings.APP_PROTECTION_TYPE.valueCompat == PROTECTION_FINGERPRINT)
                                        R.string.fingerprint_setup_successfully else R.string.protection_setup_successfully
                                ConfirmationDialog(
                                    requireActivity(),
                                    "",
                                    confirmationTextId,
                                    R.string.ok,
                                    0
                                ) { }
                            }
                        } else {
                            this@switch.checked = Settings.APP_PASSWORD_PROTECTION.valueCompat
                            false
                        }
                    }
                    true
                }
        }
        switch(getString(R.string.pref_key_settings_password_protection)) {
            titleRes = tech.nagual.common.R.string.pref_password_protect_settings
            defaultValue = Settings.SETTINGS_PASSWORD_PROTECTION.valueCompat
            persistent = false
            clickListener =
                Preference.OnClickListener { preference, holder ->
                    val tabToShow =
                        if (Settings.SETTINGS_PASSWORD_PROTECTION.valueCompat) Settings.SETTINGS_PROTECTION_TYPE.valueCompat else SHOW_ALL_TABS
                    SecurityDialog(
                        requireActivity(),
                        Settings.SETTINGS_PASSWORD_HASH.valueCompat,
                        tabToShow
                    ) { hash, type, success ->
                        if (success) {
                            val hasPasswordProtection =
                                Settings.SETTINGS_PASSWORD_PROTECTION.valueCompat
                            Settings.SETTINGS_PASSWORD_PROTECTION.putValue(!hasPasswordProtection)
                            Settings.SETTINGS_PASSWORD_HASH.putValue(
                                if (hasPasswordProtection) getString(
                                    R.string.pref_default_value_app_password_hash
                                ) else hash
                            )
                            Settings.SETTINGS_PROTECTION_TYPE.putValue(type)

                            if (Settings.SETTINGS_PASSWORD_PROTECTION.valueCompat) {
                                val confirmationTextId =
                                    if (Settings.SETTINGS_PROTECTION_TYPE.valueCompat == PROTECTION_FINGERPRINT)
                                        R.string.fingerprint_setup_successfully else R.string.protection_setup_successfully
                                ConfirmationDialog(
                                    requireActivity(),
                                    "",
                                    confirmationTextId,
                                    R.string.ok,
                                    0
                                ) { }
                            }
                        } else {
                            this@switch.checked = Settings.SETTINGS_PASSWORD_PROTECTION.valueCompat
                            false
                        }
                    }
                    true
                }
        }
        categoryHeader("main_header_appinfo") {
            titleRes = R.string.appinfo_title
        }
        pref(getString(R.string.pref_key_settings_appinfo_version)) {
            titleRes = R.string.appinfo_version
            summary = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
            persistent = false
            clickListener = Preference.OnClickListener { _, holder ->

                false
            }
        }
    }

    private fun onStorageListChanged(storages: List<Storage>) {
        val names = storages.filter { it.isVisible }.map { it.getName(requireContext()) }
        val summary =
            if (names.isNotEmpty()) ListFormatterCompat.format(names) else getString(R.string.settings_storages_summary_empty)
        val pref = rootScreen[getString(R.string.pref_key_storages)]!!
        pref.summary = summary
        pref.requestRebind()
    }

    private fun onStandardDirectoriesChanged(standardDirectories: List<StandardDirectory>) {
        val titles =
            standardDirectories.filter { it.isEnabled }.map { it.getTitle(requireContext()) }
        val summary =
            if (titles.isNotEmpty()) ListFormatterCompat.format(titles) else getString(R.string.settings_standard_directories_summary_empty)
        val pref = rootScreen[getString(R.string.pref_key_standard_directory_settings)]!!
        pref.summary = summary
        pref.requestRebind()
    }

    private fun onBookmarkDirectoryListChanged(bookmarkDirectories: List<BookmarkDirectory>) {
        val names = bookmarkDirectories.map { it.name }
        val summary =
            if (names.isNotEmpty()) ListFormatterCompat.format(names) else getString(R.string.settings_bookmark_directories_summary_empty)
        val pref = rootScreen[getString(R.string.pref_key_bookmark_directories)]!!
        pref.summary = summary
        pref.requestRebind()
    }

    override fun updateOrganizers(organizers: List<Organizer>) {
        val names = organizers.map { it.name }
        val summary =
            if (names.isNotEmpty()) ListFormatterCompat.format(names) else getString(R.string.organizer_list_empty)
        val pref = rootScreen["pref_key_organizers"]!!
        pref.summary = summary
        pref.requestRebind()
    }

    private fun onToolListChanged(tools: List<Tool>) {
        val names = tools.filter { it.isVisible }.map { it.getName() }
        val summary =
            if (names.isNotEmpty()) ListFormatterCompat.format(names) else getString(R.string.settings_tools_summary_empty)
        val pref = rootScreen[getString(R.string.pref_key_tools)]!!
        pref.summary = summary
        pref.requestRebind()
    }

//    fun onBackPressed(): Boolean {
//        if (overlayActionMode.isActive) {
//            overlayActionMode.finish()
//            return true
//        }
//        if (!preferencesAdapter.goBack()) {
//            return true
//        }
//        return false
//    }

    private fun onThemeColorChanged(themeColor: ThemeColor) {
        CustomThemeHelper.sync()
    }

    private fun onMaterialDesign3Changed(isMaterialDesign3: Boolean) {
        CustomThemeHelper.sync()
    }

    private fun onNightModeChanged(nightMode: NightMode) {
        NightModeHelper.sync()
    }

    private fun onBlackNightModeChanged(blackNightMode: Boolean) {
        CustomThemeHelper.sync()
    }
}
