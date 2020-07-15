package com.almosting.sample

import android.Manifest
import android.Manifest.permission
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import com.almosting.sample.MessageDialogFragment.MessageDialogListener
import com.almosting.sample.R.id
import com.almosting.sample.R.layout
import com.almosting.sample.R.string
import com.almosting.sample.service.ScreenRecorderService
import com.almosting.toolbox.utils.BuildCheck
import com.almosting.toolbox.utils.PermissionCheck
import java.lang.ref.WeakReference
import kotlin.math.min

/**
 * @author w.feng
 */
open class ScreenRecorderActivity : AppCompatActivity(),
  MessageDialogListener {
  private var mRecordButton: ToggleButton? = null
  private var mPauseButton: ToggleButton? = null
  private var mReceiver: MyBroadcastReceiver? = null
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(layout.activity_screen_recorder)
    mRecordButton =
      findViewById(id.record_button)
    mPauseButton =
      findViewById(id.pause_button)
    updateRecording(false, false)
    if (mReceiver == null) {
      mReceiver = MyBroadcastReceiver(this)
    }
  }

  override fun onResume() {
    super.onResume()
    val intentFilter = IntentFilter()
    intentFilter.addAction(ScreenRecorderService.ACTION_QUERY_STATUS_RESULT)
    registerReceiver(mReceiver, intentFilter)
    queryRecordingStatus()
  }

  override fun onPause() {
    unregisterReceiver(mReceiver)
    super.onPause()
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    super.onActivityResult(requestCode, resultCode, data)
    if (REQUEST_CODE_SCREEN_CAPTURE == requestCode) {
      if (resultCode != Activity.RESULT_OK) {
        // when no permission
        Toast.makeText(this, "permission denied", Toast.LENGTH_LONG)
          .show()
        return
      }
      startScreenRecorder(resultCode, data)
    }
  }

  private val mOnCheckedChangeListener: OnCheckedChangeListener =
    object : OnCheckedChangeListener {
      override fun onCheckedChanged(
        buttonView: CompoundButton,
        isChecked: Boolean
      ) {
        when (buttonView.id) {
          id.record_button -> if (checkPermissionWriteExternalStorage() && checkPermissionAudio()) {
            if (isChecked) {
              val manager =
                getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
              val permissionIntent = manager.createScreenCaptureIntent()
              startActivityForResult(
                permissionIntent,
                REQUEST_CODE_SCREEN_CAPTURE
              )
            } else {
              val intent = Intent(
                this@ScreenRecorderActivity,
                ScreenRecorderService::class.java
              ).also {
                it.action = ScreenRecorderService.ACTION_STOP
              }
              startService(intent)
            }
          } else {
            mRecordButton!!.setOnCheckedChangeListener(null)
            try {
              mRecordButton!!.isChecked = false
            } finally {
              mRecordButton!!.setOnCheckedChangeListener(this)
            }
          }
          id.pause_button -> if (isChecked) {
            val intent =
              Intent(this@ScreenRecorderActivity, ScreenRecorderService::class.java).also {
                it.action = ScreenRecorderService.ACTION_PAUSE
              }
            startService(intent)
          } else {
            val intent =
              Intent(this@ScreenRecorderActivity, ScreenRecorderService::class.java).also {
                it.action = ScreenRecorderService.ACTION_RESUME
              }
            startService(intent)
          }
          else -> {
          }
        }
      }
    }

  private fun queryRecordingStatus() {
    val intent = Intent(this, ScreenRecorderService::class.java).also {
      it.action = ScreenRecorderService.ACTION_QUERY_STATUS
    }
    startService(intent)
  }

  private fun startScreenRecorder(resultCode: Int, data: Intent?) {
    val intent = Intent(this, ScreenRecorderService::class.java).also {
      it.action = ScreenRecorderService.ACTION_START
      it.putExtra(ScreenRecorderService.EXTRA_RESULT_CODE, resultCode)
      it.putExtras(data!!)
    }
    startService(intent)
  }

  private fun updateRecording(isRecording: Boolean, isPausing: Boolean) {
    mRecordButton!!.setOnCheckedChangeListener(null)
    mPauseButton!!.setOnCheckedChangeListener(null)
    try {
      mRecordButton!!.isChecked = isRecording
      mPauseButton!!.isEnabled = isRecording
      mPauseButton!!.isChecked = isPausing
    } finally {
      mRecordButton!!.setOnCheckedChangeListener(mOnCheckedChangeListener)
      mPauseButton!!.setOnCheckedChangeListener(mOnCheckedChangeListener)
    }
  }

  private class MyBroadcastReceiver(parent: ScreenRecorderActivity) :
    BroadcastReceiver() {
    private val mWeakParent: WeakReference<ScreenRecorderActivity> = WeakReference(parent)
    override fun onReceive(
      context: Context,
      intent: Intent
    ) {
      val action = intent.action
      if (ScreenRecorderService.ACTION_QUERY_STATUS_RESULT == action) {
        val isRecording =
          intent.getBooleanExtra(ScreenRecorderService.EXTRA_QUERY_RESULT_RECORDING, false)
        val isPausing =
          intent.getBooleanExtra(ScreenRecorderService.EXTRA_QUERY_RESULT_PAUSING, false)
        val parent = mWeakParent.get()
        parent?.updateRecording(isRecording, isPausing)
      }
    }
  }
  //================================================================================
  // methods related to new permission model on Android 6 and later
  //================================================================================
  /**
   * Callback listener from MessageDialogFragmentV4
   */
  @SuppressLint("NewApi") override fun onMessageDialogResult(
    dialog: MessageDialogFragment?, requestCode: Int,
    permissions: Array<String?>?, result: Boolean
  ) {
    if (result) {
      // request permission(s) when user touched/clicked OK
      if (BuildCheck.isMarshmallow()) {
        requestPermissions(permissions!!, requestCode)
        return
      }
    }
    // check permission and call #checkPermissionResult when user canceled or not Android6(and later)
    for (permission in permissions!!) {
      checkPermissionResult(
        requestCode, permission,
        PermissionCheck.hasPermission(this, permission)
      )
    }
  }

  /**
   * callback method when app(Fragment) receive the result of permission result from ANdroid system
   */
  override fun onRequestPermissionsResult(
    requestCode: Int, permissions: Array<String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    val n = min(permissions.size, grantResults.size)
    for (i in 0 until n) {
      checkPermissionResult(
        requestCode, permissions[i],
        grantResults[i] == PackageManager.PERMISSION_GRANTED
      )
    }
  }

  /**
   * check the result of permission request
   * if app still has no permission, just show Toast
   */
  private fun checkPermissionResult(
    requestCode: Int, permission: String?,
    result: Boolean
  ) {
    // show Toast when there is no permission
    if (Manifest.permission.RECORD_AUDIO == permission) {
      onUpdateAudioPermission(result)
      if (!result) {
        Toast.makeText(
          this,
          string.permission_audio,
          Toast.LENGTH_SHORT
        ).show()
      }
    }
    if (Manifest.permission.WRITE_EXTERNAL_STORAGE == permission) {
      onUpdateExternalStoragePermission(result)
      if (!result) {
        Toast.makeText(
          this,
          string.permission_ext_storage,
          Toast.LENGTH_SHORT
        ).show()
      }
    }
    if (Manifest.permission.INTERNET == permission) {
      onUpdateNetworkPermission(result)
      if (!result) {
        Toast.makeText(
          this,
          string.permission_network,
          Toast.LENGTH_SHORT
        ).show()
      }
    }
  }

  /**
   * called when user give permission for audio recording or canceled
   */
  private fun onUpdateAudioPermission(hasPermission: Boolean) {}

  /**
   * called when user give permission for accessing external storage or canceled
   */
  private fun onUpdateExternalStoragePermission(hasPermission: Boolean) {}

  /**
   * called when user give permission for accessing network or canceled
   * this will not be called
   */
  private fun onUpdateNetworkPermission(hasPermission: Boolean) {}

  /**
   * check whether this app has write external storage
   * if this app has no permission, show dialog
   *
   * @return true this app has permission
   */
  protected fun checkPermissionWriteExternalStorage(): Boolean {
    if (!PermissionCheck.hasWriteExternalStorage(this)) {
      MessageDialogFragment.showDialog(
        this,
        REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE,
        string.permission_title,
        string.permission_ext_storage_request,
        arrayOf(permission.WRITE_EXTERNAL_STORAGE)
      )
      return false
    }
    return true
  }

  /**
   * check whether this app has permission of audio recording
   * if this app has no permission, show dialog
   *
   * @return true this app has permission
   */
  protected fun checkPermissionAudio(): Boolean {
    if (!PermissionCheck.hasAudio(this)) {
      MessageDialogFragment.showDialog(
        this,
        REQUEST_PERMISSION_AUDIO_RECORDING,
        string.permission_title,
        string.permission_audio_recording_request,
        arrayOf(permission.RECORD_AUDIO)
      )
      return false
    }
    return true
  }

  /**
   * check whether permission of network access
   * if this app has no permission, show dialog
   *
   * @return true this app has permission
   */
  private fun checkPermissionNetwork(): Boolean {
    if (!PermissionCheck.hasNetwork(this)) {
      MessageDialogFragment.showDialog(
        this,
        REQUEST_PERMISSION_NETWORK,
        string.permission_title,
        string.permission_network_request,
        arrayOf(permission.INTERNET)
      )
      return false
    }
    return true
  }

  companion object {
    private const val TAG = "MainActivity"
    private const val REQUEST_CODE_SCREEN_CAPTURE = 1
    protected const val REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 0x01
    protected const val REQUEST_PERMISSION_AUDIO_RECORDING = 0x02
    protected const val REQUEST_PERMISSION_NETWORK = 0x03
  }
}