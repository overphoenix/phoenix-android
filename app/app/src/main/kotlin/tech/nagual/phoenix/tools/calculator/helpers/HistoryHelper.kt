package tech.nagual.phoenix.tools.calculator.helpers

import android.content.Context
import android.os.Handler
import android.os.Looper
import tech.nagual.app.ensureBackgroundThread
import tech.nagual.phoenix.tools.calculator.extensions.calculatorDB
import tech.nagual.phoenix.tools.calculator.models.History

class HistoryHelper(val context: Context) {
    fun getHistory(callback: (calculations: ArrayList<History>) -> Unit) {
        ensureBackgroundThread {
            val notes = context.calculatorDB.getHistory() as ArrayList<History>

            Handler(Looper.getMainLooper()).post {
                callback(notes)
            }
        }
    }

    fun insertOrUpdateHistoryEntry(entry: History) {
        ensureBackgroundThread {
            context.calculatorDB.insertOrUpdate(entry)
        }
    }
}
