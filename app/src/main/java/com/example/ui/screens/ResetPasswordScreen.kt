package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.RodapeCard
import com.example.ui.theme.RodapeRadii
import com.example.ui.theme.RodapeIcons
import com.example.ui.theme.Muted
import com.example.ui.theme.RodapeTheme
import com.example.ui.theme.Terracota
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    onPasswordUpdated: () -> Unit,
    onUpdatePassword: suspend (newPassword: String) -> Result<Unit>,
    onCancel: () -> Unit,
) {
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    // Espelha a regra do servidor (igual ao SignUp): 8+ com minúscula,
    // maiúscula, dígito e símbolo. Antes aceitava 6+ e o servidor rejeitava
    // só depois do submit.
    val valid = password.length >= 8 &&
        password.any { it.isLowerCase() } &&
        password.any { it.isUpperCase() } &&
        password.any { it.isDigit() } &&
        password.any { !it.isLetterOrDigit() }

    val submitNewPassword: () -> Unit = {
        if (valid && !isLoading) {
            isLoading = true
            scope.launch {
                val r = onUpdatePassword(password)
                isLoading = false
                r.fold(
                    onSuccess = { onPasswordUpdated() },
                    onFailure = { errorMsg = com.example.ui.auth.AuthErrors.friendly(it, "Falha ao redefinir senha") },
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nova senha", style = MaterialTheme.typography.headlineLarge.copy(color = RodapeTheme.colors.terracota)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = RodapeIcons.Back,
                            contentDescription = "Cancelar",
                            tint = RodapeTheme.colors.terracota,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Spacer(Modifier.height(24.dp))
                RodapeCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
                    Text(
                        "Defina uma nova senha",
                        style = MaterialTheme.typography.displaySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMsg = null },
                        label = { Text("Nova senha") },
                        supportingText = {
                            Text(
                                "Mínimo 8 caracteres, com maiúscula, minúscula, número e símbolo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = RodapeTheme.colors.muted,
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitNewPassword() }),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (passwordVisible) "Ocultar senha" else "Mostrar senha",
                                    tint = RodapeTheme.colors.muted,
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                    )
                    // B2: mesmo checklist ao vivo do cadastro — o botão cinza deixava
                    // o usuário sem saber qual regra faltava.
                    if (password.isNotEmpty() && !valid) {
                        Spacer(Modifier.height(8.dp))
                        com.example.ui.components.PasswordChecklist(password)
                    }
                    errorMsg?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { submitNewPassword() },
                        enabled = valid && !isLoading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(RodapeRadii.full),
                        colors = ButtonDefaults.buttonColors(containerColor = RodapeTheme.colors.terracota),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        } else {
                            Text(
                                "Redefinir senha",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}
