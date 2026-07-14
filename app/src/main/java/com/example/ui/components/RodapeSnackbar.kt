package com.example.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.theme.RodapeRadii
import com.example.ui.theme.RodapeTheme

/**
 * Snackbar ÚNICO do app (4.3): ink sobre cream, raio da escala, ação em dourado.
 * Troque `SnackbarHost(state)` por `RodapeSnackbarHost(state)` nos Scaffolds —
 * antes cada tela herdava o default M3 (inverseSurface roxo-acinzentado).
 */
@Composable
fun RodapeSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        Snackbar(
            snackbarData = data,
            shape = RoundedCornerShape(RodapeRadii.sm),
            containerColor = RodapeTheme.colors.ink,
            contentColor = RodapeTheme.colors.cream,
            actionColor = RodapeTheme.colors.dourado,
            actionContentColor = RodapeTheme.colors.dourado,
        )
    }
}
