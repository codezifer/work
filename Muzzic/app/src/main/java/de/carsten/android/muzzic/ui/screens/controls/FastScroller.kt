package de.carsten.android.muzzic.ui.screens.controls

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FastScroller(
    alphabet: List<String>,
    onLetterSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    activeLetter: String? = null,
) {
    var draggingLetter by remember { mutableStateOf<String?>(null) }
    var columnHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    val effectiveLetter = draggingLetter ?: activeLetter
    val letterIndex = alphabet.indexOf(effectiveLetter).coerceAtLeast(-1)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(40.dp)
            .onGloballyPositioned { columnHeight = it.size.height }
            .pointerInput(alphabet) {
                detectTapGestures { offset ->
                    val index = (offset.y / columnHeight * alphabet.size).toInt().coerceIn(0, alphabet.size - 1)
                    val letter = alphabet[index]
                    onLetterSelected(letter)
                }
            }
            .pointerInput(alphabet) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val index = (offset.y / columnHeight * alphabet.size).toInt().coerceIn(0, alphabet.size - 1)
                        val letter = alphabet[index]
                        draggingLetter = letter
                        onLetterSelected(letter)
                    },
                    onDragEnd = { draggingLetter = null },
                    onDragCancel = { draggingLetter = null },
                    onDrag = { change, _ ->
                        val index = (change.position.y / columnHeight * alphabet.size).toInt().coerceIn(0, alphabet.size - 1)
                        val letter = alphabet[index]
                        if (draggingLetter != letter) {
                            draggingLetter = letter
                            onLetterSelected(letter)
                        }
                    }
                )
            },
        contentAlignment = Alignment.TopCenter
    ) {
        // Track handle (the colored marker)
        if (letterIndex != -1 && alphabet.isNotEmpty() && columnHeight > 0) {
            val itemHeight = columnHeight.toFloat() / alphabet.size
            val handleOffsetPx = (letterIndex * itemHeight) + (itemHeight / 2)
            val handleOffsetDp = with(density) { handleOffsetPx.toDp() }

            val animatedOffset by animateDpAsState(
                targetValue = handleOffsetDp - 12.dp,
                label = "handleOffset"
            )

            Box(
                modifier = Modifier
                    .offset(y = animatedOffset)
                    .padding(horizontal = 8.dp)
                    .width(24.dp)
                    .height(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxHeight()
        ) {
            alphabet.forEach { letter ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .width(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = letter,
                        fontSize = 10.sp,
                        fontWeight = if (effectiveLetter == letter) FontWeight.Bold else FontWeight.Normal,
                        color = if (effectiveLetter == letter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        draggingLetter?.let { letter ->
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-60).dp)
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letter,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
