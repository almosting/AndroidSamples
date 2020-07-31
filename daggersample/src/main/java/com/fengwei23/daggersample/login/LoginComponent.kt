package com.fengwei23.daggersample.login

import com.fengwei23.daggersample.di.ActivityScope
import dagger.Subcomponent

/**
 * @author w.feng
 * @date 2020/7/31
 */
@ActivityScope
@Subcomponent
interface LoginComponent {
  @Subcomponent.Factory
  interface Factory {
    fun create(): LoginComponent
  }

  fun inject(activity: LoginActivity)
}