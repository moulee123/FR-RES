package com.example.focuscontrol.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.buffer
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class DriveUploadResult(
    val success: Boolean,
    val driveFileId: String = "",
    val webViewLink: String = "",
    val mimeType: String = "",
    val errorMessage: String = ""
)

data class DriveFileStatus(
    val exists: Boolean,
    val isTrashed: Boolean = false,
    val webViewLink: String = "",
    val name: String = "",
    val mimeType: String = "",
    val errorMessage: String = ""
)

object GoogleDriveManager {
    private const val TAG = "GoogleDriveManager"
    private const val FOLDER_NAME = "FRERES Study Materials"
    private const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.file"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DRIVE_SCOPE))
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun getSignedInAccount(context: Context): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    suspend fun getAccessToken(context: Context): String? = withContext(Dispatchers.IO) {
        val account = getSignedInAccount(context) ?: return@withContext null
        try {
            val androidAccount = account.account
            if (androidAccount != null) {
                return@withContext GoogleAuthUtil.getToken(context, androidAccount, "oauth2:$DRIVE_SCOPE")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining access token", e)
        }
        return@withContext null
    }

    suspend fun getOrCreateFolderId(context: Context): String? = withContext(Dispatchers.IO) {
        val token = getAccessToken(context) ?: return@withContext null

        // Search for existing folder
        val searchUrl = "https://www.googleapis.com/drive/v3/files?q=name='${Uri.encode(FOLDER_NAME)}'+and+mimeType='application/vnd.google-apps.folder'+and+trashed=false&fields=files(id,name)"
        val request = Request.Builder()
            .url(searchUrl)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val files = json.getJSONArray("files")
                    if (files.length() > 0) {
                        return@withContext files.getJSONObject(0).getString("id")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching folder", e)
        }

        // Create folder if not found
        val createUrl = "https://www.googleapis.com/drive/v3/files"
        val jsonPayload = JSONObject().apply {
            put("name", FOLDER_NAME)
            put("mimeType", "application/vnd.google-apps.folder")
        }

        val createRequest = Request.Builder()
            .url(createUrl)
            .addHeader("Authorization", "Bearer $token")
            .post(jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        try {
            httpClient.newCall(createRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    return@withContext json.optString("id", null)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating folder", e)
        }

        return@withContext null
    }

    suspend fun findExistingFileInDrive(context: Context, folderId: String, filename: String): DriveUploadResult? = withContext(Dispatchers.IO) {
        val token = getAccessToken(context) ?: return@withContext null

        val query = "name='${Uri.encode(filename)}' and '$folderId' in parents and trashed=false"
        val searchUrl = "https://www.googleapis.com/drive/v3/files?q=$query&fields=files(id,name,mimeType,webViewLink)"
        
        val request = Request.Builder()
            .url(searchUrl)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val files = json.getJSONArray("files")
                    if (files.length() > 0) {
                        val fileObj = files.getJSONObject(0)
                        val id = fileObj.getString("id")
                        val link = fileObj.optString("webViewLink", "https://drive.google.com/file/d/$id/view")
                        val mime = fileObj.optString("mimeType", "")
                        return@withContext DriveUploadResult(
                            success = true,
                            driveFileId = id,
                            webViewLink = link,
                            mimeType = mime
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching existing file", e)
        }
        return@withContext null
    }

    suspend fun uploadFile(
        context: Context,
        file: File,
        filename: String,
        mimeType: String,
        onProgress: (Int) -> Unit = {}
    ): DriveUploadResult = withContext(Dispatchers.IO) {
        val token = getAccessToken(context)
        if (token.isNullOrEmpty()) {
            return@withContext DriveUploadResult(
                success = false,
                errorMessage = "Google Drive authorization required. Please sign in to Google."
            )
        }

        val folderId = getOrCreateFolderId(context)
        if (folderId.isNullOrEmpty()) {
            return@withContext DriveUploadResult(
                success = false,
                errorMessage = "Failed to access or create 'FRÈRES Study Materials' folder in Google Drive."
            )
        }

        // Avoid duplicate uploads by checking if file already exists in folder
        val existing = findExistingFileInDrive(context, folderId, filename)
        if (existing != null) {
            onProgress(100)
            return@withContext existing
        }

        val uploadUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name,mimeType,webViewLink"

        val metadataJson = JSONObject().apply {
            put("name", filename)
            put("parents", JSONArray().put(folderId))
        }

        val actualMime = if (mimeType.isNotBlank()) mimeType else getMimeTypeFromFilename(filename)

        val fileRequestBody = file.asRequestBody(actualMime.toMediaType())
        val progressRequestBody = ProgressRequestBody(fileRequestBody) { bytesWritten, totalBytes ->
            if (totalBytes > 0) {
                val percent = ((bytesWritten * 100) / totalBytes).toInt()
                onProgress(percent.coerceIn(0, 100))
            }
        }

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addPart(
                metadataJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            )
            .addPart(progressRequestBody)
            .build()

        val request = Request.Builder()
            .url(uploadUrl)
            .addHeader("Authorization", "Bearer $token")
            .post(multipartBody)
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(bodyStr)
                    val id = json.getString("id")
                    val link = json.optString("webViewLink", "https://drive.google.com/file/d/$id/view")
                    val resMime = json.optString("mimeType", actualMime)
                    onProgress(100)
                    return@withContext DriveUploadResult(
                        success = true,
                        driveFileId = id,
                        webViewLink = link,
                        mimeType = resMime
                    )
                } else {
                    Log.e(TAG, "Upload failed with code ${response.code}: $bodyStr")
                    val errorMsg = if (response.code == 401 || response.code == 403) {
                        "Google permission or authentication error (${response.code}). Please re-authenticate."
                    } else {
                        "Upload failed (${response.code}): $bodyStr"
                    }
                    return@withContext DriveUploadResult(success = false, errorMessage = errorMsg)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload exception", e)
            return@withContext DriveUploadResult(
                success = false,
                errorMessage = "Network or upload exception: ${e.message}"
            )
        }
    }

    suspend fun verifyDriveFile(context: Context, driveFileId: String): DriveFileStatus = withContext(Dispatchers.IO) {
        if (driveFileId.isBlank()) {
            return@withContext DriveFileStatus(exists = false, errorMessage = "No Drive File ID associated.")
        }

        val token = getAccessToken(context)
            ?: return@withContext DriveFileStatus(exists = false, errorMessage = "Authentication required. Please sign in to Google Drive.")

        val url = "https://www.googleapis.com/drive/v3/files/$driveFileId?fields=id,name,mimeType,trashed,webViewLink"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val trashed = json.optBoolean("trashed", false)
                    val link = json.optString("webViewLink", "https://drive.google.com/file/d/$driveFileId/view")
                    val name = json.optString("name", "")
                    val mime = json.optString("mimeType", "")
                    return@withContext DriveFileStatus(
                        exists = !trashed,
                        isTrashed = trashed,
                        webViewLink = link,
                        name = name,
                        mimeType = mime,
                        errorMessage = if (trashed) "File is in Google Drive trash." else ""
                    )
                } else if (response.code == 404) {
                    return@withContext DriveFileStatus(exists = false, errorMessage = "File not found on Google Drive (may have been deleted).")
                } else if (response.code == 401 || response.code == 403) {
                    return@withContext DriveFileStatus(exists = false, errorMessage = "Google Drive session expired or permission denied. Please sign in again.")
                } else {
                    return@withContext DriveFileStatus(exists = false, errorMessage = "Failed to reach Google Drive (HTTP ${response.code}).")
                }
            }
        } catch (e: Exception) {
            return@withContext DriveFileStatus(exists = false, errorMessage = "Network error: ${e.message}")
        }
    }

