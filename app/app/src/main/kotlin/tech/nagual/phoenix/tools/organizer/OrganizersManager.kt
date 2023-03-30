package tech.nagual.phoenix.tools.organizer

import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import tech.nagual.app.application
import me.zhanghai.android.files.util.putArgs
import me.zhanghai.android.files.util.startActivitySafe
import tech.nagual.common.R
import tech.nagual.phoenix.tools.organizer.data.model.CategoryType
import tech.nagual.phoenix.tools.organizer.data.model.NoteViewType
import tech.nagual.phoenix.tools.organizer.data.model.Organizer
import tech.nagual.phoenix.tools.organizer.data.repo.OrganizerRepository
import tech.nagual.phoenix.tools.organizer.utils.collect
import tech.nagual.phoenix.organizers.EditOrganizerDialogActivity
import tech.nagual.phoenix.organizers.EditOrganizerDialogFragment
import me.zhanghai.android.files.util.createIntent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface OrganizerEntryPoint {
    var organizerRepository: OrganizerRepository
}

class OrganizersManager {

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val organizerEntryPoint =
        EntryPointAccessors.fromApplication(
            application.applicationContext,
            OrganizerEntryPoint::class.java
        )
    val organizerRepository = organizerEntryPoint.organizerRepository

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    lateinit var organizerFlow: StateFlow<Organizer?>

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    private var organizersFlow: StateFlow<List<Organizer>> = organizerRepository.getAll()
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf(),
        )

    private val organizersListeners: MutableList<OrganizersListener> = mutableListOf()

    fun addOrganizersListener(listener: OrganizersListener) {
        if (!organizersListeners.contains(listener)) {
            organizersListeners.add(listener)
            listener.updateOrganizers(organizers)
        }
    }

    fun removeOrganizersListener(listener: OrganizersListener) {
        if (organizersListeners.contains(listener))
            organizersListeners.remove(listener)
    }

    fun init() {

        organizersFlow.collect(ProcessLifecycleOwner.get()) {
            organizers = it
            for (listener in organizersListeners)
                listener.updateOrganizers(organizers)
        }
    }

    fun open(organizer: Organizer, context: Context) {
        activeOrganizer = organizer

        organizerFlow = MutableStateFlow(activeOrganizer.id)
            .flatMapLatest { organizerRepository.getById(it) }
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null,
            )

        organizerFlow.collect(ProcessLifecycleOwner.get()) { organizer ->
            if (organizer == null) return@collect
            activeOrganizer = organizer
        }

        context.startActivitySafe(OrganizerActivity::class.createIntent())
    }

    fun edit(organizer: Organizer, context: Context) {
        context.startActivitySafe(
            EditOrganizerDialogActivity::class.createIntent()
                .putArgs(EditOrganizerDialogFragment.Args(organizer))
        )
    }

    interface OrganizersListener {
        fun updateOrganizers(organizers: List<Organizer>)
    }

    companion object {
        const val MEDIA_FOLDER = "media"

        const val REMINDERS_CHANNEL_ID = "REMINDERS_CHANNEL"
        const val BACKUPS_CHANNEL_ID = "BACKUPS_CHANNEL"
        const val UPLOADS_CHANNEL_ID = "UPLOADS_CHANNEL"
        const val PLAYBACK_CHANNEL_ID = "PLAYBACK_CHANNEL"

        lateinit var activeOrganizer: Organizer
        var organizers: List<Organizer> = listOf()

        private lateinit var INSTANCE: OrganizersManager
        fun getInstance(): OrganizersManager {
            if (!this::INSTANCE.isInitialized) {
                synchronized(OrganizersManager::class.java) {
                    if (!this::INSTANCE.isInitialized) {
                        INSTANCE = OrganizersManager()
                    }
                }
            }
            return INSTANCE
        }

        fun getCategoryTypeName(type: CategoryType): String {
            return application.getString(
                when (type) {
                    CategoryType.Variants -> R.string.category_type_variants
                    CategoryType.ExVariants -> R.string.category_type_complex_variants
                    CategoryType.AutoIncrement -> R.string.category_type_autoincrement
                    CategoryType.Geo -> R.string.category_type_geo
                    CategoryType.DateTime -> R.string.category_type_datetime
                    CategoryType.Password -> R.string.category_type_password
                }
            )
        }

        fun getCategoryTypeIconRes(type: CategoryType): Int {
            return when (type) {
                CategoryType.Variants -> R.drawable.category_type_variants_icon_white_24dp
                CategoryType.ExVariants -> R.drawable.category_type_complex_variants_icon_white_24dp
                CategoryType.AutoIncrement -> R.drawable.category_type_autoincrement_icon_white_24dp
                CategoryType.Geo -> R.drawable.category_type_geo_icon_white_24dp
                CategoryType.DateTime -> R.drawable.category_type_datetime_icon_white_24dp
                CategoryType.Password -> R.drawable.category_type_password_icon_white_24dp
            }
        }

        fun getCategoryTypeLargeIconRes(type: CategoryType): Int {
            return when (type) {
                CategoryType.Variants -> R.drawable.category_type_variants_icon_96dp
                CategoryType.ExVariants -> R.drawable.category_type_complex_variants_icon_white_96dp
                CategoryType.AutoIncrement -> R.drawable.category_type_autoincrement_icon_96dp
                CategoryType.Geo -> R.drawable.category_type_geo_icon_96dp
                CategoryType.DateTime -> R.drawable.category_type_datetime_icon_96dp
                CategoryType.Password -> R.drawable.category_type_password_icon_96dp
            }
        }

        fun getNoteTypeName(noteViewType: NoteViewType): String {
            return application.getString(
                when (noteViewType) {
                    NoteViewType.Text -> R.string.action_note_text
                    NoteViewType.TaskList -> R.string.action_note_task_list
                    NoteViewType.Categories -> R.string.action_note_categories
                }
            )
        }
    }
}