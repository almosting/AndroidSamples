package com.fengwei23.daggersample.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fengwei23.daggersample.user.UserManager
import javax.inject.Inject

/**
 * @author w.feng
 * @date 2020/7/31
 */
class LoginViewModel @Inject constructor(private val userManager: UserManager) {
  private val _loginState = MutableLiveData<LoginViewState>()
  val loginState: LiveData<LoginViewState>
    get() = _loginState

  fun login(username: String, password: String) {
    if (userManager.loginUser(username, password)) {
      _loginState.value = LoginSuccess
    } else {
      _loginState.value = LoginError
    }
  }

  fun unregister() {
    userManager.unregister()
  }

  fun getUsername(): String = userManager.username
}
