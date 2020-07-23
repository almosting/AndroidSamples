package com.fengwei23.roomwordsample

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * @author w.feng
 * @date 2020/7/23
 */
class WordViewModel(application: Application) : AndroidViewModel(application) {
  private val repository: WordRepository

  val allWords: LiveData<List<Word>>

  init {
    val wordsDao = WordRoomDatabase.getDatabase(application, viewModelScope).wordDao()
    repository = WordRepository(wordsDao)
    allWords = repository.allWords
  }

  fun insert(word: Word) = viewModelScope.launch(Dispatchers.IO) { repository.insert(word) }
}