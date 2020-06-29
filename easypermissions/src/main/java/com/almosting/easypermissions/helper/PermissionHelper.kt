package com.almosting.easypermissions.helper

import android.app.Activity
import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.annotation.StyleRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

/**
 * Created by w.feng on 2018/10/10
 * Email: fengweisb@gmail.com
 */
abstract class PermissionHelper<T>(val host: T) {
  private fun shouldShowRationale(perms: Array<String>): Boolean {
    for (perm in perms) {
      if (shouldShowRequestPermissionRationale(perm)) {
        return true
      }
    }
    return false
  }

  fun requestPermissions(
    rationale: String,
    positiveButton: String,
    negativeButton: String,
    @StyleRes theme: Int,
    requestCode: Int,
    perms: Array<String>
  ) {
    if (shouldShowRationale(perms)) {
      showRequestPermissionRationale(
        rationale, positiveButton, negativeButton, theme, requestCode, perms
      )
    } else {
      directRequestPermissions(requestCode, *perms)
    }
  }

  fun somePermissionPermanentlyDenied(perms: List<String>): Boolean {
    for (deniedPermission in perms) {
      if (permissionPermanentlyDenied(deniedPermission)) {
        return true
      }
    }
    return false
  }

  fun permissionPermanentlyDenied(perms: String): Boolean {
    return !shouldShowRequestPermissionRationale(perms)
  }

  fun somePermissionDenied(perms: Array<String>): Boolean {
    return shouldShowRationale(perms)
  }

  abstract fun directRequestPermissions(
    requestCode: Int,
    vararg perms: String
  )

  abstract fun shouldShowRequestPermissionRationale(perm: String): Boolean
  abstract fun showRequestPermissionRationale(
    rationale: String,
    positiveButton: String,
    negativeButton: String,
    @StyleRes theme: Int,
    requestCode: Int,
    perms: Array<String>
  )

  abstract val context: Context?

  companion object {
    fun newInstance(host: Activity): PermissionHelper<out Activity> {
      if (VERSION.SDK_INT < VERSION_CODES.M) {
        return LowApiPermissionsHelper(host)
      }
      return if (host is FragmentActivity) {
        FragmentActivityPermissionHelper(host)
      } else {
        ActivityPermissionHelper(host)
      }
    }

    fun newInstance(host: Fragment): PermissionHelper<out Fragment> {
      return if (VERSION.SDK_INT < VERSION_CODES.M) {
        LowApiPermissionsHelper(host)
      } else SupportFragmentPermissionHelper(host)
    }
  }
}