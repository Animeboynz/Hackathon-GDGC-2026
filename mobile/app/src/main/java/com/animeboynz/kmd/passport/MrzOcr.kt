package com.animeboynz.kmd.passport

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

object MrzOcr {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    fun recognizeBlocking(bitmap: Bitmap): String {
        val image = InputImage.fromBitmap(bitmap, 0)
        val task = recognizer.process(image)
        return Tasks.await(task).text
    }
}
