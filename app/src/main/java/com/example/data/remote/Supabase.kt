package com.example.data.remote

import com.example.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

/**
 * Cliente Supabase unico do app. Lazy thread-safe via Kotlin `object`.
 *
 * URL e chave vem do .env via plugin Secrets -> BuildConfig.
 * Usamos a publishable key (sb_publishable_...), nunca service_role.
 * RLS no servidor e a unica autorizacao.
 */
object Supabase {
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
        ) {
            install(Auth) {
                flowType = FlowType.PKCE
                scheme = "app.rodape"
                host = "login-callback"
            }
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }
    }
}
