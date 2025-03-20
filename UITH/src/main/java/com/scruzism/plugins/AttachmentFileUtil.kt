package com.scruzism.plugins

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

fun createTempFileFromUri(context: Context, uri: Uri, filename: String): File? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        if (inputStream != null) {
            val tempFile = File(context.cacheDir, filename)
            FileOutputStream(tempFile).use { fileOut ->
                inputStream.copyTo(fileOut)
            }
            tempFile
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
