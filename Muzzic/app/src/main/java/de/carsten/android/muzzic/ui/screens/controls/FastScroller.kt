package de.carsten.android.muzzic.ui.screens.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FastScroller(
    alphabet: List<String>,
    onLetterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedLetter by remember { mutableStateOf<String?>(null) }
    var columnHeight by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(40.dp)
            .onGloballyPositioned { columnHeight = it.size.height }
            .pointerInput(alphabet) {
                detectTapGestures { offset ->
                    val index = (offset.y / columnHeight * alphabet.size).toInt().coerceIn(0, alphabet.size - 1)
                    val letter = alphabet[index]
                    selectedLetter = letter
                    onLetterSelected(letter)
                }
            }
            .pointerInput(alphabet) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val index = (offset.y / columnHeight * alphabet.size).toInt().coerceIn(0, alphabet.size - 1)
                        val letter = alphabet[index]
                        selectedLetter = letter
                        onLetterSelected(letter)
                    },
                    onDragEnd = { selectedLetter = null },
                    onDragCancel = { selectedLetter = null },
                    onDrag = { change, _ ->
                        val index = (change.position.y / columnHeight * alphabet.size).toInt().coerceIn(0, alphabet.size - 1)
                        val letter = alphabet[index]
                        if (selectedLetter != letter) {
                            selectedLetter = letter
                            onLetterSelected(letter)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            alphabet.forEach { letter ->
                Text(
                    text = letter,
                    fontSize = 10.sp,
                    fontWeight = if (selectedLetter == letter) FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedLetter == letter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }

        selectedLetter?.let { letter ->
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
