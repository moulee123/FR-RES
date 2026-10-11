package com.example.focuscontrol

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.focuscontrol.model.PlannerTask
import com.example.focuscontrol.model.StudyDocument
import org.json.JSONArray
import org.json.JSONObject

class PlannerPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("focus_planner_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TASKS = "planner_tasks"
        private const val KEY_DOCUMENTS = "study_documents"
    }

    var tasks: List<PlannerTask>
        get() {
            val jsonString = prefs.getString(KEY_TASKS, "[]") ?: "[]"
            return try {
                val array = JSONArray(jsonString)
                val list = mutableListOf<PlannerTask>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        PlannerTask(
                            id = obj.getString("id"),
                            subject = obj.getString("subject"),
                            topic = obj.getString("topic"),
                            dateString = obj.getString("dateString"),
                            startTime = obj.getString("startTime"),
                            endTime = obj.getString("endTime"),
                            notes = obj.optString("notes", ""),
                            isCompleted = obj.optBoolean("isCompleted", false)
                        )
                    )
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }
        set(value) {
            val array = JSONArray()
            for (task in value) {
                val obj = JSONObject()
                obj.put("id", task.id)
                obj.put("subject", task.subject)
                obj.put("topic", task.topic)
                obj.put("dateString", task.dateString)
                obj.put("startTime", task.startTime)
                obj.put("endTime", task.endTime)
                obj.put("notes", task.notes)
                obj.put("isCompleted", task.isCompleted)
                array.put(obj)
            }
            prefs.edit { putString(KEY_TASKS, array.toString()) }
        }

    fun saveTask(task: PlannerTask) {
        val current = tasks.toMutableList()
        val index = current.indexOfFirst { it.id == task.id }
        if (index != -1) {
            current[index] = task
        } else {
            current.add(task)
        }
        tasks = current
    }

    fun deleteTask(taskId: String) {
        tasks = tasks.filter { it.id != taskId }
        documents = documents.filter { it.taskId != taskId }
    }

    var documents: List<StudyDocument>
        get() {
            val jsonString = prefs.getString(KEY_DOCUMENTS, "[]") ?: "[]"
            return try {
                val array = JSONArray(jsonString)
                val list = mutableListOf<StudyDocument>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        StudyDocument(
                            id = obj.getString("id"),
                            taskId = obj.getString("taskId"),
                            subject = obj.getString("subject"),
                            topic = obj.getString("topic"),
                            studyDateString = obj.getString("studyDateString"),
                            uploadTimestamp = obj.getLong("uploadTimestamp"),
                            filename = obj.getString("filename"),
                            mimeType = obj.optString("mimeType", ""),
                            driveFileId = obj.optString("driveFileId", ""),
                            driveWebViewLink = obj.optString("driveWebViewLink", ""),
                            internalPath = obj.optString("internalPath", ""),
                            pageCount = obj.optInt("pageCount", 0),
                            pageMetadata = obj.optString("pageMetadata", ""),
                            extractedText = obj.optString("extractedText", ""),
                            isDriveSynced = obj.optBoolean("isDriveSynced", false)
                        )
                    )
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }
        set(value) {
            val array = JSONArray()
            for (doc in value) {
                val obj = JSONObject()
                obj.put("id", doc.id)
                obj.put("taskId", doc.taskId)
                obj.put("subject", doc.subject)
                obj.put("topic", doc.topic)
                obj.put("studyDateString", doc.studyDateString)
                obj.put("uploadTimestamp", doc.uploadTimestamp)
                obj.put("filename", doc.filename)
                obj.put("mimeType", doc.mimeType)
                obj.put("driveFileId", doc.driveFileId)
                obj.put("driveWebViewLink", doc.driveWebViewLink)
                obj.put("internalPath", doc.internalPath)
                obj.put("pageCount", doc.pageCount)
                obj.put("pageMetadata", doc.pageMetadata)
                obj.put("extractedText", doc.extractedText)
                obj.put("isDriveSynced", doc.isDriveSynced)
                array.put(obj)
            }
            prefs.edit { putString(KEY_DOCUMENTS, array.toString()) }
        }

    fun saveDocument(doc: StudyDocument) {
        val current = documents.toMutableList()
        val index = current.indexOfFirst { it.id == doc.id }
        if (index != -1) {
            current[index] = doc
        } else {
            current.add(doc)
        }
        documents = current
    }

    fun deleteDocument(docId: String) {
        documents = documents.filter { it.id != docId }
    }

    fun isTaskComplete(taskId: String): Boolean {
        return tasks.find { it.id == taskId }?.isCompleted == true
    }
}
