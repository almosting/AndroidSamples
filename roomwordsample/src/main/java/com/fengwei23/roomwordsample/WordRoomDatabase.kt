package com.fengwei23.roomwordsample

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * @author w.feng
 * @date 2020/7/23
 */
@Database(entities = [Word::class], version = 1)
public abstract class WordRoomDatabase : RoomDatabase() {
  abstract fun wordDao(): WordDao

  companion object {
    @Volatile
    private var INSTANCE: WordRoomDatabase? = null

    fun getDatabase(context: Context, scope: CoroutineScope): WordRoomDatabase {

      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          WordRoomDatabase::class.java,
          "word_database"
        ).fallbackToDestructiveMigration()
          .addCallback(WordDatabaseCallback(scope))
          .build()
        INSTANCE = instance
        instance
      }
    }
  }

  private class WordDatabaseCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {

    override fun onOpen(db: SupportSQLiteDatabase) {
      super.onOpen(db)
      INSTANCE?.let { database ->
        scope.launch {
          populateDatabase(database.wordDao())
        }
      }
    }

    suspend fun populateDatabase(wordDao: WordDao) {
      wordDao.deleteAll()

      var word = Word("hello")
      wordDao.insert(word)
      word = Word("world!")
      wordDao.insert(word)
    }
  }
}