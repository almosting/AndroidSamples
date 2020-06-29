package com.almosting.easypermissions.helper;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import android.util.Log;
import com.almosting.easypermissions.RationaleDialogFragment;

/**
 * Created by w.feng on 2018/10/10
 * Email: w.feng@sunmedia.com.cn
 */
public class ActivityPermissionHelper extends PermissionHelper<Activity> {
  private static final String TAG = "ActivityPermissionHelpe";

  ActivityPermissionHelper(@NonNull Activity host) {
    super(host);
  }

  @Override
  public void directRequestPermissions(int requestCode,
                                       @NonNull String... perms) {
    ActivityCompat.requestPermissions(getHost(), perms, requestCode);
  }

  @Override
  public boolean shouldShowRequestPermissionRationale(@NonNull String perm) {
    return ActivityCompat.shouldShowRequestPermissionRationale(getHost(), perm);
  }

  @Override
  public void showRequestPermissionRationale(@NonNull String rationale,
                                             @NonNull String positiveButton,
                                             @NonNull String negativeButton,
                                             int theme,
                                             int requestCode,
                                             @NonNull String... perms) {
    FragmentManager fm = getHost().getFragmentManager();

    Fragment fragment = fm.findFragmentByTag(RationaleDialogFragment.TAG);
    if (fragment instanceof RationaleDialogFragment) {
      Log.d(TAG, "Found existing fragment, not showing rationale.");
      return;
    }

    RationaleDialogFragment
        .newInstance(positiveButton, negativeButton, rationale, theme, requestCode, perms)
        .showAllowingStateLoss(fm, RationaleDialogFragment.TAG);
  }

  @Override
  public Context getContext() {
    return getHost();
  }
}
