package com.example.di

import android.content.Context
import com.example.data.DataStoreManager
import com.example.data.db.AppDatabase
import com.example.data.db.RodapeDao
import com.example.data.remote.AuthRepository
import com.example.data.remote.RemoteRepository
import com.example.data.remote.Supabase
import com.example.data.remote.SyncEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import javax.inject.Singleton

// F4a: grafo base de dados. Por ora só o DrainQueueWorker consome (SyncEngine);
// os repos de domínio entram via @Binds no F3c e o SessionManager no F4b.
// O RemoteRepository/MainViewModel seguem se auto-construindo até lá (strangler).
@Module
@InstallIn(SingletonComponent::class)
internal object DataModule {

    // Reusa o singleton existente (warm-up no RodapeApp.onCreate continua valendo).
    @Provides
    fun provideSupabase(): SupabaseClient = Supabase.client

    // Room já é singleton via AppDatabase.get() — o provide só expõe o DAO no grafo.
    @Provides
    fun provideRodapeDao(@ApplicationContext context: Context): RodapeDao =
        AppDatabase.get(context).rodapeDao()

    @Provides
    @Singleton
    fun provideDataStoreManager(@ApplicationContext context: Context): DataStoreManager =
        DataStoreManager(context)

    // Kernel offline. @Singleton: os 25 handlers registram UMA vez no init e
    // valem pro processo inteiro (trava de drain-safety do HANDOFF).
    @Provides
    @Singleton
    fun provideSyncEngine(
        @ApplicationContext context: Context,
        supabase: SupabaseClient,
    ): SyncEngine = SyncEngine(context, supabase)

    // Wrapper fino e stateless do Supabase Auth (client é singleton) — as telas
    // de auth que criam AuthRepository() na mão seguem equivalentes.
    @Provides
    @Singleton
    fun provideAuthRepository(supabase: SupabaseClient): AuthRepository = AuthRepository(supabase)

    // F4b: fachada com a MESMA engine do processo (antes o MainViewModel
    // construía a sua — duas engines = dois registries de reload/realtime).
    @Provides
    @Singleton
    fun provideRemoteRepository(engine: SyncEngine): RemoteRepository = RemoteRepository(engine)
}
