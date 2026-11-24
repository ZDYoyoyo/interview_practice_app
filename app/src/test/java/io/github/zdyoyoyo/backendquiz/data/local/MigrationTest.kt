package io.github.zdyoyoyo.backendquiz.data.local

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 驗證資料庫升級不會弄丟第一階段的作答紀錄。
 * 做法：照 schemas/1.json 建出「舊版 App 的資料庫」並塞入資料 → 用新版 Room 開啟（會自動執行 Migration）。
 * Room 開啟時會比對資料表結構和 v2 schema，Migration 寫錯就會丟例外。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val dbName = "migration-test.db"

    private fun createV1Database() {
        val schema = Json.parseToJsonElement(
            File(System.getProperty("roomSchemaDir"), "1.json").readText()
        ).jsonObject["database"]!!.jsonObject

        val file = context.getDatabasePath(dbName).apply { parentFile?.mkdirs(); delete() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            schema["entities"]!!.jsonArray.forEach { entity ->
                val e = entity.jsonObject
                val table = e["tableName"]!!.jsonPrimitive.content
                db.execSQL(e["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table))
                e["indices"]?.jsonArray?.forEach { index ->
                    db.execSQL(index.jsonObject["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table))
                }
            }
            schema["setupQueries"]!!.jsonArray.forEach { db.execSQL(it.jsonPrimitive.content) }
            db.version = 1

            // 第一階段留下的資料：兩題答過（q1 兩次）、一題錯題
            db.execSQL(
                "INSERT INTO questions VALUES ('q1','spring','s','single','stem','[]','[\"A\"]','e',1,'h',1,1)," +
                    "('q2','database','s','single','stem','[]','[\"A\"]','e',1,'h',1,1)," +
                    "('q3','database','s','single','stem','[]','[\"A\"]','e',1,'h',1,1)"
            )
            db.execSQL(
                "INSERT INTO attempts (questionId, selected, correct, answeredAt, mode) VALUES " +
                    "('q1','B',0,1,'random'),('q1','A',1,2,'mistakes'),('q2','A',1,3,'random')"
            )
            db.execSQL("INSERT INTO mistakes VALUES ('q1',1,1,0,1,1)")
        }
    }

    // v1 → v2：舊資料完整保留，答過的題目被補進排程並設為今天到期
    @Test
    fun `migrate 1 to 2 keeps records and schedules answered questions`() = runTest {
        createV1Database()
        val today = 20_000L

        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.migration1To2 { today })
            .allowMainThreadQueries()
            .build()
        try {
            // 舊資料還在
            assertEquals(2, db.attemptDao().forQuestion("q1").size)
            assertEquals(1, db.mistakeDao().get("q1")!!.wrongCount)

            // 答過的 q1、q2 被補進排程，今天到期；沒答過的 q3 不在排程裡
            assertEquals(listOf("q1", "q2"), db.reviewDao().dueIds(today))
            assertEquals(2, db.reviewDao().observeDueCount(today).first())
            assertNull(db.reviewDao().get("q3"))
            assertNull(db.reviewDao().get("q1")!!.lastReviewEpochDay)
        } finally {
            db.close()
        }
    }
}
