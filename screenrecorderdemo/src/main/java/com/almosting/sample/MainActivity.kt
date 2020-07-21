package com.almosting.sample

import android.Manifest.permission
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.almosting.easypermissions.AfterPermissionGranted
import com.almosting.easypermissions.AppSettingsDialog
import com.almosting.easypermissions.AppSettingsDialog.Builder
import com.almosting.easypermissions.EasyPermissions
import com.almosting.easypermissions.EasyPermissions.PermissionCallbacks
import com.almosting.easypermissions.EasyPermissions.RationaleCallbacks
import com.almosting.sample.R.string
import com.almosting.sample.databinding.ActivityMainBinding
import java.util.Arrays

class MainActivity : AppCompatActivity(), PermissionCallbacks, RationaleCallbacks {
  private lateinit var binding: ActivityMainBinding
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)
    binding.buttonCamera.setOnClickListener { cameraTask() }
    binding.buttonLocationAndContacts.setOnClickListener { locationAndContactsTask() }
  }

  @AfterPermissionGranted(RC_LOCATION_CONTACTS_PERM)
  private fun locationAndContactsTask() {
    if (hasLocationAndContactsPermissions()) {
      // Have permissions, do the thing!
      Toast.makeText(
        this,
        "TODO: Location and Contacts things",
        Toast.LENGTH_LONG
      ).show()
    } else {
      // Ask for both permissions
      EasyPermissions.requestPermissions(
        this,
        getString(string.rationale_location_contacts),
        RC_LOCATION_CONTACTS_PERM,
        LOCATION
      )
    }
  }

  @AfterPermissionGranted(RC_CAMERA_PERM)
  private fun cameraTask() {
    if (hasCameraPermission()) {
      Toast.makeText(this, "TODO: Camera things", Toast.LENGTH_LONG).show()
    } else {
      Toast.makeText(
        this,
        "NOT TODO: Camera things",
        Toast.LENGTH_LONG
      ).show()
      EasyPermissions.requestPermissions(
        this,
        getString(string.rationale_camera),
        RC_CAMERA_PERM,
        arrayOf(permission.CAMERA)
      )
    }
  }

  private fun hasCameraPermission(): Boolean {
    return EasyPermissions.hasPermissions(this, arrayOf(permission.CAMERA))
  }

  private fun hasLocationAndContactsPermissions(): Boolean {
    return EasyPermissions.hasPermissions(this, LOCATION)
  }

  private fun hasStoragePermission(): Boolean {
    return EasyPermissions.hasPermissions(this, arrayOf(permission.WRITE_EXTERNAL_STORAGE))
  }

  override fun onPermissionsGranted(
    requestCode: Int,
    perms: List<String>
  ) {
    Log.d(TAG, "onPermissionsGranted:" + requestCode + ":" + perms.size)
  }

  override fun onPermissionsDenied(
    requestCode: Int,
    perms: List<String>
  ) {
    Log.d(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size)
    if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
      Builder(this).build().show()
    }
  }

  override fun onRationaleAccepted(requestCode: Int) {
    Log.d(TAG, "onRationaleAccepted:$requestCode")
  }


  override fun onRequestPermissionsResult(
    requestCode: Int, permissions: Array<String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    Log.d(TAG, "onRequestPermissionsResult: " + requestCode + ":" + Arrays.toString(grantResults))
  }

  override fun onRationaleDenied(requestCode: Int) {
    Log.d(TAG, "onRationaleDenied:$requestCode")
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
      val yes = getString(string.yes)
      val no = getString(string.no)

      // Do something after user returned from app settings screen, like showing a Toast.
      Toast.makeText(
        this,
        getString(
          string.returned_from_app_settings_to_activity,
          if (hasCameraPermission()) yes else no,
          if (hasLocationAndContactsPermissions()) yes else no
        ),
        Toast.LENGTH_LONG
      ).show()
    }
  }

  companion object {
    private const val TAG = "MainActivity"
    private val LOCATION = arrayOf(
      permission.READ_CONTACTS,
      permission.BLUETOOTH,
      permission.BLUETOOTH_ADMIN,
      permission.ACCESS_FINE_LOCATION,
      permission.WRITE_EXTERNAL_STORAGE
    )
    private const val RC_CAMERA_PERM = 123
    private const val RC_LOCATION_CONTACTS_PERM = 124
  }
}