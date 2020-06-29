package com.sunplus.easypermissions;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import android.text.TextUtils;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

/**
 * Created by w.feng on 2018/10/10
 * Email: fengweisb@gmail.com
 */
public class AppSettingsDialog implements Parcelable {

  public static final int DEFAULT_SETTINGS_REQ_CODE = 16061;

  static final String EXTRA_APP_SETTINGS = "extra_app_settings";

  @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
  public static final Parcelable.Creator<AppSettingsDialog> CREATOR =
      new Parcelable.Creator<AppSettingsDialog>() {

        @Override
        public AppSettingsDialog createFromParcel(Parcel source) {
          return new AppSettingsDialog(source);
        }

        @Override
        public AppSettingsDialog[] newArray(int size) {
          return new AppSettingsDialog[size];
        }
      };

  @StyleRes
  private final int mThemeResId;
  private final String mRationale;
  private final String mTitle;
  private final String mPositiveButtonText;
  private final String mNegativeButtonText;
  private final int mRequestCode;
  private final int mIntentFlags;

  private Object mActivityOrFragment;
  private Context mContext;

  private AppSettingsDialog(Parcel source) {
    mThemeResId = source.readInt();
    mRationale = source.readString();
    mTitle = source.readString();
    mPositiveButtonText = source.readString();
    mNegativeButtonText = source.readString();
    mRequestCode = source.readInt();
    mIntentFlags = source.readInt();
  }


  private AppSettingsDialog(@NonNull final Object activityOrFragment,
                            @StyleRes int themeResId,
                            @Nullable String rationale,
                            @Nullable String title,
                            @Nullable String positiveButtonText,
                            @Nullable String negativeButtonText,
                            int requestCode,
                            int intentFlags) {
    setActivityOrFragment(activityOrFragment);
    mThemeResId = themeResId;
    mRationale = rationale;
    mTitle = title;
    mPositiveButtonText = positiveButtonText;
    mNegativeButtonText = negativeButtonText;
    mRequestCode = requestCode;
    mIntentFlags = intentFlags;
  }

  static AppSettingsDialog fromIntent(Intent intent, Activity activity) {
    AppSettingsDialog dialog = intent.getParcelableExtra(AppSettingsDialog.EXTRA_APP_SETTINGS);
    dialog.setActivityOrFragment(activity);
    return dialog;
  }

  private void setActivityOrFragment(Object activityOrFragment) {
    mActivityOrFragment = activityOrFragment;

    if (activityOrFragment instanceof Activity) {
      mContext = (Activity) activityOrFragment;
    } else if (activityOrFragment instanceof Fragment) {
      mContext = ((Fragment) activityOrFragment).getContext();
    } else {
      throw new IllegalStateException("Unknown object: " + activityOrFragment);
    }
  }


  private void startForResult(Intent intent) {
    if (mActivityOrFragment instanceof Activity) {
      ((Activity) mActivityOrFragment).startActivityForResult(intent, mRequestCode);
    } else if (mActivityOrFragment instanceof Fragment) {
      ((Fragment) mActivityOrFragment).startActivityForResult(intent, mRequestCode);
    }
  }

  public void show() {
    startForResult(AppSettingsDialogHolderActivity.createShowDialogIntent(mContext, this));
  }

  @SuppressLint("ResourceType") AlertDialog showDialog(DialogInterface.OnClickListener positiveListener,
                         DialogInterface.OnClickListener negativeListener) {
    AlertDialog.Builder builder;
    if (mThemeResId > 0) {
      builder = new AlertDialog.Builder(mContext, mThemeResId);
    } else {
      builder = new AlertDialog.Builder(mContext);
    }
    return builder
        .setCancelable(false)
        .setTitle(mTitle)
        .setMessage(mRationale)
        .setPositiveButton(mPositiveButtonText, positiveListener)
        .setNegativeButton(mNegativeButtonText, negativeListener)
        .show();
  }


  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(mThemeResId);
    dest.writeString(mRationale);
    dest.writeString(mTitle);
    dest.writeString(mPositiveButtonText);
    dest.writeString(mNegativeButtonText);
    dest.writeInt(mRequestCode);
    dest.writeInt(mIntentFlags);
  }

  int getIntentFlags() {
    return mIntentFlags;
  }

  public static class Builder {

    private final Object mActivityOrFragment;
    private final Context mContext;
    @StyleRes
    private int mThemeResId = -1;
    private String mRationale;
    private String mTitle;
    private String mPositiveButtonText;
    private String mNegativeButtonText;
    private int mRequestCode = -1;
    private boolean mOpenInNewTask = false;

    public Builder(@NonNull Activity activity) {
      mActivityOrFragment = activity;
      mContext = activity;
    }

    public Builder(@NonNull Fragment fragment) {
      mActivityOrFragment = fragment;
      mContext = fragment.getContext();
    }


    @NonNull
    public Builder setThemeResId(@StyleRes int themeResId) {
      mThemeResId = themeResId;
      return this;
    }


    @NonNull
    public Builder setTitle(@Nullable String title) {
      mTitle = title;
      return this;
    }


    @NonNull
    public Builder setTitle(@StringRes int title) {
      mTitle = mContext.getString(title);
      return this;
    }

    @NonNull
    public Builder setRationale(@Nullable String rationale) {
      mRationale = rationale;
      return this;
    }


    @NonNull
    public Builder setRationale(@StringRes int rationale) {
      mRationale = mContext.getString(rationale);
      return this;
    }


    @NonNull
    public Builder setPositiveButton(@Nullable String text) {
      mPositiveButtonText = text;
      return this;
    }


    @NonNull
    public Builder setPositiveButton(@StringRes int textId) {
      mPositiveButtonText = mContext.getString(textId);
      return this;
    }


    @NonNull
    public Builder setNegativeButton(@Nullable String text) {
      mNegativeButtonText = text;
      return this;
    }


    @NonNull
    public Builder setNegativeButton(@StringRes int textId) {
      mNegativeButtonText = mContext.getString(textId);
      return this;
    }

    @NonNull
    public Builder setRequestCode(int requestCode) {
      mRequestCode = requestCode;
      return this;
    }


    @NonNull
    public Builder setOpenInNewTask(boolean openInNewTask) {
      mOpenInNewTask = openInNewTask;
      return this;
    }


    @NonNull
    public AppSettingsDialog build() {
      mRationale = TextUtils.isEmpty(mRationale) ?
          mContext.getString(R.string.rationale_ask_again) : mRationale;
      mTitle = TextUtils.isEmpty(mTitle) ?
          mContext.getString(R.string.title_settings_dialog) : mTitle;
      mPositiveButtonText = TextUtils.isEmpty(mPositiveButtonText) ?
          mContext.getString(android.R.string.ok) : mPositiveButtonText;
      mNegativeButtonText = TextUtils.isEmpty(mNegativeButtonText) ?
          mContext.getString(android.R.string.cancel) : mNegativeButtonText;
      mRequestCode = mRequestCode > 0 ? mRequestCode : DEFAULT_SETTINGS_REQ_CODE;

      int intentFlags = 0;
      if (mOpenInNewTask) {
        intentFlags |= Intent.FLAG_ACTIVITY_NEW_TASK;
      }

      return new AppSettingsDialog(
          mActivityOrFragment,
          mThemeResId,
          mRationale,
          mTitle,
          mPositiveButtonText,
          mNegativeButtonText,
          mRequestCode,
          intentFlags);
    }

  }

}
