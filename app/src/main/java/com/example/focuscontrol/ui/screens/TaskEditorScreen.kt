package com.example.focuscontrol.ui.screens

import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.focuscontrol.PlannerPreferences
import com.example.focuscontrol.model.PlannerTask
import com.example.focuscontrol.model.StudyDocument
import com.example.focuscontrol.ui.components.AppCard
import com.example.focuscontrol.ui.components.LogoWordmark
import com.example.focuscontrol.ui.components.SectionEyebrow
import com.example.focuscontrol.ui.theme.LavenderGradientStart
import com.example.focuscontrol.ui.theme.LavenderPrimary
import com.example.focuscontrol.ui.theme.LimeAccent
import com.example.focuscontrol.ui.theme.NavyBackground
import com.example.focuscontrol.ui.theme.NavyCardBorder
import com.example.focuscontrol.ui.theme.NavyCardInner
import com.example.focuscontrol.ui.theme.PinkAccent
import com.example.focuscontrol.ui.theme.TextDimmed
import com.example.focuscontrol.ui.theme.TextMuted
import com.example.focuscontrol.ui.theme.TextWhite
import com.example.focuscontrol.utils.DocumentTextExtractor
import com.example.focuscontrol.utils.DocumentUtils
import com.example.focuscontrol.utils.GoogleDriveManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TaskEditorScreen(
    initialDateString: String,
    existingTask: PlannerTask?,
    plannerPrefs: PlannerPreferences,
    onSave: (PlannerTask) -> Unit,
    onDelete: ((String) -> Unit)? = null,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var subject by remember { mutableStateOf(existingTask?.subject ?: "") }
    var topic by remember { mutableStateOf(existingTask?.topic ?: "") }
    var startTime by remember { mutableStateOf(existingTask?.startTime ?: "09:00") }
    var endTime by remember { mutableStateOf(existingTask?.endTime ?: "10:00") }
    var notes by remember { mutableStateOf(existingTask?.notes ?: "") }
    
    val taskId = remember { existingTask?.id ?: System.currentTimeMillis().toString() }
    
    var attachedDocuments by remember { mutableStateOf(plannerPrefs.documents.filter { it.taskId == taskId }) }
    var uploadStatusMessage by remember { mutableStateOf<String?>(null) }
    var uploadErrorMessage by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0) }

    var googleAccount by remember { mutableStateOf(GoogleDriveManager.getSignedInAccount(context)) }
    var driveAuthError by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            googleAccount = account
            driveAuthError = null
            uploadStatusMessage = "Signed in to Google Drive successfully as ${account.email}!"
        } catch (e: Exception) {
            driveAuthError = "Google Sign-In failed: ${e.message ?: "User cancelled or authorization failed."}"
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            if (googleAccount == null) {
                uploadErrorMessage = "Google Drive authorization required. Please sign in to Google Drive below before uploading."
                return@rememberLauncherForActivityResult
            }

            isUploading = true
            uploadErrorMessage = null
            uploadStatusMessage = "Preparing files for Google Drive upload..."
            uploadProgress = 0

            coroutineScope.launch(Dispatchers.IO) {
                var successCount = 0
                var failCount = 0

                for ((index, uri) in uris.withIndex()) {
                    withContext(Dispatchers.Main) {
                        uploadStatusMessage = "Uploading file ${index + 1} of ${uris.size} to Google Drive..."
                    }

                    val localDoc = DocumentUtils.copyUriToInternalStorage(
                        context, uri, taskId, subject.ifBlank { "Untitled Subject" }, topic.ifBlank { "Untitled Topic" }, initialDateString
                    )

                    if (localDoc != null) {
                        val localFile = File(localDoc.internalPath)

                        // Extract text using OCR / PDF reader
                        val pageTexts = DocumentTextExtractor.extractTextFromDocument(context, localFile)
                        val formattedText = if (pageTexts.isNotEmpty()) {
                            pageTexts.joinToString("\n\n") { "--- Page ${it.pageNumber} ---\n${it.text}" }
                        } else {
                            ""
                        }

                        // Upload to Google Drive folder "FRÈRES Study Materials"
                        val driveResult = GoogleDriveManager.uploadFile(
                            context = context,
                            file = localFile,
                            filename = localDoc.filename,
                            mimeType = localDoc.mimeType,
                            onProgress = { percent ->
                                coroutineScope.launch(Dispatchers.Main) {
                                    uploadProgress = percent
                                }
                            }
                        )

                        if (driveResult.success) {
                            val finalDoc = localDoc.copy(
                                driveFileId = driveResult.driveFileId,
                                driveWebViewLink = driveResult.webViewLink,
                                mimeType = driveResult.mimeType.ifBlank { localDoc.mimeType },
                                pageCount = pageTexts.size,
                                pageMetadata = if (pageTexts.isNotEmpty()) "${pageTexts.size} page(s) extracted" else "1 document",
                                extractedText = formattedText,
                                isDriveSynced = true
                            )
                            plannerPrefs.saveDocument(finalDoc)
                            successCount++
                        } else {
                            // Local fallback if Drive upload failed, keep as unsynced
                            val unsyncedDoc = localDoc.copy(
                                extractedText = formattedText,
                                isDriveSynced = false
                            )
                            plannerPrefs.saveDocument(unsyncedDoc)
                            failCount++
                            withContext(Dispatchers.Main) {
                                uploadErrorMessage = driveResult.errorMessage
                            }
                        }
                    } else {
                        failCount++
                    }
                }

                withContext(Dispatchers.Main) {
                    attachedDocuments = plannerPrefs.documents.filter { it.taskId == taskId }
                    isUploading = false
                    if (failCount == 0) {
                        uploadStatusMessage = "All $successCount file(s) successfully uploaded to Google Drive!"
                    } else if (successCount > 0) {
                        uploadStatusMessage = "$successCount uploaded to Drive, $failCount failed."
                    }
                }
            }
        }
    }

    val gradientBrush = Brush.horizontalGradient(listOf(LavenderGradientStart, PinkAccent))
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()) }

    Surface(modifier = Modifier.fillMaxSize(), color = NavyBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LogoWordmark(subtitle = if (existingTask != null) "Edit Task" else "Add New Task")
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.titleMedium,
                    color = LavenderPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onCancel() }
                )
            }

            // Google Drive Connection Status Card
            AppCard {
                SectionEyebrow("GOOGLE DRIVE INTEGRATION")
                Spacer(Modifier.height(8.dp))

                if (googleAccount != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Connected to Google Drive ☁️", color = LimeAccent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Text(googleAccount?.email ?: "Authorized user", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            text = "Sign Out",
                            color = PinkAccent,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                GoogleDriveManager.getGoogleSignInClient(context).signOut().addOnCompleteListener {
                                    googleAccount = null
                                    uploadStatusMessage = "Signed out of Google Drive."
                                }
                            }.padding(start = 8.dp)
                        )
                    }
                } else {
                    Text("Sign in with Google to upload and sync study materials to your dedicated 'FRÈRES Study Materials' Drive folder.", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val signInIntent = GoogleDriveManager.getGoogleSignInClient(context).signInIntent
                            googleSignInLauncher.launch(signInIntent)
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary, contentColor = TextWhite),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🔑 Connect Google Drive Account", fontWeight = FontWeight.Bold)
                    }
                }

                if (driveAuthError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(driveAuthError!!, color = PinkAccent, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Task Details Card
            AppCard {
                SectionEyebrow("TASK INFORMATION")
                Spacer(Modifier.height(10.dp))

                // Subject
                Text("Subject", style = MaterialTheme.typography.labelLarge, color = TextWhite)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = LavenderPrimary,
                        unfocusedBorderColor = NavyCardBorder,
                        focusedContainerColor = NavyCardInner,
                        unfocusedContainerColor = NavyCardInner
                    )
                )

                Spacer(Modifier.height(16.dp))

                // Topic
                Text("Topic / Unit", style = MaterialTheme.typography.labelLarge, color = TextWhite)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = LavenderPrimary,
                        unfocusedBorderColor = NavyCardBorder,
                        focusedContainerColor = NavyCardInner,
                        unfocusedContainerColor = NavyCardInner
                    )
                )

                Spacer(Modifier.height(16.dp))

                // Start Time
                Text("Start Time", style = MaterialTheme.typography.labelLarge, color = TextWhite)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = startTime,
                    onValueChange = { },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val timeParts = startTime.split(":")
                            val initialHour = timeParts.getOrNull(0)?.toIntOrNull() ?: 9
                            val initialMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
                            TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    startTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                                },
                                initialHour,
                                initialMinute,
                                false
                            ).show()
                        },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = TextWhite,
                        disabledBorderColor = NavyCardBorder,
                        disabledContainerColor = NavyCardInner
                    )
                )

                Spacer(Modifier.height(16.dp))

                // End Time
                Text("End Time", style = MaterialTheme.typography.labelLarge, color = TextWhite)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = endTime,
                    onValueChange = { },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val timeParts = endTime.split(":")
                            val initialHour = timeParts.getOrNull(0)?.toIntOrNull() ?: 10
                            val initialMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
                            TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    endTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                                },
                                initialHour,
                                initialMinute,
                                false
                            ).show()
                        },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = TextWhite,
                        disabledBorderColor = NavyCardBorder,
                        disabledContainerColor = NavyCardInner
                    )
                )
                
                Spacer(Modifier.height(16.dp))

                // Notes
                Text("Notes (Optional)", style = MaterialTheme.typography.labelLarge, color = TextWhite)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth().height(90.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = LavenderPrimary,
                        unfocusedBorderColor = NavyCardBorder,
                        focusedContainerColor = NavyCardInner,
                        unfocusedContainerColor = NavyCardInner
                    )
                )
            }

            // Attachments Card (Direct Drive Upload & Open)
            AppCard {
                SectionEyebrow("ATTACHMENTS & GOOGLE DRIVE")
                Spacer(Modifier.height(8.dp))
                
                if (attachedDocuments.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        attachedDocuments.forEach { doc ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NavyCardInner)
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(doc.filename, color = TextWhite, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(2.dp))
                                        Text("Uploaded: ${dateFormatter.format(Date(doc.uploadTimestamp))}", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                                        if (doc.isDriveSynced && doc.driveFileId.isNotBlank()) {
                                            Text("☁️ Google Drive Synced", color = LimeAccent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        } else {
                                            Text("⚠️ Not Synced to Drive", color = PinkAccent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Text(
                                        text = "Remove",
                                        color = PinkAccent,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.clickable {
                                            plannerPrefs.deleteDocument(doc.id)
                                            attachedDocuments = plannerPrefs.documents.filter { it.taskId == taskId }
                                        }.padding(start = 8.dp)
                                    )
                                }

                                Spacer(Modifier.height(8.dp))

                                // Clear Open File Action Button
                                Button(
                                    onClick = {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            if (doc.driveFileId.isNotBlank()) {
                                                val status = GoogleDriveManager.verifyDriveFile(context, doc.driveFileId)
                                                if (!status.exists) {
                                                    withContext(Dispatchers.Main) {
                                                        uploadErrorMessage = status.errorMessage
                                                    }
                                                    return@launch
                                                }
                                            }

                                            val openErr = GoogleDriveManager.openDriveFile(
                                                context = context,
                                                driveFileId = doc.driveFileId,
                                                webViewLink = doc.driveWebViewLink,
                                                localPath = doc.internalPath,
                                                mimeType = doc.mimeType
                                            )

                                            if (openErr != null) {
                                                withContext(Dispatchers.Main) {
                                                    uploadErrorMessage = openErr
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(38.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C324B), contentColor = LavenderPrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("📄 Open File in Google Drive", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                
                if (isUploading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        CircularProgressIndicator(color = LavenderPrimary)
                        Spacer(Modifier.height(6.dp))
                        Text(uploadStatusMessage ?: "Uploading to Drive...", color = TextWhite, style = MaterialTheme.typography.bodySmall)
                        Text("$uploadProgress%", color = LavenderPrimary, fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            if (googleAccount == null) {
                                val signInIntent = GoogleDriveManager.getGoogleSignInClient(context).signInIntent
                                googleSignInLauncher.launch(signInIntent)
                            } else {
                                documentPickerLauncher.launch("*/*")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        border = BorderStroke(1.dp, LavenderPrimary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LavenderPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (googleAccount == null) "🔑 Sign In & Upload Notes" else "+ Upload Notes to Google Drive", fontWeight = FontWeight.Bold)
                    }
                }

                if (uploadStatusMessage != null && !isUploading) {
                    Spacer(Modifier.height(8.dp))
                    Text(uploadStatusMessage!!, color = LimeAccent, style = MaterialTheme.typography.bodySmall)
                }
                
                if (uploadErrorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(uploadErrorMessage!!, color = PinkAccent, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Save Button
            Button(
                onClick = {
                    val updated = PlannerTask(
                        id = taskId,
                        subject = subject.ifBlank { "Untitled Subject" },
                        topic = topic.ifBlank { "Untitled Topic" },
                        dateString = existingTask?.dateString ?: initialDateString,
                        startTime = startTime,
                        endTime = endTime,
                        notes = notes
                    )
                    onSave(updated)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(gradientBrush, RoundedCornerShape(14.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = TextWhite
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Save Task", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }

            // Delete Button
            if (existingTask != null && onDelete != null) {
                OutlinedButton(
                    onClick = { onDelete(existingTask.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    border = BorderStroke(1.dp, PinkAccent),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PinkAccent),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("🗑 Delete Task", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
