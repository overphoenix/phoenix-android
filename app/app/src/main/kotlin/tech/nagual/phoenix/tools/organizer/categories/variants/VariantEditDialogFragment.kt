package tech.nagual.phoenix.tools.organizer.categories.variants

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import tech.nagual.common.ui.simpledialogs.BaseMaterialDialogFragment
import me.zhanghai.android.files.util.*
import tech.nagual.common.R
import tech.nagual.phoenix.databinding.OrganizerVariantEditDialogBinding
import tech.nagual.phoenix.tools.organizer.data.model.Variant
import tech.nagual.phoenix.tools.organizer.data.repo.CategoriesRepository
import javax.inject.Inject

@AndroidEntryPoint
class VariantEditDialogFragment : BaseMaterialDialogFragment() {
    private lateinit var variant: Variant
    private val args by args<Args>()

    @Inject
    lateinit var categoriesRepository: CategoriesRepository

    private lateinit var binding: OrganizerVariantEditDialogBinding

    override fun getTitle(): String {
        return getString(if (args.variant == null) R.string.category_variants_create_title else R.string.category_variants_edit_title)
    }

    override fun createView(savedInstanceState: Bundle?): View {
        binding = OrganizerVariantEditDialogBinding.inflate(requireContext().layoutInflater)
        binding.valueLayout.placeholderText =
            getString(R.string.category_variants_value_placeholder)
        if (savedInstanceState == null && args.variant != null) {
            variant = args.variant!!
            binding.valueEdit.setTextWithSelection(
                variant.value
            )
        }
        return binding.root
    }

    override fun submit() {
        val value =
            if (binding.valueEdit.text.isNullOrEmpty()) binding.valueLayout.placeholderText.toString()
            else binding.valueEdit.text.toString()

        when {
            this::variant.isInitialized -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    val exists = variantExistsByValue(value, ignoreId = variant.id)
                    if (!exists) {
                        val variant = variant.copy(value = value)
                        updateVariant(variant)
                        return@launch dismiss()
                    }
                    showExistsToast(value)
                }
            }
            else -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    val exists = variantExistsByValue(value)
                    if (!exists) {
                        val variant = Variant(
                            value = value,
                            categoryId = args.categoryId,
                            parentId = args.parentId
                        )
                        insertVariant(variant)
                        return@launch dismiss()
                    }
                    showExistsToast(value)
                }
            }
        }
    }

    private suspend fun showExistsToast(value: String) {
        withContext(Dispatchers.Main) {
            Toast
                .makeText(
                    requireContext(),
                    getString(R.string.category_variant_already_exists, value),
                    Toast.LENGTH_SHORT
                )
                .show()
        }
    }

    private suspend fun insertVariant(variant: Variant) {
        categoriesRepository.insertVariant(variant)
    }

    private suspend fun updateVariant(variant: Variant) {
        categoriesRepository.updateVariant(variant)
    }

    private suspend fun variantExistsByValue(value: String, ignoreId: Long? = null): Boolean {
        val variant = categoriesRepository.getVariantByValue(args.categoryId, value).first()
        return variant != null && (if (ignoreId != null) variant.id != ignoreId else true)
    }

    companion object {
        fun show(variant: Variant?, categoryId: Long, parentId: Long, fragment: Fragment) {
            VariantEditDialogFragment().putArgs(Args(variant, categoryId, parentId)).show(fragment)
        }
    }

    @Parcelize
    class Args(val variant: Variant?, val categoryId: Long, val parentId: Long) : ParcelableArgs
}
