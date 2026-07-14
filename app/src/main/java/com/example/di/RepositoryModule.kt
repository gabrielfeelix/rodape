package com.example.di

import com.example.data.remote.repo.NotificationRepository
import com.example.data.remote.repo.OfflineFirstNotificationRepository
import com.example.data.remote.repo.BookRepository
import com.example.data.remote.repo.DiscussionRepository
import com.example.data.remote.repo.MeetingRepository
import com.example.data.remote.repo.OfflineFirstBookRepository
import com.example.data.remote.repo.ModerationRepository
import com.example.data.remote.repo.OfflineFirstMeetingRepository
import com.example.data.remote.repo.OfflineFirstDiscussionRepository
import com.example.data.remote.repo.OfflineFirstModerationRepository
import com.example.data.remote.repo.OfflineFirstUserRepository
import com.example.data.remote.repo.OfflineFirstVotingRepository
import com.example.data.remote.repo.UserRepository
import com.example.data.remote.repo.VotingRepository
import com.example.data.remote.repo.OfflineFirstProgressRepository
import com.example.data.remote.repo.OfflineFirstQuoteRepository
import com.example.data.remote.repo.ProgressRepository
import com.example.data.remote.repo.QuoteRepository
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

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        impl: OfflineFirstNotificationRepository,
    ): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindQuoteRepository(
        impl: OfflineFirstQuoteRepository,
    ): QuoteRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        impl: OfflineFirstUserRepository,
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindModerationRepository(
        impl: OfflineFirstModerationRepository,
    ): ModerationRepository

    @Binds
    @Singleton
    abstract fun bindVotingRepository(
        impl: OfflineFirstVotingRepository,
    ): VotingRepository

    @Binds
    @Singleton
    abstract fun bindDiscussionRepository(
        impl: OfflineFirstDiscussionRepository,
    ): DiscussionRepository

    @Binds
    @Singleton
    abstract fun bindMeetingRepository(
        impl: OfflineFirstMeetingRepository,
    ): MeetingRepository

    @Binds
    @Singleton
    abstract fun bindBookRepository(
        impl: OfflineFirstBookRepository,
    ): BookRepository
}
