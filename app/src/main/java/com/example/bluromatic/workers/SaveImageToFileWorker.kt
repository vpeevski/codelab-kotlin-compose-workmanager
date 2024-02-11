package com.example.bluromatic.workers

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.bluromatic.DELAY_TIME_MILLIS
import com.example.bluromatic.KEY_IMAGE_URI
import com.example.bluromatic.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Saves the image to a permanent file
 */
private const val TAG = "SaveImageToFileWorker"

class SaveImageToFileWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    private val title = "Blurred Image"
    private val dateFormatter = SimpleDateFormat(
        "yyyy.MM.dd 'at' HH:mm:ss z",
        Locale.getDefault()
    )

    override suspend fun doWork(): Result {
        // Makes a notification when the work starts and slows down the work so that
        // it's easier to see each WorkRequest start, even on emulated devices
        makeStatusNotification(
            applicationContext.resources.getString(R.string.saving_image),
            applicationContext
        )

        return withContext(Dispatchers.IO) {
            delay(DELAY_TIME_MILLIS)

            val resolver = applicationContext.contentResolver
            return@withContext try {
                val resourceUri = inputData.getString(KEY_IMAGE_URI)
                val bitmap = BitmapFactory.decodeStream(
                    resolver.openInputStream(Uri.parse(resourceUri))
                )

                // Prepare the metadata for the image
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, title) // File name
                    put(MediaStore.Images.Media.DESCRIPTION, "Image blurred: $title")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg") // File type
                    // For API level 29 and above, specify the directory and use IS_PENDING to indicate it's being written
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

                // Insert the metadata to the MediaStore, getting the Uri to the file
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

                imageUri?.let {
                    resolver.openOutputStream(it).use { outputStream ->
                        // Write the bitmap to the obtained output stream
                        outputStream?.let {
                            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)) {
                                throw IOException("Failed to save bitmap.")
                            }
                            // If targeting API level 29 (Android Q) and above, update IS_PENDING to 0 after writing
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                values.clear()
                                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                                resolver.update(imageUri, values, null, null)
                            }
                            val output = workDataOf(KEY_IMAGE_URI to imageUri.toString())
                            Result.success(output)
                        }
                    }
                } ?: throw IOException("Failed to create new MediaStore record.")


//                val imageUrl = MediaStore.Images.Media.insertImage(
//                    resolver, bitmap, title, dateFormatter.format(Date())
//                )
//                if (imageUri != null && imageUri.toString().isNotEmpty()) {
//                    val output = workDataOf(KEY_IMAGE_URI to imageUri)
//
//                    Result.success(output)
//                } else {
//                    Log.e(
//                        TAG,
//                        applicationContext.resources.getString(R.string.writing_to_mediaStore_failed)
//                    )
//                    Result.failure()
//                }

            } catch (exception: Exception) {
                Log.e(
                    TAG,
                    applicationContext.resources.getString(R.string.error_saving_image),
                    exception
                )
                Result.failure()
            }
        }
    }
}