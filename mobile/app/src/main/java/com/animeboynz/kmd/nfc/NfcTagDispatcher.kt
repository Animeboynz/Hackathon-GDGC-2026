package com.animeboynz.kmd.nfc

import android.nfc.Tag
import java.util.concurrent.CopyOnWriteArraySet

/**
 * When non-null, [MainActivity] enables NFC reader mode and forwards tags here.
 */
object NfcTagDispatcher {
    private val listeners = CopyOnWriteArraySet<(Boolean) -> Unit>()

    @Volatile
    var consumer: ((Tag) -> Unit)? = null
        set(value) {
            field = value
            listeners.forEach { it(value != null) }
        }

    fun addConsumerStateListener(listener: (Boolean) -> Unit) {
        listeners += listener
        listener(consumer != null)
    }

    fun removeConsumerStateListener(listener: (Boolean) -> Unit) {
        listeners -= listener
    }
}
