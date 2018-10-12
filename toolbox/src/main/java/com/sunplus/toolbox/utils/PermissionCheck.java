package com.sunplus.toolbox.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
public final class PermissionCheck {
  public static void dumpPermissions(final Context context) {
    if (context == null) {
      return;
    }
    try {
      final PackageManager pm = context.getPackageManager();
      final List<PermissionGroupInfo> list =
          pm.getAllPermissionGroups(PackageManager.GET_META_DATA);
      for (final PermissionGroupInfo info : list) {
        Log.d("PermissionCheck", info.name);
      }
    } catch (final Exception e) {
      Log.w("", e);
    }
  }

  /**
   * 确认权限
   *
   * @return 如果指定的权限存在，则为True
   */
  @SuppressLint("NewApi")
  public static boolean hasPermission(final Context context, final String permissionName) {
    if (context == null) {
      return false;
    }
    boolean result = false;
    try {
      final int check;
      if (BuildCheck.isMarshmallow()) {
        check = context.checkSelfPermission(permissionName);
      } else {
        final PackageManager pm = context.getPackageManager();
        check = pm.checkPermission(permissionName, context.getPackageName());
      }
      switch (check) {
        case PackageManager.PERMISSION_DENIED:
          break;
        case PackageManager.PERMISSION_GRANTED:
          result = true;
          break;
        default:
          break;
      }
    } catch (final Exception e) {
      Log.w("", e);
    }
    return result;
  }

  /**
   * 确认是否有录音任务
   *
   * @return 如果有录音权限，则为真
   */
  public static boolean hasAudio(final Context context) {
    return hasPermission(context, Manifest.permission.RECORD_AUDIO);
  }

  /**
   * 检查您是否有权访问网络
   *
   * @return 如果存在对网络的访问权限，则为True
   */
  public static boolean hasNetwork(final Context context) {
    return hasPermission(context, Manifest.permission.INTERNET);
  }

  /**
   * 检查是否有对外部存储的写入权限
   *
   * @return 如果存在对外部存储的写入权限，则为True
   */
  public static boolean hasWriteExternalStorage(final Context context) {
    return hasPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
  }

  /**
   * 检查您是否具有外部存储的读取权限
   *
   * @return 如果有外部存储的读取权限，则为True
   */
  @SuppressLint("InlinedApi")
  public static boolean hasReadExternalStorage(final Context context) {
    if (BuildCheck.isAndroid4()) {
      return hasPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
    } else {
      return hasPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }
  }

  /**
   * 检查您是否有权访问位置信息
   */
  public static boolean hasAccessLocation(final Context context) {
    return hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        && hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
  }

  /**
   * 确认是否有低精度位置信息访问权限
   */
  public static boolean hasAccessCoarseLocation(final Context context) {
    return hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION);
  }

  /**
   * 检查您是否具有高精度的位置信息访问权限
   */
  public static boolean hasAccessFineLocation(final Context context) {
    return hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
  }

  /**
   * 是否可以访问相机
   */
  public static boolean hasCamera(final Context context) {
    return hasPermission(context, Manifest.permission.CAMERA);
  }

  /**
   * 移至详细的申请设定（不能获得许可等）
   */
  public static void openSettings(final Context context) {
    final Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
    final Uri uri = Uri.fromParts("package", context.getPackageName(), null);
    intent.setData(uri);
    context.startActivity(intent);
  }

  /**
   * AndroidManifest.xml检查应设置的权限
   *
   * @return 如果它是一个空列表，则包括所有权限
   * @throws IllegalArgumentException
   * @throws PackageManager.NameNotFoundException
   */
  public static List<String> missingPermissions(final Context context, final String[] expectations)
      throws IllegalArgumentException, PackageManager.NameNotFoundException {
    return missingPermissions(context, new ArrayList<String>(Arrays.asList(expectations)));
  }

  /**
   * AndroidManifest.xml检查应设置的权限
   *
   * @return 如果它是一个空列表，则包括所有权限
   * @throws IllegalArgumentException
   * @throws PackageManager.NameNotFoundException
   */
  public static List<String> missingPermissions(final Context context,
                                                final List<String> expectations)
      throws IllegalArgumentException, PackageManager.NameNotFoundException {
    if (context == null || expectations == null) {
      throw new IllegalArgumentException("context or expectations is null");
    }
    final PackageManager pm = context.getPackageManager();
    final PackageInfo pi =
        pm.getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
    final String[] info = pi.requestedPermissions;
    if (info != null) {
      for (String i : info) {
        expectations.remove(i);
      }
    }
    return expectations;
  }
}
