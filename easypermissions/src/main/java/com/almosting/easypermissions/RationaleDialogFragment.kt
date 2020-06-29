package com.almosting.easypermissions

import android.app.Dialog
import android.app.DialogFragment
import android.app.FragmentManager
import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY
import androidx.annotation.StyleRes
import com.almosting.easypermissions.EasyPermissions.PermissionCallbacks
import com.almosting.easypermissions.EasyPermissions.RationaleCallbacks

/**
 * Created by w.feng on 2018/10/10
 * Email: fengweisb@gmail.com
 */
@RestrictTo(LIBRARY)
class RationaleDialogFragment : DialogFragment() {
  private var mPermissionCallbacks: PermissionCallbacks? = null
  private var mRationaleCallbacks: RationaleCallbacks? = null
  private var mStateSaved = false
  override fun onAttach(context: Context) {
    super.onAttach(context)
    if (parentFragment != null) {
      if (parentFragment is PermissionCallbacks) {
        mPermissionCallbacks = parentFragment as PermissionCallbacks
      }
      if (parentFragment is RationaleCallbacks) {
        mRationaleCallbacks = parentFragment as RationaleCallbacks
      }
    }
    if (context is PermissionCallbacks) {
      mPermissionCallbacks = context
    }
    if (context is RationaleCallbacks) {
      mRationaleCallbacks = context
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    mStateSaved = true
    super.onSaveInstanceState(outState)
  }

  fun showAllowingStateLoss(
    manager: FragmentManager,
    tag: String?
  ) {
    // API 26 added this convenient method
    if (VERSION.SDK_INT >= VERSION_CODES.O) {
      if (manager.isStateSaved) {
        return
      }
    }
    if (mStateSaved) {
      return
    }
    show(manager, tag)
  }

  override fun onDetach() {
    super.onDetach()
    mPermissionCallbacks = null
  }

  override fun onCreateDialog(savedInstanceState: Bundle): Dialog {
    // Rationale dialog should not be cancelable
    isCancelable = false

    // Get config from arguments, create click listener
    val config = RationaleDialogConfig(arguments)
    val clickListener =
      RationaleDialogClickListener(this, config, mPermissionCallbacks, mRationaleCallbacks)

    // Create an AlertDialog
    return config.createFrameworkDialog(activity, clickListener)
  }

  companion object {
    const val TAG = "RationaleDialogFragment"
    fun newInstance(
      positiveButton: String,
      negativeButton: String,
      rationaleMsg: String,
      @StyleRes theme: Int,
      requestCode: Int,
      permissions: Array<String>
    ): RationaleDialogFragment {
      val dialogFragment = RationaleDialogFragment()
      val config = RationaleDialogConfig(
        positiveButton, negativeButton, rationaleMsg, theme, requestCode, permissions
      )
      dialogFragment.arguments = config.toBundle()
      return dialogFragment
    }
  }
}