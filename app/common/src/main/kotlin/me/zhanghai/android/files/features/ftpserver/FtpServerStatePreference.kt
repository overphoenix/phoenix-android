package me.zhanghai.android.files.features.ftpserver

import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import tech.nagual.common.preferences.PreferencesAdapter
import tech.nagual.common.preferences.preferences.SwitchPreference
import tech.nagual.common.R

class FtpServerStatePreference(private val fragment: Fragment) :
    SwitchPreference(fragment.getString(R.string.pref_key_ftp_server_state)) {
    private val observer = Observer<FtpServerService.State> { onStateChanged(it) }

    init {
        persistent = false
    }

    override fun onAttach() {
        super.onAttach()
        FtpServerService.stateLiveData.observeForever(observer)
    }

    override fun onDetach() {
        super.onDetach()
        FtpServerService.stateLiveData.removeObserver(observer)
    }

    private fun onStateChanged(state: FtpServerService.State) {
        val summaryRes = when (state) {
            FtpServerService.State.STARTING -> R.string.ftp_server_state_summary_starting
            FtpServerService.State.RUNNING -> R.string.ftp_server_state_summary_running
            FtpServerService.State.STOPPING -> R.string.ftp_server_state_summary_stopping
            FtpServerService.State.STOPPED -> R.string.ftp_server_state_summary_stopped
        }
        summary = fragment.requireContext().getString(summaryRes)
        checked = state == FtpServerService.State.STARTING
                || state == FtpServerService.State.RUNNING
        enabled = !(state == FtpServerService.State.STARTING
                || state == FtpServerService.State.STOPPING)
    }

    override fun onClick(holder: PreferencesAdapter.ViewHolder) {
        super.onClick(holder)
        FtpServerService.toggle(fragment.requireContext())
    }
}