package tech.nagual.phoenix.tools.organizer.workflows

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import tech.nagual.phoenix.tools.organizer.OrganizersManager
import tech.nagual.phoenix.tools.organizer.data.model.Attachment
import tech.nagual.phoenix.tools.organizer.data.model.Folder
import tech.nagual.phoenix.tools.organizer.data.model.NoteViewType
import tech.nagual.phoenix.tools.organizer.data.model.Workflow
import tech.nagual.phoenix.tools.organizer.data.repo.FolderRepository
import tech.nagual.phoenix.tools.organizer.data.repo.WorkflowRepository
import tech.nagual.phoenix.tools.organizer.preferences.DefaultNoteType
import tech.nagual.phoenix.tools.organizer.preferences.getAllForOrganizer
import tech.nagual.phoenix.tools.organizer.preferences.PreferenceRepository
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class WorkflowViewModel @Inject constructor(
    val workflowRepository: WorkflowRepository,
    private val folderRepository: FolderRepository,
    private val preferenceRepository: PreferenceRepository
) :
    ViewModel() {

    var isNotInitialized = true
    private val workflowIdFlow: MutableStateFlow<Long?> = MutableStateFlow(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val data = workflowIdFlow
        .filterNotNull()
        .flatMapLatest { workflowRepository.getById(it) }
        .filterNotNull()
        .flatMapLatest { workflow ->
            getFolderData(workflow.folderId).flatMapLatest { folder ->
                preferenceRepository.getAllForOrganizer().map { prefs ->
                    Data(
                        workflow = workflow,
                        folder = folder,
                        isInitialized = true,
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Data(),
        )

    suspend fun initialize(
        workflowId: Long,
        workflowName: String
    ): Long {
        val defaultNoteType: DefaultNoteType = with(Dispatchers.Main) {
            preferenceRepository.get<DefaultNoteType>().first()
        }

        val id = if (workflowId > 0L)
            workflowId
        else
            workflowRepository.insert(
                Workflow(
                    name = workflowName,
                    creationDate = Instant.now().epochSecond,
                    noteViewType = when (defaultNoteType) {
                        DefaultNoteType.SIMPLE -> NoteViewType.Text
                        DefaultNoteType.TASK_LIST -> NoteViewType.TaskList
                        DefaultNoteType.CATEGORIES -> NoteViewType.Categories
                    },
                    categories = OrganizersManager.activeOrganizer.categories,
                    organizerId = OrganizersManager.activeOrganizer.id
                ),
            )

        workflowIdFlow.emit(id)

        isNotInitialized = false
        return id
    }

    private fun getFolderData(folderId: Long?): Flow<Folder?> {
        return folderId?.let { id -> folderRepository.getById(id) } ?: flow { emit(null) }
    }

    fun setNoteType(noteViewType: NoteViewType) = update { workflow ->
        workflow.copy(
            noteViewType = noteViewType,
        )
    }

    fun setFolderId(folderId: Long?) = update { workflow ->
        workflow.copy(
            folderId = folderId
        )
    }

    fun setAttachmentType(attachmentType: Attachment.Type?) = update { workflow ->
        workflow.copy(
            attachmentType = attachmentType
        )
    }

    fun setNoteHidden(isHidden: Boolean) = update { workflow ->
        workflow.copy(
            isHiddenNote = isHidden
        )
    }

    private inline fun update(crossinline transform: suspend (Workflow) -> Workflow) {
        viewModelScope.launch(Dispatchers.IO) {
            val workflow = data.value.workflow ?: return@launch
            val new = transform(workflow)
            workflowRepository.update(new)

            return@launch
        }
    }

    data class Data(
        val workflow: Workflow? = null,
        val folder: Folder? = null,
        val isInitialized: Boolean = false,
    )
}
