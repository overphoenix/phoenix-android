package tech.nagual.phoenix.tools.gps

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import dagger.hilt.android.AndroidEntryPoint
import tech.nagual.phoenix.R
import tech.nagual.app.BaseNavigationActivity
import tech.nagual.app.powerManager
import tech.nagual.common.permissions.Permission
import tech.nagual.common.permissions.askForPermissions
import tech.nagual.common.permissions.isAllGranted
import tech.nagual.common.permissions.rationale.createDialogRationale
import tech.nagual.common.ui.simpledialogs.SimpleDialog
import tech.nagual.phoenix.databinding.GpsActivityBinding

@AndroidEntryPoint
class GpsActivity : BaseNavigationActivity() {

    private val primaryDestinations = setOf(
        R.id.gps_map_fragment
    )

    private lateinit var binding: GpsActivityBinding

    // Flag to prevent the service from starting in case we're going through a permission workflow
    // This is required because the service needs to start and show a notification, but the
    // permission workflow causes the service to stop and start multiple times.
    private var permissionWorkflowInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = GpsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        appBarConfiguration = AppBarConfiguration(
            primaryDestinations,
        )

        navController =
            (supportFragmentManager.findFragmentById(R.id.gps_nav_host_fragment) as NavHostFragment).navController

        // listen for navigation changes
        navController.addOnDestinationChangedListener { _, destination, _ ->
//            when (destination.id) {
//                R.id.trackbook_track_fragment -> {
//                    runOnUiThread {
//                        run {
//                            // mark menu item "Tracks" as checked
//                            bottomNavigationView.menu.findItem(R.id.trackbook_tracklist_fragment).isChecked =
//                                true
//                        }
//                    }
//                }
//                else -> {
//                    // do nothing
//                }
//            }
        }

        /**
         * Whether the user has allowed the permissions absolutely required to run the app.
         * Currently this is location and file storage.
         */
        if (!this.isAllGranted(
                Permission.ACCESS_COARSE_LOCATION,
                Permission.ACCESS_FINE_LOCATION,
                Permission.ACCESS_BACKGROUND_LOCATION
            )
        ) {
            permissionWorkflowInProgress = true
            askUserForBasicPermissions()
        }

        if (intent != null) handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (!permissionWorkflowInProgress) {
            GpsManager.getInstance().startService()
        }
//        enableDisableMenuItems()
    }

    override fun onDestroy() {
        GpsManager.getInstance().stopServiceIfRequired()
        super.onDestroy()
    }

    private fun askUserForBasicPermissions() {
        askForPermissions(
            Permission.ACCESS_COARSE_LOCATION,
            Permission.ACCESS_FINE_LOCATION
        ) { result ->
            val denied: Set<Permission> = result.denied()
            if (denied.isNotEmpty()) {
                showAlert(
                    getString(R.string.gpslogger_permissions_rationale_title),
                    getString(R.string.gpslogger_permissions_permanently_denied),
                    this
                )
                permissionWorkflowInProgress = false
            } else {
                askUserForBackgroundPermissions()
            }
        }
    }

    private fun askUserForBackgroundPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val rationaleHandler =
                createDialogRationale(R.string.gpslogger_permissions_rationale_title) {
                    onPermission(
                        Permission.ACCESS_BACKGROUND_LOCATION,
                        getString(
                            R.string.gpslogger_permissions_background_location,
                            packageManager.backgroundPermissionOptionLabel
                        )
                    )
                }

            askForPermissions(
                Permission.ACCESS_BACKGROUND_LOCATION,
                rationaleHandler = rationaleHandler
            ) { result ->
                val granted = result.isAllGranted(Permission.ACCESS_BACKGROUND_LOCATION)
                if (granted) {
                    askUserToDisableBatteryOptimization()
                } else {
                    showAlert(
                        getString(R.string.gpslogger_permissions_rationale_title),
                        getString(R.string.gpslogger_permissions_permanently_denied),
                        this
                    )
                    permissionWorkflowInProgress = false
                }
            }
        } else {
            askUserToDisableBatteryOptimization()
        }
    }

    @SuppressLint("BatteryLife")
    fun askUserToDisableBatteryOptimization() {
        val intent = Intent()
        val packageName = packageName
        try {
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.data = Uri.parse("package:$packageName")
                batteryOptimizationLauncher.launch(intent)
            } else {
                // On older Android versions, a device might report that it is already ignoring battery optimizations. It's lying.
                // https://stackoverflow.com/questions/50231908/powermanager-isignoringbatteryoptimizations-always-returns-true-even-if-is-remov
                // https://issuetracker.google.com/issues/37067894?pli=1
                permissionWorkflowInProgress = false
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private val batteryOptimizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { permissionWorkflowInProgress = false }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) handleIntent(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun handleIntent(intent: Intent) {
        navController.handleDeepLink(intent)
//        when (intent.action) {
//            Intent.ACTION_SEND -> {
//                val title = intent.getStringExtra(Intent.EXTRA_TITLE) ?: ""
//                val content = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
//
//                val link = NavDeepLinkBuilder(this)
//                    .setGraph(R.navigation.notes_nav_graph)
//                    .setDestination(R.id.notes_fragment_editor)
//                    .setArguments(
//                        bundleOf(
//                            "transitionName" to "",
//                            "newNoteTitle" to title,
//                            "newNoteContent" to content,
//                        )
//                    )
//                    .createTaskStackBuilder()
//                    .first()
//
//                navController.handleDeepLink(link)
//            }
//            else -> navController.handleDeepLink(intent)
//        }
    }

    fun showAlert(title: String?, message: String?, activity: Activity?) {
        tech.nagual.common.ui.simpledialogs.SimpleDialog.build()
            .title(title)
            .msgHtml(message)
            .show(activity as FragmentActivity?)
    }
}
