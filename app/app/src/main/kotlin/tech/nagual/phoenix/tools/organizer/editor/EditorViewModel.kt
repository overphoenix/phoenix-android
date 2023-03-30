package tech.nagual.phoenix.tools.organizer.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.nagual.common.preferences.datastore.defaultOf
import me.zhanghai.android.files.util.removeFirst
import tech.nagual.phoenix.tools.gps.GpsManager
import tech.nagual.phoenix.tools.organizer.OrganizersManager
import tech.nagual.phoenix.tools.organizer.data.model.*
import tech.nagual.phoenix.tools.organizer.data.repo.CategoriesRepository
import tech.nagual.phoenix.tools.organizer.data.repo.FolderRepository
import tech.nagual.phoenix.tools.organizer.data.repo.NoteRepository
import tech.nagual.phoenix.tools.organizer.preferences.DateFormat
import tech.nagual.phoenix.tools.organizer.preferences.ShowDate
import tech.nagual.phoenix.tools.organizer.preferences.TimeFormat
import tech.nagual.phoenix.tools.organizer.preferences.getAllForOrganizer
import tech.nagual.phoenix.tools.organizer.preferences.PreferenceRepository
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val folderRepository: FolderRepository,
    private val categoriesRepository: CategoriesRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {

    var inEditMode: Boolean = false
    var isNotInitialized = true

    private val noteIdFlow: MutableStateFlow<Long?> = MutableStateFlow(null)

    var selectedRange = 0 to 0

    @OptIn(ExperimentalCoroutinesApi::class)
    val data = noteIdFlow
        .filterNotNull()
        .flatMapLatest { noteRepository.getById(it) }
        .filterNotNull()
        .flatMapLatest { note ->
            getFolderData(note.folderId).flatMapLatest { folder ->
                preferenceRepository.getAllForOrganizer().map { prefs ->
                    Data(
                        note = note,
                        folder = folder,
                        dateTimeFormats = prefs.dateFormat to prefs.timeFormat,
                        showDates = prefs.showDate == ShowDate.YES,
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

    private fun getFolderData(folderId: Long?): Flow<Folder?> {
        return folderId?.let { id -> folderRepository.getById(id) } ?: flow { emit(null) }
    }

    fun initialize(
        noteId: Long,
        newNoteHidden: Boolean,
        newNoteTitle: String,
        newNoteContent: String,
        newNoteAttachments: List<Attachment>,
        newNoteRawCategories: List<RawCategory>,
        newNoteViewType: NoteViewType,
        newNoteFolderId: Long?,
        workflowId: Long = 0
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = if (noteId > 0L) noteId
            else {
                noteRepository.insertNote(
                    Note(
                        title = newNoteTitle,
                        content = newNoteContent,
                        folderId = newNoteFolderId,
                        type = newNoteViewType,
                        isHidden = newNoteHidden,
                        attachments = newNoteAttachments,
                        organizerId = OrganizersManager.activeOrganizer.id
                    ),
                )
            }

            if (noteId == 0L) {
                inEditMode = true
                if (newNoteRawCategories.isNotEmpty()) {
                    var note = noteRepository.getById(id).first()!!
                    for (rawCategory in newNoteRawCategories) {
                        if (rawCategory.isActive()) {
                            val rawVariant = createRawVariant(rawCategory.id, workflowId)
                            if (rawVariant != null)
                                note = addOrReplaceVariantInternal(note, rawVariant)
                        }
                    }
                    noteRepository.updateNotes(note)
                }
            }

            withContext(Dispatchers.Main) {
                noteIdFlow.emit(id)
            }

            isNotInitialized = false
        }
    }

    fun setNoteTitle(title: String) = update { note ->
        note.copy(
            title = title,
            modifiedDate = Instant.now().epochSecond,
        )
    }

    fun setNoteContent(content: String) = update { note ->
        note.copy(
            content = content,
            modifiedDate = Instant.now().epochSecond,
        )
    }

    fun setColor(color: NoteColor) = update { note ->
        note.copy(
            color = color,
            modifiedDate = Instant.now().epochSecond,
        )
    }

    fun deleteAttachment(attachment: Attachment) = update { note ->
        note.copy(
            attachments = note.attachments
                .filterNot { it.path == attachment.path }
                .toMutableList(),
            modifiedDate = Instant.now().epochSecond,
        )
    }

    fun insertAttachments(vararg attachments: Attachment) = update { note ->
        note.copy(
            attachments = note.attachments + attachments,
            modifiedDate = Instant.now().epochSecond,
        )
    }

    fun getCategoryById(categoryId: Long): Flow<Category?> =
        categoriesRepository.getById(categoryId)

    private suspend fun createRawVariant(categoryId: Long, workflowId: Long = 0): RawVariant? {
        val category = getCategoryById(categoryId).first()!!
        when (category.type) {
            CategoryType.Variants -> {
                val variant = try {
                    getDefaultVariantForCategory(categoryId, workflowId)
                } catch (throwable: Throwable) {
                    if (workflowId > 0 && throwable is UnknownError) {
                        getDefaultVariantForCategory(categoryId, 0)
                    } else {
                        throw throwable
                    }
                }
                return createRawVariantInternal(variant, category)
            }
            CategoryType.ExVariants -> {
                val exVariant = try {
                    getDefaultExVariantForCategory(categoryId, workflowId)
                } catch (throwable: Throwable) {
                    if (workflowId > 0 && throwable is UnknownError) {
                        getDefaultExVariantForCategory(categoryId, 0)
                    } else {
                        throw throwable
                    }
                }
                return if (exVariant.isNotEmpty()) {
                    var child: RawChildVariant? = null
                    var prevRawChildVariant: RawChildVariant? = null
                    for (i in (1 until exVariant.size).reversed()) {
                        prevRawChildVariant = if (i == exVariant.size - 1) {
                            child = RawChildVariant(
                                id = exVariant[i].id,
                                value = exVariant[i].value
                            )
                            child
                        } else
                            RawChildVariant(
                                id = exVariant[i].id,
                                value = exVariant[i].value,
                                child = prevRawChildVariant
                            )
                    }
                    RawVariant(
                        id = exVariant[0].id,
                        value = exVariant[0].value,
                        child = child,
                        categoryId = category.id,
                        categoryName = category.name,
                        categoryType = category.type,
                        flags = 0L
                    )
                } else null
            }
            CategoryType.AutoIncrement -> {
                val variant = try {
                    createAutoIncrementVariant(categoryId, workflowId)
                } catch (throwable: Throwable) {
                    if (workflowId > 0 && throwable is UnknownError) {
                        createAutoIncrementVariant(categoryId, 0)
                    } else {
                        throw throwable
                    }
                }
                return createRawVariantInternal(variant, category)
            }
            CategoryType.Geo -> {
                val variant = createGeoVariant(categoryId)
                return createRawVariantInternal(variant, category)
            }
            CategoryType.DateTime -> {
                return null
            }
            CategoryType.Password -> {
                return null
            }
        }
    }

    private fun createRawVariantInternal(variant: Variant?, category: Category): RawVariant? {
        return if (variant != null)
            RawVariant(
                id = variant.id,
                value = variant.value,
                categoryId = category.id,
                categoryName = category.name,
                categoryType = category.type,
                flags = 0L
            )
        else null
    }

    suspend fun addCategoryToNote(categoryId: Long) {
        val rawVariant = createRawVariant(categoryId)
        if (rawVariant != null) {
            update { note ->
                addOrReplaceVariantInternal(note, rawVariant)
            }
        }
    }

    suspend fun getVariantsForCategory(categoryId: Long, parentId: Long = 0): List<Variant> {
        return categoriesRepository.getVariantsByCategoryId(categoryId, parentId).first()
    }

    suspend fun getAllEssentialCategories(): List<Category> {
        val nonEmpties = categoriesRepository.getAllNonEmpty().first()
        val autogenerated = categoriesRepository.getAllAutogenerated().first()
        val result: MutableList<Category> = mutableListOf()
        result.addAll(autogenerated)
        result.addAll(nonEmpties)
        return result
    }

    private fun addOrReplaceVariantInternal(note: Note, rawVariant: RawVariant): Note {
        val variants = note.variants.toMutableList()
        val index = variants.indexOfFirst { it.categoryId == rawVariant.categoryId }
        if (index != -1) {
            val oldRawVariant = variants[index]
            variants[index] = oldRawVariant.copy(
                id = rawVariant.id,
                value = rawVariant.value
            )
        } else {
            variants += rawVariant
        }

        return note.copy(
            variants = variants,
            modifiedDate = Instant.now().epochSecond,
        )
    }

    fun replaceVariant(rawVariant: RawVariant) = update { note ->
        val variants = note.variants.toMutableList()
        variants.apply {
            val index = indexOfFirst { it.categoryId == rawVariant.categoryId }
            val oldVariant = this[index]
            this[index] = oldVariant.copy(
                id = rawVariant.id,
                value = rawVariant.value,
                child = rawVariant.child
            )
        }

        note.copy(
            variants = variants,
            modifiedDate = Instant.now().epochSecond,
        )
    }

    fun moveCategory(fromPosition: Int, toPosition: Int) = update { note ->
        val variants = note.variants.toMutableList()
        variants.apply { add(toPosition, removeAt(fromPosition)) }
        note.copy(
            variants = variants,
            modifiedDate = Instant.now().epochSecond,
        )
    }

    fun removeCategory(categoryId: Long) = update { note ->
        val variants = note.variants.toMutableList()
        variants.apply { removeFirst { it.categoryId == categoryId } }
        note.copy(
            variants = variants,
            modifiedDate = Instant.now().epochSecond,
        )
    }

    suspend fun createAutoIncrementVariant(categoryId: Long, workflowId: Long): Variant =
        categoriesRepository.createAutoIncrementVariant(categoryId, workflowId)

    suspend fun createGeoVariant(categoryId: Long): Variant {
        return categoriesRepository.createGeoVariant(
            categoryId,
            GpsManager.getInstance().currentBestLocation
        )
    }

    private suspend fun getDefaultVariantForCategory(
        categoryId: Long,
        workflowId: Long
    ): Variant? =
        categoriesRepository.getDefaultVariantForCategory(categoryId, workflowId)

    private suspend fun getDefaultExVariantForCategory(
        categoryId: Long,
        workflowId: Long
    ): List<Variant> =
        categoriesRepository.getDefaultExVariantForCategory(categoryId, workflowId)

    suspend fun getVariantByValue(categoryId: Long, value: String): Variant? =
        categoriesRepository.getVariantByValue(categoryId, value).first()


    fun updateTaskList(list: List<NoteTask>) = update { note ->
        note.copy(
            taskList = list,
            modifiedDate = Instant.now().epochSecond,
        )
    }

    fun changeViewType(viewType: NoteViewType) = update { note ->
        note.copy(
            type = viewType,
//            modifiedDate = Instant.now().epochSecond,
        )
    }

    private inline fun update(crossinline transform: suspend (Note) -> Note) {
        viewModelScope.launch(Dispatchers.IO) {
            val note = data.value.note ?: return@launch
            val new = transform(note)
            noteRepository.updateNotes(new)

            return@launch
        }
    }

    data class Data(
        val note: Note? = null,
        val folder: Folder? = null,
        val dateTimeFormats: Pair<DateFormat, TimeFormat> = defaultOf<DateFormat>() to defaultOf<TimeFormat>(),
        val showDates: Boolean = true,
        val isInitialized: Boolean = false,
    )
}
