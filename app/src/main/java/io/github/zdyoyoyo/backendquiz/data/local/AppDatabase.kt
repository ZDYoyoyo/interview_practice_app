package io.github.zdyoyoyo.backendquiz.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.zdyoyoyo.backendquiz.data.local.dao.AttemptDao
import io.github.zdyoyoyo.backendquiz.data.local.dao.BankMetaDao
import io.github.zdyoyoyo.backendquiz.data.local.dao.MistakeDao
import io.github.zdyoyoyo.backendquiz.data.local.dao.QuestionDao
import io.github.zdyoyoyo.backendquiz.data.local.dao.ReviewDao
import io.github.zdyoyoyo.backendquiz.data.local.entity.AttemptEntity
import io.github.zdyoyoyo.backendquiz.data.local.entity.BankMetaEntity
import io.github.zdyoyoyo.backendquiz.data.local.entity.MistakeEntity
import io.github.zdyoyoyo.backendquiz.data.local.entity.QuestionEntity
import io.github.zdyoyoyo.backendquiz.data.local.entity.ReviewStateEntity
import java.time.LocalDate

/**
 * 資料庫版本每次改 schema 就要 +1，並寫 Migration。
 * 不能用 fallbackToDestructiveMigration，否則升級時作答紀錄會被清空。
 *
 * 版本歷史：
 *   v1：questions / attempts / mistakes / bank_meta
 *   v2：新增 review_states（SM-2 排程）
 */
@Database(
    entities = [
        QuestionEntity::class,
        AttemptEntity::class,
        MistakeEntity::class,
        BankMetaEntity::class,
        ReviewStateEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao
    abstract fun attemptDao(): AttemptDao
    abstract fun mistakeDao(): MistakeDao
    abstract fun bankMetaDao(): BankMetaDao
    abstract fun reviewDao(): ReviewDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
                .addMigrations(MIGRATION_1_2)
                .build()

        const val DB_NAME = "backend_quiz.db"

        /**
         * v1 → v2：
         * 1. 建立 review_states 表（SQL 與 Room 產生的 schemas/2.json 完全一致，否則開啟時會驗證失敗）
         * 2. 把第一階段已經做過的題目補進排程，設定為「今天該複習」，舊紀錄馬上有用
         */
        fun migration1To2(today: () -> Long = { LocalDate.now().toEpochDay() }) = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `review_states` (`questionId` TEXT NOT NULL, " +
                        "`easiness` REAL NOT NULL, `intervalDays` INTEGER NOT NULL, `repetitions` INTEGER NOT NULL, " +
                        "`dueEpochDay` INTEGER NOT NULL, `lastReviewEpochDay` INTEGER, PRIMARY KEY(`questionId`))"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_review_states_dueEpochDay` ON `review_states` (`dueEpochDay`)"
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO review_states
                        (questionId, easiness, intervalDays, repetitions, dueEpochDay, lastReviewEpochDay)
                    SELECT DISTINCT questionId, 2.5, 0, 0, ?, NULL FROM attempts
                    """.trimIndent(),
                    arrayOf<Any>(today()),
                )
            }
        }

        val MIGRATION_1_2 = migration1To2()
    }
}
