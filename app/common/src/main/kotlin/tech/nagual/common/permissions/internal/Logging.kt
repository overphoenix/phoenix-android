package tech.nagual.common.permissions.internal

import android.util.Log
import tech.nagual.common.BuildConfig

internal fun Any.log(
    message: String,
    vararg args: Any?
) {
    if (BuildConfig.DEBUG) {
        try {
            Log.d(this::class.java.simpleName, message.format(*args))
        } catch (_: Exception) {
        }
    }
}

internal fun Any.warn(
    message: String,
    vararg args: Any?
) {
    if (BuildConfig.DEBUG) {
        try {
            Log.w(this::class.java.simpleName, message.format(*args))
        } catch (_: Exception) {
        }
    }
}
