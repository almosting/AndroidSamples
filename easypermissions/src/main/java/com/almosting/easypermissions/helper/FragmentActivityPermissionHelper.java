package com.almosting.easypermissions.helper;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

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
