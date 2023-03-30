package tech.nagual.common.permissions.coroutines

import android.os.Looper
import java.lang.Thread.currentThread

internal fun checkMainThread() =
    check(Looper.myLooper() == Looper.getMainLooper()) {
        "Expected to be called on the main thread but was ${currentThread().name}"
    }
