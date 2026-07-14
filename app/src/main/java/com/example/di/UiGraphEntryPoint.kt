package com.example.di

import com.example.data.DataStoreManager
import com.example.data.remote.AuthRepository
import com.example.data.remote.RemoteRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Ponte do grafo Hilt pra dentro do MainViewModel (F5.0).
 *
 * O MainViewModel ainda NÃO é `@HiltViewModel` (conversão vem por tela no F5), então busca
 * seus colaboradores @Singleton via este EntryPoint. Ganho-chave: a fachada RemoteRepository
 * passa a usar o MESMO SyncEngine @Singleton do Hilt — um engine só do lado da UI (antes a
 * fachada criava o seu, separado do provido, causando realtime duplicado).
 *
 * DataStoreManager e AuthRepository já eram inócuos de duplicar (DataStore é singleton por
 * arquivo via delegate; AuthRepository wrappa o mesmo `supabase.auth`), mas roteá-los aqui
 * dá fonte única e alimenta o SessionManager com as mesmas instâncias.
 *
 * `internal` porque expõe RemoteRepository (agora internal). Hilt gera no mesmo módulo.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface UiGraphEntryPoint {
    fun remoteRepository(): RemoteRepository
    fun dataStoreManager(): DataStoreManager
    fun authRepository(): AuthRepository
}
