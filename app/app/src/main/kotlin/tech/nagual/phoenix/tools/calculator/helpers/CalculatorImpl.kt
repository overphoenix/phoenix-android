package tech.nagual.phoenix.tools.calculator.helpers

import android.content.Context
import tech.nagual.phoenix.R
import tech.nagual.common.extensions.toast
import net.objecthunter.exp4j.ExpressionBuilder
import tech.nagual.common.extensions.showErrorToast
import tech.nagual.common.helpers.*
import tech.nagual.phoenix.tools.calculator.models.History
import tech.nagual.common.helpers.*
import java.math.BigDecimal

class CalculatorImpl(calculator: Calculator, private val context: Context) {
    private var callback: Calculator? = calculator

    private var baseValue = 0.0
    private var secondValue = 0.0
    private var inputDisplayedFormula = "0"
    private var lastKey = ""
    private var lastOperation = ""
    private val operations = listOf("+", "-", "×", "÷", "^", "%", "√")
    private val operationsRegex = "[-+×÷^%√]".toPattern()
    private val numbersRegex = "[^0-9,.]".toRegex()

    init {
        showNewResult("0")
    }

    private fun addDigit(number: Int) {
        if (inputDisplayedFormula == "0") {
            inputDisplayedFormula = ""
        }

        inputDisplayedFormula += number
        addThousandsDelimiter()
        showNewResult(inputDisplayedFormula)
    }

    private fun zeroClicked() {
        val valueToCheck = inputDisplayedFormula.trimStart('-').replace(",", "")
        val value = valueToCheck.substring(valueToCheck.indexOfAny(operations) + 1)
        if (value != "0" || value.contains(".")) {
            addDigit(0)
        }
    }

    private fun decimalClicked() {
        val valueToCheck = inputDisplayedFormula.trimStart('-').replace(",", "")
        val value = valueToCheck.substring(valueToCheck.indexOfAny(operations) + 1)
        if (!value.contains(".")) {
            when {
                value == "0" && !valueToCheck.contains(operationsRegex.toRegex()) -> inputDisplayedFormula = "0."
                value == "" -> inputDisplayedFormula += "0."
                else -> inputDisplayedFormula += "."
            }
        }

        lastKey = CALC_DECIMAL
        showNewResult(inputDisplayedFormula)
    }

    private fun addThousandsDelimiter() {
        val valuesToCheck = numbersRegex.split(inputDisplayedFormula).filter { it.trim().isNotEmpty() }
        valuesToCheck.forEach {
            var newString = Formatter.addGroupingSeparators(it)
            // allow writing numbers like 0.003
            if (it.contains(".")) {
                newString = newString.substringBefore(".") + ".${it.substringAfter(".")}"
            }

            inputDisplayedFormula = inputDisplayedFormula.replace(it, newString)
        }
    }

    fun handleOperation(operation: String) {
        if (inputDisplayedFormula == Double.NaN.toString()) {
            inputDisplayedFormula = "0"
        }

        if (inputDisplayedFormula == "") {
            inputDisplayedFormula = "0"
        }

        if (operation == CALC_ROOT && inputDisplayedFormula == "0") {
            if (lastKey != CALC_DIGIT) {
                inputDisplayedFormula = "1√"
            }
        }

        val lastChar = inputDisplayedFormula.last().toString()
        if (lastChar == ".") {
            inputDisplayedFormula = inputDisplayedFormula.dropLast(1)
        } else if (operations.contains(lastChar)) {
            inputDisplayedFormula = inputDisplayedFormula.dropLast(1)
            inputDisplayedFormula += getSign(operation)
        } else if (!inputDisplayedFormula.trimStart('-').contains(operationsRegex.toRegex())) {
            inputDisplayedFormula += getSign(operation)
        }

        if (lastKey == CALC_DIGIT || lastKey == CALC_DECIMAL) {
            if (lastOperation != "" && operation == CALC_PERCENT) {
                handlePercent()
            } else {
                // split to multiple lines just to see when does the crash happen
                secondValue = when (operation) {
                    CALC_PLUS -> getSecondValue()
                    CALC_MINUS -> getSecondValue()
                    CALC_MULTIPLY -> getSecondValue()
                    CALC_DIVIDE -> getSecondValue()
                    CALC_ROOT -> getSecondValue()
                    CALC_POWER -> getSecondValue()
                    CALC_PERCENT -> getSecondValue()
                    else -> getSecondValue()
                }

                calculateResult()

                if (!operations.contains(inputDisplayedFormula.last().toString())) {
                    if (!inputDisplayedFormula.contains("÷")) {
                        inputDisplayedFormula += getSign(operation)
                    }
                }
            }
        }

        if (getSecondValue() == 0.0 && inputDisplayedFormula.contains("÷")) {
            lastKey = DIVIDE
            lastOperation = DIVIDE
        } else {
            lastKey = operation
            lastOperation = operation
        }

        showNewResult(inputDisplayedFormula)
    }

