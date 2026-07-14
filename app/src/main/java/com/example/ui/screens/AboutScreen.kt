package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import com.example.ui.components.Overline
import com.example.ui.components.TbButton
import com.example.ui.components.TbButtonSize
import com.example.ui.components.TbButtonVariant
import com.example.ui.components.RodapeCard
import com.example.ui.theme.*
import com.example.util.URL_PRIVACIDADE
import com.example.util.URL_TERMOS
import com.example.util.openBugReport
import com.example.util.openEmailFeedback
import com.example.util.openUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            // 3.13: app bar unificada com as irmãs — título na escala (Literata
            // via titleLarge) e back terracota.
            TopAppBar(
                title = { Text("Sobre o Rodapé", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(RodapeIcons.Back, contentDescription = "Voltar", tint = RodapeTheme.colors.terracota)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
                        text = "Pra denunciar conteúdo inadequado ou bloquear alguém, use o menu (⋮) " +
                            "no próprio conteúdo ou no membro. Admins veem as denúncias em " +
                            "Gerenciar clube › Denúncias pendentes.",
                        style = MaterialTheme.typography.bodySmall.copy(color = RodapeTheme.colors.muted)
                    )
                }
            }

            // ── Termos de uso ──
            item {
                SectionTitle("TERMOS DE USO")
                RodapeCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Usando o Rodapé, você concorda em respeitar direitos autorais e " +
                                "usar a ferramenta de boa-fé com seu clube, sem publicar conteúdo " +
                                "abusivo, ilegal ou ofensivo. O app é fornecido como está, sem " +
                                "garantias. Estamos em fase inicial — quebra de funcionalidade pode " +
                                "acontecer.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = InterFontFamily,
                                color = RodapeTheme.colors.inkSoft,
                                lineHeight = 22.sp
                            )
                        )
                        TbButton(
                            text = "Ler os termos completos",
                            onClick = { openUrl(context, URL_TERMOS) },
                            variant = TbButtonVariant.Outline,
                            size = TbButtonSize.Sm,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // ── Privacidade ──
            item {
                SectionTitle("PRIVACIDADE")
                RodapeCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "O conteúdo do clube (comentários, frases, votos e listas de " +
                                "leitura) fica num servidor privado (Supabase) pra sincronizar entre " +
                                "os membros, e é visível só pra quem faz parte do clube. Não há " +
                                "publicidade nem rastreamento de terceiros. Você pode excluir " +
                                "sua conta e seus dados a qualquer momento. Detalhes na política " +
                                "de privacidade completa.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = InterFontFamily,
                                color = RodapeTheme.colors.inkSoft,
                                lineHeight = 22.sp
                            )
                        )
                        TbButton(
                            text = "Ler a política de privacidade",
                            onClick = { openUrl(context, URL_PRIVACIDADE) },
                            variant = TbButtonVariant.Outline,
                            size = TbButtonSize.Sm,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
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
                // 3.13: feedback é convite, não ação neutra — oliva-soft acolhe.
                TbButton(
                    text = "Mandar feedback",
                    onClick = { openEmailFeedback(context) },
                    variant = TbButtonVariant.OlivaSoft,
                    size = TbButtonSize.Md,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Reportar bug: já anexa device + último erro registrado no corpo.
                TbButton(
                    text = "Reportar um problema",
                    onClick = { openBugReport(context) },
                    variant = TbButtonVariant.Outline,
                    size = TbButtonSize.Sm,
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
    Overline(
        text = text,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}
