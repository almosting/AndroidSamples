package com.fengwei23.daggersample.di

import com.fengwei23.daggersample.storage.SharedPreferencesStorage
import com.fengwei23.daggersample.storage.Storage
import dagger.Binds
import dagger.Module

/**
 * @author w.feng
 * @date 2020/7/31
 */
@Module
abstract class StorageModule {
  @Binds
  abstract fun provideStorage(store: SharedPreferencesStorage): Storage
}