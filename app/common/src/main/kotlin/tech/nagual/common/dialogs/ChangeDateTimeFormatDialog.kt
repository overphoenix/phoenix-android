package tech.nagual.common.dialogs

import android.app.Activity
import android.text.format.DateFormat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import tech.nagual.common.R
import tech.nagual.common.R.id.*
import tech.nagual.common.databinding.DialogChangeDateTimeFormatBinding
import tech.nagual.common.extensions.setupDialogStuff
import tech.nagual.common.helpers.*
import tech.nagual.common.extensions.baseConfig
import java.util.*

class ChangeDateTimeFormatDialog(val activity: Activity, val callback: () -> Unit) {
    private val binding: DialogChangeDateTimeFormatBinding =
        DialogChangeDateTimeFormatBinding.inflate(activity.layoutInflater)

    init {
        val view = binding.root
        view.apply {
            binding.changeDateTimeDialogRadioOne.text = formatDateSample(DATE_FORMAT_ONE)
            binding.changeDateTimeDialogRadioTwo.text = formatDateSample(DATE_FORMAT_TWO)
            binding.changeDateTimeDialogRadioThree.text = formatDateSample(DATE_FORMAT_THREE)
            binding.changeDateTimeDialogRadioFour.text = formatDateSample(DATE_FORMAT_FOUR)
            binding.changeDateTimeDialogRadioFive.text = formatDateSample(DATE_FORMAT_FIVE)
            binding.changeDateTimeDialogRadioSix.text = formatDateSample(DATE_FORMAT_SIX)
            binding.changeDateTimeDialogRadioSeven.text = formatDateSample(DATE_FORMAT_SEVEN)
            binding.changeDateTimeDialogRadioEight.text = formatDateSample(DATE_FORMAT_EIGHT)

            val formatButton = when (activity.baseConfig.dateFormat) {
                DATE_FORMAT_ONE -> binding.changeDateTimeDialogRadioOne
                DATE_FORMAT_TWO -> binding.changeDateTimeDialogRadioTwo
                DATE_FORMAT_THREE -> binding.changeDateTimeDialogRadioThree
                DATE_FORMAT_FOUR -> binding.changeDateTimeDialogRadioFour
                DATE_FORMAT_FIVE -> binding.changeDateTimeDialogRadioFive
                DATE_FORMAT_SIX -> binding.changeDateTimeDialogRadioSix
                DATE_FORMAT_SEVEN -> binding.changeDateTimeDialogRadioSeven
                else -> binding.changeDateTimeDialogRadioEight
            }
            formatButton.isChecked = true
        }

        MaterialAlertDialogBuilder(activity)
            .setPositiveButton(R.string.ok) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                activity.setupDialogStuff(view, this)
            }
    }

    private fun dialogConfirmed() {
        activity.baseConfig.dateFormat =
            when (binding.changeDateTimeDialogRadioGroup.checkedRadioButtonId) {
                change_date_time_dialog_radio_one -> DATE_FORMAT_ONE
                change_date_time_dialog_radio_two -> DATE_FORMAT_TWO
                change_date_time_dialog_radio_three -> DATE_FORMAT_THREE
                change_date_time_dialog_radio_four -> DATE_FORMAT_FOUR
                change_date_time_dialog_radio_five -> DATE_FORMAT_FIVE
                change_date_time_dialog_radio_six -> DATE_FORMAT_SIX
                change_date_time_dialog_radio_seven -> DATE_FORMAT_SEVEN
                else -> DATE_FORMAT_EIGHT
            }

        callback()
    }

    companion object {
        val sampleTS = 1613422500000    // February 15, 2021

        fun formatDateSample(format: String): String {
            val cal = Calendar.getInstance(Locale.ENGLISH)
            cal.timeInMillis = sampleTS
            return DateFormat.format(format, cal).toString()
        }
    }
}
