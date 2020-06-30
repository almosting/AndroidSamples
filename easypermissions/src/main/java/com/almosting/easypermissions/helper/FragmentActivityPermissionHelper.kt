package com.almosting.easypermissions.helper

import android.content.Context
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

/**
 * Created by w.feng on 2018/10/10
 * Email: fengweisb@gmail.com
 */
class FragmentActivityPermissionHelper(host: FragmentActivity) :
  BaseSupportPermissionHelper<FragmentActivity>(host) {
  override val supportFragmentManager: FragmentManager
    get() = host.supportFragmentManager

  override fun directRequestPermissions(
    requestCode: Int,
    vararg perms: String
  ) {
    ActivityCompat.requestPermissions(host, perms, requestCode)
  }

  override fun shouldShowRequestPermissionRationale(perm: String): Boolean {
    return ActivityCompat.shouldShowRequestPermissionRationale(host, perm)
  }

  override val context: Context?
    get() = host
}