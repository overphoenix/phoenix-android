package tech.nagual.phoenix.tools.calculator.extensions

import android.content.Context
import tech.nagual.phoenix.tools.calculator.databases.CalculatorDatabase
import tech.nagual.phoenix.tools.calculator.interfaces.CalculatorDao

val Context.calculatorDB: CalculatorDao
    get() = CalculatorDatabase.getInstance(applicationContext).CalculatorDao()
