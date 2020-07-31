package com.fengwei23.daggersample.di

import dagger.Component
import javax.inject.Singleton

/**
 * @author w.feng
 * @date 2020/7/31
 */
@Singleton
@Component(modules = [TestStorageModule::class])
interface TestAppComponent : AppComponent