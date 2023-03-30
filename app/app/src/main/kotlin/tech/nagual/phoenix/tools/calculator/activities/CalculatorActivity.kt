package tech.nagual.phoenix.tools.calculator.activities

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import me.grantland.widget.AutofitHelper
import tech.nagual.common.extensions.performHapticFeedback
import me.zhanghai.android.files.util.showToast
import tech.nagual.phoenix.databinding.CalcActivityMainBinding
import tech.nagual.phoenix.tools.calculator.databases.CalculatorDatabase
import tech.nagual.phoenix.tools.calculator.dialogs.HistoryDialog
import tech.nagual.phoenix.tools.calculator.helpers.Calculator
import tech.nagual.phoenix.tools.calculator.helpers.CalculatorImpl
import tech.nagual.phoenix.tools.calculator.helpers.HistoryHelper
import tech.nagual.common.R
import tech.nagual.common.activities.BaseSimpleActivity
import tech.nagual.common.extensions.appLaunched
import tech.nagual.common.extensions.value
import tech.nagual.common.helpers.*
import tech.nagual.common.extensions.copyToClipboard

class CalculatorActivity : tech.nagual.common.activities.BaseSimpleActivity(), Calculator {
    private lateinit var binding: CalcActivityMainBinding

    private var vibrateOnButtonPress = true

    lateinit var calc: CalculatorImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CalcActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        appLaunched()

        calc = CalculatorImpl(this, applicationContext)

        binding.btnPlus.setOnClickListener { calc.handleOperation(CALC_PLUS); checkHaptic(it) }
        binding.btnMinus.setOnClickListener { calc.handleOperation(CALC_MINUS); checkHaptic(it) }
        binding.btnMultiply.setOnClickListener { calc.handleOperation(CALC_MULTIPLY); checkHaptic(it) }
        binding.btnDivide.setOnClickListener { calc.handleOperation(CALC_DIVIDE); checkHaptic(it) }
        binding.btnPercent.setOnClickListener { calc.handleOperation(CALC_PERCENT); checkHaptic(it) }
        binding.btnPower.setOnClickListener { calc.handleOperation(CALC_POWER); checkHaptic(it) }
        binding.btnRoot.setOnClickListener { calc.handleOperation(CALC_ROOT); checkHaptic(it) }

        binding.btnMinus.setOnLongClickListener {
            calc.turnToNegative()
        }

        binding.btnClear.setOnClickListener { calc.handleClear(); checkHaptic(it) }
        binding.btnClear.setOnLongClickListener { calc.handleReset(); true }

        getButtonIds().forEach {
            it.setOnClickListener { calc.numpadClicked(it.id); checkHaptic(it) }
        }

        binding.btnEquals.setOnClickListener { calc.handleEquals(); checkHaptic(it) }
        binding.formula.setOnLongClickListener { copyToClipboard(false) }
        binding.result.setOnLongClickListener { copyToClipboard(true) }

        AutofitHelper.create(binding.result)
        AutofitHelper.create(binding.formula)
        checkAppOnSDCard()
    }

    override fun onResume() {
        super.onResume()

        vibrateOnButtonPress = config.vibrateOnButtonPress

        val adjustedPrimaryColor = getColor(R.color.color_primary)
        arrayOf(
            binding.btnPercent,
            binding.btnPower,
            binding.btnRoot,
            binding.btnClear,
            binding.btnReset,
            binding.btnDivide,
            binding.btnMultiply,
            binding.btnPlus,
            binding.btnMinus,
            binding.btnEquals,
            binding.btnDecimal
        ).forEach {
            it.setTextColor(adjustedPrimaryColor)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            CalculatorDatabase.destroyInstance()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.calc_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.history -> showHistory()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun showHistory() {
        HistoryHelper(this).getHistory {
            if (it.isEmpty()) {
                showToast(R.string.history_empty)
            } else {
                HistoryDialog(this, it, calc)
            }
        }
    }

    private fun checkHaptic(view: View) {
        if (vibrateOnButtonPress) {
            view.performHapticFeedback()
        }
    }

    private fun getButtonIds() =
        arrayOf(
            binding.btnDecimal,
            binding.btn0,
            binding.btn1,
            binding.btn2,
            binding.btn3,
            binding.btn4,
            binding.btn5,
            binding.btn6,
            binding.btn7,
            binding.btn8,
            binding.btn9
        )

    private fun copyToClipboard(copyResult: Boolean): Boolean {
        var value = binding.formula.value
        if (copyResult) {
            value = binding.result.value
        }

        return if (value.isEmpty()) {
            false
        } else {
            copyToClipboard(value)
            true
        }
    }

    override fun showNewResult(value: String, context: Context) {
        binding.result.text = value
    }

    override fun showNewFormula(value: String, context: Context) {
        binding.formula.text = value
    }
}
