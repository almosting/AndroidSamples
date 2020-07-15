package com.almosting.easypermissions.helper

import android.app.Activity
import android.content.Context
import androidx.fragment.app.Fragment

/**
 * Created by w.feng on 2018/10/10
 * Email: fengweisb@gmail.com
 */
class LowApiPermissionsHelper<T> internal constructor(host: T) :
  PermissionHelper<T>(host) {
  override fun directRequestPermissions(
    requestCode: Int,
    vararg perms: String
  ) {
    throw IllegalStateException("Should never be requesting permissions on API < 23!")
  }

  override fun shouldShowRequestPermissionRationale(perm: String): Boolean {
    return false
  }

  override fun showRequestPermissionRationale(
    rationale: String,
    positiveButton: String,
    negativeButton: String, theme: Int,
    requestCode: Int, perms: Array<String>
  ) {
    throw IllegalStateException("Should never be requesting permissions on API < 23!")
  }

  override val context: Context?
    get() = when (host) {
      is Activity -> host
      is Fragment -> (host as Fragment).context
      else -> throw IllegalStateException("Unknown host: $host")
    }
}