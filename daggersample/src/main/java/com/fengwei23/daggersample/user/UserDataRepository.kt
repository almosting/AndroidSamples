package com.fengwei23.daggersample.user

import javax.inject.Inject
import kotlin.random.Random

/**
 * @author w.feng
 * @date 2020/7/31
 */
@LoggedUserScope
class UserDataRepository @Inject constructor(private val userManager: UserManager) {

  val username: String
    get() = userManager.username

  var unreadNotifications: Int

  init {
    unreadNotifications = randomInt()
  }

  fun refreshUnreadNotifications() {
    unreadNotifications = randomInt()
  }
}

fun randomInt(): Int {
  return Random.nextInt(until = 100)
}