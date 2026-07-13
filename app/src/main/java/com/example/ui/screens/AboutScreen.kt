package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.components.TbButton
import com.example.ui.components.TbButtonSize
import com.example.ui.components.TbButtonVariant
import com.example.ui.components.RodapeCard
import com.example.ui.theme.*
import com.example.util.openEmailFeedback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sobre o Rodapé") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(RodapeIcons.Back, contentDescription = "Voltar")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 24.dp)
        ) {
            // ── Cabeçalho ──
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(RodapeTheme.colors.terracota),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "R",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontFamily = LiterataFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = RodapeTheme.colors.cream,
                                fontSize = 36.sp
                            )
                        )
                    }
                    Text(
                        text = "Rodapé",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = LiterataFontFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = "Versão ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodySmall.copy(color = RodapeTheme.colors.muted)
                    )
                }
            }

            // ── Sobre ──
            item {
                SectionTitle("SOBRE")
                RodapeCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "O Rodapé é uma ferramenta de organização pra clubes de leitura. " +
                            "Aqui você marca os livros que o grupo tá lendo, organiza encontros, " +
                            "guarda frases marcantes e segue o ritmo de cada um — sem pressão.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = InterFontFamily,
                            color = RodapeTheme.colors.inkSoft,
                            lineHeight = 22.sp
                        )
                    )
                }
            }

            // ── Direitos autorais ──
            item {
                SectionTitle("DIREITOS AUTORAIS")
                RodapeCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "O Rodapé não hospeda nem distribui livros. As capas vêm de APIs " +
                            "públicas (Open Library e Google Books) e os links adicionados pelos " +
                            "administradores apontam pra fontes externas — confira sempre se você " +
                            "tem o direito de acessá-las.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = InterFontFamily,
                            color = RodapeTheme.colors.inkSoft,
                            lineHeight = 22.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Pra denunciar conteúdo inadequado ou pedir remoção, use o botão de feedback.",
                        style = MaterialTheme.typography.bodySmall.copy(color = RodapeTheme.colors.muted)
                    )
                }
            }

            // ── Termos de uso ──
            item {
                SectionTitle("TERMOS DE USO")
                RodapeCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Usando o Rodapé, você concorda em respeitar direitos autorais e " +
                            "usar a ferramenta de boa-fé com seu clube. O app é fornecido como está, " +
                            "sem garantias. Estamos em fase inicial — quebra de funcionalidade pode " +
                            "acontecer.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = InterFontFamily,
                            color = RodapeTheme.colors.inkSoft,
                            lineHeight = 22.sp
                        )
                    )
                }
            }

            // ── Privacidade ──
            item {
                SectionTitle("PRIVACIDADE")
                RodapeCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "O conteúdo do clube (comentários, frases, votos e listas de " +
                            "leitura) fica num servidor privado (Supabase) pra sincronizar entre " +
                            "os membros, e é visível só pra quem faz parte do clube. Não há " +
                            "publicidade nem rastreamento de terceiros. Você pode pedir a exclusão " +
                            "da sua conta e dos seus dados a qualquer momento. Detalhes na política " +
                            "de privacidade completa.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = InterFontFamily,
                            color = RodapeTheme.colors.inkSoft,
                            lineHeight = 22.sp
                        )
                    )
                }
            }

            // ── Bibliotecas ──
            item {
                SectionTitle("FEITO COM")
                RodapeCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            "Jetpack Compose & Material 3" to "Apache 2.0",
                            "Room (SQLite)" to "Apache 2.0",
                            "Retrofit + Moshi" to "Apache 2.0",
                            "Coil" to "Apache 2.0",
                            "CameraX" to "Apache 2.0",
                            "Open Library API" to "domínio público",
                            "Google Books API" to "Google Terms",
                            "Literata & Inter (Google Fonts)" to "SIL Open Font License"
                        ).forEach { (lib, license) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = lib,
                                    style = MaterialTheme.typography.bodySmall.copy(color = RodapeTheme.colors.inkSoft)
                                )
                                Text(
                                    text = license,
                                    style = MaterialTheme.typography.labelSmall.copy(color = RodapeTheme.colors.muted)
                                )
                            }
                        }
                    }
                }
            }

            // ── Feedback ──
            item {
                Spacer(modifier = Modifier.height(8.dp))
                TbButton(
                    text = "Mandar feedback",
                    onClick = { openEmailFeedback(context) },
                    variant = TbButtonVariant.Outline,
                    size = TbButtonSize.Md,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text(
                    text = "Feito com 💚 pra clubes de leitura.",
                    style = MaterialTheme.typography.labelSmall.copy(color = RodapeTheme.colors.muted),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            color = RodapeTheme.colors.muted,
            letterSpacing = 1.sp
        ),
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}
