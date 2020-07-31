package com.fengwei23.daggersample

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS

/**
 * @author w.feng
 * @date 2020/7/31
 */
object LiveDataTestUtil {
  fun <T> getValue(liveData: LiveData<T>): T {
    val data = arrayOfNulls<Any>(1)
    val latch = CountDownLatch(1)
    val observer = object : Observer<T> {
      override fun onChanged(t: T) {
        data[0] = t
        latch.countDown()
        liveData.removeObserver(this)
      }
    }
    liveData.observeForever(observer)
    latch.await(2, SECONDS)
    @Suppress("UNCHECKED_CAST")
    return data[0] as T
  }
}