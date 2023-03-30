package tech.nagual.phoenix.tools.organizer.tags

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import me.zhanghai.android.files.util.valueCompat
import tech.nagual.phoenix.tools.organizer.data.model.Tag
import tech.nagual.phoenix.tools.organizer.data.repo.TagRepository
import javax.inject.Inject

@Parcelize
data class TagData(val tag: Tag, val inNote: Boolean) : Parcelable

@HiltViewModel
class TagsViewModel @Inject constructor(private val tagRepository: TagRepository) : ViewModel() {

    private val _selectedTagsLiveData = MutableLiveData(tagItemSetOf())
    val selectedTagsLiveData: LiveData<TagItemSet>
        get() = _selectedTagsLiveData
    val selectedTags: TagItemSet
        get() = _selectedTagsLiveData.valueCompat

    fun selectTag(tag: TagData, selected: Boolean) {
        selectTags(tagItemSetOf(tag), selected)
    }

    fun selectTags(tags: TagItemSet, selected: Boolean) {
        val selectedTags = _selectedTagsLiveData.valueCompat
        if (selectedTags === tags) {
            if (!selected && selectedTags.isNotEmpty()) {
                selectedTags.clear()
                _selectedTagsLiveData.value = selectedTags
            }
            return
        }
        var changed = false
        for (tag in tags) {
            changed = changed or if (selected) {
                selectedTags.add(tag)
            } else {
                selectedTags.remove(tag)
            }
        }
        if (changed) {
            _selectedTagsLiveData.value = selectedTags
        }
    }

    fun clearSelectedTags() {
        val selectedTags = _selectedTagsLiveData.valueCompat
        if (selectedTags.isEmpty()) {
            return
        }
        selectedTags.clear()
        _selectedTagsLiveData.value = selectedTags
    }

    fun getData(noteId: Long? = null): Flow<List<TagData>> {
        return when (noteId) {
            null -> tagRepository.getAll().map { tags ->
                tags.map { TagData(it, false) }
            }
            else -> tagRepository.getByNoteId(noteId).flatMapLatest { noteTags ->
                tagRepository.getAll().map { tags ->
                    tags.map { TagData(it, it in noteTags) }
                }
            }
        }
    }

    fun addTagToNote(tagId: Long, noteId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            tagRepository.addTagToNote(tagId, noteId)
        }
    }

    fun deleteTagFromNote(tagId: Long, noteId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            tagRepository.deleteTagFromNote(tagId, noteId)
        }
    }

    fun deleteTags(tagItemSet: TagItemSet) {
        val tags = tagItemSet.map { it.tag }.toTypedArray()
        deleteTags(*tags)
        selectTags(tagItemSet, false)
    }

    fun deleteTags(vararg tags: Tag) {
        viewModelScope.launch(Dispatchers.IO) {
            tagRepository.delete(*tags)
        }
    }
}
