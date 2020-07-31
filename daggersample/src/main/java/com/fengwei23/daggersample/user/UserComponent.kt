package com.fengwei23.daggersample.user

import com.fengwei23.daggersample.main.MainActivity
import com.fengwei23.daggersample.settings.SettingsActivity
import dagger.Subcomponent

/**
 * @author w.feng
 * @date 2020/7/31
 */
@LoggedUserScope
@Subcomponent
interface UserComponent {
  @Subcomponent.Factory
  interface Factory {
    fun create(): UserComponent
  }

  fun inject(activity: MainActivity)
  fun inject(activity: SettingsActivity)
}