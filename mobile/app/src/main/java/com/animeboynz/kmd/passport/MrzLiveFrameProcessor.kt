package com.animeboynz.kmd.passport

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

/**
 * Runs MRZ extraction on a single camera frame: ML Kit on the live [Image] first (cheap), then a
 * bitmap + bottom-crop fallback when the direct path misses (common on noisy previews).
 */
object MrzLiveFrameProcessor {

    private const val JPEG_QUALITY = 88

    fun scan(imageProxy: ImageProxy): Td3Mrz? {
        val mediaImage = imageProxy.image ?: return null
        val rotation = imageProxy.imageInfo.rotationDegrees

        runCatching {
            val text = MrzOcr.recognizeTextBlockingMedia(mediaImage, rotation)
            MrzParser.extractTd3FromMlKit(text)
        }.getOrNull()?.let { return it }

        val bitmap = imageProxyToBitmap(imageProxy) ?: return null
        return try {
            val cropped = runCatching { MrzOcr.cropMrzStrip(bitmap) }.getOrNull()
            val fromCrop = cropped?.let { c ->
                runCatching { MrzParser.extractTd3FromMlKit(MrzOcr.recognizeTextBlocking(c)) }.getOrNull()
            }
            if (fromCrop != null) return fromCrop
            runCatching { MrzParser.extractTd3FromMlKit(MrzOcr.recognizeTextBlocking(bitmap)) }.getOrNull()
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        return try {
            when (imageProxy.format) {
                ImageFormat.YUV_420_888 -> nv21JpegToBitmap(imageProxy)
                ImageFormat.JPEG -> {
                    val buffer = imageProxy.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Fast path: pack YUV_420_888 into NV21 and JPEG-compress. Simpler than stride-perfect copies;
     * sufficient as a fallback after [InputImage.fromMediaImage].
     */
    private fun nv21JpegToBitmap(imageProxy: ImageProxy): Bitmap? {
        val image = imageProxy.image ?: return null
        val w = imageProxy.width
        val h = imageProxy.height
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
        val yuv = YuvImage(nv21, ImageFormat.NV21, w, h, null)
        val out = ByteArrayOutputStream()
        yuv.compressToJpeg(Rect(0, 0, w, h), JPEG_QUALITY, out)
        val jpeg = out.toByteArray()
        return BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
    }
}
