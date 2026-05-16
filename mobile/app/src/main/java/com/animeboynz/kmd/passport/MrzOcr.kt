package com.animeboynz.kmd.passport

import android.graphics.Bitmap
import android.media.Image
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

object MrzOcr {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Crops the bottom [fraction] of the bitmap where TD3 MRZ lines usually sit on a passport
     * photo page (hold the document upright in frame).
     */
    fun cropMrzStrip(bitmap: Bitmap, fraction: Float = 0.42f): Bitmap {
        val h = bitmap.height
        val w = bitmap.width
        val cropH = (h * fraction.coerceIn(0.2f, 0.6f)).toInt().coerceAtLeast(1)
        val top = (h - cropH).coerceAtLeast(0)
        return Bitmap.createBitmap(bitmap, 0, top, w, cropH)
    }

    fun recognizeTextBlocking(bitmap: Bitmap): Text {
        val image = InputImage.fromBitmap(bitmap, 0)
        val task = recognizer.process(image)
        return Tasks.await(task)
    }

    /**
     * Runs ML Kit on a camera [Image] (typically YUV_420_888). Caller must keep the image valid
     * until this returns (do not close the hosting [androidx.camera.core.ImageProxy] first).
     */
    fun recognizeTextBlockingMedia(image: Image, rotationDegrees: Int): Text {
        val inputImage = InputImage.fromMediaImage(image, rotationDegrees)
        return Tasks.await(recognizer.process(inputImage))
    }

    /** Flat OCR string (entire image); prefer [recognizeTextBlocking] + structured MRZ parsing. */
    fun recognizeBlocking(bitmap: Bitmap): String {
        return recognizeTextBlocking(bitmap).text
    }
}
