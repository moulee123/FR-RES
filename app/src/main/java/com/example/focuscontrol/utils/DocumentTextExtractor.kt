package com.example.focuscontrol.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

data class ExtractedPageText(
    val pageNumber: Int,
    val text: String
)

object DocumentTextExtractor {
    private const val TAG = "DocumentTextExtractor"

    suspend fun extractTextFromDocument(context: Context, file: File): List<ExtractedPageText> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ExtractedPageText>()
        if (!file.exists()) return@withContext results

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        try {
            val extension = file.extension.lowercase()
            if (extension == "pdf") {
                val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val pdfRenderer = PdfRenderer(fileDescriptor)

                for (i in 0 until pdfRenderer.pageCount) {
                    val page = pdfRenderer.openPage(i)
                    // Render page at 2x scale for better OCR accuracy
                    val bitmap = Bitmap.createBitmap(
                        page.width * 2,
                        page.height * 2,
                        Bitmap.Config.ARGB_8888
                    )
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    val inputImage = InputImage.fromBitmap(bitmap, 0)
                    val visionText = recognizer.process(inputImage).await()
                    val pageText = visionText.text.trim()

                    if (pageText.isNotBlank()) {
                        results.add(ExtractedPageText(pageNumber = i + 1, text = pageText))
                    }
                    bitmap.recycle()
                }

                pdfRenderer.close()
                fileDescriptor.close()
            } else if (extension == "txt" || extension == "doc" || extension == "docx") {
                val plainText = file.readText().trim()
                if (plainText.isNotBlank()) {
                    results.add(ExtractedPageText(pageNumber = 1, text = plainText))
                }
            } else {
                // Image file (.jpg, .jpeg, .png, .webp, etc.)
                val inputImage = InputImage.fromFilePath(context, Uri.fromFile(file))
                val visionText = recognizer.process(inputImage).await()
                val pageText = visionText.text.trim()

                if (pageText.isNotBlank()) {
                    results.add(ExtractedPageText(pageNumber = 1, text = pageText))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text from ${file.name}", e)
        } finally {
            try {
                recognizer.close()
            } catch (_: Exception) {}
        }

        return@withContext results
    }
}
