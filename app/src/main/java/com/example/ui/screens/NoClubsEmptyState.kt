package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.TbButton
import com.example.ui.components.TbButtonSize
import com.example.ui.components.TbButtonVariant
import com.example.ui.theme.Oliva
import com.example.ui.theme.RodapeTheme
import com.example.ui.theme.Terracota

/**
 * Tela mostrada DEPOIS do login quando o usuario nao e membro de nenhum clube
 * ainda. Substitui as 5 tabs (que dependem de um clube ativo) por uma boas-vindas
 * com 2 CTAs claros.
 *
 * Logout fica disponivel discretamente embaixo — o user pode trocar de conta
 * sem ter que primeiro criar um clube.
 */
@Composable
fun NoClubsEmptyState(
    userFirstName: String?,
    onCreateClub: () -> Unit,
    onJoinClub: () -> Unit,
    onSignOut: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Topo: logo + saudacao
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo),
                    contentDescription = "Logo do Rodapé",
                    modifier = Modifier.size(88.dp),
                )

                Spacer(modifier = Modifier.height(24.dp))

                val olivaColor = RodapeTheme.colors.oliva
                val terracotaColor = RodapeTheme.colors.terracota
                Text(
                    text = buildAnnotatedString {
                        append("Oi")
                        userFirstName?.takeIf { it.isNotBlank() }?.let { name ->
                            append(", ")
                            withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = olivaColor)) {
                                append(name)
                            }
                        }
                        withStyle(SpanStyle(color = terracotaColor)) { append(".") }
                    },
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Você ainda não está em nenhum clube.\nQuer começar um, ou entrar num que já existe?",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 320.dp),
                )
            }

            // Meio: CTAs
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TbButton(
                    text = "Criar um clube",
                    onClick = onCreateClub,
                    variant = TbButtonVariant.Terra,
                    size = TbButtonSize.Lg,
                    modifier = Modifier.fillMaxWidth(),
                )

                TbButton(
                    text = "Entrar com código",
                    onClick = onJoinClub,
                    variant = TbButtonVariant.Outline,
                    size = TbButtonSize.Lg,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Fundo: opcao discreta de sair
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.TextButton(onClick = onSignOut) {
                    Text(
                        "Sair",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        }
    }
}
