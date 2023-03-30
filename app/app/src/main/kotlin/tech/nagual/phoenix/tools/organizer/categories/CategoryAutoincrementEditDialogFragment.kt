package tech.nagual.phoenix.tools.organizer.categories

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.files.util.layoutInflater
import me.zhanghai.android.files.util.putArgs
import me.zhanghai.android.files.util.show
import tech.nagual.common.R
import tech.nagual.phoenix.databinding.OrganizerCategoryAutoincrementEditDialogBinding
import tech.nagual.phoenix.tools.organizer.data.model.Category
import tech.nagual.phoenix.tools.organizer.data.model.CategoryType
import tech.nagual.phoenix.tools.organizer.data.model.CategoryVariable
import tech.nagual.phoenix.tools.organizer.data.repo.CategoriesRepository

@AndroidEntryPoint
class CategoryAutoincrementEditDialogFragment : CategoryBaseEditDialogFragment() {
    private lateinit var binding: OrganizerCategoryAutoincrementEditDialogBinding

    override fun createView(savedInstanceState: Bundle?): View {
        binding =
            OrganizerCategoryAutoincrementEditDialogBinding.inflate(requireContext().layoutInflater)

        binding.nameLayout.placeholderText = getString(R.string.category_display_name_placeholder)
        binding.curValueLayout.placeholderText = VAR_CURRENT_DEFAULT.toString()
//        binding.minValueLayout.placeholderText = VAR_MINIMUM_DEFAULT.toString()
//        binding.maxValueLayout.placeholderText = VAR_MAXIMUM_DEFAULT.toString()
        binding.incValueLayout.placeholderText = VAR_INCREMENT_DEFAULT.toString()

        if (savedInstanceState == null && args.category != null) {
            category = args.category!!
            setFieldValue(binding.nameEdit, binding.nameLayout, category.name, true)
        }

        return binding.root
    }

    override fun onAfterDialogCreated(dialog: Dialog) {
//        var minimum = VAR_MINIMUM_DEFAULT
//        var maximum = VAR_MAXIMUM_DEFAULT
        var current = VAR_CURRENT_DEFAULT
        var increment = VAR_INCREMENT_DEFAULT


        if (isNewCategory) {
            setFieldValue(binding.curValueEdit, binding.curValueLayout, current.toString())
//            setFieldValue(binding.minValueEdit, binding.minValueLayout, minimum.toString())
//            setFieldValue(binding.maxValueEdit, binding.maxValueLayout, maximum.toString())
            setFieldValue(binding.incValueEdit, binding.incValueLayout, increment.toString())
        } else {
            lifecycleScope.launch(Dispatchers.IO) {
                current = categoriesRepository.getVariable(
                    category.id,
                    CategoriesRepository.getVariableName(
                        CategoriesRepository.VAR_AUTOINC_CURRENT,
                        args.workflowId
                    )
                )
                    .first()?.value?.toLong() ?: VAR_CURRENT_DEFAULT
//                minimum = categoriesRepository.getVariable(category.id, VAR_MINIMUM)
//                    .first()?.value?.toLong() ?: VAR_MINIMUM_DEFAULT
//                maximum = categoriesRepository.getVariable(category.id, VAR_MAXIMUM)
//                    .first()?.value?.toLong() ?: VAR_MAXIMUM_DEFAULT
                increment = categoriesRepository.getVariable(
                    category.id,
                    CategoriesRepository.getVariableName(
                        CategoriesRepository.VAR_AUTOINC_INCREMENT,
                        args.workflowId
                    )
                )
                    .first()?.value?.toLong() ?: VAR_INCREMENT_DEFAULT

                withContext(Dispatchers.Main) {
                    setFieldValue(binding.curValueEdit, binding.curValueLayout, current.toString())
//                    setFieldValue(binding.minValueEdit, binding.minValueLayout, minimum.toString())
//                    setFieldValue(binding.maxValueEdit, binding.maxValueLayout, maximum.toString())
                    setFieldValue(
                        binding.incValueEdit,
                        binding.incValueLayout,
                        increment.toString()
                    )
                }
            }
        }
    }

    override fun submit() {
        val name = getFieldValue(binding.nameEdit, binding.nameLayout)
        val current = getFieldValue(binding.curValueEdit, binding.curValueLayout)
//        val minimum = getFieldValue(binding.minValueEdit, binding.minValueLayout)
//        val maximum = getFieldValue(binding.maxValueEdit, binding.maxValueLayout)
        val increment = getFieldValue(binding.incValueEdit, binding.incValueLayout)

//        if (minimum.toLong() >= maximum.toLong()) {
//            Toast
//                .makeText(
//                    requireContext(),
//                    getString(R.string.category_autoincrement_min_greater_max),
//                    Toast.LENGTH_SHORT
//                )
//                .show()
//            return
//        }

        lifecycleScope.launch(Dispatchers.IO) {
            val categoryId = saveOrUpdateCommonFields(name)
            if (categoryId != null) {
                updateVariables(categoryId, current, /*minimum, maximum, */increment)
                return@launch dismiss()
            }
        }
    }

    private suspend fun updateVariables(
        categoryId: Long,
        current: String,
//        minimum: String,
//        maximum: String,
        increment: String
    ) {
        categoriesRepository.setVariable(
            CategoryVariable(
                name = CategoriesRepository.getVariableName(
                    CategoriesRepository.VAR_AUTOINC_CURRENT,
                    args.workflowId
                ),
                value = current,
                categoryId = categoryId
            )
        )
//        categoriesRepository.setVariable(
//            CategoryVariable(
//                name = VAR_MINIMUM,
//                value = minimum,
//                categoryId = categoryId
//            )
//        )
//        categoriesRepository.setVariable(
//            CategoryVariable(
//                name = VAR_MAXIMUM,
//                value = maximum,
//                categoryId = categoryId
//            )
//        )
        categoriesRepository.setVariable(
            CategoryVariable(
                name = CategoriesRepository.getVariableName(
                    CategoriesRepository.VAR_AUTOINC_INCREMENT,
                    args.workflowId
                ),
                value = increment,
                categoryId = categoryId
            )
        )
    }

    companion object {
        const val VAR_CURRENT_DEFAULT = 1L

//        const val VAR_MINIMUM_DEFAULT = 1L
//        const val VAR_MAXIMUM_DEFAULT = 100L

        const val VAR_INCREMENT_DEFAULT = 1L

        fun show(
            category: Category?,
            categoryType: CategoryType,
            fragment: Fragment,
            workflowId: Long = 0
        ) {
            CategoryAutoincrementEditDialogFragment().putArgs(
                Args(
                    category,
                    categoryType,
                    workflowId
                )
            )
                .show(fragment)
        }
    }
}
