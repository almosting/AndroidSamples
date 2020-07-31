package com.fengwei23.daggersample.registration

import com.fengwei23.daggersample.user.UserManager
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

/**
 * @author w.feng
 * @date 2020/7/31
 */
class RegistrationViewModelTest {

  private lateinit var userManager: UserManager
  private lateinit var viewModel: RegistrationViewModel

  @Before
  fun setup() {
    userManager = mock(UserManager::class.java)
    viewModel = RegistrationViewModel(userManager)
  }

  @Test
  fun `Register user calls userManager`() {
    viewModel.updateUserData("username", "password")
    viewModel.acceptTCs()
    viewModel.registerUser()

    verify(userManager).registerUser("username", "password")
  }
}