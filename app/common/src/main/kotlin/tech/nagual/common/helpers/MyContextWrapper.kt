package tech.nagual.common.helpers

import android.annotation.TargetApi
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import java.util.*

// language forcing used at "Use english language", taken from https://stackoverflow.com/a/40704077/1967672
class MyContextWrapper(context: Context) : ContextWrapper(context) {

    fun wrap(context: Context, language: String): ContextWrapper {
        var newContext = context
        val config = newContext.resources.configuration
        val sysLocale: Locale? = getSystemLocale(config)

        if (language != "" && sysLocale!!.language != language) {
            val locale = Locale(language)
            Locale.setDefault(locale)
            setSystemLocale(config, locale)
        }

        newContext = newContext.createConfigurationContext(config)
        return MyContextWrapper(newContext)
    }

    private fun getSystemLocaleLegacy(config: Configuration) = config.locale

    @TargetApi(Build.VERSION_CODES.N)
    private fun getSystemLocale(config: Configuration) = config.locales.get(0)

    private fun setSystemLocaleLegacy(config: Configuration, locale: Locale) {
        config.locale = locale
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun setSystemLocale(config: Configuration, locale: Locale) {
        config.setLocale(locale)
    }
}
