@file:Suppress("unused")

package tech.nagual.common.permissions

import android.Manifest
import android.annotation.SuppressLint

@SuppressLint("InlinedApi")
enum class Permission(val value: String) {
    UNKNOWN(""),

    ACTIVITY_RECOGNITION(Manifest.permission.ACTIVITY_RECOGNITION),

    READ_CALENDAR(Manifest.permission.READ_CALENDAR),

    WRITE_CALENDAR(Manifest.permission.WRITE_CALENDAR),

    CAMERA(Manifest.permission.CAMERA),

    READ_CONTACTS(Manifest.permission.READ_CONTACTS),
    WRITE_CONTACTS(Manifest.permission.WRITE_CONTACTS),
    GET_ACCOUNTS(Manifest.permission.GET_ACCOUNTS),

    ACCESS_FINE_LOCATION(Manifest.permission.ACCESS_FINE_LOCATION),
    ACCESS_COARSE_LOCATION(Manifest.permission.ACCESS_COARSE_LOCATION),
    ACCESS_BACKGROUND_LOCATION(Manifest.permission.ACCESS_BACKGROUND_LOCATION),

    RECORD_AUDIO(Manifest.permission.RECORD_AUDIO),

    READ_PHONE_STATE(Manifest.permission.READ_PHONE_STATE),
    CALL_PHONE(Manifest.permission.CALL_PHONE),
    READ_CALL_LOG(Manifest.permission.READ_CALL_LOG),
    WRITE_CALL_LOG(Manifest.permission.WRITE_CALL_LOG),
    ADD_VOICEMAIL(Manifest.permission.ADD_VOICEMAIL),
    USE_SIP(Manifest.permission.USE_SIP),

    BODY_SENSORS(Manifest.permission.BODY_SENSORS),

    SEND_SMS(Manifest.permission.SEND_SMS),
    RECEIVE_SMS(Manifest.permission.RECEIVE_SMS),
    READ_SMS(Manifest.permission.READ_SMS),
    RECEIVE_WAP_PUSH(Manifest.permission.RECEIVE_WAP_PUSH),
    RECEIVE_MMS(Manifest.permission.RECEIVE_MMS),

    READ_EXTERNAL_STORAGE(Manifest.permission.READ_EXTERNAL_STORAGE),
    WRITE_EXTERNAL_STORAGE(Manifest.permission.WRITE_EXTERNAL_STORAGE),

    SYSTEM_ALERT_WINDOW(Manifest.permission.SYSTEM_ALERT_WINDOW),

    /** @deprecated */
    @Suppress("DEPRECATION")
    @Deprecated("Manifest.permission.PROCESS_OUTGOING_CALLS is deprecated.")
    PROCESS_OUTGOING_CALLS(Manifest.permission.PROCESS_OUTGOING_CALLS);

    companion object {
        @JvmStatic
        fun parse(raw: String): Permission {
            return values().singleOrNull { it.value == raw } ?: UNKNOWN
        }
    }
}
