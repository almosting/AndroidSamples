package com.almosting.easypermissions.helper

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.core.app.ActivityCompat
import com.almosting.easypermissions.RationaleDialogFragment

/**
 * Created by w.feng on 2018/10/10
 * Email: w.feng@sunmedia.com.cn
 */
class ActivityPermissionHelper internal constructor(host: Activity) :
  PermissionHelper<Activity>(host) {
  override fun directRequestPermissions(
    requestCode: Int,
    vararg perms: String
  ) {
    ActivityCompat.requestPermissions(host, perms, requestCode)
  }

  override fun shouldShowRequestPermissionRationale(perm: String): Boolean {
    return ActivityCompat.shouldShowRequestPermissionRationale(host, perm)
  }

  override fun showRequestPermissionRationale(
    rationale: String,
    positiveButton: String,
    negativeButton: String,
    theme: Int,
    requestCode: Int,
    perms: Array<String>
  ) {
    val fm = host.fragmentManager
    val fragment = fm.findFragmentByTag(RationaleDialogFragment.TAG)
    if (fragment is RationaleDialogFragment) {
      Log.d(
        TAG,
        "Found existing fragment, not showing rationale."
      )
      return
    }
    RationaleDialogFragment.newInstance(
      positiveButton,
      negativeButton,
      rationale,
      theme,
      requestCode,
      perms
    )
      .showAllowingStateLoss(fm, RationaleDialogFragment.TAG)
  }

  override val context: Context?
    get() = host

  companion object {
    private const val TAG = "ActivityPermissionHelpe"
  }
}