    suspend fun downloadOrExportDriveDocument(
        context: Context,
        driveFileId: String,
        mimeType: String,
        targetFile: File
    ): String? = withContext(Dispatchers.IO) {
        val token = getAccessToken(context) ?: return@withContext null

        val isGoogleDoc = mimeType.contains("google-apps.document") || mimeType.contains("google-apps")
        val downloadUrl = if (isGoogleDoc) {
            "https://www.googleapis.com/drive/v3/files/$driveFileId/export?mimeType=text/plain"
        } else {
            "https://www.googleapis.com/drive/v3/files/$driveFileId?alt=media"
        }

        val request = Request.Builder()
            .url(downloadUrl)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val inputStream = response.body?.byteStream() ?: return@withContext null
                    FileOutputStream(targetFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    return@withContext targetFile.absolutePath
                } else {
                    Log.e(TAG, "Download/Export failed: ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading Drive document", e)
        }
        return@withContext null
    }

    fun openDriveFile(context: Context, driveFileId: String, webViewLink: String, localPath: String, mimeType: String): String? {
        try {
            // First try launching Google Drive webViewLink or direct drive URL
            val driveUri = if (webViewLink.isNotBlank()) {
                Uri.parse(webViewLink)
            } else if (driveFileId.isNotBlank()) {
                Uri.parse("https://drive.google.com/file/d/$driveFileId/view")
            } else {
                null
            }

            if (driveUri != null) {
                val intent = Intent(Intent.ACTION_VIEW, driveUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return null // Success, no error
            }

            // Fallback: local file viewing via FileProvider
            if (localPath.isNotBlank()) {
                val file = File(localPath)
                if (file.exists()) {
                    val fileUri: Uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    val actualMime = if (mimeType.isNotBlank()) mimeType else getMimeTypeFromFilename(file.name)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(fileUri, actualMime)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    return null // Success
                }
            }

            return "Could not locate file to open."
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open file", e)
            return "Unable to open file: ${e.message}"
        }
    }

    fun getMimeTypeFromFilename(filename: String): String {
        val ext = filename.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "pdf" -> "application/pdf"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            "txt" -> "text/plain"
            "doc", "docx" -> "application/msword"
            else -> "application/octet-stream"
        }
    }
}

class ProgressRequestBody(
    private val delegate: RequestBody,
    private val onProgress: (bytesWritten: Long, totalBytes: Long) -> Unit
) : RequestBody() {
    override fun contentType() = delegate.contentType()
    override fun contentLength() = delegate.contentLength()

    override fun writeTo(sink: BufferedSink) {
        val countingSink = object : ForwardingSink(sink) {
            private var bytesWritten = 0L
            override fun write(source: Buffer, byteCount: Long) {
                super.write(source, byteCount)
                bytesWritten += byteCount
                onProgress(bytesWritten, contentLength())
            }
        }
        val bufferedSink = countingSink.buffer()
        delegate.writeTo(bufferedSink)
        bufferedSink.flush()
    }
}
