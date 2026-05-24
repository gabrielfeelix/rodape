package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

@Database(
    entities = [
        User::class,
        Club::class,
        ClubMember::class,
        Book::class,
        ClubBook::class,
        Chapter::class,
        UserProgress::class,
        Comment::class,
        Reaction::class,
        Vote::class,
        Meeting::class,
        MeetingRsvp::class,
        DbNotification::class,
        SavedQuote::class,
        BookSummary::class,
        BookRating::class,
        BookSuggestion::class,
        VotingRound::class,
        MeetingPattern::class,
        MemberRemoval::class,
        MeetingMinutes::class,
        MeetingNote::class,
        PendingMutation::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun rodapeDao(): RodapeDao
    abstract fun pendingMutationDao(): PendingMutationDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        /**
         * Banco e GLOBAL pro app, nao por usuario — porque RLS no servidor ja garante
         * que cada user so consegue baixar SEU dado, entao o cache local naturalmente
         * fica isolado. Ao trocar de conta (logout/login), chamar `dao.clearAll()`
         * pra evitar leak de dados entre contas no mesmo device.
         */
        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "rodape-cache.db"
            )
                // Migration strategy: durante dev, drop-and-recreate. Em producao
                // futura, escrever Migration explicito por versao.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                .also { INSTANCE = it }
        }
    }
}
