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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.unit.dp
import com.example.focuscontrol.model.StudySchedule
import com.example.focuscontrol.ui.components.AppCard
import com.example.focuscontrol.ui.components.LogoWordmark
import com.example.focuscontrol.ui.components.SectionEyebrow
import com.example.focuscontrol.ui.theme.LavenderGradientStart
import com.example.focuscontrol.ui.theme.LavenderPrimary
import com.example.focuscontrol.ui.theme.NavyCardBorder
import com.example.focuscontrol.ui.theme.NavyCardInner
import com.example.focuscontrol.ui.theme.PinkAccent
import com.example.focuscontrol.ui.theme.TextDimmed
import com.example.focuscontrol.ui.theme.TextMuted
import com.example.focuscontrol.ui.theme.TextWhite

import androidx.compose.ui.platform.LocalContext
import com.example.focuscontrol.model.StudyCycle
import java.util.Locale
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.input.KeyboardType

data class CycleInputState(
    val id: Long,
    val studyHoursStr: String,
    val studyMinutesStr: String,
    val breakHoursStr: String,
    val breakMinutesStr: String
)

@Composable
fun ScheduleEditorScreen(
    initialSchedule: StudySchedule? = null,
    onSave: (StudySchedule) -> Unit,
    onDelete: ((StudySchedule) -> Unit)? = null,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialSchedule?.name ?: "New Study Schedule") }
    var startTime by remember { mutableStateOf(initialSchedule?.startTime ?: "18:00") }
    
    val initialCycles = if (initialSchedule?.cycles.isNullOrEmpty()) {
        listOf(
            CycleInputState(
                id = System.currentTimeMillis(),
                studyHoursStr = ((initialSchedule?.studyDurationMinutes ?: 45) / 60).toString(),
                studyMinutesStr = ((initialSchedule?.studyDurationMinutes ?: 45) % 60).toString(),
                breakHoursStr = ((initialSchedule?.breakDurationMinutes ?: 10) / 60).toString(),
                breakMinutesStr = ((initialSchedule?.breakDurationMinutes ?: 10) % 60).toString()
            )
        )
    } else {
        initialSchedule.cycles.mapIndexed { index, cycle ->
            CycleInputState(
                id = System.currentTimeMillis() + index,
                studyHoursStr = (cycle.studyDurationMinutes / 60).toString(),
                studyMinutesStr = (cycle.studyDurationMinutes % 60).toString(),
                breakHoursStr = (cycle.breakDurationMinutes / 60).toString(),
                breakMinutesStr = (cycle.breakDurationMinutes % 60).toString()
            )
        }
    }

    var cycles by remember { mutableStateOf(initialCycles) }
    
    var selectedDays by remember { mutableStateOf(initialSchedule?.weekdays?.toSet() ?: setOf("Mon", "Tue", "Wed", "Thu", "Fri")) }

    val allDays = listOf("Mon" to "M", "Tue" to "T", "Wed" to "W", "Thu" to "T", "Fri" to "F", "Sat" to "S", "Sun" to "S")
    val gradientBrush = Brush.horizontalGradient(listOf(LavenderGradientStart, PinkAccent))

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
            LogoWordmark(subtitle = if (initialSchedule != null) "Edit Schedule" else "Create Schedule")

            Text(
                text = "Cancel",
                style = MaterialTheme.typography.titleMedium,
                color = LavenderPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onCancel() }
            )
        }

        // Form Card
        AppCard {
            SectionEyebrow("SCHEDULE DETAILS")
            Spacer(Modifier.height(10.dp))

            // Schedule Name
            Text("Schedule Name", style = MaterialTheme.typography.labelLarge, color = TextWhite)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
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
                        val initialHour = timeParts.getOrNull(0)?.toIntOrNull() ?: 18
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

        }

        // Cycles
        cycles.forEachIndexed { index, cycleState ->
            AppCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionEyebrow("CYCLE ${index + 1}")
                    if (cycles.size > 1) {
                        Text(
                            text = "Remove",
                            style = MaterialTheme.typography.labelMedium,
                            color = PinkAccent,
                            modifier = Modifier.clickable {
                                cycles = cycles.filter { it.id != cycleState.id }
                            }
                        )
                    }
                }
                
                Spacer(Modifier.height(10.dp))
                
                Text("Study Duration", style = MaterialTheme.typography.labelLarge, color = TextWhite)
                Spacer(Modifier.height(8.dp))
                DurationInputRow(
                    hoursStr = cycleState.studyHoursStr,
                    minutesStr = cycleState.studyMinutesStr,
                    onHoursChange = { newText -> 
                        val filtered = newText.filter { it.isDigit() }.take(2)
                        cycles = cycles.map { if (it.id == cycleState.id) it.copy(studyHoursStr = filtered) else it }
                    },
                    onMinutesChange = { newText -> 
                        val filtered = newText.filter { it.isDigit() }.take(2)
                        cycles = cycles.map { if (it.id == cycleState.id) it.copy(studyMinutesStr = filtered) else it }
                    }
                )

                Spacer(Modifier.height(16.dp))

                Text("Break Duration", style = MaterialTheme.typography.labelLarge, color = TextWhite)
                Spacer(Modifier.height(8.dp))
                DurationInputRow(
                    hoursStr = cycleState.breakHoursStr,
                    minutesStr = cycleState.breakMinutesStr,
                    onHoursChange = { newText -> 
                        val filtered = newText.filter { it.isDigit() }.take(2)
                        cycles = cycles.map { if (it.id == cycleState.id) it.copy(breakHoursStr = filtered) else it }
                    },
                    onMinutesChange = { newText -> 
                        val filtered = newText.filter { it.isDigit() }.take(2)
                        cycles = cycles.map { if (it.id == cycleState.id) it.copy(breakMinutesStr = filtered) else it }
                    }
                )
            }
        }

        // Add Cycle Button
        OutlinedButton(
            onClick = {
                cycles = cycles + CycleInputState(
                    id = System.currentTimeMillis(),
                    studyHoursStr = "0",
                    studyMinutesStr = "45",
                    breakHoursStr = "0",
                    breakMinutesStr = "10"
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            border = BorderStroke(1.dp, LavenderPrimary),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = LavenderPrimary),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("+ Add Cycle", fontWeight = FontWeight.Bold)
        }
        
        AppCard {

            // Weekdays
            Text("Active Days", style = MaterialTheme.typography.labelLarge, color = TextWhite)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allDays.forEach { (dayId, dayLabel) ->
                    val isSelected = selectedDays.contains(dayId)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) LavenderPrimary else NavyCardInner)
                            .clickable {
                                selectedDays = if (isSelected) selectedDays - dayId else selectedDays + dayId
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color(0xFF090C15) else TextDimmed
                        )
                    }
                }
            }
        }

        // Study Mode Extension Notice Card
        AppCard {
            SectionEyebrow("SCHEDULE POLICY")
            Spacer(Modifier.height(6.dp))
            Text(
                text = "5-Minute Extension Option",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "When a study session finishes, you can extend it by 5 minutes. Repeated 5-minute study extensions keep apps blocked during the extra time without shifting future scheduled clock times.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }

        // Save Button
        Button(
            onClick = {
                val processedCycles = cycles.map { cycleState ->
                    val sHours = cycleState.studyHoursStr.toIntOrNull() ?: 0
                    val sMins = cycleState.studyMinutesStr.toIntOrNull() ?: 0
                    val totalStudy = sHours * 60 + sMins
                    val finalStudy = if (totalStudy > 0) totalStudy else 45

                    val bHours = cycleState.breakHoursStr.toIntOrNull() ?: 0
                    val bMins = cycleState.breakMinutesStr.toIntOrNull() ?: 0
                    val totalBreak = bHours * 60 + bMins
                    val finalBreak = if (totalBreak > 0) totalBreak else 10
                    
                    StudyCycle(studyDurationMinutes = finalStudy, breakDurationMinutes = finalBreak)
                }
            
                val updated = StudySchedule(
                    id = initialSchedule?.id ?: System.currentTimeMillis().toString(),
                    name = name,
                    startTime = startTime,
                    studyDurationMinutes = processedCycles.firstOrNull()?.studyDurationMinutes ?: 45,
                    breakDurationMinutes = processedCycles.firstOrNull()?.breakDurationMinutes ?: 10,
                    weekdays = selectedDays.toList(),
                    isEnabled = true,
                    blockedAppCount = initialSchedule?.blockedAppCount ?: 4,
                    cycles = processedCycles
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
            Text("Save Schedule", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }

        if (initialSchedule != null && onDelete != null) {
            OutlinedButton(
                onClick = { onDelete(initialSchedule) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                border = BorderStroke(1.dp, PinkAccent),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PinkAccent),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("🗑 Delete Schedule", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DurationInputRow(
    hoursStr: String,
    minutesStr: String,
    onHoursChange: (String) -> Unit,
    onMinutesChange: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = hoursStr,
            onValueChange = onHoursChange,
            label = { Text("Hours", color = TextDimmed) },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
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
        OutlinedTextField(
            value = minutesStr,
            onValueChange = onMinutesChange,
            label = { Text("Minutes", color = TextDimmed) },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
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
    }
}
