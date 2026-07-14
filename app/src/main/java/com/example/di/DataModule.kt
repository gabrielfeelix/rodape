package com.example.di

import android.content.Context
import com.example.data.DataStoreManager
import com.example.data.db.AppDatabase
import com.example.data.db.PendingMutationDao
import com.example.data.db.RodapeDao
import com.example.data.remote.AuthRepository
import com.example.data.remote.Supabase
import com.example.data.remote.SyncEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import javax.inject.Singleton

/**
 * Grafo de DI da camada de dados (F4a — scaffolding Hilt).
 *
 * Fornece os singletons-fundacao que os repos de dominio (F3c) e os VMs (F5)
 * vao INJETAR conforme forem fatiados. Nesta fase nada consome o grafo ainda —
 * ele so lastreia os proximos passos do strangler-fig. Zero mudanca de
 * comportamento: as classes existentes continuam se construindo manualmente.
 *
 * `internal` porque provê SyncEngine (kernel internal do pacote data.remote);
 * o Hilt gera no mesmo modulo Gradle, entao enxerga sem problema.
 *
 * NOTA: RemoteRepository (god-repo) não é provido AQUI, mas desde o F5.0 tem `@Inject
 * constructor(SyncEngine, ...)` + `@Singleton` → o Hilt o auto-provê, usando o MESMO
 * SyncEngine deste módulo (um engine só na UI). O MainViewModel o busca via UiGraphEntryPoint
 * enquanto não é @HiltViewModel. Segue estrangulado (some quando a última tela sair da VM).
 */
@Module
@InstallIn(SingletonComponent::class)
internal object DataModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = Supabase.client

    @Provides
    @Singleton
    fun provideSyncEngine(
        @ApplicationContext appContext: Context,
        supabase: SupabaseClient,
    ): SyncEngine = SyncEngine(appContext, supabase)

    @Provides
    @Singleton
    fun provideRodapeDao(@ApplicationContext appContext: Context): RodapeDao =
        AppDatabase.get(appContext).rodapeDao()

    @Provides
    @Singleton
    fun providePendingMutationDao(@ApplicationContext appContext: Context): PendingMutationDao =
        AppDatabase.get(appContext).pendingMutationDao()

    @Provides
    @Singleton
    fun provideDataStoreManager(@ApplicationContext appContext: Context): DataStoreManager =
        DataStoreManager(appContext)

    @Provides
    @Singleton
    fun provideAuthRepository(supabase: SupabaseClient): AuthRepository =
        AuthRepository(supabase)
}
