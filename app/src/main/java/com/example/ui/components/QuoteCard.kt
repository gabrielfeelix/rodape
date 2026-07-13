package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Divider
import com.example.ui.theme.LiterataFontFamily
import com.example.ui.theme.RodapeTheme

@Composable
fun QuoteCard(
    texto: String,
    ref: String,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = RodapeTheme.colors.cardSurface),
        border = BorderStroke(0.5.dp, RodapeTheme.colors.divider),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Box(modifier = Modifier.padding(top = 0.dp)) {
            // Decorative opening quote mark
            Text(
                text = "“",
                fontFamily = LiterataFontFamily,
                fontSize = 72.sp,
                color = RodapeTheme.colors.olivaSoft,
                lineHeight = 72.sp,
                modifier = Modifier
                    .padding(start = 12.dp, top = 0.dp)
                    .align(Alignment.TopStart),
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                // Quote text — offset to overlap with decorative mark
                Text(
                    text = texto,
                    fontFamily = LiterataFontFamily,
                    fontStyle = FontStyle.Italic,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = RodapeTheme.colors.inkSoft,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 40.dp, bottom = 12.dp),
                )

                // Divider + footer row
                HorizontalDivider(
                    color = RodapeTheme.colors.dividerSoft,
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = ref,
                        style = MaterialTheme.typography.bodySmall,
                        color = RodapeTheme.colors.muted,
                        modifier = Modifier.weight(1f),
                    )
                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            // Alvo de toque de 48dp (ícone visual continua 18dp)
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Apagar frase",
                                tint = RodapeTheme.colors.muted,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
