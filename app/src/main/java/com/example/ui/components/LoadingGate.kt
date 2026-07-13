package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Oliva
import com.example.ui.theme.RodapeTheme
import kotlinx.coroutines.delay

/**
 * O app é local-first e o Room emite lista vazia imediatamente no cold start —
 * antes do primeiro sync chegar. Sem um sinal explícito de loading, telas
 * mostravam "não encontrado"/empty state por alguns segundos e depois "pulavam"
 * pro conteúdo real.
 *
 * Este gate segura um estado de loading enquanto (a) não há dado E (b) ainda
 * estamos dentro da janela de graça do primeiro sync. Se o dado chegar, o
 * conteúdo aparece na hora; se realmente não existir nada, o empty state real
 * aparece após a janela.
 */
@Composable
fun rememberShowLoading(hasData: Boolean, graceMs: Long = 2_500): Boolean {
    var graceOver by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(graceMs)
        graceOver = true
    }
    return !hasData && !graceOver
}

/** Loading de tela cheia, discreto, na cor da marca. */
@Composable
fun CenteredLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = RodapeTheme.colors.oliva,
            strokeWidth = 3.dp,
            modifier = Modifier.size(32.dp),
        )
    }
}
