package com.fengwei23.daggersample

import android.app.Application
import com.fengwei23.daggersample.di.AppComponent
import com.fengwei23.daggersample.di.DaggerAppComponent

/**
 * @author w.feng
 * @date 2020/7/31
 */
open class MyApplication : Application() {
  val appComponent: AppComponent by lazy {
    initializeComponent()
  }

  open fun initializeComponent(): AppComponent {
    // Creates an instance of AppComponent using its Factory constructor
    // We pass the applicationContext that will be used as Context in the graph
    return DaggerAppComponent.factory().create(applicationContext)
  }
}