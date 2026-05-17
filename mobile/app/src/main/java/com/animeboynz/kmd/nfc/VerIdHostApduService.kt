package com.animeboynz.kmd.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle

class VerIdHostApduService : HostApduService() {
    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        val command = commandApdu ?: return VerIdNfcProtocol.instructionNotSupported
        return when {
            command.isSelectAid() -> VerIdNfcProtocol.success
            command.isReadChunk() -> readChunk(command[2].toInt() and 0xFF)
            else -> VerIdNfcProtocol.instructionNotSupported
        }
    }

    override fun onDeactivated(reason: Int) = Unit

    private fun readChunk(index: Int): ByteArray {
        val payloadBytes = NfcCredentialPayloadStore.payload
            ?.toByteArray(Charsets.UTF_8)
            ?: return VerIdNfcProtocol.conditionsNotSatisfied
        val start = index * VerIdNfcProtocol.MAX_CHUNK_SIZE
        if (start >= payloadBytes.size) return VerIdNfcProtocol.notFound

        val end = minOf(start + VerIdNfcProtocol.MAX_CHUNK_SIZE, payloadBytes.size)
        val hasMore = end < payloadBytes.size
        val chunk = payloadBytes.copyOfRange(start, end)
        return byteArrayOf(if (hasMore) 1 else 0) + chunk + VerIdNfcProtocol.success
    }

    private fun ByteArray.isSelectAid(): Boolean {
        if (size < 5) return false
        if (this[0] != 0x00.toByte() || this[1] != 0xA4.toByte() || this[2] != 0x04.toByte()) return false
        val length = this[4].toInt() and 0xFF
        if (size < 5 + length) return false
        return copyOfRange(5, 5 + length).contentEquals(VerIdNfcProtocol.aidBytes)
    }

    private fun ByteArray.isReadChunk(): Boolean {
        return size >= 4 && this[0] == 0x80.toByte() && this[1] == 0xCA.toByte()
    }
}
