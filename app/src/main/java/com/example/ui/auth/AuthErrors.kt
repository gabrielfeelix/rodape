package com.example.ui.auth

/**
 * Traduz mensagens de erro do Supabase Auth pra portugues curto e amigavel.
 *
 * O supabase-kt em alguns casos passa o objeto inteiro (URL + headers + payload)
 * no `Throwable.message`, o que e horrivel pro usuario. Aqui detectamos codigos
 * conhecidos via substring e devolvemos texto simples.
 *
 * Fallback: se nada bater, retornamos uma mensagem generica em vez do JSON cru.
 */
object AuthErrors {

    /** Converte qualquer throwable de auth numa string curta em pt-BR. */
    fun friendly(throwable: Throwable, fallback: String = "Algo deu errado, tente novamente"): String {
        val raw = throwable.message.orEmpty()
        val lower = raw.lowercase()

        return when {
            // Senha fraca
            lower.contains("weak_password") || lower.contains("password should contain") ->
                "Senha muito fraca. Use letras maiusculas, minusculas, numeros e simbolos (ex: Rodape@123)."
            lower.contains("password should be at least") ->
                "Senha curta demais (minimo 8 caracteres)."

            // Email
            lower.contains("invalid_email") || lower.contains("invalid email") ->
                "Email invalido."
            lower.contains("email_address_invalid") ->
                "Email invalido."
            lower.contains("user already registered") || lower.contains("user_already_exists") ->
                "Ja existe uma conta com esse email. Tente entrar."
            lower.contains("email not confirmed") ->
                "Confirme seu email antes de entrar — veja sua caixa de entrada."

            // Login
            lower.contains("invalid_credentials") || lower.contains("invalid login credentials") ->
                "Email ou senha incorretos."
            lower.contains("invalid grant") ->
                "Email ou senha incorretos."

            // Google Sign-In: conta existente com outro provider
            lower.contains("account_not_linked") || lower.contains("identity_not_found") ||
                lower.contains("account reauth") || lower.contains("user_not_found") ->
                "Essa conta foi criada com email/senha. Entre usando sua senha — depois voce pode vincular o Google nas configuracoes."
            // Cancelamento do fluxo Google. O CredentialManager devolve textos crus
            // como "activity is cancelled by the user" / OAuth cancelado — sem esse
            // caso vazava ingles pro usuario.
            lower.contains("cancel") ->
                "Login com Google cancelado."
            // Nenhuma conta/credencial Google no device
            // (CredentialManager: "No credentials available" / "no credential").
            lower.contains("no credential") ->
                "Nenhuma conta Google disponivel neste dispositivo. Adicione uma conta Google nas configuracoes e tente de novo."

            // Rate limit
            lower.contains("over_email_send_rate_limit") || lower.contains("too many requests") ||
                lower.contains("for security purposes") || lower.contains("you can only request this after") ->
                "Muitas tentativas em pouco tempo. Aguarde alguns minutos antes de tentar de novo."

            // Senha ausente/invalida no cadastro
            lower.contains("signup requires a valid password") || lower.contains("password is required") ->
                "Informe uma senha valida."

            // Captcha (caso volte a ser habilitado)
            lower.contains("captcha") ->
                "Verificacao de captcha falhou. Tente novamente."

            // Rede
            lower.contains("network") || lower.contains("unable to resolve host") ||
                lower.contains("failed to connect") || lower.contains("timeout") ->
                "Sem conexao com a internet. Verifique sua rede."

            // Fallback: NUNCA ecoar a mensagem crua (vem sempre em ingles do
            // supabase-kt). Mensagem generica amigavel em pt-BR.
            else -> fallback
        }
    }
}