    fun turnToNegative(): Boolean {
        if (inputDisplayedFormula.isEmpty()) {
            return false
        }

        if (!inputDisplayedFormula.trimStart('-').any { it.toString() in operations } && inputDisplayedFormula.replace(",", "").toDouble() != 0.0) {
            inputDisplayedFormula = if (inputDisplayedFormula.first() == '-') {
                inputDisplayedFormula.substring(1)
            } else {
                "-$inputDisplayedFormula"
            }

            showNewResult(inputDisplayedFormula)
            return true
        }

        return false
    }

    // handle percents manually, it doesn't seem to be possible via net.objecthunter:exp4j. "%" is used only for modulo there
    // handle cases like 10+200% here
    private fun handlePercent() {
        var result = calculatePercentage(baseValue, getSecondValue(), lastOperation)
        if (result.isInfinite() || result.isNaN()) {
            result = 0.0
        }

        showNewFormula("${baseValue.format()}${getSign(lastOperation)}${getSecondValue().format()}%")
        inputDisplayedFormula = result.format()
        showNewResult(result.format())
        baseValue = result
    }

    fun handleEquals() {
        if (lastKey == CALC_EQUALS) {
            calculateResult()
        }

        if (lastKey != CALC_DIGIT && lastKey != CALC_DECIMAL) {
            return
        }

        secondValue = getSecondValue()
        calculateResult()
        if ((lastOperation == CALC_DIVIDE || lastOperation == CALC_PERCENT) && secondValue == 0.0) {
            lastKey = CALC_DIGIT
            return
        }

        lastKey = CALC_EQUALS
    }

    private fun getSecondValue(): Double {
        val valueToCheck = inputDisplayedFormula.trimStart('-').replace(",", "")
        var value = valueToCheck.substring(valueToCheck.indexOfAny(operations) + 1)
        if (value == "") {
            value = "0"
        }

        return try {
            value.toDouble()
        } catch (e: NumberFormatException) {
            context.showErrorToast(e)
            0.0
        }
    }

    private fun calculateResult() {
        if (lastOperation == CALC_ROOT && inputDisplayedFormula.startsWith("√")) {
            baseValue = 1.0
        }

        if (lastKey != CALC_EQUALS) {
            val valueToCheck = inputDisplayedFormula.trimStart('-').replace(",", "")
            val parts = valueToCheck.split(operationsRegex).filter { it != "" }
            if (parts.isEmpty()) {
                return
            }

            baseValue = Formatter.stringToDouble(parts.first())
            if (inputDisplayedFormula.startsWith("-")) {
                baseValue *= -1
            }

            secondValue = parts.getOrNull(1)?.replace(",", "")?.toDouble() ?: secondValue
        }

        if (lastOperation != "") {
            val sign = getSign(lastOperation)
            val expression = "${baseValue.format()}$sign${secondValue.format()}".replace("√", "sqrt").replace("×", "*").replace("÷", "/")

            try {
                if (sign == "÷" && secondValue == 0.0) {
                    context.toast(R.string.calc_formula_divide_by_zero_error)
                    return
                }

                if (sign == "%") {
                    val second = secondValue / 100f
                    "${baseValue.format()}*${second.format()}".replace("√", "sqrt").replace("×", "*").replace("÷", "/")
                }

                // handle percents manually, it doesn't seem to be possible via net.objecthunter:exp4j. "%" is used only for modulo there
                // handle cases like 10%200 here
                val result = if (sign == "%") {
                    val second = secondValue / 100f
                    ExpressionBuilder("${baseValue.format().replace(",", "")}*${second.format()}").build().evaluate()
                } else {
                    // avoid Double rounding errors at expressions like 5250,74 + 14,98
                    if (sign == "+" || sign == "-") {
                        val first = BigDecimal.valueOf(baseValue)
                        val second = BigDecimal.valueOf(secondValue)
                        val bigDecimalResult = when (sign) {
                            "-" -> first.minus(second)
                            else -> first.plus(second)
                        }
                        bigDecimalResult.toDouble()
                    } else {
                        ExpressionBuilder(expression.replace(",", "")).build().evaluate()
                    }
                }

                if (result.isInfinite() || result.isNaN()) {
                    context.toast(R.string.unknown_error_occurred)
                    return
                }

                showNewResult(result.format())
                baseValue = result
                val newFormula = expression.replace("sqrt", "√").replace("*", "×").replace("/", "÷")
                HistoryHelper(context).insertOrUpdateHistoryEntry(History(null, newFormula, result.format(), System.currentTimeMillis()))
                inputDisplayedFormula = result.format()
                showNewFormula(newFormula)
            } catch (e: Exception) {
                context.toast(R.string.unknown_error_occurred)
            }
        }
    }

