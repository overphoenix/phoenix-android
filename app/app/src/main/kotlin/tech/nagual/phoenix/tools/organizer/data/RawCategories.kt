package tech.nagual.phoenix.tools.organizer.data

import me.zhanghai.android.files.util.removeFirst
import tech.nagual.phoenix.tools.organizer.data.model.Category
import tech.nagual.phoenix.tools.organizer.data.model.RawCategory

object RawCategories {
    fun addCategory(
        oldCategories: List<RawCategory>,
        categoryId: Long,
        category: Category
    ): List<RawCategory> {
        val categories = oldCategories.toMutableList()
        categories.apply {
            val index = indexOfFirst { it.id == categoryId }
            if (index != -1) {
                val rawCategory = this[index]
                this[index] = rawCategory.copy(
                    name = category.name
                )
            } else {
                this += RawCategory(
                    id = categoryId,
                    name = category.name,
                    type = category.type,
                    flags = 0L
                )
            }
        }
        return categories
    }

    fun updateCategory(
        oldCategories: List<RawCategory>,
        category: Category
    ): List<RawCategory> {
        val categories = oldCategories.toMutableList()
        categories.apply {
            val index = indexOfFirst { it.id == category.id }
            val rawCategory = this[index]
            this[index] = rawCategory.copy(
                name = category.name
            )
        }
        return categories
    }

    fun deleteCategory(oldCategories: List<RawCategory>, categoryId: Long): List<RawCategory> {
        val categories = oldCategories.toMutableList()
        categories.apply { removeFirst { it.id == categoryId } }
        return categories
    }

    fun moveCategory(
        oldCategories: List<RawCategory>,
        fromPosition: Int,
        toPosition: Int
    ): List<RawCategory> {
        val categories = oldCategories.toMutableList()
        categories.apply { add(toPosition, removeAt(fromPosition)) }
        return categories
    }

    fun updateFlags(
        oldCategories: List<RawCategory>,
        categoryId: Long,
        flags: Long
    ): List<RawCategory> {
        val categories = oldCategories.toMutableList()
        categories.apply {
            val index = indexOfFirst { it.id == categoryId }
            val rawCategory = this[index]
            this[index] = rawCategory.copy(
                flags = flags
            )
        }
        return categories
    }
}