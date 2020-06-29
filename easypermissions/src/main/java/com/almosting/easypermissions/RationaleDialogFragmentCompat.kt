package com.almosting.easypermissions

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.FragmentManager
import com.almosting.easypermissions.EasyPermissions.PermissionCallbacks
import com.almosting.easypermissions.EasyPermissions.RationaleCallbacks

/**
 * Created by w.feng on 2018/10/10
 * Email: fengweisb@gmail.com
 */
class RationaleDialogFragmentCompat : AppCompatDialogFragment() {
  private var mPermissionCallbacks: PermissionCallbacks? = null
  private var mRationaleCallbacks: RationaleCallbacks? = null
  fun showAllowingStateLoss(
    manager: FragmentManager,
    tag: String?
  ) {
    if (manager.isStateSaved) {
      return
    }
    show(manager, tag)
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    if (parentFragment != null) {
      if (parentFragment is PermissionCallbacks) {
        mPermissionCallbacks = parentFragment as PermissionCallbacks?
      }
      if (parentFragment is RationaleCallbacks) {
        mRationaleCallbacks = parentFragment as RationaleCallbacks?
      }
    }
    if (context is PermissionCallbacks) {
      mPermissionCallbacks = context
    }
    if (context is RationaleCallbacks) {
      mRationaleCallbacks = context
    }
  }

  override fun onDetach() {
    super.onDetach()
    mPermissionCallbacks = null
    mRationaleCallbacks = null
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    // Rationale dialog should not be cancelable
    isCancelable = false

    // Get config from arguments, create click listener
    val config = RationaleDialogConfig(arguments)
    val clickListener =
      RationaleDialogClickListener(this, config, mPermissionCallbacks, mRationaleCallbacks)

    // Create an AlertDialog
    return config.createSupportDialog(context, clickListener)
  }

  companion object {
    const val TAG = "RationaleDialogFragmentCompat"
    fun newInstance(
      rationaleMsg: String,
      positiveButton: String,
      negativeButton: String,
      @StyleRes theme: Int,
      requestCode: Int,
      permissions: Array<String>
    ): RationaleDialogFragmentCompat {

      // Create new Fragment
      val dialogFragment = RationaleDialogFragmentCompat()

      // Initialize configuration as arguments
      val config = RationaleDialogConfig(
        positiveButton, negativeButton, rationaleMsg, theme, requestCode, permissions
      )
      dialogFragment.arguments = config.toBundle()
      return dialogFragment
    }
  }
}