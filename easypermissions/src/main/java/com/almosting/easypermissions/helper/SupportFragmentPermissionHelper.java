package com.almosting.easypermissions.helper;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

/**
 * Created by w.feng on 2018/10/10
 * Email: fengweisb@gmail.com
 */
public class SupportFragmentPermissionHelper extends BaseSupportPermissionHelper<Fragment> {
  public SupportFragmentPermissionHelper(@NonNull Fragment host) {
    super(host);
  }

  @Override
  public FragmentManager getSupportFragmentManager() {
    return getHost().getChildFragmentManager();
  }

  @Override
  public void directRequestPermissions(int requestCode, @NonNull String... perms) {
    getHost().requestPermissions(perms, requestCode);
  }

  @Override
  public boolean shouldShowRequestPermissionRationale(@NonNull String perm) {
    return getHost().shouldShowRequestPermissionRationale(perm);
  }

  @Override
  public Context getContext() {
    return getHost().getActivity();
  }
}
