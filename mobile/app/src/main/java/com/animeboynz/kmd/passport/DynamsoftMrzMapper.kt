package com.animeboynz.kmd.passport

import com.dynamsoft.mrzscannerbundle.ui.MRZData
import com.dynamsoft.mrzscannerbundle.ui.MRZScanResult

object DynamsoftMrzMapper {

    const val DOCUMENT_TYPE_TD3_PASSPORT = "MRTD_TD3_PASSPORT"

    fun passportTd3OrNull(scanResult: MRZScanResult): Td3Mrz? {
        if (scanResult.resultStatus != MRZScanResult.EnumResultStatus.RS_FINISHED) return null
        val data = scanResult.data ?: return null
        if (data.documentType != DOCUMENT_TYPE_TD3_PASSPORT) return null
        return mrzLinesFromParsedData(data)
    }

    fun humanReadableFailure(scanResult: MRZScanResult): String? {
        return when (scanResult.resultStatus) {
            MRZScanResult.EnumResultStatus.RS_CANCELED -> null
            MRZScanResult.EnumResultStatus.RS_EXCEPTION ->
                scanResult.errorString.takeIf { it.isNotBlank() } ?: "MRZ scanner error"
            MRZScanResult.EnumResultStatus.RS_FINISHED -> {
                val data = scanResult.data
                when {
                    data == null -> "No MRZ data in scan result."
                    data.documentType != DOCUMENT_TYPE_TD3_PASSPORT ->
                        "NFC onboarding expects a passport (TD3). Scanned: ${data.documentType}."
                    else -> "Could not parse TD3 MRZ text. Try manual entry."
                }
            }
            else -> "Unexpected scan status: ${scanResult.resultStatus}"
        }
    }

    private fun mrzLinesFromParsedData(data: MRZData): Td3Mrz? {
        val mrzRaw = data.mrzText.trim()
        MrzParser.extractTd3FromOcr(mrzRaw)?.let { return it }

        val lines = mrzRaw.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.size >= 2) {
            MrzParser.parseManualLines(lines[lines.size - 2], lines[lines.size - 1])
                .getOrNull()?.let { return it }
        }

        val compact = mrzRaw.replace("\r", "").replace("\n", "").replace(" ", "")
        if (compact.length >= Td3Mrz.MRZ_LINE_LENGTH * 2) {
            val l1 = compact.substring(0, Td3Mrz.MRZ_LINE_LENGTH)
            val l2 = compact.substring(Td3Mrz.MRZ_LINE_LENGTH, Td3Mrz.MRZ_LINE_LENGTH * 2)
            MrzParser.parseManualLines(l1, l2).getOrNull()?.let { return it }
        }
        return null
    }
}
