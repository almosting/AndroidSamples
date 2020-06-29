package com.sunplus.easypermissions;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.StyleRes;



/**
 * Created by w.feng on 2018/10/10
 * Email: fengweisb@gmail.com
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class RationaleDialogFragment extends DialogFragment {

  public static final String TAG = "RationaleDialogFragment";

  private EasyPermissions.PermissionCallbacks mPermissionCallbacks;
  private EasyPermissions.RationaleCallbacks mRationaleCallbacks;
  private boolean mStateSaved = false;

  public static RationaleDialogFragment newInstance(
      @NonNull String positiveButton,
      @NonNull String negativeButton,
      @NonNull String rationaleMsg,
      @StyleRes int theme,
      int requestCode,
      @NonNull String[] permissions) {


    RationaleDialogFragment dialogFragment = new RationaleDialogFragment();


    RationaleDialogConfig config = new RationaleDialogConfig(
        positiveButton, negativeButton, rationaleMsg, theme, requestCode, permissions);
    dialogFragment.setArguments(config.toBundle());

    return dialogFragment;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    if (getParentFragment() != null) {
      if (getParentFragment() instanceof EasyPermissions.PermissionCallbacks) {
        mPermissionCallbacks = (EasyPermissions.PermissionCallbacks) getParentFragment();
      }
      if (getParentFragment() instanceof EasyPermissions.RationaleCallbacks) {
        mRationaleCallbacks = (EasyPermissions.RationaleCallbacks) getParentFragment();
      }
    }

    if (context instanceof EasyPermissions.PermissionCallbacks) {
      mPermissionCallbacks = (EasyPermissions.PermissionCallbacks) context;
    }

    if (context instanceof EasyPermissions.RationaleCallbacks) {
      mRationaleCallbacks = (EasyPermissions.RationaleCallbacks) context;
    }
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    mStateSaved = true;
    super.onSaveInstanceState(outState);
  }


  public void showAllowingStateLoss(FragmentManager manager, String tag) {
    // API 26 added this convenient method
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      if (manager.isStateSaved()) {
        return;
      }
    }

    if (mStateSaved) {
      return;
    }

    show(manager, tag);
  }

  @Override
  public void onDetach() {
    super.onDetach();
    mPermissionCallbacks = null;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    // Rationale dialog should not be cancelable
    setCancelable(false);

    // Get config from arguments, create click listener
    RationaleDialogConfig config = new RationaleDialogConfig(getArguments());
    RationaleDialogClickListener clickListener =
        new RationaleDialogClickListener(this, config, mPermissionCallbacks, mRationaleCallbacks);

    // Create an AlertDialog
    return config.createFrameworkDialog(getActivity(), clickListener);
  }
}
