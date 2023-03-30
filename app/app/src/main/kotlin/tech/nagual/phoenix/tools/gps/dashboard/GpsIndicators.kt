package tech.nagual.phoenix.tools.gps.dashboard

import me.zhanghai.android.files.util.removeFirst
import me.zhanghai.android.files.util.valueCompat
import tech.nagual.phoenix.tools.gps.GpsManager

object GpsIndicators {
    fun addOrReplace(indicator: GpsIndicator) {
        val indicators = GpsManager.INDICATORS.valueCompat.toMutableList().apply {
            val index = indexOfFirst { it.id == indicator.id }
            if (index != -1) {
                this[index] = indicator
            } else {
                this += indicator
            }
        }
        GpsManager.INDICATORS.putValue(indicators)
    }

    fun replace(indicator: GpsIndicator) {
        val indicators = GpsManager.INDICATORS.valueCompat.toMutableList()
            .apply { this[indexOfFirst { it.id == indicator.id }] = indicator }
        GpsManager.INDICATORS.putValue(indicators)
    }

    fun move(fromPosition: Int, toPosition: Int) {
        val indicators = GpsManager.INDICATORS.valueCompat.toMutableList()
            .apply { add(toPosition, removeAt(fromPosition)) }
        GpsManager.INDICATORS.putValue(indicators)
    }

    fun remove(indicator: GpsIndicator) {
        val indicators = GpsManager.INDICATORS.valueCompat.toMutableList()
            .apply { removeFirst { it.id == indicator.id } }
        GpsManager.INDICATORS.putValue(indicators)
    }
}
