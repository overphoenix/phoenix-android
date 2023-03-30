@file:Suppress("unused")

package tech.nagual.common.permissions.internal

import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.Lifecycle.Event.*
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent

internal fun Any?.maybeObserveLifecycle(
    vararg watchFor: Event,
    onEvent: (Event) -> Unit
): Lifecycle? {
    if (this is LifecycleOwner) {
        return Lifecycle(this, watchFor, onEvent)
    }
    return null
}

internal class Lifecycle(
    private var lifecycleOwner: LifecycleOwner?,
    private var watchFor: Array<out Event>,
    private var onEvent: ((Event) -> Unit)?
) : LifecycleObserver {
    init {
        lifecycleOwner?.lifecycle?.addObserver(this)
    }

    @OnLifecycleEvent(ON_CREATE)
    fun onCreate() {
        if (watchFor.isEmpty() || ON_CREATE in watchFor) {
            onEvent?.invoke(ON_CREATE)
        }
    }

    @OnLifecycleEvent(ON_START)
    fun onStart() {
        if (watchFor.isEmpty() || ON_START in watchFor) {
            onEvent?.invoke(ON_START)
        }
    }

    @OnLifecycleEvent(ON_RESUME)
    fun onResume() {
        if (watchFor.isEmpty() || ON_RESUME in watchFor) {
            onEvent?.invoke(ON_RESUME)
        }
    }

    @OnLifecycleEvent(ON_PAUSE)
    fun onPause() {
        if (watchFor.isEmpty() || ON_PAUSE in watchFor) {
            onEvent?.invoke(ON_PAUSE)
        }
    }

    @OnLifecycleEvent(ON_STOP)
    fun onStop() {
        if (watchFor.isEmpty() || ON_STOP in watchFor) {
            onEvent?.invoke(ON_STOP)
        }
    }

    @OnLifecycleEvent(ON_DESTROY)
    fun onDestroy() {
        lifecycleOwner?.lifecycle?.removeObserver(this)
        lifecycleOwner = null

        if (watchFor.isEmpty() || ON_DESTROY in watchFor) {
            onEvent?.invoke(ON_DESTROY)
        }
        onEvent = null
    }
}
