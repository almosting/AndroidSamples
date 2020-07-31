package com.fengwei23.daggersample.storage

import android.content.Context
import javax.inject.Inject

/**
 * @author w.feng
 * @date 2020/7/31
 */
class SharedPreferencesStorage @Inject constructor(context: Context) : Storage {
  private val sharedPreferences = context.getSharedPreferences("Dagger", Context.MODE_PRIVATE)
  override fun setString(key: String, value: String) {
    with(sharedPreferences.edit()) {
      putString(key, value)
      apply()
    }
  }

  override fun getString(key: String): String {
    return sharedPreferences.getString(key, "")!!
  }
}