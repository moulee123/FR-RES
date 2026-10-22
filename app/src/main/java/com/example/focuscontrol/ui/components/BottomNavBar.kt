package com.example.focuscontrol.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focuscontrol.ui.navigation.MainNavDestination
import com.example.focuscontrol.ui.theme.LavenderPrimary
import com.example.focuscontrol.ui.theme.LavenderSecondary
import com.example.focuscontrol.ui.theme.NavyCard
import com.example.focuscontrol.ui.theme.NavyCardBorder
import com.example.focuscontrol.ui.theme.TextMuted

@Composable
fun FocusBottomNavBar(
    currentDestination: MainNavDestination,
    onNavigateTo: (MainNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = NavyCard,
        border = BorderStroke(1.dp, NavyCardBorder),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainNavDestination.entries.forEach { destination ->
                val isSelected = currentDestination == destination

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onNavigateTo(destination) }
                        .padding(vertical = 2.dp, horizontal = 1.dp)
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(LavenderSecondary)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = destination.iconEmoji,
                                fontSize = 19.sp
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier.padding(vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = destination.iconEmoji,
                                fontSize = 19.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = destination.label,
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            letterSpacing = (-0.2).sp
                        ),
                        color = if (isSelected) LavenderPrimary else TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        softWrap = false,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
