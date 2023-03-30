package tech.nagual.phoenix.tools.organizer.categories.variants

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.zhanghai.android.files.util.valueCompat
import tech.nagual.phoenix.tools.organizer.data.model.Variant
import tech.nagual.phoenix.tools.organizer.data.repo.CategoriesRepository
import javax.inject.Inject

@HiltViewModel
class VariantsViewModel @Inject constructor(
    private val categoriesRepository: CategoriesRepository
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun variants(categoryId: Long, parentId: Long = 0): StateFlow<List<Variant>> =
        categoriesRepository.getVariantsByCategoryId(categoryId, parentId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = (listOf())
            )

    private val _selectedVariantsLiveData = MutableLiveData(variantItemSetOf())
    val selectedVariantsLiveData: LiveData<VariantItemSet>
        get() = _selectedVariantsLiveData
    val selectedVariants: VariantItemSet
        get() = _selectedVariantsLiveData.valueCompat

    fun selectVariant(variant: Variant, selected: Boolean) {
        selectVariants(variantItemSetOf(variant), selected)
    }

    fun selectVariants(variants: VariantItemSet, selected: Boolean) {
        val selectedVariants = _selectedVariantsLiveData.valueCompat
        if (selectedVariants === variants) {
            if (!selected && selectedVariants.isNotEmpty()) {
                selectedVariants.clear()
                _selectedVariantsLiveData.value = selectedVariants
            }
            return
        }
        var changed = false
        for (variant in variants) {
            changed = changed or if (selected) {
                selectedVariants.add(variant)
            } else {
                selectedVariants.remove(variant)
            }
        }
        if (changed) {
            _selectedVariantsLiveData.value = selectedVariants
        }
    }

    fun clearSelectedVariants() {
        val selectedVariants = _selectedVariantsLiveData.valueCompat
        if (selectedVariants.isEmpty()) {
            return
        }
        selectedVariants.clear()
        _selectedVariantsLiveData.value = selectedVariants
    }

    fun deleteVariants(variantItemSet: VariantItemSet) {
        val arrayOfVariants = variantItemSet.toTypedArray()
        this.deleteVariants(*arrayOfVariants)
        selectVariants(variantItemSet, false)
    }

    fun deleteVariants(vararg variants: Variant) {
        viewModelScope.launch(Dispatchers.IO) {
            categoriesRepository.deleteVariant(*variants)
        }
    }

    fun createAutoIncrementVariant(categoryId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            categoriesRepository.createAutoIncrementVariant(categoryId)
        }
    }

    fun createGeoVariant(categoryId: Long, location: Location) {
        viewModelScope.launch(Dispatchers.IO) {
            categoriesRepository.createGeoVariant(categoryId, location)
        }
    }
}
