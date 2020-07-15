package com.almosting.easypermissions.helper

import android.util.Log
import androidx.fragment.app.FragmentManager
import com.almosting.easypermissions.RationaleDialogFragmentCompat

/**
 * Created by w.feng on 2018/10/10
 * Email: fengweisb@gmail.com
 */
abstract class BaseSupportPermissionHelper<T> internal constructor(host: T) :
  PermissionHelper<T>(host) {
  abstract val supportFragmentManager: FragmentManager
  override fun showRequestPermissionRationale(
    rationale: String,
    positiveButton: String,
    negativeButton: String,
    theme: Int,
    requestCode: Int,
    perms: Array<String>
  ) {
    val fm = supportFragmentManager
    val fragment = fm.findFragmentByTag(RationaleDialogFragmentCompat.TAG)
    if (fragment is RationaleDialogFragmentCompat) {
      Log.d(TAG, "Found existing fragment, not showing rationale.")
      return
    }
    RationaleDialogFragmentCompat.newInstance(
      rationale,
      positiveButton,
      negativeButton,
      theme,
      requestCode,
      perms
    ).showAllowingStateLoss(fm, RationaleDialogFragmentCompat.TAG)
  }

  companion object {
    private const val TAG = "BaseSupportPermissionHe"
  }
}