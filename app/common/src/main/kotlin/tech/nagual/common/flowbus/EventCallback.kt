package tech.nagual.common.flowbus

interface EventCallback<T> {
    /**
     * This function will be called for received event
     */
    fun onEvent(event: T)
}
