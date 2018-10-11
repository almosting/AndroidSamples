package com.fengwei23.androidsample;

import android.Manifest;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.sunplus.easypermissions.AfterPermissionGranted;
import com.sunplus.easypermissions.AppSettingsDialog;
import com.sunplus.easypermissions.EasyPermissions;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks,
    EasyPermissions.RationaleCallbacks {

  private static final String TAG = "MainActivity";
  private static final String[] LOCATION = {
      Manifest.permission.READ_CONTACTS,
      Manifest.permission.BLUETOOTH,
      Manifest.permission.BLUETOOTH_ADMIN,
      Manifest.permission.ACCESS_FINE_LOCATION,
      Manifest.permission.WRITE_EXTERNAL_STORAGE
  };
  private static final int RC_CAMERA_PERM = 123;
  private static final int RC_LOCATION_CONTACTS_PERM = 124;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    findViewById(R.id.button_camera).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        cameraTask();
      }
    });

    findViewById(R.id.button_location_and_contacts).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        locationAndContactsTask();
      }
    });
  }

  @AfterPermissionGranted(RC_LOCATION_CONTACTS_PERM)
  private void locationAndContactsTask() {
    if (hasLocationAndContactsPermissions()) {
      // Have permissions, do the thing!
      Toast.makeText(this, "TODO: Location and Contacts things", Toast.LENGTH_LONG).show();
    } else {
      // Ask for both permissions
      EasyPermissions.requestPermissions(
          this,
          getString(R.string.rationale_location_contacts),
          RC_LOCATION_CONTACTS_PERM,
          LOCATION);
    }
  }

  @AfterPermissionGranted(RC_CAMERA_PERM)
  private void cameraTask() {
    if (hasCameraPermission()) {
      Toast.makeText(this, "TODO: Camera things", Toast.LENGTH_LONG).show();
    } else {
      Toast.makeText(this, "NOT TODO: Camera things", Toast.LENGTH_LONG).show();
      EasyPermissions.requestPermissions(this,
          getString(R.string.rationale_camera),
          RC_CAMERA_PERM,
          Manifest.permission.CAMERA);
    }
  }

  private boolean hasCameraPermission() {
    return EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA);
  }

  private boolean hasLocationAndContactsPermissions() {
    return EasyPermissions.hasPermissions(this, LOCATION);
  }

  private boolean hasStoragePermission() {
    return EasyPermissions.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
  }

  @Override
  public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
    Log.d(TAG, "onPermissionsGranted:" + requestCode + ":" + perms.size());
  }

  @Override
  public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
    Log.d(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size());
    if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
      new AppSettingsDialog.Builder(this).build().show();
    }
  }

  @Override
  public void onRationaleAccepted(int requestCode) {
    Log.d(TAG, "onRationaleAccepted:" + requestCode);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    Log.d(TAG, "onRequestPermissionsResult: " + requestCode + ":" + Arrays.toString(grantResults));
  }

  @Override
  public void onRationaleDenied(int requestCode) {
    Log.d(TAG, "onRationaleDenied:" + requestCode);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
      String yes = getString(R.string.yes);
      String no = getString(R.string.no);

      // Do something after user returned from app settings screen, like showing a Toast.
      Toast.makeText(
          this,
          getString(R.string.returned_from_app_settings_to_activity,
              hasCameraPermission() ? yes : no,
              hasLocationAndContactsPermissions() ? yes : no),
          Toast.LENGTH_LONG)
          .show();
    }
  }
}
