package com.almosting.easypermissions

import android.content.Context
import android.content.DialogInterface.OnClickListener
import android.os.Bundle
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AlertDialog.Builder

/**
 * Created by w.feng on 2018/10/10
 * Email: fengweisb@gmail.com
 */
class RationaleDialogConfig {
  var positiveButton: String
  var negativeButton: String
  var theme: Int
  var requestCode: Int
  var rationaleMsg: String
  var permissions: Array<String>

  internal constructor(
    positiveButton: String,
    negativeButton: String,
    rationaleMsg: String,
    @StyleRes theme: Int,
    requestCode: Int,
    permissions: Array<String>
  ) {
    this.positiveButton = positiveButton
    this.negativeButton = negativeButton
    this.rationaleMsg = rationaleMsg
    this.theme = theme
    this.requestCode = requestCode
    this.permissions = permissions
  }

  internal constructor(bundle: Bundle) {
    positiveButton = bundle.getString(KEY_POSITIVE_BUTTON)!!
    negativeButton = bundle.getString(KEY_NEGATIVE_BUTTON)!!
    rationaleMsg = bundle.getString(KEY_RATIONALE_MESSAGE)!!
    theme = bundle.getInt(KEY_THEME)
    requestCode = bundle.getInt(KEY_REQUEST_CODE)
    permissions = bundle.getStringArray(KEY_PERMISSIONS)!!
  }

  fun toBundle(): Bundle {
    val bundle = Bundle()
    bundle.putString(KEY_POSITIVE_BUTTON, positiveButton)
    bundle.putString(KEY_NEGATIVE_BUTTON, negativeButton)
    bundle.putString(KEY_RATIONALE_MESSAGE, rationaleMsg)
    bundle.putInt(KEY_THEME, theme)
    bundle.putInt(KEY_REQUEST_CODE, requestCode)
    bundle.putStringArray(KEY_PERMISSIONS, permissions)
    return bundle
  }

  fun createSupportDialog(
    context: Context?,
    listener: OnClickListener?
  ): AlertDialog {
    return Builder(context!!, theme)
      .setCancelable(false)
      .setPositiveButton(positiveButton, listener)
      .setNegativeButton(negativeButton, listener)
      .setMessage(rationaleMsg)
      .create()
  }

  fun createFrameworkDialog(
    context: Context?,
    listener: OnClickListener?
  ): AlertDialog {
    return Builder(context!!, theme)
      .setCancelable(false)
      .setPositiveButton(positiveButton, listener)
      .setNegativeButton(negativeButton, listener)
      .setMessage(rationaleMsg)
      .create()
  }

  companion object {
    private const val KEY_POSITIVE_BUTTON = "positiveButton"
    private const val KEY_NEGATIVE_BUTTON = "negativeButton"
    private const val KEY_RATIONALE_MESSAGE = "rationaleMsg"
    private const val KEY_THEME = "theme"
    private const val KEY_REQUEST_CODE = "requestCode"
    private const val KEY_PERMISSIONS = "permissions"
  }
}