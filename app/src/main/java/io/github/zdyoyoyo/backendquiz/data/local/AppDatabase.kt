package io.github.zdyoyoyo.backendquiz.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.zdyoyoyo.backendquiz.data.local.dao.AttemptDao
import io.github.zdyoyoyo.backendquiz.data.local.dao.BankMetaDao
import io.github.zdyoyoyo.backendquiz.data.local.dao.MistakeDao
import io.github.zdyoyoyo.backendquiz.data.local.dao.QuestionDao
import io.github.zdyoyoyo.backendquiz.data.local.entity.AttemptEntity
import io.github.zdyoyoyo.backendquiz.data.local.entity.BankMetaEntity
import io.github.zdyoyoyo.backendquiz.data.local.entity.MistakeEntity
import io.github.zdyoyoyo.backendquiz.data.local.entity.QuestionEntity

/**
 * 資料庫版本每次改 schema 就要 +1，並寫 Migration（之後階段會示範）。
 * 不能用 fallbackToDestructiveMigration，否則升級時作答紀錄會被清空。
 */
@Database(
    entities = [QuestionEntity::class, AttemptEntity::class, MistakeEntity::class, BankMetaEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao
    abstract fun attemptDao(): AttemptDao
    abstract fun mistakeDao(): MistakeDao
    abstract fun bankMetaDao(): BankMetaDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "backend_quiz.db").build()
    }
}
