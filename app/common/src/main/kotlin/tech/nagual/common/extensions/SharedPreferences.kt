package tech.nagual.common.extensions

import android.content.SharedPreferences

/* Puts a Double value in SharedPreferences */
fun SharedPreferences.Editor.putDouble(key: String, double: Double) =
    putLong(key, java.lang.Double.doubleToRawLongBits(double))


/* gets a Double value from SharedPreferences */
fun SharedPreferences.getDouble(key: String, default: Double) =
    java.lang.Double.longBitsToDouble(getLong(key, java.lang.Double.doubleToRawLongBits(default)))