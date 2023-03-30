package tech.nagual.app

import androidx.navigation.NavController
import androidx.navigation.ui.AppBarConfiguration

open class BaseNavigationActivity : BaseActivity() {
    lateinit var appBarConfiguration: AppBarConfiguration
    lateinit var navController: NavController
}