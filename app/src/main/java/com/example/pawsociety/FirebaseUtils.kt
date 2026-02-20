package com.example.pawsociety

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*

object FileValidator {
    private const val MAX_FILE_SIZE = 5 * 1024 * 1024 // 5MB
    private val ALLOWED_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")

    fun validateImage(context: Context, uri: Uri): Resource<Unit> {
        val contentResolver = context.contentResolver
        
        // Check size
        val fileSize = contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0
        if (fileSize > MAX_FILE_SIZE) {
            return Resource.Error("File size exceeds 5MB limit")
        }

        // Check extension/mime type
        val mimeType = contentResolver.getType(uri)
        if (mimeType?.startsWith("image/") == true) {
            return Resource.Success(Unit)
        }

        val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            ?: mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
        
        if (extension?.lowercase() !in ALLOWED_EXTENSIONS) {
            return Resource.Error("Invalid file type. Please select an image.")
        }

        return Resource.Success(Unit)
    }

    fun compressAndGetFile(context: Context, uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        
        val compressedFile = File(context.cacheDir, "${UUID.randomUUID()}.jpg")
        FileOutputStream(compressedFile).use { it.write(outputStream.toByteArray()) }
        
        return compressedFile
    }
}
