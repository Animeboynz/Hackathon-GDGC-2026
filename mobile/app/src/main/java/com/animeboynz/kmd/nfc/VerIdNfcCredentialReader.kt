package com.animeboynz.kmd.nfc

import android.nfc.Tag
import android.nfc.tech.IsoDep
import java.io.ByteArrayOutputStream

object VerIdNfcCredentialReader {
    fun read(tag: Tag): Result<String> = runCatching {
        val isoDep = IsoDep.get(tag) ?: error("This NFC tag does not support VerID transfer")
        isoDep.use {
            it.timeout = 5_000
            it.connect()

            val selectResponse = it.transceive(VerIdNfcProtocol.selectAidCommand())
            if (!VerIdNfcProtocol.isSuccess(selectResponse)) {
                error("No VerID credential found over NFC")
            }

            val output = ByteArrayOutputStream()
            for (index in 0..255) {
                val response = it.transceive(VerIdNfcProtocol.readChunkCommand(index))
                if (!VerIdNfcProtocol.isSuccess(response)) {
                    error("Could not read VerID credential over NFC")
                }

                val data = VerIdNfcProtocol.dataWithoutStatus(response)
                if (data.isEmpty()) error("Empty VerID NFC response")
                val hasMore = data[0].toInt() == 1
                output.write(data, 1, data.size - 1)
                if (!hasMore) {
                    return@runCatching output.toString(Charsets.UTF_8.name())
                }
            }
            error("VerID NFC credential was too large")
        }
    }
}
