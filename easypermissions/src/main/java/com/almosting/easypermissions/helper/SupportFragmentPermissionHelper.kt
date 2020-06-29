package com.almosting.easypermissions.helper

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

/**
 * Created by w.feng on 2018/10/10
 * Email: fengweisb@gmail.com
 */
class SupportFragmentPermissionHelper(host: Fragment) :
  BaseSupportPermissionHelper<Fragment>(host) {
  override val supportFragmentManager: FragmentManager
    get() = host.childFragmentManager

  override fun directRequestPermissions(
    requestCode: Int,
    vararg perms: String
  ) {
    host.requestPermissions(perms, requestCode)
  }

  override fun shouldShowRequestPermissionRationale(perm: String): Boolean {
    return host.shouldShowRequestPermissionRationale(perm)
  }

  override val context: Context?
    get() = host.activity
}