package com.sunplus.easypermissions.helper;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

/**
 * Created by w.feng on 2018/10/10
 * Email: fengweisb@gmail.com
 */
public class FragmentActivityPermissionHelper
    extends BaseSupportPermissionHelper<FragmentActivity> {
  public FragmentActivityPermissionHelper(@NonNull FragmentActivity host) {
    super(host);
  }

  @Override
  public FragmentManager getSupportFragmentManager() {
    return getHost().getSupportFragmentManager();
  }

  @Override
  public void directRequestPermissions(int requestCode, @NonNull String... perms) {
    ActivityCompat.requestPermissions(getHost(), perms, requestCode);
  }

  @Override
  public boolean shouldShowRequestPermissionRationale(@NonNull String perm) {
    return ActivityCompat.shouldShowRequestPermissionRationale(getHost(),perm);
  }

  @Override
  public Context getContext() {
    return getHost();
  }
}
