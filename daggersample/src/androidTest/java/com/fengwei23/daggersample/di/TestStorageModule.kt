package com.fengwei23.daggersample.di

import com.fengwei23.daggersample.storage.FakeStorage
import com.fengwei23.daggersample.storage.Storage
import dagger.Binds
import dagger.Module

/**
 * @author w.feng
 * @date 2020/7/31
 */
@Module
abstract class TestStorageModule {
  @Binds
  abstract fun provideStorage(storage: FakeStorage): Storage
}