    private fun calculatePercentage(baseValue: Double, secondValue: Double, sign: String): Double {
        return when (sign) {
            CALC_MULTIPLY -> {
                val partial = 100 / secondValue
                baseValue / partial
            }
            CALC_DIVIDE -> {
                val partial = 100 / secondValue
                baseValue * partial
            }
            CALC_PLUS -> {
                val partial = baseValue / (100 / secondValue)
                baseValue.plus(partial)
            }
            CALC_MINUS -> {
                val partial = baseValue / (100 / secondValue)
                baseValue.minus(partial)
            }
            CALC_PERCENT -> {
                val partial = (baseValue % secondValue) / 100
                partial
            }
            else -> baseValue / (100 * secondValue)
        }
    }

    private fun showNewResult(value: String) {
        callback!!.showNewResult(value, context)
    }

    private fun showNewFormula(value: String) {
        callback!!.showNewFormula(value, context)
    }

    fun handleClear() {
        val lastDeletedValue = inputDisplayedFormula.lastOrNull().toString()
        var newValue = inputDisplayedFormula.dropLast(1)
        if (newValue == "") {
            newValue = "0"
        } else {
            if (operations.contains(lastDeletedValue) || lastKey == EQUALS) {
                lastOperation = ""
            }
            val lastValue = newValue.last().toString()
            lastKey = when {
                operations.contains(lastValue) -> {
                    CLEAR
                }
                lastValue == "." -> {
                    DECIMAL
                }
                else -> {
                    DIGIT
                }
            }
        }

        newValue = newValue.trimEnd(',')
        inputDisplayedFormula = newValue
        addThousandsDelimiter()
        showNewResult(inputDisplayedFormula)
    }

    fun handleReset() {
        resetValues()
        showNewResult("0")
        showNewFormula("")
        inputDisplayedFormula = ""
    }

    private fun resetValues() {
        baseValue = 0.0
        secondValue = 0.0
        lastKey = ""
        lastOperation = ""
    }

    private fun getSign(lastOperation: String) = when (lastOperation) {
        CALC_MINUS -> "-"
        CALC_MULTIPLY -> "×"
        CALC_DIVIDE -> "÷"
        CALC_PERCENT -> "%"
        CALC_POWER -> "^"
        CALC_ROOT -> "√"
        else -> "+"
    }

    fun numpadClicked(id: Int) {
        if (inputDisplayedFormula == Double.NaN.toString()) {
            inputDisplayedFormula = ""
        }

        if (lastKey == CALC_EQUALS) {
            lastOperation = CALC_EQUALS
        }

        lastKey = CALC_DIGIT

        when (id) {
            R.id.btn_decimal -> decimalClicked()
            R.id.btn_0 -> zeroClicked()
            R.id.btn_1 -> addDigit(1)
            R.id.btn_2 -> addDigit(2)
            R.id.btn_3 -> addDigit(3)
            R.id.btn_4 -> addDigit(4)
            R.id.btn_5 -> addDigit(5)
            R.id.btn_6 -> addDigit(6)
            R.id.btn_7 -> addDigit(7)
            R.id.btn_8 -> addDigit(8)
            R.id.btn_9 -> addDigit(9)
        }
    }

    fun addNumberToFormula(number: String) {
        handleReset()
        inputDisplayedFormula = number
        addThousandsDelimiter()
        showNewResult(inputDisplayedFormula)
    }
}
