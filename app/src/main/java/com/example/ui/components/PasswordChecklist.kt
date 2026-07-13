package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Muted
import com.example.ui.theme.Oliva

/** Regra única de senha forte, compartilhada por cadastro e redefinição. */
fun isStrongPassword(pw: String): Boolean =
    pw.length >= 8 &&
        pw.any { it.isUpperCase() } &&
        pw.any { it.isLowerCase() } &&
        pw.any { it.isDigit() } &&
        pw.any { !it.isLetterOrDigit() }

/**
 * Checklist ao vivo do que ainda falta na senha (✓ oliva / ○ apagado). Antes só
 * o cadastro tinha isso; a tela de redefinir deixava um botão cinza sem dizer o
 * que faltava.
 */
@Composable
fun PasswordChecklist(password: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        PwReq("Pelo menos 8 caracteres", password.length >= 8)
        PwReq("Uma letra maiúscula", password.any { it.isUpperCase() })
        PwReq("Uma letra minúscula", password.any { it.isLowerCase() })
        PwReq("Um número", password.any { it.isDigit() })
        PwReq("Um símbolo (ex.: @, !, #)", password.any { !it.isLetterOrDigit() })
    }
}

@Composable
private fun PwReq(label: String, met: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (met) "✓" else "○",
            color = if (met) Oliva else Muted,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            color = if (met) Oliva else Muted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
