package com.example.util

/**
 * Fallback ÚNICO de nome-de-exibição pra usuário logado cujo nome ainda não
 * carregou (Room frio) ou está em branco.
 *
 * I2 do plano de pendências: antes divergia — a saudação caía em "Leitor(a)"
 * e o header/avatar em "Você" pro mesmo usuário anônimo. Agora um só ponto.
 */
const val DISPLAY_NAME_FALLBACK = "Leitor(a)"

/** Nome de exibição consistente: nome do perfil → nome do auth (JWT) → fallback. */
fun displayName(nome: String?, supaName: String? = null): String =
    nome?.trim()?.ifBlank { null }
        ?: supaName?.trim()?.ifBlank { null }
        ?: DISPLAY_NAME_FALLBACK

/** Primeiro nome (pra saudação), com o mesmo fallback consistente. */
fun displayFirstName(nome: String?, supaName: String? = null): String =
    displayName(nome, supaName).substringBefore(" ")
