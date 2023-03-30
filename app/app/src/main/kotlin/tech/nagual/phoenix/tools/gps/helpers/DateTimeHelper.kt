package tech.nagual.phoenix.tools.gps.helpers

import android.content.Context
import android.location.Location
import tech.nagual.common.R
import tech.nagual.phoenix.tools.gps.Keys
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object DateTimeHelper {

    /* Converts milliseconds to mm:ss or hh:mm:ss */
    fun convertToReadableTime(context: Context, milliseconds: Long): String {
        val timeString: String
        val hours: Long = TimeUnit.MILLISECONDS.toHours(milliseconds)
        val minutes: Long =
            TimeUnit.MILLISECONDS.toMinutes(milliseconds) % TimeUnit.HOURS.toMinutes(1)
        val seconds: Long =
            TimeUnit.MILLISECONDS.toSeconds(milliseconds) % TimeUnit.MINUTES.toSeconds(1)
        val h: String = context.getString(R.string.trackbook_abbreviation_hours)
        val m: String = context.getString(R.string.trackbook_abbreviation_minutes)
        val s: String = context.getString(R.string.trackbook_abbreviation_seconds)

        timeString = when (milliseconds >= Keys.ONE_HOUR_IN_MILLISECONDS) {
            // CASE: format hh:mm:ss
            true -> {
                "$hours $h $minutes $m $seconds $s"
            }
            // CASE: format mm:ss
            false -> {
                "$minutes $m $seconds $s"
            }
        }
        return timeString
    }

    /* Create sortable string for date - used for filenames  */
    fun convertToSortableDateString(date: Date): String {
        val dateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
        return dateFormat.format(date)
    }

    /* Creates a readable string for date - used in the UI */
    fun convertToReadableDate(date: Date, dateStyle: Int = DateFormat.LONG): String {
        return DateFormat.getDateInstance(dateStyle, Locale.getDefault()).format(date)
    }

    /* Creates a readable string date and time - used in the UI */
    fun convertToReadableDateAndTime(
        date: Date,
        dateStyle: Int = DateFormat.SHORT,
        timeStyle: Int = DateFormat.SHORT
    ): String {
        return "${
            DateFormat.getDateInstance(dateStyle, Locale.getDefault()).format(date)
        } ${DateFormat.getTimeInstance(timeStyle, Locale.getDefault()).format(date)}"
    }

    /* Calculates time difference between two locations */
    fun calculateTimeDistance(previousLocation: Location?, location: Location): Long {
        var timeDifference: Long = 0L
        // two data points needed to calculate time difference
        if (previousLocation != null) {
            // get time difference
            timeDifference = location.time - previousLocation.time
        }
        return timeDifference
    }
}