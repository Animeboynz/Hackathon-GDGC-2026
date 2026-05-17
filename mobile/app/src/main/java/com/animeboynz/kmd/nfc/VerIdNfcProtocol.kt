package com.animeboynz.kmd.nfc

object VerIdNfcProtocol {
    const val AID = "F0766572494401"
    const val MAX_CHUNK_SIZE = 220

    val success = byteArrayOf(0x90.toByte(), 0x00.toByte())
    val notFound = byteArrayOf(0x6A.toByte(), 0x83.toByte())
    val conditionsNotSatisfied = byteArrayOf(0x69.toByte(), 0x85.toByte())
    val instructionNotSupported = byteArrayOf(0x6D.toByte(), 0x00.toByte())

    val aidBytes: ByteArray = AID.chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()

    fun selectAidCommand(): ByteArray {
        return byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, aidBytes.size.toByte()) +
            aidBytes +
            byteArrayOf(0x00)
    }

    fun readChunkCommand(index: Int): ByteArray {
        return byteArrayOf(0x80.toByte(), 0xCA.toByte(), index.toByte(), 0x00, 0x00)
    }

    fun isSuccess(response: ByteArray): Boolean {
        return response.size >= 2 &&
            response[response.lastIndex - 1] == success[0] &&
            response[response.lastIndex] == success[1]
    }

    fun dataWithoutStatus(response: ByteArray): ByteArray {
        return response.copyOfRange(0, response.size - 2)
    }
}
