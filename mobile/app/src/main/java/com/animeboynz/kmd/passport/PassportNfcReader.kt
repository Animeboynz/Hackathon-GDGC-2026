package com.animeboynz.kmd.passport

import android.nfc.Tag
import android.nfc.tech.IsoDep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.sf.scuba.smartcards.IsoDepCardService
import org.jmrtd.BACKey
import org.jmrtd.PassportService
import org.jmrtd.lds.LDSFileUtil
import org.jmrtd.lds.icao.DG1File
import org.jmrtd.lds.icao.MRZInfo

data class PassportChipSummary(
    val mrzInfo: MRZInfo,
    val rawMrzDisplay: String,
)

object PassportNfcReader {

    suspend fun readDg1(tag: Tag, mrz: Td3Mrz): Result<PassportChipSummary> = withContext(Dispatchers.IO) {
        val isoDep = IsoDep.get(tag) ?: return@withContext Result.failure(
            IllegalStateException("This tag does not support ISO-DEP (not a typical ePassport)."),
        )
        var passport: PassportService? = null
        try {
            isoDep.timeout = 12_000
            val cardService = IsoDepCardService(isoDep)
            passport = PassportService(
                cardService,
                PassportService.NORMAL_MAX_TRANCEIVE_LENGTH,
                PassportService.DEFAULT_MAX_BLOCKSIZE,
                true,
                false,
            )
            passport.open()
            passport.sendSelectApplet(false)
            val mrzInfo = MrzParser.parseMrzInfo(mrz)
            val bacKey = BACKey(
                mrzInfo.documentNumber,
                mrzInfo.dateOfBirth,
                mrzInfo.dateOfExpiry,
            )
            passport.doBAC(bacKey)
            val result = passport.getInputStream(PassportService.EF_DG1).use { dg1Stream ->
                val dg1 = LDSFileUtil.getLDSFile(PassportService.EF_DG1, dg1Stream) as DG1File
                val chipMrz = dg1.getMRZInfo()
                Result.success(
                    PassportChipSummary(
                        mrzInfo = chipMrz,
                        rawMrzDisplay = chipMrz.toString().trim(),
                    ),
                )
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            runCatching { passport?.close() }
            runCatching { isoDep.close() }
        }
    }
}
