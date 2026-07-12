package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.BuildConfig
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
        BookFavorite::class,
        BookSuggestion::class,
        VotingRound::class,
        MeetingPattern::class,
        MemberRemoval::class,
        MeetingMinutes::class,
        MeetingNote::class,
        PendingMutation::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun rodapeDao(): RodapeDao
    abstract fun pendingMutationDao(): PendingMutationDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // v1 -> v2: adicionou a tabela pending_mutations (fila offline). Sem esta
        // migration o app CRASHAVA no upgrade de quem tinha v1 instalado.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `pending_mutations` (" +
                        "`id` TEXT NOT NULL, `kind` TEXT NOT NULL, `payload` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, `attempts` INTEGER NOT NULL, " +
                        "`lastError` TEXT, PRIMARY KEY(`id`))"
                )
            }
        }

        // v2 -> v3:
        //  - votes: PK passa a incluir votingRoundId (evita colapso de votos entre
        //    rodadas). Recria a tabela e descarta linhas sem round (ambíguas).
        //  - meetings: nova coluna dataEpoch (instante real, pra ordenar cronologicamente).
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `votes_new` (" +
                        "`votingRoundId` TEXT NOT NULL, `clubBookId` TEXT NOT NULL, " +
                        "`userId` TEXT NOT NULL, `votedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`votingRoundId`, `clubBookId`, `userId`))"
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO `votes_new` (`votingRoundId`, `clubBookId`, `userId`, `votedAt`) " +
                        "SELECT `votingRoundId`, `clubBookId`, `userId`, `votedAt` FROM `votes` " +
                        "WHERE `votingRoundId` IS NOT NULL"
                )
                db.execSQL("DROP TABLE `votes`")
                db.execSQL("ALTER TABLE `votes_new` RENAME TO `votes`")
                db.execSQL("ALTER TABLE `meetings` ADD COLUMN `dataEpoch` INTEGER NOT NULL DEFAULT 0")
            }
        }

        // v3 -> v4: nova tabela book_favorites (favorito pessoal de livro, cross-clube).
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `book_favorites` (" +
                        "`userId` TEXT NOT NULL, `bookId` TEXT NOT NULL, " +
                        "`criadoEm` INTEGER NOT NULL, PRIMARY KEY(`userId`, `bookId`))"
                )
            }
        }

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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .apply {
                    // Migrations explicitas acima preservam o cache e a fila offline
                    // no upgrade. Em DEBUG mantemos o fallback destrutivo como rede de
                    // seguranca pra iteracao rapida com schema instavel; em RELEASE,
                    // jamais destruir — um schema novo sem Migration deve crashar
                    // visivelmente em vez de apagar dado real do usuario.
                    if (BuildConfig.DEBUG) {
                        fallbackToDestructiveMigration(dropAllTables = true)
                    }
                }
                .build()
                .also { INSTANCE = it }
        }
    }
}
