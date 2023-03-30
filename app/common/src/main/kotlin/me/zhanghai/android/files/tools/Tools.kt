package me.zhanghai.android.files.tools

import androidx.annotation.DrawableRes
import me.zhanghai.android.files.util.removeFirst
import me.zhanghai.android.files.util.valueCompat
import tech.nagual.settings.Settings

object Tools {
    data class ToolInfo(
        var packageName: String = "",
        @DrawableRes
        var iconRes: Int = -1
    );

    // Initialize these during application initialization.
    lateinit var infos: Map<String, ToolInfo>
    lateinit var list: List<Tool>

    fun addOrReplace(tool: Tool) {
        val tools = Settings.TOOLS.valueCompat.toMutableList().apply {
            val index = indexOfFirst { it.id == tool.id }
            if (index != -1) {
                this[index] = tool
            } else {
                this += tool
            }
        }
        Settings.TOOLS.putValue(tools)
    }

    fun replace(tool: Tool) {
        val tools = Settings.TOOLS.valueCompat.toMutableList()
            .apply { this[indexOfFirst { it.id == tool.id }] = tool }
        Settings.TOOLS.putValue(tools)
    }

    fun move(fromPosition: Int, toPosition: Int) {
        val tools = Settings.TOOLS.valueCompat.toMutableList()
            .apply { add(toPosition, removeAt(fromPosition)) }
        Settings.TOOLS.putValue(tools)
    }

    fun remove(tool: Tool) {
        val tools = Settings.TOOLS.valueCompat.toMutableList()
            .apply { removeFirst { it.id == tool.id } }
        Settings.TOOLS.putValue(tools)
    }
}
