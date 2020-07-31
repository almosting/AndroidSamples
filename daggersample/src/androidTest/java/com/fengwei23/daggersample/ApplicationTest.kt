package com.fengwei23.daggersample

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.fengwei23.daggersample.main.MainActivity
import org.junit.Test

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ApplicationTest {
  @Test
  fun runApp() {
    ActivityScenario.launch(MainActivity::class.java)
    onView(withText("Register to Dagger World!")).check(matches(isDisplayed()))
    onView(withId(R.id.username)).perform(typeText("username"), closeSoftKeyboard())
    onView(withId(R.id.password)).perform(typeText("password"), closeSoftKeyboard())
    onView(withId(R.id.next)).perform(click())

    // Registration/T&Cs
    onView(withText("Terms and Conditions")).check(matches(isDisplayed()))
    onView(withText("REGISTER")).perform(click())

    // Main
    onView(withText("Hello username!")).check(matches(isDisplayed()))
    onView(withText("SETTINGS")).perform(click())

    // Settings
    onView(withText("REFRESH NOTIFICATIONS")).check(matches(isDisplayed()))
    onView(withText("LOGOUT")).perform(click())

    // Login
    onView(withText("Welcome to Dagger World!")).check(matches(isDisplayed()))
    onView(withId(R.id.password)).perform(typeText("password"), closeSoftKeyboard())
    onView(withText("LOGIN")).perform(click())

    // Main
    onView(withText("Hello username!")).check(matches(isDisplayed()))
    onView(withText("SETTINGS")).perform(click())

    // Settings
    onView(withText("REFRESH NOTIFICATIONS")).check(matches(isDisplayed()))
    onView(withText("LOGOUT")).perform(click())

    // Login
    onView(withText("Welcome to Dagger World!")).check(matches(isDisplayed()))
    onView(withId(R.id.unregister)).perform(click())

    // Registration
    onView(withText("Register to Dagger World!")).check(matches(isDisplayed()))
  }
}