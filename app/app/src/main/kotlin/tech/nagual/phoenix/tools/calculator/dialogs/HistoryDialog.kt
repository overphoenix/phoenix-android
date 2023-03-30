package tech.nagual.phoenix.tools.calculator.dialogs

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import tech.nagual.app.ensureBackgroundThread
import tech.nagual.common.extensions.setupDialogStuff
import me.zhanghai.android.files.util.showToast
import tech.nagual.common.R
import tech.nagual.phoenix.tools.calculator.activities.CalculatorActivity
import tech.nagual.phoenix.tools.calculator.adapters.HistoryAdapter
import tech.nagual.phoenix.tools.calculator.extensions.calculatorDB
import tech.nagual.phoenix.tools.calculator.helpers.CalculatorImpl
import tech.nagual.phoenix.tools.calculator.models.History
import tech.nagual.phoenix.databinding.CalcDialogHistoryBinding

class HistoryDialog() {

    constructor(
        activity: CalculatorActivity,
        items: List<History>,
        calculator: CalculatorImpl
    ) : this() {
        val binding = CalcDialogHistoryBinding.inflate(activity.layoutInflater)

        val dialog = MaterialAlertDialogBuilder(activity)
            .setPositiveButton(R.string.ok, null)
            .setNeutralButton(R.string.clear_history) { _, _ ->
                ensureBackgroundThread {
                    activity.applicationContext.calculatorDB.deleteHistory()
                    activity.showToast(R.string.history_cleared)
                }
            }.create().apply {
                activity.setupDialogStuff(binding.root, this, R.string.history)
            }

        binding.historyList.adapter = HistoryAdapter(activity, items, calculator) {
            dialog.dismiss()
        }
    }
}
