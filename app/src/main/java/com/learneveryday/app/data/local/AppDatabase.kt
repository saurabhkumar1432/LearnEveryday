package com.learneveryday.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.learneveryday.app.data.local.dao.*
import com.learneveryday.app.data.local.entity.*

@Database(
    entities = [
        CurriculumEntity::class,
        LessonEntity::class,
        ProgressEntity::class,
        AIConfigEntity::class,
        GenerationQueueEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun curriculumDao(): CurriculumDao
    abstract fun lessonDao(): LessonDao
    abstract fun progressDao(): ProgressDao
    abstract fun aiConfigDao(): AIConfigDao
    abstract fun generationQueueDao(): GenerationQueueDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        private const val DATABASE_NAME = "learneveryday_database"
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration() // For development; use proper migrations in production
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        // For testing purposes
        fun getInMemoryDatabase(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()
        }
    }
}
