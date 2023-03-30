package tech.nagual.phoenix.tools.gps.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder

object GsonUtil {
    fun getCustomGson(): Gson {
        val gsonBuilder = GsonBuilder()
        gsonBuilder.setDateFormat("dd.MM.yyyy HH:mm")
        gsonBuilder.excludeFieldsWithoutExposeAnnotation()
        return gsonBuilder.create()
    }
}