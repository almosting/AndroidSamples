package com.almosting.easypermissions

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import androidx.fragment.app.Fragment
import com.almosting.easypermissions.EasyPermissions.PermissionCallbacks
import com.almosting.easypermissions.EasyPermissions.RationaleCallbacks
import com.almosting.easypermissions.helper.PermissionHelper
import java.util.Arrays

/**
 * Created by w.feng on 2018/10/10
 * Email: fengweisb@gmail.com
 */
class RationaleDialogClickListener : OnClickListener {
  private var mHost: Any?
  private var mConfig: RationaleDialogConfig
  private var mCallbacks: PermissionCallbacks?
  private var mRationaleCallbacks: RationaleCallbacks?

  internal constructor(
    compatDialogFragment: RationaleDialogFragmentCompat,
    config: RationaleDialogConfig,
    callbacks: PermissionCallbacks?,
    rationaleCallbacks: RationaleCallbacks?
  ) {
    mHost =
      if (compatDialogFragment.parentFragment != null) compatDialogFragment.parentFragment else compatDialogFragment.activity
    mConfig = config
    mCallbacks = callbacks
    mRationaleCallbacks = rationaleCallbacks
  }

  internal constructor(
    dialogFragment: RationaleDialogFragment,
    config: RationaleDialogConfig,
    callbacks: PermissionCallbacks?,
    dialogCallback: RationaleCallbacks?
  ) {
    mHost = dialogFragment.activity
    mConfig = config
    mCallbacks = callbacks
    mRationaleCallbacks = dialogCallback
  }

  override fun onClick(dialog: DialogInterface, which: Int) {
    val requestCode = mConfig.requestCode
    if (which == Dialog.BUTTON_POSITIVE) {
      val permissions = mConfig.permissions
      if (mRationaleCallbacks != null) {
        mRationaleCallbacks!!.onRationaleAccepted(requestCode)
      }
      if (mHost is Fragment) {
        PermissionHelper.Companion.newInstance(mHost as Fragment)
          .directRequestPermissions(requestCode, *permissions)
      } else if (mHost is Activity) {
        PermissionHelper.Companion.newInstance(mHost as Activity)
          .directRequestPermissions(requestCode, *permissions)
      } else {
        throw RuntimeException("Host must be an Activity or Fragment!")
      }
    } else {
      if (mRationaleCallbacks != null) {
        mRationaleCallbacks!!.onRationaleDenied(requestCode)
      }
      notifyPermissionDenied()
    }
  }

  private fun notifyPermissionDenied() {
    if (mCallbacks != null) {
      mCallbacks!!.onPermissionsDenied(
        mConfig.requestCode,
        Arrays.asList(*mConfig.permissions)
      )
    }
  }
}