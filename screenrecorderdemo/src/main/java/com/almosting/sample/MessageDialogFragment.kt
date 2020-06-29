package com.almosting.sample

import android.R.drawable
import android.R.string
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog.Builder
import android.app.Dialog
import android.app.DialogFragment
import android.app.Fragment
import android.os.Bundle
import android.util.Log
import com.almosting.toolbox.utils.BuildCheck

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
class MessageDialogFragment : DialogFragment() {
  interface MessageDialogListener {
    fun onMessageDialogResult(
      dialog: MessageDialogFragment?, requestCode: Int,
      permissions: Array<String?>?, result: Boolean
    )
  }

  private var mDialogListener: MessageDialogListener? = null

  @SuppressLint("NewApi")
  override fun onAttach(activity: Activity) {
    super.onAttach(activity)
    //获取回调接口
    if (activity is MessageDialogListener) {
      mDialogListener = activity
    }
    if (mDialogListener == null) {
      val fragment = targetFragment
      if (fragment is MessageDialogListener) {
        mDialogListener = fragment
      }
    }
    if (mDialogListener == null) {
      if (BuildCheck.isAndroid42()) {
        val target = parentFragment
        if (target is MessageDialogListener) {
          mDialogListener = target
        }
      }
    }
    if (mDialogListener == null) {
      throw ClassCastException(activity.toString())
    }
  }

  override fun onCreateDialog(savedInstanceState: Bundle): Dialog {
    val args =
      savedInstanceState ?: arguments
    val requestCode = arguments.getInt("requestCode")
    val idTitle = arguments.getInt("title")
    val idMessage = arguments.getInt("message")
    val permissions = args.getStringArray("permissions")
    return Builder(activity)
      .setIcon(drawable.ic_dialog_alert)
      .setTitle(idTitle)
      .setMessage(idMessage)
      .setPositiveButton(
        string.ok
      ) { dialog, whichButton ->
        try {
          mDialogListener!!.onMessageDialogResult(
            this@MessageDialogFragment, requestCode,
            permissions, true
          )
        } catch (e: Exception) {
          Log.w(TAG, e)
        }
      }
      .setNegativeButton(
        string.cancel
      ) { dialog, whichButton ->
        try {
          mDialogListener!!.onMessageDialogResult(
            this@MessageDialogFragment, requestCode,
            permissions, false
          )
        } catch (e: Exception) {
          Log.w(TAG, e)
        }
      }
      .create()
  }

  companion object {
    private val TAG = MessageDialogFragment::class.java.simpleName
    fun showDialog(
      parent: Activity, requestCode: Int,
      idTitle: Int, idMessage: Int,
      permissions: Array<String>
    ): MessageDialogFragment {
      val dialog = newInstance(requestCode, idTitle, idMessage, permissions)
      dialog.show(parent.fragmentManager, TAG)
      return dialog
    }

    fun showDialog(
      parent: Fragment, requestCode: Int,
      idTitle: Int, idMessage: Int,
      permissions: Array<String>
    ): MessageDialogFragment {
      val dialog =
        newInstance(requestCode, idTitle, idMessage, permissions)
      dialog.setTargetFragment(parent, parent.id)
      dialog.show(parent.fragmentManager, TAG)
      return dialog
    }

    fun newInstance(
      requestCode: Int, idTitle: Int,
      idMessage: Int,
      permissions: Array<String>
    ): MessageDialogFragment {
      val fragment = MessageDialogFragment()
      val args = Bundle()
      args.putInt("requestCode", requestCode)
      args.putInt("title", idTitle)
      args.putInt("message", idMessage)
      args.putStringArray(
        "permissions",
        permissions
      )
      fragment.arguments = args
      return fragment
    }
  }
}