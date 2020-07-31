package com.fengwei23.daggersample.registration.enterdetails

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fengwei23.daggersample.LiveDataTestUtil
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * @author w.feng
 * @date 2020/7/31
 */
class EnterDetailsViewModelTest {

  // Executes each task synchronously using Architecture Components.
  @get:Rule
  var instantExecutorRule = InstantTaskExecutorRule()

  private lateinit var viewModel: EnterDetailsViewModel

  @Before
  fun setup() {
    viewModel = EnterDetailsViewModel()
  }

  @Test
  fun `ValidateInput gives error when username is invalid`() {
    viewModel.validateInput("user", "password")

    assertTrue(LiveDataTestUtil.getValue(viewModel.enterDetailsState) is EnterDetailsError)
  }

  @Test
  fun `ValidateInput gives error when password is invalid`() {
    viewModel.validateInput("username", "pass")

    assertTrue(LiveDataTestUtil.getValue(viewModel.enterDetailsState) is EnterDetailsError)
  }

  @Test
  fun `ValidateInput succeeds when input is valid`() {
    viewModel.validateInput("username", "password")

    assertTrue(LiveDataTestUtil.getValue(viewModel.enterDetailsState) is EnterDetailsSuccess)
  }
}