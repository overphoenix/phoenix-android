package tech.nagual.phoenix.tools.organizer.preferences

import tech.nagual.common.preferences.datastore.EnumPreference
import tech.nagual.common.preferences.datastore.HasNameResource
import tech.nagual.common.preferences.datastore.key
import tech.nagual.common.R
import java.util.concurrent.TimeUnit

enum class DefaultNoteType(override val nameResource: Int) : HasNameResource,
    EnumPreference by key("organizer_default_note_type") {
    SIMPLE(R.string.preferences_note_text) {
        override val isDefault = true
    },
    TASK_LIST(R.string.preferences_note_task_list),
    CATEGORIES(R.string.preferences_note_categories)
}

enum class LayoutMode(override val nameResource: Int) : HasNameResource,
    EnumPreference by key("organizer_layout_mode") {
    LIST(R.string.preferences_layout_mode_list) {
        override val isDefault = true
    },
    GRID(R.string.preferences_layout_mode_grid)
}

enum class SortMethod(override val nameResource: Int) : HasNameResource,
    EnumPreference by key("organizer_sort_method") {
    TITLE_ASC(R.string.notes_sort_by_title_asc),
    TITLE_DESC(R.string.notes_sort_by_title_desc),
    CREATION_ASC(R.string.notes_sort_by_created_asc),
    CREATION_DESC(R.string.notes_sort_by_created_desc),
    MODIFIED_ASC(R.string.notes_sort_by_modified_asc),
    MODIFIED_DESC(R.string.notes_sort_by_modified_desc) {
        override val isDefault = true
    },
}

enum class DateFormat(val patternResource: Int) : EnumPreference by key("organizer_date_format") {
    dd_MM_yyyy(R.string.dd_MM_yyyy) {
        override val isDefault = true
    },
    d_MM_yyyy(R.string.d_MM_yyyy),
    MM_d_yyyy(R.string.MM_d_yyyy),
    d_MMMM_yyyy(R.string.d_MMMM_yyyy),
}

enum class TimeFormat(val patternResource: Int) : EnumPreference by key("organizer_time_format") {
    HH_mm(R.string.HH_mm) {
        override val isDefault = true
    },
    hh_mm(R.string.hh_mm),
}

enum class ShowDate(override val nameResource: Int) : HasNameResource,
    EnumPreference by key("organizer_show_date") {
    YES(R.string.yes) {
        override val isDefault = true
    },
    NO(R.string.no),
}

enum class GroupNotesWithoutNotebook(
    override val nameResource: Int,
) : HasNameResource, EnumPreference by key("organizer_group_unassigned_notes") {
    YES(R.string.yes),
    NO(R.string.no) {
        override val isDefault = true
    },
}

enum class NoteDeletionTime(
    override val nameResource: Int,
    val interval: Long,
) : HasNameResource, EnumPreference by key("organizer_note_deletion_time") {
    WEEK(R.string.organizer_pref_note_deletion_time_week, TimeUnit.DAYS.toSeconds(7)),
    TWO_WEEKS(R.string.organizer_pref_note_deletion_time_two_weeks, TimeUnit.DAYS.toSeconds(14)),
    MONTH(R.string.organizer_pref_note_deletion_time_month, TimeUnit.DAYS.toSeconds(30)),
    INSTANTLY(R.string.organizer_pref_note_deletion_time_instantly, 0L) {
        override val isDefault = true
    };

    fun toDays() = TimeUnit.SECONDS.toDays(this.interval)
}
