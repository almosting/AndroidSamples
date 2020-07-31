package com.fengwei23.daggersample.main

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.fengwei23.daggersample.MyApplication
import com.fengwei23.daggersample.R
import com.fengwei23.daggersample.login.LoginActivity
import com.fengwei23.daggersample.registration.RegistrationActivity
import com.fengwei23.daggersample.settings.SettingsActivity
import javax.inject.Inject

class MainActivity : AppCompatActivity() {
  @Inject
  lateinit var mainViewModel: MainViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val userManager = (application as MyApplication).appComponent.userManager()
    if (!userManager.isUserLoggedIn()) {
      if (!userManager.isUserRegistered()) {
        startActivity(Intent(this, RegistrationActivity::class.java))
        finish()
      } else {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
      }
    } else {
      setContentView(R.layout.activity_main)

      // If the MainActivity needs to be displayed, we get the UserComponent from the
      // application graph and gets this Activity injected
      userManager.userComponent!!.inject(this)
      setupViews()
    }
  }

  override fun onResume() {
    super.onResume()
    findViewById<TextView>(R.id.notifications).text = mainViewModel.notificationsText
  }

  private fun setupViews() {
    findViewById<TextView>(R.id.hello).text = mainViewModel.welcomeText
    findViewById<Button>(R.id.settings).setOnClickListener {
      startActivity(Intent(this, SettingsActivity::class.java))
    }
  }
}
