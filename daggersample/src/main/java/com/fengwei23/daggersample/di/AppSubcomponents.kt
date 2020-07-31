package com.fengwei23.daggersample.di

import com.fengwei23.daggersample.login.LoginComponent
import com.fengwei23.daggersample.registration.RegistrationComponent
import com.fengwei23.daggersample.user.UserComponent
import dagger.Module

/**
 * @author w.feng
 * @date 2020/7/31
 */
@Module(
  subcomponents = [
    RegistrationComponent::class,
    LoginComponent::class,
    UserComponent::class
  ]
)
class AppSubcomponents