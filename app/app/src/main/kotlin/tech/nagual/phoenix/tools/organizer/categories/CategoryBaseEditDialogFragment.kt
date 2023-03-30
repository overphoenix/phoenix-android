package tech.nagual.phoenix.tools.organizer.categories

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import tech.nagual.common.ui.simpledialogs.BaseMaterialDialogFragment
import me.zhanghai.android.files.util.ParcelableArgs
import me.zhanghai.android.files.util.args
import me.zhanghai.android.files.util.setTextWithSelection
import tech.nagual.common.R
import tech.nagual.phoenix.tools.organizer.ActivityViewModel
import tech.nagual.phoenix.tools.organizer.OrganizersManager
import tech.nagual.phoenix.tools.organizer.data.model.Category
import tech.nagual.phoenix.tools.organizer.data.model.CategoryType
import tech.nagual.phoenix.tools.organizer.data.repo.CategoriesRepository
import tech.nagual.phoenix.tools.organizer.data.repo.WorkflowRepository
import javax.inject.Inject

@AndroidEntryPoint
abstract class CategoryBaseEditDialogFragment : BaseMaterialDialogFragment() {
    protected val args by args<Args>()

    lateinit var category: Category

    val activityModel: ActivityViewModel by activityViewModels()

    @Inject
    lateinit var categoriesRepository: CategoriesRepository

    @Inject
    lateinit var workflowRepository: WorkflowRepository

    protected val isNewCategory: Boolean
        get() = !this::category.isInitialized

    override fun getTitle(): String {
        return getString(
            R.string.category_title,
            OrganizersManager.getCategoryTypeName(args.categoryType)
        )
    }

    override fun createView(savedInstanceState: Bundle?): View {
        TODO("Not yet implemented")
    }

    override fun customizeDialog(builder: MaterialAlertDialogBuilder) {
        val category = args.category
        if (category != null) {
            builder.setNeutralButton(R.string.remove) { _, _ -> deleteCategory(category) }
        }
    }

    protected fun getFieldValue(editText: TextInputEditText, layout: TextInputLayout): String =
        (if (editText.text.isNullOrEmpty()) layout.placeholderText else editText.text).toString()

    protected fun setFieldValue(
        editText: TextInputEditText,
        layout: TextInputLayout,
        value: String,
        withSelection: Boolean = false
    ) {
//        if (value != layout.placeholderText) {
        when (withSelection) {
            true -> editText.setTextWithSelection(value)
            false -> editText.setText(value)
//            }
        }
    }

    private suspend fun showExistsToast(name: String) {
        withContext(Dispatchers.Main) {
            Toast
                .makeText(
                    requireContext(),
                    getString(R.string.category_already_exists, name),
                    Toast.LENGTH_SHORT
                )
                .show()
        }
    }

    protected suspend fun saveOrUpdateCommonFields(name: String): Long? {
        when (isNewCategory) {
            true -> {
                val exists = categoryExistsByName(name)
                if (!exists) {
                    val category = Category(
                        name = name,
                        type = args.categoryType
                    )
                    return categoriesRepository.insertEverywhere(
                        category,
                        OrganizersManager.activeOrganizer
                    )
                }
                showExistsToast(name)
            }
            else -> {
                val exists = categoryExistsByName(name, ignoreId = category.id)
                if (!exists) {
                    val category = category.copy(name = name)
                    categoriesRepository.updateEverywhere(
                        category,
                        OrganizersManager.activeOrganizer
                    )
                    return category.id
                }
                showExistsToast(name)
            }
        }
        return null
    }

    protected fun deleteCategory(category: Category) {
        lifecycleScope.launch(Dispatchers.IO) {
            // Delete from categories table
            categoriesRepository.deleteEverywhere(category, OrganizersManager.activeOrganizer)
        }
    }

    protected suspend fun categoryExistsByName(name: String, ignoreId: Long? = null): Boolean {
        val category =
            categoriesRepository.getByName(name).first()
        return category != null && (if (ignoreId != null) category.id != ignoreId else true)
    }

    @Parcelize
    class Args(val category: Category?, val categoryType: CategoryType, val workflowId: Long = 0) : ParcelableArgs
}
