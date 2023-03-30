package tech.nagual.phoenix.tools.organizer.categories

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.nagual.common.ui.simpledialogs.SimpleDialog
import tech.nagual.common.ui.simpledialogs.list.SimpleListDialog
import me.zhanghai.android.files.util.layoutInflater
import me.zhanghai.android.files.util.putArgs
import me.zhanghai.android.files.util.show
import tech.nagual.common.R
import tech.nagual.phoenix.databinding.OrganizerCategoryVariantsEditDialogBinding
import tech.nagual.phoenix.tools.organizer.data.model.Category
import tech.nagual.phoenix.tools.organizer.data.model.CategoryType
import tech.nagual.phoenix.tools.organizer.data.model.CategoryVariable
import tech.nagual.phoenix.tools.organizer.data.model.Variant
import tech.nagual.phoenix.tools.organizer.data.repo.CategoriesRepository

@AndroidEntryPoint
class CategoryVariantsEditDialogFragment : CategoryBaseEditDialogFragment(),
    tech.nagual.common.ui.simpledialogs.SimpleDialog.OnDialogResultListener {
    private lateinit var binding: OrganizerCategoryVariantsEditDialogBinding

    private var newExVariants = mutableListOf<Variant>()
    private var exId = 0L

    override fun createView(savedInstanceState: Bundle?): View {
        binding =
            OrganizerCategoryVariantsEditDialogBinding.inflate(requireContext().layoutInflater)

        binding.nameLayout.placeholderText = getString(R.string.category_display_name_placeholder)
        if (savedInstanceState == null && args.category != null) {
            category = args.category!!
            binding.nameLayout.placeholderText = category.name
            setFieldValue(binding.nameEdit, binding.nameLayout, category.name, true)
        }

        return binding.root
    }

    private fun showVariantsSelectionDialog(variants: List<Variant>) {
        tech.nagual.common.ui.simpledialogs.list.SimpleListDialog.build()
            .title(R.string.organizer_category_choose_one)
            .choiceMode(tech.nagual.common.ui.simpledialogs.list.SimpleListDialog.SINGLE_CHOICE_DIRECT)
            .items(variants.map { it.value }.toTypedArray())
            .show(this, VARIANT_CHOICE)
    }

    private suspend fun updateDefaultField(value: String) {
        withContext(Dispatchers.Main) {
            setFieldValue(
                binding.defValueText,
                binding.defValueLayout,
                value
            )
        }
    }

    override fun onAfterDialogCreated(dialog: Dialog) {
        if (!isNewCategory) {
            lifecycleScope.launch(Dispatchers.IO) {
                val variants =
                    categoriesRepository.getVariantsByCategoryId(args.category!!.id).first()

                if (variants.isNotEmpty()) {
                    binding.defValueLayout.isVisible = true
                    binding.defValueLayout.setDropDown(true)
                    binding.defValueText.setTextIsSelectable(false)
                    binding.defValueText.setOnClickListener {
                        newExVariants.clear()
                        showVariantsSelectionDialog(variants)
                    }

                    if (category.type == CategoryType.ExVariants) {
                        val exVariants = categoriesRepository.getDefaultExVariantForCategory(
                            category.id,
                            args.workflowId
                        )
                        if (exVariants.isNotEmpty()) {
                            updateDefaultField(exVariants.joinToString("\n") { it.value })
                        }
                    } else {
                        val defaultVariant = categoriesRepository.getDefaultVariantForCategory(
                            category.id,
                            args.workflowId
                        )
                        if (defaultVariant != null) {
                            updateDefaultField(defaultVariant.value)
                        }
                    }
                }
            }
        }
    }

    override fun submit() {
        val name = getFieldValue(binding.nameEdit, binding.nameLayout)

        lifecycleScope.launch(Dispatchers.IO) {
            if (saveOrUpdateCommonFields(name) != null) {
                if (args.category != null) {
                    if (category.type == CategoryType.ExVariants) {
                        for (variant in newExVariants) {
                            categoriesRepository.setVariable(
                                CategoryVariable(
                                    name = CategoriesRepository.getVariableName(
                                        CategoriesRepository.VAR_VARIANT_DEFAULT_VALUE,
                                        args.workflowId,
                                        variant.parentId
                                    ),
                                    value = variant.value,
                                    categoryId = category.id
                                )
                            )
                        }
                        newExVariants.clear()
                    } else {
                        categoriesRepository.setVariable(
                            CategoryVariable(
                                name = CategoriesRepository.getVariableName(
                                    CategoriesRepository.VAR_VARIANT_DEFAULT_VALUE,
                                    args.workflowId
                                ),
                                value = getFieldValue(binding.defValueText, binding.defValueLayout),
                                categoryId = category.id
                            )
                        )
                    }
                }
                return@launch dismiss()
            }
        }
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        when (which) {
            tech.nagual.common.ui.simpledialogs.SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE -> {
                if (VARIANT_CHOICE == dialogTag) {
                    val variantValue = extras.getString(tech.nagual.common.ui.simpledialogs.list.SimpleListDialog.SELECTED_SINGLE_LABEL)
                    if (category.type == CategoryType.ExVariants) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            val subVariant = categoriesRepository.getVariantByValue(category.id, variantValue!!).first()
                            newExVariants.add(subVariant!!)
                            exId = subVariant.id
                            val variants =
                                categoriesRepository.getVariantsByCategoryId(category.id, exId)
                                    .first()
                            if (variants.isNotEmpty()) {
                                withContext(Dispatchers.Main) {
                                    showVariantsSelectionDialog(variants)
                                }
                            } else {
                                updateDefaultField(newExVariants.joinToString("\n") { it.value })
                            }
                        }
                    } else {
                        setFieldValue(binding.defValueText, binding.defValueLayout, variantValue!!)
                    }
                    return true
                }
                return false
            }
            tech.nagual.common.ui.simpledialogs.SimpleDialog.OnDialogResultListener.BUTTON_NEGATIVE -> {
                if (category.type == CategoryType.ExVariants) {
                    newExVariants.clear()
                }
            }
        }
        return false
    }

    companion object {
        const val VARIANT_CHOICE = "variantChoice"

        fun show(
            category: Category?,
            categoryType: CategoryType,
            fragment: Fragment,
            workflowId: Long = 0
        ) {
            CategoryVariantsEditDialogFragment().putArgs(Args(category, categoryType, workflowId))
                .show(fragment)
        }
    }
}
