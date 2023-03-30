package tech.nagual.phoenix.tools.organizer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import dagger.hilt.android.AndroidEntryPoint
import tech.nagual.phoenix.R
import tech.nagual.phoenix.databinding.OrganizerActivityBinding
import tech.nagual.app.BaseNavigationActivity
import tech.nagual.common.extensions.hideKeyboard
import tech.nagual.phoenix.tools.organizer.backup.BackupService

@AndroidEntryPoint
class OrganizerActivity : BaseNavigationActivity() {

    private lateinit var binding: OrganizerActivityBinding
    private val activityModel: ActivityViewModel by viewModels()

//    private lateinit var navigationFragment: NavigationFragment

    private val primaryDestinations = setOf(
        R.id.organizer_main_fragment
    )
    private val secondaryDestinations = setOf(
        R.id.organizer_folder_fragment,
        R.id.organizer_editor_fragment,
        R.id.organizer_manage_notebooks_fragment,
        R.id.organizer_search_fragment,
        R.id.organizer_settings_fragment,
        R.id.organizer_tags_fragment,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = OrganizerActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        appBarConfiguration = AppBarConfiguration(
            primaryDestinations,
        )

//        if (savedInstanceState == null) {
//            navigationFragment = NavigationFragment()
//            supportFragmentManager.commit { add(R.id.navigationFragment, navigationFragment) }
//        } else {
//            navigationFragment = supportFragmentManager.findFragmentById(R.id.navigationFragment)
//                    as NavigationFragment
//        }

        navController =
            (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController

        navController.addOnDestinationChangedListener { _, _, _ ->
            currentFocus?.hideKeyboard()
        }

//        activityModel.folders.collect(this) { (showDefaultNotebook, notebooks) ->
//            val notebookIds = (notebooks.map { it.id.toInt() } + R.id.default_notebook).toSet()

            // Remove deleted notebooks from the menu
//            (primaryDestinations + secondaryDestinations + notebookIds).let { dests ->
//                var index = 0
//                while (index < notebooksMenu.size()) {
//                    val item = notebooksMenu.getItem(index)
//                    if (item.itemId !in dests) notebooksMenu.removeItem(item.itemId) else index++
//                }
//            }
//
//            createNotebookMenuItems(notebooks)
//
//            val defaultTitle = getString(R.string.default_notebook)
//            notebooksMenu.findItem(R.id.nav_default_notebook)?.apply {
//                isVisible = showDefaultNotebook
//                title =
//                    defaultTitle + " (${getString(R.string.default_string)})".takeIf { notebooks.any { it.name == defaultTitle } }
//                        .orEmpty()
//            }
//        }

        // androidx.fragment:1.3.3 caused the FragmentContainerView to apply padding to itself when
        // the attribute fitsSystemWindows is enabled. We override it here and let the fragments decide their padding
//        ViewCompat.setOnApplyWindowInsetsListener(binding.navHostFragment) { view, insets ->
//            insets
//        }

        if (intent != null) handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) handleIntent(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }


    override fun onBackPressed() {
//        navController.currentDestination?.id
        super.onBackPressed()
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val title = intent.getStringExtra(Intent.EXTRA_TITLE) ?: ""
                val content = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""

                val link = NavDeepLinkBuilder(this)
                    .setGraph(R.navigation.organizer_nav_graph)
                    .setDestination(R.id.organizer_editor_fragment)
                    .setArguments(
                        bundleOf(
                            "transitionName" to "",
                            "newNoteTitle" to title,
                            "newNoteContent" to content,
                        )
                    )
                    .createTaskStackBuilder()
                    .first()

                navController.handleDeepLink(link)
            }
            else -> navController.handleDeepLink(intent)
        }
    }

    fun startBackup(backupUri: Uri) {
        BackupService.export(this, activityModel.notesToProcess, BackupService.ExportType.EXPORT_ONLY_NOTES, true, backupUri)
    }


    fun restoreNotes(backupUri: Uri) {
        BackupService.import(this, backupUri)
    }
}
