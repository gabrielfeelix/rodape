package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LiterataFontFamily
import com.example.ui.theme.RodapeTheme

/**
 * Estado vazio ÚNICO (Onda 1 do PLANO-UI). Antes cada tela inventava o seu
 * (ícones 36/48/64, layouts diferentes). Ícone tingido em disco olivaSoft, título
 * Literata, descrição muted centrada, ação opcional. Passe [illustration] pra uma
 * ilustração de marca no lugar do ícone (ex.: prateleira vazia).
 */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    illustration: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            illustration != null -> illustration()
            icon != null -> Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(RodapeTheme.colors.olivaSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = RodapeTheme.colors.olivaDark,
                    modifier = Modifier.size(32.dp),
                )
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = LiterataFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = RodapeTheme.colors.ink,
            ),
            textAlign = TextAlign.Center,
        )

        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = RodapeTheme.colors.muted,
                ),
                textAlign = TextAlign.Center,
            )
        }

        if (action != null) {
            Spacer(Modifier.height(4.dp))
            action()
        }
    }
}
