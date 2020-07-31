package com.fengwei23.daggersample.storage

import javax.inject.Inject

/**
 * @author w.feng
 * @date 2020/7/31
 */
class FakeStorage @Inject constructor() : Storage {

  private val map = mutableMapOf<String, String>()

  override fun setString(key: String, value: String) {
    map[key] = value
  }

  override fun getString(key: String): String {
    return map[key].orEmpty()
  }
}