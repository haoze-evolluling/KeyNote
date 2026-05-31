package com.haoze.keynote.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.haoze.keynote.data.db.dao.BillDao
import com.haoze.keynote.data.db.dao.CategoryDao
import com.haoze.keynote.data.db.dao.NoteDao
import com.haoze.keynote.data.db.dao.ScheduleDao
import com.haoze.keynote.data.db.dao.TagDao
import com.haoze.keynote.data.db.entity.BillEntity
import com.haoze.keynote.data.db.entity.CategoryEntity
import com.haoze.keynote.data.db.entity.NoteEntity
import com.haoze.keynote.data.db.entity.NoteTagCrossRef
import com.haoze.keynote.data.db.entity.ScheduleEntity
import com.haoze.keynote.data.db.entity.TagEntity

@Database(
    entities = [NoteEntity::class, TagEntity::class, NoteTagCrossRef::class, BillEntity::class, ScheduleEntity::class, CategoryEntity::class],
    version = 10,
    exportSchema = false
)
abstract class NoteDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao
    abstract fun billDao(): BillDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN summary TEXT DEFAULT NULL")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS bills (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        item TEXT NOT NULL,
                        amount REAL NOT NULL,
                        date INTEGER NOT NULL
                    )
                """)
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS schedules (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        date INTEGER NOT NULL,
                        noteId INTEGER,
                        createdAt INTEGER NOT NULL
                    )
                """)
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE schedules ADD COLUMN endDate INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE schedules ADD COLUMN location TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE schedules ADD COLUMN description TEXT DEFAULT NULL")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE notes SET summary = '[' || '\"' || REPLACE(summary, '\"', '\\\"') || '\"' || ']' WHERE summary IS NOT NULL AND summary != ''")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        isDefault INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("ALTER TABLE bills ADD COLUMN categoryId INTEGER DEFAULT NULL")
                val categories = listOf("吃饭", "交通", "购物", "娱乐", "住房", "医疗", "教育", "其他")
                categories.forEach { name ->
                    db.execSQL("INSERT INTO categories (name, isDefault) VALUES ('$name', 1)")
                }
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN deletedAt INTEGER DEFAULT NULL")
                db.execSQL("UPDATE notes SET deletedAt = updatedAt WHERE isDeleted = 1")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bills ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE bills ADD COLUMN deletedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE schedules ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE schedules ADD COLUMN deletedAt INTEGER DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "keynote_database"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
