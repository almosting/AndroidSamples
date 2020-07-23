package com.fengwei23.roomwordsample

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * @author w.feng
 * @date 2020/7/23
 */
@Dao
interface WordDao {
  @Query("SELECT * from word_table ORDER BY word ASC")
  fun getAlphabetizedWords(): LiveData<List<Word>>

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insert(word: Word)

  @Query("DELETE FROM word_table")
  suspend fun deleteAll()
}