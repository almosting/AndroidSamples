package com.almosting.easypermissions

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * Created by w.feng on 2018/10/10
 * Email: fengweisb@gmail.com
 */
@RestrictTo(LIBRARY_GROUP)
class AppSettingsDialogHolderActivity : AppCompatActivity(),
  OnClickListener {
  private var mDialog: AlertDialog? = null
  private var mIntentFlags = 0
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val appSettingsDialog: AppSettingsDialog =
      AppSettingsDialog.fromIntent(intent, this)
    mIntentFlags = appSettingsDialog.intentFlags
    mDialog = appSettingsDialog.showDialog(this, this)
  }

  override fun onDestroy() {
    super.onDestroy()
    if (mDialog != null && mDialog!!.isShowing) {
      mDialog!!.dismiss()
    }
  }

  override fun onClick(dialog: DialogInterface, which: Int) {
    if (which == Dialog.BUTTON_POSITIVE) {
      val intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
          .setData(Uri.fromParts("package", packageName, null))
      intent.addFlags(mIntentFlags)
      startActivityForResult(intent, APP_SETTINGS_RC)
    } else if (which == Dialog.BUTTON_NEGATIVE) {
      setResult(Activity.RESULT_CANCELED)
      finish()
    } else {
      throw IllegalStateException("Unknown button type:$which")
    }
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    super.onActivityResult(requestCode, resultCode, data)
    setResult(resultCode, data)
    finish()
  }

  companion object {
    private const val APP_SETTINGS_RC = 7534
    fun createShowDialogIntent(
      context: Context?,
      dialog: AppSettingsDialog?
    ): Intent {
      val intent =
        Intent(context, AppSettingsDialogHolderActivity::class.java)
      intent.putExtra(AppSettingsDialog.EXTRA_APP_SETTINGS, dialog)
      return intent
    }
  }
}