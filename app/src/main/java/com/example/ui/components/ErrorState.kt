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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LiterataFontFamily
import com.example.ui.theme.RodapeIcons
import com.example.ui.theme.RodapeTheme

/**
 * Estado de ERRO/OFFLINE ÚNICO (Fase 3 · 4.3). Espelha o [EmptyState] mas com a
 * carga semântica do erro: disco terracota + ícone de alerta, e — o ponto do
 * padrão — um retry CONSISTENTE. Antes só o Suggest tinha "Tentar de novo" (feito
 * à mão); qualquer superfície que falhe (busca, sync, carregamento) deve reusar
 * este componente pra que o gesto de recuperação seja sempre o mesmo.
 *
 * `onRetry` desenha o botão padrão "Tentar de novo" (Outline). `action` é um slot
 * extra pra ações secundárias (ex.: "Cadastrar manualmente") abaixo do retry.
 */
@Composable
fun ErrorState(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    onRetry: (() -> Unit)? = null,
    retryLabel: String = "Tentar de novo",
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(RodapeTheme.colors.terracotaSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = RodapeIcons.Warning,
                contentDescription = null,
                tint = RodapeTheme.colors.terracota,
                modifier = Modifier.size(32.dp),
            )
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

        if (onRetry != null) {
            Spacer(Modifier.height(4.dp))
            TbButton(
                text = retryLabel,
                onClick = onRetry,
                variant = TbButtonVariant.Outline,
                modifier = Modifier.fillMaxWidth(0.8f),
            )
        }

        if (action != null) {
            action()
        }
    }
}
