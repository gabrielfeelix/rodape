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
        // IMPORTANTE: usamos a legacy anon JWT em vez da publishable key (sb_publishable_).
        // Motivo: testes mostraram que RPCs autenticadas (`/rest/v1/rpc/X`) retornavam
        // 401 com a publishable key, mesmo com Auth plugin instalado e usuario logado.
        // GETs simples (`/rest/v1/profiles?id=...`) funcionavam normalmente. Trocar
        // pra anon JWT resolveu — supabase-kt 3.6 parece ter um bug ou comportamento
        // diferenciado entre as duas chaves nesse caminho de codigo.
        // Ambas sao igualmente seguras pra cliente (RLS no servidor decide tudo).
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
        ) {
            install(Auth) {
                flowType = FlowType.PKCE
                scheme = "app.tramabook"
                host = "login-callback"
            }
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }
    }
}
