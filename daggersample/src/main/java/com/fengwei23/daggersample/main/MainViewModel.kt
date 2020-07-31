package com.fengwei23.daggersample.main

import com.fengwei23.daggersample.user.UserDataRepository
import javax.inject.Inject

/**
 * @author w.feng
 * @date 2020/7/31
 */
class MainViewModel @Inject constructor(private val userDataRepository: UserDataRepository){
  val welcomeText:String
    get() = "Hello"

  val notificationsText:String
    get() = ""
}