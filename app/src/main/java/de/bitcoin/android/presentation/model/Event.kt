package de.bitcoin.android.presentation.model

open class Event<out T>(private val data: T) {

    var hasBeenHandled = false
        private set // Allow external read but not write

    /**
     * Returns the data and prevents its use again.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            data
        }
    }

    /**
     * Returns the data, even if it's already been handled.
     */
    fun getData(): T = data
}