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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.focuscontrol.PlannerPreferences
import com.example.focuscontrol.model.PlannerTask
import com.example.focuscontrol.ui.components.AppCard
import com.example.focuscontrol.ui.components.LogoWordmark
import com.example.focuscontrol.ui.components.SectionEyebrow
import com.example.focuscontrol.ui.theme.LavenderGradientStart
import com.example.focuscontrol.ui.theme.LavenderPrimary
import com.example.focuscontrol.ui.theme.LimeAccent
import com.example.focuscontrol.ui.theme.NavyCardInner
import com.example.focuscontrol.ui.theme.PinkAccent
import com.example.focuscontrol.ui.theme.TextDimmed
import com.example.focuscontrol.ui.theme.TextMuted
import com.example.focuscontrol.ui.theme.TextWhite
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun PlannerHomeScreen(
    plannerPrefs: PlannerPreferences,
    refreshTrigger: Int,
    onNavigateToStudyMaterials: () -> Unit,
    onOpenTask: (String) -> Unit,
    onAddTask: (String) -> Unit,
    onToggleTaskCompletion: (PlannerTask) -> Unit
) {
    // Calendar State
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    // Fetch tasks, triggered by refreshTrigger
    val tasks = remember(refreshTrigger) { plannerPrefs.tasks }
    
    // Day tasks
    val selectedDateString = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
    val dayTasks = tasks.filter { it.dateString == selectedDateString }.sortedBy { it.startTime }
    
    // Compute progress for the selected day
    val totalDayTasks = dayTasks.size
    val completedDayTasks = dayTasks.count { plannerPrefs.isTaskComplete(it.id) }
    val isDayComplete = totalDayTasks > 0 && completedDayTasks == totalDayTasks

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        LogoWordmark(subtitle = "Project Planner & AI Notes")

        // Progress Card
        AppCard {
            SectionEyebrow("DAILY PROGRESS")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isDayComplete) "Day Completed!" else "$completedDayTasks / $totalDayTasks Tasks",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isDayComplete) LimeAccent else TextWhite
                    )
                    Text(
                        text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
                
                // Progress Circle Placeholder
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val progress = if (totalDayTasks > 0) completedDayTasks.toFloat() / totalDayTasks else 0f
                    CircularProgressIndicator(
                        progress = { progress },
                        color = if (isDayComplete) LimeAccent else LavenderPrimary,
                        trackColor = NavyCardInner,
                        strokeWidth = 6.dp,
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Monthly Calendar
        AppCard {
            SectionEyebrow("PLANNER CALENDAR")
            Spacer(Modifier.height(12.dp))
            
            // Month Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "<",
                    style = MaterialTheme.typography.titleLarge,
                    color = LavenderPrimary,
                    modifier = Modifier.clickable { currentMonth = currentMonth.minusMonths(1) }.padding(8.dp)
                )
                Text(
                    text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = ">",
                    style = MaterialTheme.typography.titleLarge,
                    color = LavenderPrimary,
                    modifier = Modifier.clickable { currentMonth = currentMonth.plusMonths(1) }.padding(8.dp)
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Weekday Headers
            val daysOfWeek = listOf("M", "T", "W", "T", "F", "S", "S")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                daysOfWeek.forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextDimmed,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Calendar Grid
            val firstDayOfMonth = currentMonth.atDay(1)
            val daysInMonth = currentMonth.lengthOfMonth()
            val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1 (Mon) to 7 (Sun)
            
            val totalCells = daysInMonth + firstDayOfWeek - 1
            val weeks = Math.ceil(totalCells / 7.0).toInt()
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (week in 0 until weeks) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        for (day in 0..6) {
                            val cellIndex = week * 7 + day
                            val dayOfMonth = cellIndex - firstDayOfWeek + 2
                            
                            if (dayOfMonth in 1..daysInMonth) {
                                val date = currentMonth.atDay(dayOfMonth)
                                val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                val isSelected = date == selectedDate
                                
                                // Status indicator
                                val dateTasks = tasks.filter { it.dateString == dateString }
                                val totalCount = dateTasks.size
                                val completedCount = dateTasks.count { plannerPrefs.isTaskComplete(it.id) }
                                
                                val statusDotColor = when {
                                    totalCount == 0 -> null
                                    completedCount == totalCount -> LimeAccent
                                    completedCount > 0 -> LavenderPrimary
                                    else -> PinkAccent
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) LavenderPrimary.copy(alpha = 0.3f) else Color.Transparent)
                                        .clickable { selectedDate = date },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = dayOfMonth.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isSelected) LavenderPrimary else TextWhite,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (statusDotColor != null) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(statusDotColor)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        
        // Tasks for Selected Date
        SectionEyebrow("TASKS FOR ${selectedDate.format(DateTimeFormatter.ofPattern("MMM d"))}")
        if (dayTasks.isEmpty()) {
            AppCard {
                Text("No tasks scheduled for this date.", color = TextMuted)
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NavyCardInner)
                        .clickable { onAddTask(selectedDateString) }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+ Add Task", color = LavenderPrimary, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            dayTasks.forEach { task ->
                val isComplete = plannerPrefs.isTaskComplete(task.id)
                AppCard(
                    modifier = Modifier.clickable { onOpenTask(task.id) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${task.startTime} - ${task.endTime}",
                                style = MaterialTheme.typography.labelSmall,
                                color = LavenderPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = task.subject,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = task.topic,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isComplete) LimeAccent.copy(alpha = 0.2f) else NavyCardInner)
                                .clickable {
                                    val updatedTask = task.copy(isCompleted = !task.isCompleted)
                                    onToggleTaskCompletion(updatedTask)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isComplete) "DONE ✓" else "PENDING",
                                color = if (isComplete) LimeAccent else TextDimmed,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NavyCardInner)
                    .clickable { onAddTask(selectedDateString) }
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("+ Add Another Task", color = LavenderPrimary, fontWeight = FontWeight.Bold)
            }
        }



        // My Study Materials Card
        AppCard(
            modifier = Modifier.clickable { onNavigateToStudyMaterials() }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    SectionEyebrow("MY STUDY MATERIALS")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "View & Search Uploads",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }
                Text("View All →", color = LavenderPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}
