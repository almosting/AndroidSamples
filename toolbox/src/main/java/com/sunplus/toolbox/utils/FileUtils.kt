package com.sunplus.toolbox.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.GregorianCalendar
import java.util.Locale

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
object FileUtils {
  private val TAG: String? = "FileUtils"
  @JvmField var DIR_NAME: String = "UsbWebCamera"
  private val mDateTimeFormat: SimpleDateFormat? =
    SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US)

  fun getDirName(): String {
    return if (TextUtils.isEmpty(DIR_NAME)) "Serenegiant" else DIR_NAME
  }

  /**
   * 生成用于捕获的文件名
   *
   * @param type Environment.DIRECTORY_MOVIES / Environment.DIRECTORY_DCIM
   * @param ext .mp4 .png或.jpeg
   * @return 如果无法写入，则返回null
   */
  fun getCaptureFile(
    context: Context?,
    type: String?, ext: String?,
    save_tree_id: Int
  ): File? {
    return getCaptureFile(
      context,
      type,
      null,
      ext,
      save_tree_id
    )
  }

  fun getCaptureFile(
    context: Context?,
    type: String?, prefix: String?, ext: String?,
    save_tree_id: Int
  ): File? {

    // 生成保存目标文件名
    var result: File? = null
    val file_name =
      (if (TextUtils.isEmpty(prefix)) getDateTimeString() else prefix + getDateTimeString()) + ext
    if (save_tree_id > 0 && SDUtils.hasStorageAccess(context!!, save_tree_id)) {
      //			result = SDUtils.createStorageFile(context, save_tree_id, "*/*", file_name);
      result = SDUtils.createStorageDir(context, save_tree_id)
      if (result == null || !result.canWrite()) {
        Log.w(TAG, "I can not write it.")
        result = null
      }
      if (result != null) {
        result = File(result, getDirName())
      }
    }
    if (result == null) {
      // 回退到主外部存储（缺少WRITE_EXTERNAL_STORAGE失败）
      val dir = getCaptureDir(context, type, 0)
      if (dir != null) {
        dir.mkdirs()
        if (dir.canWrite()) {
          result = dir
        }
      }
    }
    if (result != null) {
      result = File(result, file_name)
    }
    //		Log.i(TAG, "getCaptureFile:result=" + result);
    return result
  }

  @SuppressLint("NewApi") fun getCaptureDir(
    context: Context?,
    type: String?, save_tree_id: Int
  ): File? {

    //		Log.i(TAG, "getCaptureDir:save_tree_id=" + save_tree_id + ", context=" + context);
    var result: File? = null
    if (save_tree_id > 0 && SDUtils.hasStorageAccess(context!!, save_tree_id)) {
      result = SDUtils.createStorageDir(context, save_tree_id)
      //			Log.i(TAG, "getCaptureDir:createStorageDir=" + result);
    }
    val dir = if (result != null) File(
      result,
      getDirName()
    ) else File(
      Environment.getExternalStoragePublicDirectory(type),
      getDirName()
    )
    dir.mkdirs() //在Nexus 5中，如果路径根本不存在，则该值不会正确返回，因此会生成路径
    //		Log.i(TAG, "getCaptureDir:" + result);
    return if (dir.canWrite()) {
      dir
    } else null
  }

  /**
   * 获取表示当前日期和时间的字符串
   */
  fun getDateTimeString(): String? {
    val now = GregorianCalendar()
    return mDateTimeFormat?.format(now.time)
  }

  fun getExternalMounts(): String? {
    var externalpath: String? = null
    var internalpath = ""
    val runtime = Runtime.getRuntime()
    try {
      var line: String?
      val proc = runtime.exec("mount")
      val br = BufferedReader(
        InputStreamReader(proc.inputStream)
      )
      while (br.readLine().also { line = it } != null) {
        //    			Log.i(TAG, "getExternalMounts:" + line);
        if (line!!.contains("secure")) {
          continue
        }
        if (line!!.contains("asec")) {
          continue
        }
        if (line!!.contains("fat")) { //external card
          val columns: Array<String?> = line!!.split(" ".toRegex()).toTypedArray()
          if (columns.size > 1 && !TextUtils.isEmpty(columns[1])) {
            externalpath = columns[1]
            if (!externalpath!!.endsWith("/")) {
              externalpath = "$externalpath/"
            }
          }
        } else if (line!!.contains("fuse")) { //internal storage
          val columns: Array<String?> = line!!.split(" ".toRegex()).toTypedArray()
          if (columns.size > 1) {
            internalpath = internalpath + "[" + columns[1] + "]"
          }
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    //		Log.i(TAG, "Path of sd card external: " + externalpath);
    //		Log.i(TAG, "Path of internal memory: " + internalpath);
    return externalpath
  }

  // 对外部存储空间的限制（假设为每分钟10 MB，实际为7到8 MB）
  var FREE_RATIO = 0.03f // 如果可用空间大于3％
  var FREE_SIZE_OFFSET = 20 * 1024 * 1024.toFloat()
  var FREE_SIZE = 300 * 1024 * 1024 // 如果可用空间超过300 MB，则确定
    .toFloat()
  var FREE_SIZE_MINUTE = 40 * 1024 * 1024 // 每分钟的电影容量（因为它在5 Mbps时约为38 MB）
    .toFloat()
  var CHECK_INTERVAL = 45 * 1000L // 可用空间，EOS检查间隔[毫秒]（= 45秒）

  /**
   * 检索存储信息
   *
   * @return 如果无法访问，则为null
   */
  fun getStorageInfo(
    context: Context?,
    type: String, save_tree_id: Int
  ): StorageInfo? {
    if (context != null) {
      try {
        // 可以写入外部存储区域
        // 如果没有外部存储权限，则返回null
        val dir =
          getCaptureDir(context, type, save_tree_id)
        //					Log.i(TAG, "checkFreeSpace:dir=" + dir);
        if (dir != null) {
          val freeSpace: Float = (if (dir.canWrite()) dir.usableSpace else 0.toFloat()) as Float
          return StorageInfo(dir.totalSpace, freeSpace as Long)
        }
      } catch (e: Exception) {
        Log.w("getStorageInfo:", e)
      }
    }
    return null
  }

  /**
   * 检查主外部存储上的可用空间
   * 如果主外部存储空间至少为FREE_RATIO（5％）且高于FREE_SIZE（20 MB），则返回true
   *
   * @return 使用可能であればtrue
   */
  fun checkFreeSpace(
    context: Context?,
    max_duration: Long, start_time: Long,
    save_tree_id: Int
  ): Boolean {
    //		Log.i(TAG, "checkFreeSpace:save_tree_id=" + save_tree_id + ", context=" + context);
    return if (context == null) {
      false
    } else checkFreeSpace(
      context,
      FREE_RATIO,
      if (max_duration > 0 // 设置最大录制时间时
      ) (max_duration - (System.currentTimeMillis() - start_time)) / 60000f
          * FREE_SIZE_MINUTE + FREE_SIZE_OFFSET else FREE_SIZE,
      save_tree_id
    )
  }

  /**
   * 检查主外部存储上的可用空间
   *
   * @param ratio 可用空间百分比（0 - 1）
   * @param minFree 最小可用空间[字节]
   * @return 如果可用则为真
   */
  fun checkFreeSpace(
    context: Context?,
    ratio: Float, minFree: Float,
    saveTreeId: Int
  ): Boolean {
    if (context == null) {
      return false
    }
    var result = false
    try {
      //			Log.v("checkFreeSpace", "getExternalStorageState=" + Environment.getExternalStorageState());
      //			final String state = Environment.getExternalStorageState();
      //			if (Environment.MEDIA_MOUNTED.equals(state) ||
      //				!Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
      // 可以写入外部存储区域
      // 如果没有外部存储权限，则返回null
      val dir = getCaptureDir(
        context,
        Environment.DIRECTORY_DCIM,
        saveTreeId
      )
      //				Log.i(TAG, "checkFreeSpace:dir=" + dir);
      if (dir != null) {
        val freeSpace: Float = (if (dir.canWrite()) dir.usableSpace else 0.toFloat()) as Float
        if (dir.totalSpace > 0) {
          result = freeSpace / dir.totalSpace > ratio || freeSpace > minFree
        }
      }
      //				Log.v("checkFreeSpace:", "freeSpace=" + freeSpace);
      //				Log.v("checkFreeSpace:", "getTotalSpace=" + dir.getTotalSpace());
      //				Log.v("checkFreeSpace:", "result=" + result);
      //			}
    } catch (e: Exception) {
      Log.w("checkFreeSpace:", e)
    }
    return result
  }

  /**
   * 获得可用空间
   *
   * @param type Environment.DIRECTORY_DCIM等
   */
  fun getAvailableFreeSpace(
    context: Context?,
    type: String?, saveTreeId: Int
  ): Long {
    var result: Long = 0
    if (context != null) {
      val dir =
        getCaptureDir(context, type, saveTreeId)
      if (dir != null) {
        result = if (dir.canWrite()) dir.usableSpace else 0
      }
    }
    return result
  }

  /**
   * 获得可用空间的百分比
   *
   * @param type Environment.DIRECTORY_DCIM等
   */
  fun getFreeRatio(
    context: Context?,
    type: String?, saveTreeId: Int
  ): Float {
    if (context != null) {
      val dir =
        getCaptureDir(context, type, saveTreeId)
      if (dir != null) {
        val freeSpace: Float = (if (dir.canWrite()) dir.usableSpace else 0.toFloat()) as Float
        if (dir.totalSpace > 0) {
          return freeSpace / dir.totalSpace
        }
      }
    }
    return 0F
  }

  /**
   * 删除文件名后缀
   */
  fun removeFileExtension(path: String?): String? {
    val ix = if (!TextUtils.isEmpty(path)) path!!.lastIndexOf(".") else -1
    return if (ix > 0) {
      path!!.substring(0, ix)
    } else {
      path
    }
  }

  /**
   * 替换文件名末尾的后缀
   * 如果path为null或为空，则不执行任何操作
   * 如果没有扩展名，则授予新的扩展名
   *
   * @param newExt 点扩展字符串
   */
  fun replaceFileExtension(
    path: String?,
    newExt: String
  ): String? {
    if (!TextUtils.isEmpty(path)) {
      val ix = path!!.lastIndexOf(".")
      return if (ix > 0) {
        path.substring(0, ix) + newExt
      } else {
        path + newExt
      }
    }
    return path
  }
}