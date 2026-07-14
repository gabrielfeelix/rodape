package com.example.di

import com.example.data.remote.repo.DiscussionRepository
import com.example.data.remote.repo.ModerationRepository
import com.example.data.remote.repo.OfflineFirstDiscussionRepository
import com.example.data.remote.repo.NotificationRepository
import com.example.data.remote.repo.OfflineFirstModerationRepository
import com.example.data.remote.repo.OfflineFirstNotificationRepository
import com.example.data.remote.repo.OfflineFirstProgressRepository
import com.example.data.remote.repo.OfflineFirstQuoteRepository
import com.example.data.remote.repo.ProgressRepository
import com.example.data.remote.repo.QuoteRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// F3c: bindings interface → OfflineFirst*Impl. Consumidores via DI chegam no
// F4b/F5 (SessionManager + VMs por tela); o RemoteRepository-fachada constrói
// os mesmos impls manualmente com a engine dele até lá (strangler).
@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProgressRepository(impl: OfflineFirstProgressRepository): ProgressRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: OfflineFirstNotificationRepository): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindQuoteRepository(impl: OfflineFirstQuoteRepository): QuoteRepository

    @Binds
    @Singleton
    abstract fun bindModerationRepository(impl: OfflineFirstModerationRepository): ModerationRepository

    @Binds
    @Singleton
    abstract fun bindDiscussionRepository(impl: OfflineFirstDiscussionRepository): DiscussionRepository
}
