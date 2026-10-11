package com.example.focuscontrol.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.focuscontrol.model.StudyDocument
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object DocumentUtils {
    fun copyUriToInternalStorage(context: Context, uri: Uri, taskId: String, subject: String, topic: String, dateString: String): StudyDocument? {
        try {
            var filename = "upload_${System.currentTimeMillis()}"
            var mimeType = context.contentResolver.getType(uri) ?: ""

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        filename = cursor.getString(nameIndex)
                    }
                }
            }

            if (mimeType.isBlank()) {
                mimeType = GoogleDriveManager.getMimeTypeFromFilename(filename)
            }

            val internalDir = File(context.filesDir, "study_materials")
            if (!internalDir.exists()) {
                internalDir.mkdirs()
            }

            val destFile = File(internalDir, "${UUID.randomUUID()}_$filename")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            return StudyDocument(
                id = UUID.randomUUID().toString(),
                taskId = taskId,
                subject = subject,
                topic = topic,
                studyDateString = dateString,
                uploadTimestamp = System.currentTimeMillis(),
                filename = filename,
                mimeType = mimeType,
                internalPath = destFile.absolutePath,
                extractedText = ""
            )
        } catch (e: Exception) {
            Log.e("DocumentUtils", "Failed to copy document", e)
            return null
        }
    }
}
