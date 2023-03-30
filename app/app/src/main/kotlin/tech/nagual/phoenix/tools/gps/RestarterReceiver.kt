package tech.nagual.phoenix.tools.gps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import tech.nagual.phoenix.tools.gps.common.IntentConstants

class RestarterReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val wasRunning = intent.getBooleanExtra("was_running", false)
        val serviceIntent = Intent(context, GpsService::class.java)
        if (wasRunning) {
            serviceIntent.putExtra(IntentConstants.IMMEDIATE_START, true)
        } else {
            serviceIntent.putExtra(IntentConstants.IMMEDIATE_STOP, true)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}