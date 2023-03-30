package tech.nagual.common.dialogs

import android.app.Activity
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import tech.nagual.common.R
import tech.nagual.common.databinding.DialogCustomIntervalPickerBinding
import tech.nagual.common.extensions.beVisibleIf
import tech.nagual.common.extensions.setupDialogStuff
import tech.nagual.common.helpers.DAY_SECONDS
import tech.nagual.common.helpers.HOUR_SECONDS
import tech.nagual.common.helpers.MINUTE_SECONDS
import tech.nagual.common.extensions.hideKeyboard
import tech.nagual.common.extensions.showKeyboard
import tech.nagual.common.extensions.value

class CustomIntervalPickerDialog(
    val activity: Activity,
    val selectedSeconds: Int = 0,
    val showSeconds: Boolean = false,
    val callback: (minutes: Int) -> Unit
) {
    var dialog: AlertDialog
    private val binding: DialogCustomIntervalPickerBinding =
        DialogCustomIntervalPickerBinding.inflate(activity.layoutInflater)
    var view = binding.root as ViewGroup

    init {
        view.apply {
            binding.dialogRadioSeconds.beVisibleIf(showSeconds)
            when {
                selectedSeconds == 0 -> binding.dialogRadioView.check(R.id.dialog_radio_minutes)
                selectedSeconds % DAY_SECONDS == 0 -> {
                    binding.dialogRadioView.check(R.id.dialog_radio_days)
                    binding.dialogCustomIntervalValue.setText((selectedSeconds / DAY_SECONDS).toString())
                }
                selectedSeconds % HOUR_SECONDS == 0 -> {
                    binding.dialogRadioView.check(R.id.dialog_radio_hours)
                    binding.dialogCustomIntervalValue.setText((selectedSeconds / HOUR_SECONDS).toString())
                }
                selectedSeconds % MINUTE_SECONDS == 0 -> {
                    binding.dialogRadioView.check(R.id.dialog_radio_minutes)
                    binding.dialogCustomIntervalValue.setText((selectedSeconds / MINUTE_SECONDS).toString())
                }
                else -> {
                    binding.dialogRadioView.check(R.id.dialog_radio_seconds)
                    binding.dialogCustomIntervalValue.setText(selectedSeconds.toString())
                }
            }
        }

        dialog = MaterialAlertDialogBuilder(activity)
            .setPositiveButton(R.string.ok) { dialogInterface, i -> confirmReminder() }
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                activity.setupDialogStuff(view, this) {
                    showKeyboard(binding.dialogCustomIntervalValue)
                }
            }
    }

    private fun confirmReminder() {
        val value = binding.dialogCustomIntervalValue.value
        val multiplier = getMultiplier(binding.dialogRadioView.checkedRadioButtonId)
        val minutes = Integer.valueOf(if (value.isEmpty()) "0" else value)
        callback(minutes * multiplier)
        activity.hideKeyboard()
        dialog.dismiss()
    }

    private fun getMultiplier(id: Int) = when (id) {
        R.id.dialog_radio_days -> DAY_SECONDS
        R.id.dialog_radio_hours -> HOUR_SECONDS
        R.id.dialog_radio_minutes -> MINUTE_SECONDS
        else -> 1
    }
}
