package com.sunplus.easypermissions.helper;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import com.sunplus.easypermissions.RationaleDialogFragmentCompat;

/**
 * Created by w.feng on 2018/10/10
 * Email: fengweisb@gmail.com
 */
public abstract class BaseSupportPermissionHelper<T> extends PermissionHelper<T> {
  private static final String TAG = "BaseSupportPermissionHe";

  BaseSupportPermissionHelper(@NonNull T host) {
    super(host);
  }

  public abstract FragmentManager getSupportFragmentManager();

  @Override
  public void showRequestPermissionRationale(@NonNull String rationale,
                                             @NonNull String positiveButton,
                                             @NonNull String negativeButton,
                                             int theme,
                                             int requestCode,
                                             @NonNull String... perms) {
    FragmentManager fm = getSupportFragmentManager();

    Fragment fragment = fm.findFragmentByTag(RationaleDialogFragmentCompat.TAG);
    if (fragment instanceof RationaleDialogFragmentCompat) {
      Log.d(TAG, "Found existing fragment, not showing rationale.");
      return;
    }

    RationaleDialogFragmentCompat
        .newInstance(rationale, positiveButton, negativeButton, theme, requestCode, perms)
        .showAllowingStateLoss(fm, RationaleDialogFragmentCompat.TAG);
  }
}
