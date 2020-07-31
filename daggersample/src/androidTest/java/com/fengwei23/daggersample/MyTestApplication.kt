package com.fengwei23.daggersample

import com.fengwei23.daggersample.di.AppComponent

/**
 * @author w.feng
 * @date 2020/7/31
 */
class MyTestApplication : MyApplication() {
  override fun initializeComponent(): AppComponent {
    return DaggerTestAppComponent.create()
  }
}