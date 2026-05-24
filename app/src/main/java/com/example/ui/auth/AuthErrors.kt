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

            // Rate limit
            lower.contains("over_email_send_rate_limit") || lower.contains("too many requests") ->
                "Muitas tentativas. Aguarde um minuto e tente de novo."

            // Captcha (caso volte a ser habilitado)
            lower.contains("captcha") ->
                "Verificacao de captcha falhou. Tente novamente."

            // Rede
            lower.contains("network") || lower.contains("unable to resolve host") ||
                lower.contains("failed to connect") || lower.contains("timeout") ->
                "Sem conexao com a internet. Verifique sua rede."

            // Generico — se a msg crua e curta e nao tem URL/headers, usa ela
            raw.length in 1..120 && !raw.contains("Headers") && !raw.contains("https://") ->
                raw

            else -> fallback
        }
    }
}
