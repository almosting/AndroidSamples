package com.fengwei23.roomwordsample

import androidx.lifecycle.LiveData

/**
 * @author w.feng
 * @date 2020/7/23
 */
class WordRepository(private val wordDao: WordDao) {
  val allWords: LiveData<List<Word>> = wordDao.getAlphabetizedWords()

  suspend fun insert(word: Word) {
    wordDao.insert(word)
  }
}