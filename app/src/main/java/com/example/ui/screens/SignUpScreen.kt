package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.RodapeCard
import com.example.ui.theme.RodapeRadii
import com.example.ui.theme.RodapeIcons
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.Ink
import com.example.ui.theme.LiterataFontFamily
import com.example.ui.theme.Muted
import com.example.ui.theme.Oliva
import com.example.ui.theme.OlivaDark
import com.example.ui.theme.Paper
import com.example.ui.theme.RodapeTheme
import com.example.ui.theme.Terracota
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onNavigateBack: () -> Unit,
    onSignUp: suspend (email: String, password: String, name: String) -> Result<Unit>,
    onSignInWithGoogle: suspend () -> Result<Unit>,
    onSignedUp: () -> Unit,
    onGoogleSignedIn: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showConfirmHint by remember { mutableStateOf(false) }
    // Email de fato usado no cadastro (fixado no submit) — mostrado no "muro" de
    // confirmação e reutilizado no reenvio.
    var confirmedEmail by remember { mutableStateOf("") }
    // Feedback do "Reenviar email" (sucesso ou falha), separado do errorMsg do form.
    var resendMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }

    // Validacao local espelha a regra do servidor (Auth -> Password requirements:
    // Lowercase + Uppercase + Digits + Symbols, min 8 chars).
    val nameValid = name.trim().length >= 2
    val emailValid = email.contains("@") && email.length >= 5
    val pwValid = password.length >= 8 &&
        password.any { it.isLowerCase() } &&
        password.any { it.isUpperCase() } &&
        password.any { it.isDigit() } &&
        password.any { !it.isLetterOrDigit() }
    val formValid = nameValid && emailValid && pwValid

    val submitSignUp: () -> Unit = {
        if (formValid && !isLoading) {
            isLoading = true
            errorMsg = null
            scope.launch {
                val r = onSignUp(email, password, name.trim())
                isLoading = false
                r.fold(
                    onSuccess = {
                        confirmedEmail = email
                        resendMsg = null
                        showConfirmHint = true
                    },
                    onFailure = { errorMsg = com.example.ui.auth.AuthErrors.friendly(it, "Falha ao criar conta") },
                )
            }
        }
    }

    // Reenvio real da confirmação: chamar signUp de novo com o mesmo email ainda
    // não confirmado faz o Supabase disparar outro email de confirmação (não cria
    // conta duplicada). Sem endpoint dedicado exposto por este callback, esse é o
    // caminho de reenvio de verdade — nada de botão decorativo.
    val resendConfirmation: () -> Unit = {
        if (!isLoading && confirmedEmail.isNotBlank()) {
            isLoading = true
            resendMsg = null
            scope.launch {
                val r = onSignUp(confirmedEmail, password, name.trim())
                isLoading = false
                r.fold(
                    onSuccess = { resendMsg = "Reenviamos o link para $confirmedEmail. Confira sua caixa (e o spam)." },
                    onFailure = { resendMsg = com.example.ui.auth.AuthErrors.friendly(it, "Não deu pra reenviar agora. Tente em instantes.") },
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(RodapeIcons.Back, "Voltar", tint = RodapeTheme.colors.ink)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RodapeTheme.colors.paper),
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
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (showConfirmHint) {
                        Text(
                            "Conta criada!",
                            style = MaterialTheme.typography.displaySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Enviamos um link de confirmação para:",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            confirmedEmail,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = RodapeTheme.colors.terracota,
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Abra o link antes de entrar. Confira também a caixa de spam.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = RodapeTheme.colors.muted),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        resendMsg?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                it,
                                color = RodapeTheme.colors.oliva,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { liveRegion = LiveRegionMode.Polite },
                            )
                        }

                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = onSignedUp,
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(RodapeRadii.full),
                            colors = ButtonDefaults.buttonColors(containerColor = RodapeTheme.colors.terracota),
                        ) {
                            Text("Voltar para login", color = Color.White)
                        }

                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { resendConfirmation() },
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(RodapeRadii.full),
                            border = BorderStroke(1.dp, RodapeTheme.colors.terracota),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RodapeTheme.colors.terracota),
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = RodapeTheme.colors.terracota, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                            } else {
                                Text("Reenviar email")
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        // "Corrigir email": volta ao formulário editável sem perder o que
                        // foi digitado (name/email/password ficam no rememberSaveable).
                        TextButton(
                            onClick = {
                                showConfirmHint = false
                                resendMsg = null
                                errorMsg = null
                            },
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Corrigir email", color = RodapeTheme.colors.muted)
                        }
                    } else {
                        // Design: screens-onboarding.jsx — "Vamos criar." à esquerda.
                        Text(
                            "Vamos criar.",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontFamily = LiterataFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 36.sp,
                                letterSpacing = (-0.8).sp,
                                color = RodapeTheme.colors.ink
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Em poucos segundos você está lendo junto.",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = InterFontFamily, fontSize = 14.sp, color = RodapeTheme.colors.muted
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(24.dp))

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it; errorMsg = null },
                            label = { Text("Nome") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { emailFocusRequester.requestFocus() }),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading,
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it.trim(); errorMsg = null },
                            label = { Text("Email") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }),
                            modifier = Modifier.fillMaxWidth().focusRequester(emailFocusRequester),
                            enabled = !isLoading,
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; errorMsg = null },
                            label = { Text("Senha") },
                            supportingText = {
                                Text(
                                    "8+ caracteres com maiúsculas, minúsculas, números e símbolos (ex.: Rodape@123)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { submitSignUp() }),
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
                            modifier = Modifier.fillMaxWidth().focusRequester(passwordFocusRequester),
                            enabled = !isLoading,
                        )

                        // Checklist inline: mostra exatamente o que ainda falta na senha,
                        // pra o botão cinza não deixar o usuário no escuro.
                        if (password.isNotEmpty() && !pwValid) {
                            Spacer(Modifier.height(8.dp))
                            Column(Modifier.fillMaxWidth()) {
                                PasswordReq("Pelo menos 8 caracteres", password.length >= 8)
                                PasswordReq("Uma letra maiúscula", password.any { it.isUpperCase() })
                                PasswordReq("Uma letra minúscula", password.any { it.isLowerCase() })
                                PasswordReq("Um número", password.any { it.isDigit() })
                                PasswordReq("Um símbolo (ex.: @, !, #)", password.any { !it.isLetterOrDigit() })
                            }
                        }

                        errorMsg?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                            )
                        }

                        // Resumo do que ainda bloqueia o cadastro (nome/email/senha), pra
                        // explicar por que o botão "Cadastrar" está desabilitado.
                        val faltamCampos = buildList {
                            if (!nameValid) add("seu nome")
                            if (!emailValid) add("um email válido")
                            if (!pwValid) add("uma senha forte")
                        }
                        if (faltamCampos.isNotEmpty() &&
                            (name.isNotEmpty() || email.isNotEmpty() || password.isNotEmpty())
                        ) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Falta: " + faltamCampos.joinToString(", "),
                                style = MaterialTheme.typography.bodySmall.copy(color = RodapeTheme.colors.muted),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { submitSignUp() },
                            enabled = formValid && !isLoading,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(RodapeRadii.full),
                            colors = ButtonDefaults.buttonColors(containerColor = RodapeTheme.colors.terracota),
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                            } else {
                                Text(
                                    "Cadastrar",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        // Botao Google segue o mesmo branding do LoginScreen. Funciona como
                        // signup OU login — Supabase cria a conta automaticamente no primeiro
                        // contato via Google, entao um mesmo handler serve pros dois fluxos.
                        OutlinedButton(
                            onClick = {
                                isLoading = true
                                errorMsg = null
                                scope.launch {
                                    val r = onSignInWithGoogle()
                                    isLoading = false
                                    r.fold(
                                        onSuccess = { onGoogleSignedIn() },
                                        onFailure = { errorMsg = com.example.ui.auth.AuthErrors.friendly(it, "Falha no Google Sign-In") },
                                    )
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(RodapeRadii.full),
                            border = BorderStroke(1.dp, Color(0xFFDADCE0)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF1F1F1F),
                            ),
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_google_g_logo),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Continuar com Google",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1F1F1F),
                                ),
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                        // Link cruzado com o Login (a "frasezinha" que o dono pediu).
                        TextButton(
                            onClick = onNavigateToLogin,
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Já tem conta? Entre", color = RodapeTheme.colors.olivaDark)
                        }
                    }
                }
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

/** Linha do checklist de senha: verde quando o requisito é atendido. */
@Composable
private fun PasswordReq(label: String, met: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (met) "✓" else "○",
            color = if (met) RodapeTheme.colors.oliva else RodapeTheme.colors.muted,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            color = if (met) RodapeTheme.colors.oliva else RodapeTheme.colors.muted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
