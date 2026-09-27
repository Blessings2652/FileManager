package com.shizuku.filemanager.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

object AdjustmentEngine {
    fun getFilterMatrix(filterName: String): FloatArray {
        return when (filterName) {
            "B&W" -> floatArrayOf(
                0.33f, 0.33f, 0.33f, 0f, 0f,
                0.33f, 0.33f, 0.33f, 0f, 0f,
                0.33f, 0.33f, 0.33f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
            "Vintage" -> floatArrayOf(
                1.1f, 0f, 0f, 0f, 20f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 0.8f, 0f, -20f,
                0f, 0f, 0f, 1f, 0f
            )
            "Cinematic" -> floatArrayOf(
                0.9f, 0f, 0f, 0f, 0f,
                0f, 1.1f, 0f, 0f, 0f,
                0f, 0f, 1.2f, 0f, 10f,
                0f, 0f, 0f, 1f, 0f
            )
            "Warm" -> floatArrayOf(
                1.2f, 0f, 0f, 0f, 0f,
                0f, 1.1f, 0f, 0f, 0f,
                0f, 0f, 0.9f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
            "Cool" -> floatArrayOf(
                0.9f, 0f, 0f, 0f, 0f,
                0f, 1.1f, 0f, 0f, 0f,
                0f, 0f, 1.3f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
            else -> floatArrayOf(
                1f, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        }
    }

    fun applyAdjustments(
        bitmap: Bitmap,
        brightness: Float = 0f,
        contrast: Float = 1f,
        saturation: Float = 1f,
        filterName: String = "Original"
    ): Bitmap {
        val cm = ColorMatrix()
        
        // Base Filter
        cm.postConcat(ColorMatrix(getFilterMatrix(filterName)))
        
        // Saturation
        val satMatrix = ColorMatrix()
        satMatrix.setSaturation(saturation)
        cm.postConcat(satMatrix)
        
        // Contrast & Brightness
        val contrastMatrix = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, brightness,
            0f, contrast, 0f, 0f, brightness,
            0f, 0f, contrast, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(contrastMatrix)
        
        return applyColorMatrix(bitmap, cm)
    }

    private fun applyColorMatrix(bitmap: Bitmap, cm: ColorMatrix): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }
}
