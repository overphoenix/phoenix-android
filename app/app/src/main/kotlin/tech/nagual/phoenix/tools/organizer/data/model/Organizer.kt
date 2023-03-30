package tech.nagual.phoenix.tools.organizer.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Entity(tableName = "organizers")
@Serializable
@Parcelize
data class Organizer(
    val ordinal: Int = 0,
    val name: String,
    val description: String = "",
    override val categories: List<RawCategory> = listOf(),
    @PrimaryKey(autoGenerate = true) val id: Long = 0L
) : RawCategoriesHolder, Parcelable
