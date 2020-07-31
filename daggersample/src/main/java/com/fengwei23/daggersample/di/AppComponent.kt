package com.fengwei23.daggersample.di

import android.content.Context
import com.fengwei23.daggersample.login.LoginComponent
import com.fengwei23.daggersample.registration.RegistrationComponent
import com.fengwei23.daggersample.user.UserManager
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

/**
 * @author w.feng
 * @date 2020/7/31
 */
@Singleton
@Component(modules = [StorageModule::class, AppSubcomponents::class])
interface AppComponent {
  @Component.Factory
  interface Factory {
    fun create(@BindsInstance context: Context): AppComponent
  }
  fun registrationComponent(): RegistrationComponent.Factory
  fun loginComponent(): LoginComponent.Factory
  fun userManager(): UserManager
}
