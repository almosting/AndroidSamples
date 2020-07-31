package com.fengwei23.daggersample.user

import com.fengwei23.daggersample.storage.Storage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author w.feng
 * @date 2020/7/31
 */

private const val REGISTERED_USER = "registered_user"
private const val PASSWORD_SUFFIX = "password"

@Singleton
class UserManager @Inject constructor(
  private val storage: Storage,
  private val userComponentFactory: UserComponent.Factory
) {

  var userComponent: UserComponent? = null
    private set

  val username: String
    get() = storage.getString(REGISTERED_USER)

  fun isUserLoggedIn() = userComponent != null

  fun isUserRegistered() = storage.getString(REGISTERED_USER).isNotEmpty()

  fun registerUser(username: String, password: String) {
    storage.setString(REGISTERED_USER, username)
    storage.setString("$username$PASSWORD_SUFFIX", password)
    userJustLoggedIn()
  }

  fun loginUser(username: String, password: String): Boolean {
    val registeredUser = this.username
    if (registeredUser != username) return false

    val registeredPassword = storage.getString("$username$PASSWORD_SUFFIX")
    if (registeredPassword != password) return false

    userJustLoggedIn()
    return true
  }

  fun logout() {
    // When the user logs out, we remove the instance of UserComponent from memory
    userComponent = null
  }

  fun unregister() {
    val username = storage.getString(REGISTERED_USER)
    storage.setString(REGISTERED_USER, "")
    storage.setString("$username$PASSWORD_SUFFIX", "")
    logout()
  }

  private fun userJustLoggedIn() {
    // When the user logs in, we create a new instance of UserComponent
    userComponent = userComponentFactory.create()
  }
}