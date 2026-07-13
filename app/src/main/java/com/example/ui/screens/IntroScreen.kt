package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.TbButton
import com.example.ui.components.TbButtonSize
import com.example.ui.components.TbButtonVariant
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Intro de primeiro uso — antes de pedir conta.
 *
 * Explica o Rodapé em 4 telas (clube privado, ler no ritmo do grupo, discutir
 * sem spoiler, encontros de verdade). Pulável a qualquer momento. Aparece UMA
 * vez por device (DataStore `intro_seen`); ao terminar/pular chama [onFinished],
 * que marca visto e revela o Welcome.
 *
 * Sem assets externos de propósito (CSP do design system): as ilustrações são
 * compostas com formas + emoji nas cores da marca, fiéis ao claude-design.
 */
private data class IntroPage(
    val emoji: String,
    val accent: Color,
    val accentSoft: Color,
    val title: String,
    val body: String,
)

@Composable
fun IntroScreen(onFinished: () -> Unit) {
    val pages = remember {
        listOf(
            IntroPage(
                emoji = "📖",
                accent = Terracota,
                accentSoft = TerracotaSoft,
                title = "Um clube de leitura só de vocês",
                body = "Reúna a galera num clube privado. Sugiram livros e votem juntos no próximo — só entra quem vocês convidam.",
            ),
            IntroPage(
                emoji = "📚",
                accent = Oliva,
                accentSoft = OlivaSoft,
                title = "Leiam no ritmo do grupo",
                body = "Marque seu progresso capítulo a capítulo e veja onde todo mundo está. Sem cobrança — no ritmo de vocês.",
            ),
            IntroPage(
                emoji = "💬",
                accent = Terracota,
                accentSoft = TerracotaSoft,
                title = "Conversem sem estragar a surpresa",
                body = "Comente cada capítulo com barreira de spoiler: quem ainda não chegou lá não vê o que vem pela frente.",
            ),
            IntroPage(
                emoji = "📅",
                accent = Oliva,
                accentSoft = OlivaSoft,
                title = "Encontros de verdade",
                body = "Marquem a data, confirmem presença e guardem as frases que marcaram vocês. O clube acontece no mundo real.",
            ),
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex

    // Back físico: recua uma página; na primeira, deixa o sistema tratar.
    BackHandler(enabled = pagerState.currentPage > 0) {
        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 24.dp),
    ) {
        // Topo: wordmark discreto + "Pular"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Rodapé",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = LiterataFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                ),
            )
            TextButton(onClick = onFinished) {
                Text(
                    text = "Pular",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        color = Muted,
                    ),
                )
            }
        }

        // Páginas
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { page ->
            val p = pages[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                IntroArt(emoji = p.emoji, accent = p.accent, accentSoft = p.accentSoft)
                Spacer(Modifier.height(40.dp))
                Text(
                    text = p.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = LiterataFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink,
                        lineHeight = 34.sp,
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = p.body,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = InterFontFamily,
                        color = Muted,
                        lineHeight = 24.sp,
                    ),
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Indicador de páginas (barrinhas — a atual estica em terracota)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(pages.size) { i ->
                val active = i == pagerState.currentPage
                val width by animateDpAsState(if (active) 22.dp else 8.dp, label = "dotW")
                val color by animateColorAsState(if (active) Terracota else Divider, label = "dotC")
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(8.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(color),
                )
            }
        }

        // CTA
        TbButton(
            text = if (isLast) "Começar" else "Próximo",
            onClick = {
                if (isLast) onFinished()
                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            variant = TbButtonVariant.Terra,
            size = TbButtonSize.Lg,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
    }
}

/** Ilustração composta: disco suave da cor de destaque + emoji + pingos de profundidade. */
@Composable
private fun IntroArt(emoji: String, accent: Color, accentSoft: Color) {
    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        // pingo maior atrás, deslocado
        Box(
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.14f)),
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .align(Alignment.BottomStart)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.22f)),
        )
        // disco principal
        Box(
            modifier = Modifier
                .size(148.dp)
                .clip(CircleShape)
                .background(accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = emoji,
                fontSize = 72.sp,
                // decorativo: o título ao lado já carrega o significado pro leitor de tela
                modifier = Modifier.semantics { contentDescription = "" },
            )
        }
    }
}
