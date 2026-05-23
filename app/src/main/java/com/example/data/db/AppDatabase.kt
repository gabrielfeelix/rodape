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
        MeetingNote::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun tramabookDao(): TramabookDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tramabook_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
