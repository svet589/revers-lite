package com.revers.messenger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.revers.messenger.database.entities.MessageEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MessageBubble(
    message: MessageEntity,
    isOutgoing: Boolean
) {
    val bubbleColor = if (isOutgoing) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.secondary
    }

    val textColor = if (isOutgoing) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSecondary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isOutgoing) 16.dp else 4.dp,
                    bottomEnd = if (isOutgoing) 4.dp else 16.dp
                ))
                .background(bubbleColor)
                .padding(12.dp)
        ) {
            Text(
                text = message.ciphertext,
                color = textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal
            )

            Text(
                text = formatTimestamp(message.timestamp),
                color = if (isOutgoing) Color.White.copy(alpha = 0.6f)
                else MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.6f),
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 4.dp)
            )
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val format = if (isToday(date)) {
        SimpleDateFormat("HH:mm", Locale.getDefault())
    } else {
        SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault())
    }
    return format.format(date)
}

fun isToday(date: Date): Boolean {
    val today = Calendar.getInstance()
    val other = Calendar.getInstance().apply { time = date }
    return today.get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
}
