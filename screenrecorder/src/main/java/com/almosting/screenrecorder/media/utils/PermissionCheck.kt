package com.almosting.toolbox.utils

import android.Manifest.permission
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.net.Uri
import android.provider.Settings
import android.util.Log

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
object PermissionCheck {
  fun dumpPermissions(context: Context?) {
    if (context == null) {
      return
    }
    try {
      val pm = context.packageManager
      val list =
        pm.getAllPermissionGroups(PackageManager.GET_META_DATA)
      for (info in list) {
        Log.d("PermissionCheck", info.name)
      }
    } catch (e: Exception) {
      Log.w("", e)
    }
  }

  /**
   * 确认权限
   *
   * @return 如果指定的权限存在，则为True
   */
  @SuppressLint("NewApi") fun hasPermission(
    context: Context?,
    permissionName: String?
  ): Boolean {
    if (context == null) {
      return false
    }
    var result = false
    try {
      val check: Int
      check = if (BuildCheck.isMarshmallow()) {
        context.checkSelfPermission(permissionName!!)
      } else {
        val pm = context.packageManager
        pm.checkPermission(permissionName, context.packageName)
      }
      when (check) {
        PackageManager.PERMISSION_DENIED -> {
        }
        PackageManager.PERMISSION_GRANTED -> result = true
        else -> {
        }
      }
    } catch (e: Exception) {
      Log.w("", e)
    }
    return result
  }

  /**
   * 确认是否有录音任务
   *
   * @return 如果有录音权限，则为真
   */
  fun hasAudio(context: Context?): Boolean {
    return hasPermission(context, permission.RECORD_AUDIO)
  }

  /**
   * 检查您是否有权访问网络
   *
   * @return 如果存在对网络的访问权限，则为True
   */
  fun hasNetwork(context: Context?): Boolean {
    return hasPermission(context, permission.INTERNET)
  }

  /**
   * 检查是否有对外部存储的写入权限
   *
   * @return 如果存在对外部存储的写入权限，则为True
   */
  fun hasWriteExternalStorage(context: Context?): Boolean {
    return hasPermission(
      context,
      permission.WRITE_EXTERNAL_STORAGE
    )
  }

  /**
   * 检查您是否具有外部存储的读取权限
   *
   * @return 如果有外部存储的读取权限，则为True
   */
  @SuppressLint("InlinedApi")
  fun hasReadExternalStorage(context: Context?): Boolean {
    return if (BuildCheck.isAndroid4()) {
      hasPermission(
        context,
        permission.READ_EXTERNAL_STORAGE
      )
    } else {
      hasPermission(
        context,
        permission.WRITE_EXTERNAL_STORAGE
      )
    }
  }

  /**
   * 检查您是否有权访问位置信息
   */
  fun hasAccessLocation(context: Context?): Boolean {
    return (hasPermission(
      context,
      permission.ACCESS_COARSE_LOCATION
    )
        && hasPermission(context, permission.ACCESS_FINE_LOCATION))
  }

  /**
   * 确认是否有低精度位置信息访问权限
   */
  fun hasAccessCoarseLocation(context: Context?): Boolean {
    return hasPermission(
      context,
      permission.ACCESS_COARSE_LOCATION
    )
  }

  /**
   * 检查您是否具有高精度的位置信息访问权限
   */
  fun hasAccessFineLocation(context: Context?): Boolean {
    return hasPermission(context, permission.ACCESS_FINE_LOCATION)
  }

  /**
   * 是否可以访问相机
   */
  fun hasCamera(context: Context?): Boolean {
    return hasPermission(context, permission.CAMERA)
  }

  /**
   * 移至详细的申请设定（不能获得许可等）
   */
  fun openSettings(context: Context?) {
    val intent =
      Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri = Uri.fromParts("package", context?.getPackageName(), null)
    intent.data = uri
    context?.startActivity(intent)
  }

  /**
   * AndroidManifest.xml检查应设置的权限
   *
   * @return 如果它是一个空列表，则包括所有权限
   * @throws IllegalArgumentException
   * @throws PackageManager.NameNotFoundException
   */
  @Throws(
    IllegalArgumentException::class,
    NameNotFoundException::class
  ) fun missingPermissions(
    context: Context?,
    expectations: MutableList<String>
  ): MutableList<String> {
    require(!(context == null)) { "context or expectations is null" }
    val pm = context.packageManager
    val pi =
      pm.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
    val info = pi.requestedPermissions
    if (info != null) {
      for (i in info) {
        expectations.remove(i)
      }
    }
    return expectations
  }
}