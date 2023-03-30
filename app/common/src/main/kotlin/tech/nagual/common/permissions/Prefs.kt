package tech.nagual.common.permissions

import android.content.Context
import android.content.Context.MODE_PRIVATE

internal interface Prefs {
    fun set(
        key: String,
        value: Any
    )

    operator fun <T : Any> get(key: String): T?
}

internal class RealPrefs(context: Context) : Prefs {
    private val sharedPrefs = context.getSharedPreferences(KEY_ASSENT_PREFS, MODE_PRIVATE)

    override fun set(
        key: String,
        value: Any
    ) {
        with(sharedPrefs.edit()) {
            when (value) {
                is String -> putString(key, value)
                is Boolean -> putBoolean(key, value)
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is Float -> putFloat(key, value)
                else -> error("Cannot put value $value in shared preferences.")
            }
            apply()
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(key: String): T? {
        return sharedPrefs.all[key] as? T
    }
}

private const val KEY_ASSENT_PREFS = "[tech.nagual.phoenix.common.permissions-prefs]"
