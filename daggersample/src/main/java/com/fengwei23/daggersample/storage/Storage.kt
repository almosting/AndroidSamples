package com.fengwei23.daggersample.storage

/**
 * @author w.feng
 * @date 2020/7/31
 */
interface Storage {
  fun setString(key: String, value: String)
  fun getString(key: String): String
}