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
import androidx.compose.ui.graphics.graphicsLayer
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
/** Tipo da spot illustration Compose-drawn (3.10) — geometria pura de marca. */
private enum class IntroArtKind { Book, Shelf, Chat, Calendar }

private data class IntroPage(
    val art: IntroArtKind,
    val accent: Color,
    val accentSoft: Color,
    val title: String,
    val body: String,
)

@Composable
fun IntroScreen(onFinished: () -> Unit) {
    val terracotaColor = RodapeTheme.colors.terracota
    val terracotaSoftColor = RodapeTheme.colors.terracotaSoft
    val olivaColor = RodapeTheme.colors.oliva
    val olivaSoftColor = RodapeTheme.colors.olivaSoft
    val pages = remember {
        listOf(
            IntroPage(
                art = IntroArtKind.Book,
                accent = terracotaColor,
                accentSoft = terracotaSoftColor,
                title = "Um clube de leitura só de vocês",
                body = "Reúna a galera num clube privado. Sugiram livros e votem juntos no próximo — só entra quem vocês convidam.",
            ),
            IntroPage(
                art = IntroArtKind.Shelf,
                accent = olivaColor,
                accentSoft = olivaSoftColor,
                title = "Leiam no ritmo do grupo",
                body = "Marque seu progresso capítulo a capítulo e veja onde todo mundo está. Sem cobrança — no ritmo de vocês.",
            ),
            IntroPage(
                art = IntroArtKind.Chat,
                accent = terracotaColor,
                accentSoft = terracotaSoftColor,
                title = "Conversem sem estragar a surpresa",
                body = "Comente cada capítulo com barreira de spoiler: quem ainda não chegou lá não vê o que vem pela frente.",
            ),
            IntroPage(
                art = IntroArtKind.Calendar,
                accent = olivaColor,
                accentSoft = olivaSoftColor,
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
            .background(RodapeTheme.colors.paper)
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
                    color = RodapeTheme.colors.ink,
                ),
            )
            TextButton(onClick = onFinished) {
                Text(
                    text = "Pular",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        color = RodapeTheme.colors.muted,
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
                // Parallax (3.10): a arte anda mais rápido que o texto durante o
                // swipe (offset da página dirige translação/escala/alpha) —
                // profundidade sem custo. reduceMotion: parada.
                val introReduce = reduceMotion()
                IntroArt(
                    kind = p.art,
                    accent = p.accent,
                    accentSoft = p.accentSoft,
                    modifier = if (introReduce) Modifier else Modifier.graphicsLayer {
                        val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        translationX = -pageOffset * 56.dp.toPx()
                        val f = 1f - kotlin.math.abs(pageOffset).coerceIn(0f, 1f)
                        alpha = 0.4f + 0.6f * f
                        scaleX = 0.92f + 0.08f * f
                        scaleY = 0.92f + 0.08f * f
                    },
                )
                Spacer(Modifier.height(40.dp))
                Text(
                    text = p.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = LiterataFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = RodapeTheme.colors.ink,
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
                        color = RodapeTheme.colors.muted,
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
                val color by animateColorAsState(if (active) RodapeTheme.colors.terracota else RodapeTheme.colors.divider, label = "dotC")
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

/**
 * Ilustração composta (3.10): disco suave + spot illustration Compose-drawn em
 * GEOMETRIA pura de marca (rounded rects, círculos, traços — nada de path
 * arriscado) + pingos de profundidade. Fim do emoji-como-arte.
 */
@Composable
private fun IntroArt(
    kind: IntroArtKind,
    accent: Color,
    accentSoft: Color,
    modifier: Modifier = Modifier,
) {
    val ink = RodapeTheme.colors.ink
    val cream = RodapeTheme.colors.cream
    Box(
        modifier = modifier.size(200.dp),
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
        // disco principal + arte (decorativa: o título carrega o significado)
        Box(
            modifier = Modifier
                .size(148.dp)
                .clip(CircleShape)
                .background(accentSoft)
                .semantics { contentDescription = "" },
            contentAlignment = Alignment.Center,
        ) {
            when (kind) {
                IntroArtKind.Book -> {
                    // Livro aberto: duas páginas cream com linhas de texto,
                    // lombada no acento.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BookPageArt(cream = cream, line = accent.copy(alpha = 0.45f), flip = false)
                        Box(
                            Modifier
                                .width(4.dp)
                                .height(58.dp)
                                .background(accent)
                        )
                        BookPageArt(cream = cream, line = accent.copy(alpha = 0.45f), flip = true)
                    }
                }
                IntroArtKind.Shelf -> {
                    // Prateleira: lombadas em alturas variadas + linha, com uma
                    // lombada no acento (a leitura atual do grupo).
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Box(Modifier.size(13.dp, 36.dp).clip(RoundedCornerShape(3.dp)).background(ink.copy(alpha = 0.25f)))
                            Box(Modifier.size(15.dp, 52.dp).clip(RoundedCornerShape(3.dp)).background(accent))
                            Box(Modifier.size(12.dp, 44.dp).clip(RoundedCornerShape(3.dp)).background(cream))
                            Box(Modifier.size(14.dp, 32.dp).clip(RoundedCornerShape(3.dp)).background(ink.copy(alpha = 0.35f)))
                        }
                        Spacer(Modifier.height(3.dp))
                        Box(
                            Modifier
                                .width(84.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(ink.copy(alpha = 0.30f))
                        )
                    }
                }
                IntroArtKind.Chat -> {
                    // Conversa: bolha grande cream com traços + bolha-resposta
                    // no acento, deslocada — diálogo à primeira vista.
                    Box {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp, 14.dp, 14.dp, 4.dp))
                                .background(cream)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                        ) {
                            Box(Modifier.size(52.dp, 5.dp).clip(RoundedCornerShape(999.dp)).background(ink.copy(alpha = 0.30f)))
                            Box(Modifier.size(36.dp, 5.dp).clip(RoundedCornerShape(999.dp)).background(ink.copy(alpha = 0.20f)))
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 22.dp, y = 26.dp)
                                .clip(RoundedCornerShape(14.dp, 14.dp, 4.dp, 14.dp))
                                .background(accent)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            Box(Modifier.size(30.dp, 5.dp).clip(RoundedCornerShape(999.dp)).background(cream.copy(alpha = 0.85f)))
                        }
                    }
                }
                IntroArtKind.Calendar -> {
                    // Calendário: cabeçalho no acento + grade de dias com o dia
                    // do encontro destacado.
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(cream),
                    ) {
                        Box(
                            Modifier
                                .width(84.dp)
                                .height(18.dp)
                                .background(accent)
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                            modifier = Modifier.padding(12.dp),
                        ) {
                            repeat(3) { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                    repeat(4) { col ->
                                        val isMeetingDay = row == 1 && col == 2
                                        Box(
                                            Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isMeetingDay) accent
                                                    else ink.copy(alpha = 0.18f)
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Página do livro aberto da IntroArt: retângulo cream com 3 traços de texto. */
@Composable
private fun BookPageArt(cream: Color, line: Color, flip: Boolean) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = if (flip) Alignment.Start else Alignment.End,
        modifier = Modifier
            .clip(
                if (flip) RoundedCornerShape(topStart = 2.dp, topEnd = 10.dp, bottomEnd = 10.dp, bottomStart = 2.dp)
                else RoundedCornerShape(topStart = 10.dp, topEnd = 2.dp, bottomEnd = 2.dp, bottomStart = 10.dp)
            )
            .background(cream)
            .padding(horizontal = 9.dp, vertical = 11.dp)
            .width(30.dp),
    ) {
        Box(Modifier.size(28.dp, 4.dp).clip(RoundedCornerShape(999.dp)).background(line))
        Box(Modifier.size(22.dp, 4.dp).clip(RoundedCornerShape(999.dp)).background(line.copy(alpha = 0.6f)))
        Box(Modifier.size(25.dp, 4.dp).clip(RoundedCornerShape(999.dp)).background(line))
    }
}
