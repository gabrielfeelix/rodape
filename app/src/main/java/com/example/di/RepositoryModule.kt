package com.example.di

import com.example.data.remote.repo.OfflineFirstProgressRepository
import com.example.data.remote.repo.ProgressRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * @Binds interface de repo de domínio → impl offline-first (F3c).
 *
 * Cada repo fatiado da god-repo ganha uma linha aqui. Isto força o Hilt a
 * validar que a impl é construtível (SyncEngine + SupabaseClient + RodapeDao,
 * providos no DataModule) — a 1ª validação real do grafo do F4a.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProgressRepository(
        impl: OfflineFirstProgressRepository,
    ): ProgressRepository
}
