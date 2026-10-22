package com.example.focuscontrol.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.focuscontrol.PlannerPreferences
import com.example.focuscontrol.model.StudyDocument
import com.example.focuscontrol.ui.components.AppCard
import com.example.focuscontrol.ui.components.LogoWordmark
import com.example.focuscontrol.ui.components.SectionEyebrow
import com.example.focuscontrol.ui.theme.LavenderPrimary
import com.example.focuscontrol.ui.theme.LimeAccent
import com.example.focuscontrol.ui.theme.NavyBackground
import com.example.focuscontrol.ui.theme.NavyCardBorder
import com.example.focuscontrol.ui.theme.NavyCardInner
import com.example.focuscontrol.ui.theme.PinkAccent
import com.example.focuscontrol.ui.theme.TextDimmed
import com.example.focuscontrol.ui.theme.TextMuted
import com.example.focuscontrol.ui.theme.TextWhite
import com.example.focuscontrol.utils.GoogleDriveManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MyStudyMaterialsScreen(
    onBack: () -> Unit,
    onOpenTask: (String) -> Unit
) {
    val context = LocalContext.current
    val plannerPrefs = remember { PlannerPreferences(context) }
    var searchQuery by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    
    val allDocs = remember { plannerPrefs.documents }
    val filteredDocs = remember(searchQuery) {
        allDocs.filter {
            searchQuery.isBlank() ||
            it.filename.contains(searchQuery, ignoreCase = true) ||
            it.subject.contains(searchQuery, ignoreCase = true) ||
            it.topic.contains(searchQuery, ignoreCase = true)
        }
    }
    
    val groupedByDate = remember(filteredDocs) {
        filteredDocs.groupBy { it.studyDateString }.toSortedMap(reverseOrder())
    }

    Surface(modifier = Modifier.fillMaxSize(), color = NavyBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LogoWordmark(subtitle = "My Study Materials")
                Text(
                    text = "Back",
                    style = MaterialTheme.typography.titleMedium,
                    color = LavenderPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onBack() }
                )
            }
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by filename, subject or topic...", color = TextDimmed) },
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

            if (statusMessage != null) {
                Text(
                    text = statusMessage!!,
                    color = PinkAccent,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (groupedByDate.isEmpty()) {
                    item {
                        Text(
                            text = "No study materials found.",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 20.dp)
                        )
                    }
                }
                
                groupedByDate.forEach { (dateStr, docs) ->
                    item {
                        SectionEyebrow("SCHEDULED: $dateStr")
                        Spacer(Modifier.height(8.dp))
                    }
                    items(docs, key = { it.id }) { doc ->
                        DocumentCard(
                            doc = doc,
                            onOpenTask = onOpenTask,
                            onError = { err -> statusMessage = err }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentCard(
    doc: StudyDocument,
    onOpenTask: (String) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val formatter = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val uploadDateStr = remember(doc.uploadTimestamp) { formatter.format(Date(doc.uploadTimestamp)) }
    
    AppCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = doc.filename,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(LavenderPrimary.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(doc.subject, color = LavenderPrimary, style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(doc.topic, color = TextWhite, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Uploaded: $uploadDateStr", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                    if (doc.isDriveSynced && doc.driveFileId.isNotBlank()) {
                        Text("☁️ Google Drive Synced", color = LimeAccent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    } else {
                        Text("⚠️ Local Storage Only", color = TextDimmed, style = MaterialTheme.typography.labelSmall)
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "View Task →",
                        color = LavenderPrimary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onOpenTask(doc.taskId) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Prominent Open File Action Button
            Button(
                onClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        if (doc.driveFileId.isNotBlank()) {
                            val status = GoogleDriveManager.verifyDriveFile(context, doc.driveFileId)
                            if (!status.exists) {
                                withContext(Dispatchers.Main) {
                                    onError(status.errorMessage)
                                }
                                return@launch
                            }
                        }

                        val err = GoogleDriveManager.openDriveFile(
                            context = context,
                            driveFileId = doc.driveFileId,
                            webViewLink = doc.driveWebViewLink,
                            localPath = doc.internalPath,
                            mimeType = doc.mimeType
                        )

                        if (err != null) {
                            withContext(Dispatchers.Main) {
                                onError(err)
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyCardInner, contentColor = LavenderPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("📄 Open File", fontWeight = FontWeight.Bold)
            }
        }
    }
}
