package de.carsten.android.muzzic.ui.screens.cards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.carsten.android.muzzic.ui.theme.AppTheme

@Composable
fun MuzzicCard(
    modifier: Modifier = Modifier,
    header: @Composable (() -> Unit)? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    onClick: (() -> Unit)? = null,
    content: @Composable (contentColor: Color) -> Unit,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.Companion.clickable(onClick = onClick) else Modifier.Companion),
        colors =
            CardDefaults.cardColors(
                containerColor = backgroundColor,
                contentColor = contentColor,
            ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.Companion.fillMaxWidth()) {
            header?.let {
                Box(modifier = Modifier.Companion.fillMaxWidth()) { it() }
            }
            Column(
                modifier =
                    Modifier.Companion
                        .padding(16.dp)
                        .fillMaxWidth(),
                horizontalAlignment = Alignment.Companion.CenterHorizontally,
            ) {
                content(contentColor)
            }
        }
    }
}
