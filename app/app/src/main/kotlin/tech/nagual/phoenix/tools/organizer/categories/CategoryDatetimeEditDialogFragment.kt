package tech.nagual.phoenix.tools.organizer.categories

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.zhanghai.android.files.util.layoutInflater
import me.zhanghai.android.files.util.putArgs
import me.zhanghai.android.files.util.show
import tech.nagual.common.R
import tech.nagual.phoenix.databinding.OrganizerCategoryDatetimeEditDialogBinding
import tech.nagual.phoenix.tools.organizer.data.model.Category
import tech.nagual.phoenix.tools.organizer.data.model.CategoryType

@AndroidEntryPoint
class CategoryDatetimeEditDialogFragment : CategoryBaseEditDialogFragment() {
    private lateinit var binding: OrganizerCategoryDatetimeEditDialogBinding

    override fun createView(savedInstanceState: Bundle?): View {
        binding =
            OrganizerCategoryDatetimeEditDialogBinding.inflate(requireContext().layoutInflater)

        binding.nameLayout.placeholderText = getString(R.string.category_display_name_placeholder)
        if (savedInstanceState == null && args.category != null) {
            category = args.category!!
            setFieldValue(binding.nameEdit, binding.nameLayout, category.name, true)
        }

        return binding.root
    }

    override fun submit() {
        val name = getFieldValue(binding.nameEdit, binding.nameLayout)

        lifecycleScope.launch(Dispatchers.IO) {
            if (saveOrUpdateCommonFields(name) != null) {
                return@launch dismiss()
            }
        }
    }

    companion object {
        fun show(category: Category?, categoryType: CategoryType, fragment: Fragment, workflowId: Long = 0) {
            CategoryDatetimeEditDialogFragment().putArgs(Args(category, categoryType, workflowId))
                .show(fragment)
        }
    }
}
