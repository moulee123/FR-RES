package com.example.focuscontrol.model

data class PlannerTask(
    val id: String,
    val subject: String,
    val topic: String,
    val dateString: String, // YYYY-MM-DD
    val startTime: String, // HH:mm
    val endTime: String, // HH:mm
    val notes: String = "",
    val isCompleted: Boolean = false
)

data class StudyDocument(
    val id: String,
    val taskId: String,
    val subject: String,
    val topic: String,
    val studyDateString: String, // YYYY-MM-DD from the task
    val uploadTimestamp: Long, // Actual upload date and time
    val filename: String,
    val mimeType: String = "",
    val driveFileId: String = "",
    val driveWebViewLink: String = "",
    val internalPath: String = "", // Path in app's internal storage
    val pageCount: Int = 0,
    val pageMetadata: String = "", // e.g. "Page 1-5" or JSON page details
    val extractedText: String = "",
    val isDriveSynced: Boolean = false
)
