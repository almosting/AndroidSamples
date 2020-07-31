package com.fengwei23.daggersample

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/**
 * @author w.feng
 * @date 2020/7/31
 */
class MyCustomTestRunner : AndroidJUnitRunner() {
  override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application {
    return super.newApplication(cl, MyTestApplication::class.java.name, context)
  }
}