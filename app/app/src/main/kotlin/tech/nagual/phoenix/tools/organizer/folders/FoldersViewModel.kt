package tech.nagual.phoenix.tools.organizer.folders

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.zhanghai.android.files.util.valueCompat
import tech.nagual.phoenix.tools.organizer.data.model.Folder
import tech.nagual.phoenix.tools.organizer.data.repo.FolderRepository
import javax.inject.Inject

@HiltViewModel
class FoldersViewModel @Inject constructor(private val folderRepository: FolderRepository) :
    ViewModel() {

    private val _selectedNotebooksLiveData = MutableLiveData(folderItemSetOf())
    val selectedNotebooksLiveData: LiveData<FolderItemSet>
        get() = _selectedNotebooksLiveData
    val selectedNotebooks: FolderItemSet
        get() = _selectedNotebooksLiveData.valueCompat

    fun selectNotebook(folder: Folder, selected: Boolean) {
        selectNotebooks(folderItemSetOf(folder), selected)
    }

    fun selectNotebooks(folders: FolderItemSet, selected: Boolean) {
        val selectedNotebooks = _selectedNotebooksLiveData.valueCompat
        if (selectedNotebooks === folders) {
            if (!selected && selectedNotebooks.isNotEmpty()) {
                selectedNotebooks.clear()
                _selectedNotebooksLiveData.value = selectedNotebooks
            }
            return
        }
        var changed = false
        for (folder in folders) {
            changed = changed or if (selected) {
                selectedNotebooks.add(folder)
            } else {
                selectedNotebooks.remove(folder)
            }
        }
        if (changed) {
            _selectedNotebooksLiveData.value = selectedNotebooks
        }
    }

    fun clearSelectedNotebooks() {
        val selectedFolders = _selectedNotebooksLiveData.valueCompat
        if (selectedFolders.isEmpty()) {
            return
        }
        selectedFolders.clear()
        _selectedNotebooksLiveData.value = selectedFolders
    }

    fun deleteFolders(folderItemSet: FolderItemSet) {
        val notebooks = folderItemSet.toTypedArray()
        deleteFolders(*notebooks)
        selectNotebooks(folderItemSet, false)
    }

    fun deleteFolders(vararg folders: Folder) {
        viewModelScope.launch(Dispatchers.IO) {
            folderRepository.delete(*folders)
        }
    }
}
