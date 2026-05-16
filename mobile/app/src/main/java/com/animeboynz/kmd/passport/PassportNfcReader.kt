package com.animeboynz.kmd.passport

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.nfc.Tag
import android.nfc.tech.IsoDep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.sf.scuba.smartcards.IsoDepCardService
import org.jmrtd.BACKey
import org.jmrtd.PassportService
import org.jmrtd.lds.LDSFileUtil
import org.jmrtd.lds.icao.DG1File
import org.jmrtd.lds.icao.DG2File
import org.jmrtd.lds.icao.MRZInfo

data class PassportChipSummary(
    val mrzInfo: MRZInfo,
    val rawMrzDisplay: String,
    /** Portrait from chip (DG2) when encoded as JPEG; JP2/WSQ not decoded here. */
    val portrait: Bitmap? = null,
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

            val baseSummary = passport.getInputStream(PassportService.EF_DG1).use { dg1Stream ->
                val dg1 = LDSFileUtil.getLDSFile(PassportService.EF_DG1, dg1Stream) as DG1File
                val chipMrz = dg1.getMRZInfo()
                PassportChipSummary(
                    mrzInfo = chipMrz,
                    rawMrzDisplay = chipMrz.toString().trim(),
                )
            }

            val portrait = runCatching { readJpegPortrait(passport) }.getOrNull()
            Result.success(baseSummary.copy(portrait = portrait))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            runCatching { passport?.close() }
            runCatching { isoDep.close() }
        }
    }

    private fun readJpegPortrait(passport: PassportService): Bitmap? {
        return passport.getInputStream(PassportService.EF_DG2).use { stream ->
            val file = LDSFileUtil.getLDSFile(PassportService.EF_DG2, stream) as? DG2File
                ?: return@use null
            for (faceInfo in file.faceInfos) {
                for (faceImage in faceInfo.faceImageInfos) {
                    val mime = faceImage.mimeType?.lowercase().orEmpty()
                    val bytes = faceImage.imageInputStream.use { it.readBytes() }
                    if (mime.contains("jpeg") && !mime.contains("2000")) {
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { return@use it }
                    }
                    if (mime == "image/jp2" || mime.contains("jpeg2000")) {
                        // Many chips use JP2; decoding needs jj2000 (see AndroidPassportReader ImageUtil).
                        continue
                    }
                }
            }
            null
        }
    }
}
