package com.example.focuscontrol.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.focuscontrol.ui.theme.NavyCardInner
import com.example.focuscontrol.ui.theme.PinkAccent
import com.example.focuscontrol.ui.theme.PurpleAccent
import com.example.focuscontrol.ui.theme.TextDimmed
import com.example.focuscontrol.ui.theme.TextMuted
import com.example.focuscontrol.ui.theme.TextWhite

@Composable
fun ScheduleScreen(
    schedules: List<StudySchedule>,
    onAddSchedule: () -> Unit,
    onEditSchedule: (StudySchedule) -> Unit,
    onToggleSchedule: (StudySchedule, Boolean) -> Unit,
    onBack: () -> Unit
) {
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
            Text(
                text = "Back",
                style = MaterialTheme.typography.titleMedium,
                color = LavenderPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onBack() }.padding(end = 12.dp)
            )

            Box(modifier = Modifier.weight(1f)) {
                LogoWordmark(subtitle = "Study Mode • Automated Schedules")
            }

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = onAddSchedule,
                modifier = Modifier
                    .height(40.dp)
                    .background(gradientBrush, RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = TextWhite
                ),
                contentPadding = PaddingValues(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("➕ Add", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            }
        }

        // Info Banner
        AppCard {
            SectionEyebrow("STUDY MODE • SCHEDULE AUTOMATION")
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Automated Study Timers",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Configure custom study and break intervals. When study sessions finish, you can extend by 5 minutes without shifting future scheduled clock times.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }

        // Schedules List
        SectionEyebrow("YOUR STUDY SCHEDULES (${schedules.size})")

        Spacer(Modifier.height(4.dp))
        
        schedules.forEach { schedule ->
            ScheduleCardItem(
                schedule = schedule,
                onToggle = { enabled ->
                    onToggleSchedule(schedule, enabled)
                },
                onClick = { onEditSchedule(schedule) }
            )
        }
    }
}

@Composable
private fun ScheduleCardItem(
    schedule: StudySchedule,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val allDays = listOf("Mon" to "M", "Tue" to "T", "Wed" to "W", "Thu" to "T", "Fri" to "F", "Sat" to "S", "Sun" to "S")

    AppCard(
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(LavenderPrimary.copy(alpha = 0.18f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${schedule.studyDurationMinutes}m Study • ${schedule.breakDurationMinutes}m Break",
                            style = MaterialTheme.typography.labelMedium,
                            color = LavenderPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = schedule.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Starts at ${schedule.startTime} • ${schedule.blockedAppCount} Apps Restricted",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }

            Switch(
                checked = schedule.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TextWhite,
                    checkedTrackColor = PurpleAccent,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = NavyCardInner
                )
            )
        }

        Spacer(Modifier.height(14.dp))

        // Weekdays Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            allDays.forEach { (dayId, dayLabel) ->
                val isSelected = schedule.weekdays.contains(dayId)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) LavenderPrimary else NavyCardInner
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color(0xFF090C15) else TextDimmed
                    )
                }
            }
        }
    }
}
