package com.example.focuscontrol.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.focuscontrol.R
import com.example.focuscontrol.ui.theme.DigitalTimerLargeStyle
import com.example.focuscontrol.ui.theme.LavenderPrimary
import com.example.focuscontrol.ui.theme.LogoPixelTextStyle
import com.example.focuscontrol.ui.theme.NavyCard
import com.example.focuscontrol.ui.theme.NavyCardBorder
import com.example.focuscontrol.ui.theme.TextWhite

@Composable
fun LogoWordmark(
    modifier: Modifier = Modifier,
    subtitle: String = "Deep Focus Assistant"
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.freres_logo),
            contentDescription = "FRÈRES Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .padding(end = 12.dp)
                .size(40.dp)
                .clip(CircleShape)
        )
        Column(
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "FRÈRES",
                style = LogoPixelTextStyle,
                color = TextWhite
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = LavenderPrimary
            )
        }
    }
}

@Composable
fun DigitalTimerDisplay(
    timeText: String,
    modifier: Modifier = Modifier,
    label: String = "REMAINING TIME"
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionEyebrow(label)
        Spacer(Modifier.height(4.dp))
        Text(
            text = timeText,
            style = DigitalTimerLargeStyle,
            color = TextWhite,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    borderBrush: Brush = Brush.linearGradient(listOf(NavyCardBorder, NavyCardBorder)),
    backgroundColor: Color = NavyCard,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = backgroundColor,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, borderBrush)
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

@Composable
fun SectionEyebrow(text: String, modifier: Modifier = Modifier) = Text(
    text = text.uppercase(),
    modifier = modifier,
    style = MaterialTheme.typography.labelSmall,
    fontWeight = FontWeight.Bold,
    color = LavenderPrimary,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis
)
