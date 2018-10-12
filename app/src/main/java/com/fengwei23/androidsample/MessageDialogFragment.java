package com.fengwei23.androidsample;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import com.sunplus.toolbox.utils.BuildCheck;

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
public class MessageDialogFragment extends DialogFragment {
  private static final String TAG = MessageDialogFragment.class.getSimpleName();

  public interface MessageDialogListener {
    void onMessageDialogResult(final MessageDialogFragment dialog, final int requestCode,
                               final String[] permissions, final boolean result);
  }

  public static MessageDialogFragment showDialog(final Activity parent, final int requestCode,
                                                 final int idTitle, final int idMessage,
                                                 final String[] permissions) {
    final MessageDialogFragment dialog =
        newInstance(requestCode, idTitle, idMessage, permissions);
    dialog.show(parent.getFragmentManager(), TAG);
    return dialog;
  }

  public static MessageDialogFragment showDialog(final Fragment parent, final int requestCode,
                                                 final int idTitle, final int idMessage,
                                                 final String[] permissions) {
    final MessageDialogFragment dialog =
        newInstance(requestCode, idTitle, idMessage, permissions);
    dialog.setTargetFragment(parent, parent.getId());
    dialog.show(parent.getFragmentManager(), TAG);
    return dialog;
  }

  public static MessageDialogFragment newInstance(final int requestCode, final int idTitle,
                                                  final int idMessage,
                                                  final String[] permissions) {
    final MessageDialogFragment fragment = new MessageDialogFragment();
    final Bundle args = new Bundle();
    args.putInt("requestCode", requestCode);
    args.putInt("title", idTitle);
    args.putInt("message", idMessage);
    args.putStringArray("permissions", permissions != null ? permissions : new String[] {});
    fragment.setArguments(args);
    return fragment;
  }

  private MessageDialogListener mDialogListener;

  public MessageDialogFragment() {
    super();
  }

  @SuppressLint("NewApi")
  @Override
  public void onAttach(final Activity activity) {
    super.onAttach(activity);
    //获取回调接口
    if (activity instanceof MessageDialogListener) {
      mDialogListener = (MessageDialogListener) activity;
    }
    if (mDialogListener == null) {
      final Fragment fragment = getTargetFragment();
      if (fragment instanceof MessageDialogListener) {
        mDialogListener = (MessageDialogListener) fragment;
      }
    }
    if (mDialogListener == null) {
      if (BuildCheck.isAndroid42()) {
        final Fragment target = getParentFragment();
        if (target instanceof MessageDialogListener) {
          mDialogListener = (MessageDialogListener) target;
        }
      }
    }
    if (mDialogListener == null) {
      throw new ClassCastException(activity.toString());
    }
  }


  @Override
  public Dialog onCreateDialog(final Bundle savedInstanceState) {
    final Bundle args = savedInstanceState != null ? savedInstanceState : getArguments();
    final int requestCode = getArguments().getInt("requestCode");
    final int idTitle = getArguments().getInt("title");
    final int idMessage = getArguments().getInt("message");
    final String[] permissions = args.getStringArray("permissions");

    return new AlertDialog.Builder(getActivity())
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setTitle(idTitle)
        .setMessage(idMessage)
        .setPositiveButton(android.R.string.ok,
            new DialogInterface.OnClickListener() {
              @Override
              public void onClick(final DialogInterface dialog, final int whichButton) {
                try {
                  mDialogListener.onMessageDialogResult(MessageDialogFragment.this, requestCode,
                      permissions, true);
                } catch (final Exception e) {
                  Log.w(TAG, e);
                }
              }
            }
        )
        .setNegativeButton(android.R.string.cancel,
            new DialogInterface.OnClickListener() {
              @Override
              public void onClick(final DialogInterface dialog, int whichButton) {
                try {
                  mDialogListener.onMessageDialogResult(MessageDialogFragment.this, requestCode,
                      permissions, false);
                } catch (final Exception e) {
                  Log.w(TAG, e);
                }
              }
            }
        )
        .create();
  }
}
