package com.fengwei23.daggersample.login

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fengwei23.daggersample.LiveDataTestUtil
import com.fengwei23.daggersample.user.UserManager
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenever

/**
 * @author w.feng
 * @date 2020/7/31
 */
class LoginViewModelTest {

  // Executes each task synchronously using Architecture Components.
  @get:Rule var instantExecutorRule = InstantTaskExecutorRule()

  private lateinit var viewModel: LoginViewModel
  private lateinit var userManager: UserManager

  @Before
  fun setup() {
    userManager = mock(UserManager::class.java)
    viewModel = LoginViewModel(userManager)
  }

  @Test
  fun `Get username`() {
    whenever(userManager.username).thenReturn("Username")

    val username = viewModel.getUsername()

    assertEquals("Username", username)
  }

  @Test
  fun `Login emits success`() {
    whenever(userManager.loginUser(anyString(), anyString())).thenReturn(true)

    viewModel.login("username", "login")

    assertEquals(LiveDataTestUtil.getValue(viewModel.loginState), LoginSuccess)
  }

  @Test
  fun `Login emits error`() {
    whenever(userManager.loginUser(anyString(), anyString())).thenReturn(false)

    viewModel.login("username", "login")

    assertEquals(LiveDataTestUtil.getValue(viewModel.loginState), LoginError)
  }

  @Test
  fun `Login unregisters`() {
    viewModel.unregister()

    verify(userManager).unregister()
  }
}