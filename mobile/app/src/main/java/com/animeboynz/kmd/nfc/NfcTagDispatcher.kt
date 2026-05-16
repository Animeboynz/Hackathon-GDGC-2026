package com.animeboynz.kmd.nfc

import android.nfc.Tag

/**
 * When non-null, [MainActivity] enables NFC foreground dispatch and forwards tags here.
 */
object NfcTagDispatcher {
    @Volatile
    var consumer: ((Tag) -> Unit)? = null
}
