package tech.nagual.phoenix.tools.organizer.categories

import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import tech.nagual.phoenix.R
import tech.nagual.phoenix.tools.organizer.data.model.Category
import tech.nagual.phoenix.tools.organizer.data.model.CategoryType
import tech.nagual.phoenix.tools.organizer.data.model.RawCategory
import tech.nagual.phoenix.tools.organizer.data.repo.CategoriesRepository
import tech.nagual.phoenix.tools.organizer.utils.navigateSafely
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    val categoriesRepository: CategoriesRepository
) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    val categories: StateFlow<List<Category>> =
        categoriesRepository.getAll()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = listOf()
            )

    private fun getCategoryById(categoryId: Long): Flow<Category?> =
        categoriesRepository.getById(categoryId)

    fun deleteCategories(categoryItemSet: CategoryItemSet) {
        val categories = categoryItemSet.toTypedArray()
        deleteCategories(*categories)
    }

    fun deleteCategories(vararg categories: Category) {
        viewModelScope.launch(Dispatchers.IO) {
            categoriesRepository.delete(*categories)
        }
    }

    fun openCategory(rawCategory: RawCategory, fragment: Fragment) {
        fragment.findNavController().navigateSafely(
            R.id.organizer_manage_variants_fragment,
            bundleOf(
                "categoryId" to rawCategory.id,
                "categoryType" to rawCategory.type,
                "categoryName" to rawCategory.name
            )
        )
    }

    suspend fun editCategory(rawCategory: RawCategory, fragment: Fragment, workflowId: Long = 0) {
        val category = getCategoryById(rawCategory.id).first()

        when (rawCategory.type) {
            CategoryType.Variants,
            CategoryType.ExVariants -> CategoryVariantsEditDialogFragment.show(
                category,
                rawCategory.type,
                fragment,
                workflowId
            )
            CategoryType.AutoIncrement -> CategoryAutoincrementEditDialogFragment.show(
                category,
                rawCategory.type,
                fragment,
                workflowId
            )
            CategoryType.Geo -> CategoryGeoEditDialogFragment.show(
                category,
                rawCategory.type,
                fragment,
                workflowId
            )
            CategoryType.DateTime -> CategoryDatetimeEditDialogFragment.show(
                category,
                rawCategory.type,
                fragment,
                workflowId
            )
            CategoryType.Password -> CategoryPasswordEditDialogFragment.show(
                category,
                rawCategory.type,
                fragment,
                workflowId
            )
        }
    }
}
