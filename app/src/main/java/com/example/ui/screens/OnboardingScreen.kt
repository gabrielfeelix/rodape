package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Avatar
import com.example.ui.components.TbButton
import com.example.ui.components.TbButtonSize
import com.example.ui.components.TbButtonVariant
import com.example.ui.theme.*

/**
 * Onboarding pos-primeiro-login.
 *
 * Aparece UMA vez por usuario logado neste device (controle via DataStore
 * `onboardedUsersFlow`). 3 passos rapidos:
 *  1. Escolher avatar entre 12 ilustrados
 *  2. Confirmar/editar apelido (vem do JWT como nome completo)
 *  3. Escolher tamanho de fonte preferido (A−, A, A+, A++)
 *
 * Ao final chama [onComplete] com (apelido, avatarUrl, fontScale).
 * O caller persiste no perfil + marca onboarded no DataStore.
 */
@Composable
fun OnboardingScreen(
    initialName: String,
    initialAvatarUrl: String,
    initialFontScale: Float,
    onComplete: (nome: String, avatarUrl: String, fontScale: Float) -> Unit,
) {
    var step by remember { mutableStateOf(0) } // 0=avatar, 1=apelido, 2=fonte
    var avatarUrl by remember { mutableStateOf(initialAvatarUrl.ifBlank { "preset:leitor" }) }
    var apelido by remember { mutableStateOf(initialName) }
    var fontScale by remember { mutableStateOf(initialFontScale) }

    // Fonte única em Avatar.kt (só domínio público — sem risco de IP).
    val presetNames = com.example.ui.components.presetAvatarKeys.map {
        it to com.example.ui.components.presetDisplayName(it)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Spacer(modifier = Modifier.height(36.dp))
            Text(
                text = when (step) {
                    0 -> "Quem é você no clube?"
                    1 -> "Como te chamam?"
                    else -> "Tamanho da letra"
                },
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = LiterataFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                ),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when (step) {
                    0 -> "Escolha o avatar que vai te representar"
                    1 -> "Pode ser o seu primeiro nome ou um apelido"
                    else -> "Vale pro app todo. Dá pra mudar depois nas configurações"
                },
                style = MaterialTheme.typography.bodyMedium.copy(color = Muted),
                textAlign = TextAlign.Center,
            )
        }

        when (step) {
            0 -> item {
                // Preview do avatar selecionado
                Spacer(modifier = Modifier.height(8.dp))
                Avatar(
                    name = apelido.ifBlank { "Você" },
                    avatarUrl = avatarUrl,
                    size = 100.dp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Grid 4 colunas
                val rows = presetNames.chunked(4)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    rows.forEach { rowPresets ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            rowPresets.forEach { (preset, label) ->
                                val isSelected = avatarUrl == preset
                                Box(
                                    modifier = Modifier
                                        .width(60.dp)
                                        .height(76.dp)
                                        .clickable { avatarUrl = preset },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Avatar(
                                        name = label,
                                        avatarUrl = preset,
                                        size = 52.dp,
                                        ring = if (isSelected) Terracota else null,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            1 -> item {
                Spacer(modifier = Modifier.height(24.dp))
                Avatar(
                    name = apelido.ifBlank { "Você" },
                    avatarUrl = avatarUrl,
                    size = 100.dp,
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = apelido,
                    onValueChange = { apelido = it.take(40) },
                    label = { Text("Seu nome ou apelido") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Terracota,
                        focusedLabelColor = Terracota,
                    ),
                )
                Text(
                    text = "${apelido.length}/40",
                    style = MaterialTheme.typography.labelSmall.copy(color = Muted),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            }
            else -> item {
                Spacer(modifier = Modifier.height(24.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = CardSoft,
                    border = BorderStroke(1.dp, Divider),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Como você lê melhor?",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = LiterataFontFamily,
                                color = Ink,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val options = listOf(
                                0.9f to "A−",
                                1.0f to "A",
                                1.15f to "A+",
                                1.30f to "A++",
                            )
                            options.forEach { (scale, label) ->
                                val selected = kotlin.math.abs(fontScale - scale) < 0.05f
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) Terracota else Cream)
                                        .border(
                                            1.dp,
                                            if (selected) Terracota else Divider,
                                            RoundedCornerShape(12.dp),
                                        )
                                        .clickable { fontScale = scale }
                                        .padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = LiterataFontFamily,
                                            fontSize = (18 * scale).sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selected) Cream else Ink,
                                        ),
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Texto de exemplo no tamanho selecionado:",
                            style = MaterialTheme.typography.labelSmall.copy(color = Muted),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Era uma vez uma rosa em outro planeta…",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = LiterataFontFamily,
                                fontSize = (16 * fontScale).sp,
                                color = Ink,
                            ),
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (step > 0) {
                    TbButton(
                        text = "Voltar",
                        onClick = { step-- },
                        variant = TbButtonVariant.Outline,
                        size = TbButtonSize.Lg,
                        modifier = Modifier.weight(1f),
                    )
                }
                val canAdvance = when (step) {
                    1 -> apelido.trim().length >= 2
                    else -> true
                }
                TbButton(
                    text = if (step < 2) "Continuar" else "Pronto!",
                    onClick = {
                        if (!canAdvance) return@TbButton
                        if (step < 2) {
                            step++
                        } else {
                            onComplete(apelido.trim(), avatarUrl, fontScale)
                        }
                    },
                    variant = TbButtonVariant.Terra,
                    size = TbButtonSize.Lg,
                    enabled = canAdvance,
                    modifier = Modifier.weight(if (step > 0) 1f else 1f),
                )
            }
            Spacer(modifier = Modifier.height(36.dp))
        }

        // Indicador de progresso (3 bolinhas)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(3) { i ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (i == step) 10.dp else 8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (i == step) Terracota else Divider),
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
