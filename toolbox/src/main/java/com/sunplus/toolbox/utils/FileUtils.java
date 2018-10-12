package com.sunplus.toolbox.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
public class FileUtils {
  private static final String TAG = "FileUtils";
  public static String DIR_NAME = "UsbWebCamera";
  private static final SimpleDateFormat mDateTimeFormat =
      new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);

  @NonNull
  public static String getDirName() {
    return TextUtils.isEmpty(DIR_NAME)
        ? "Serenegiant" : DIR_NAME;
  }

  /**
   * 生成用于捕获的文件名
   *
   * @param type Environment.DIRECTORY_MOVIES / Environment.DIRECTORY_DCIM
   * @param ext .mp4 .png或.jpeg
   * @return 如果无法写入，则返回null
   */
  public static final File getCaptureFile(final Context context,
                                          final String type, final String ext,
                                          final int save_tree_id) {

    return getCaptureFile(context, type, null, ext, save_tree_id);
  }

  public static final File getCaptureFile(final Context context,
                                          final String type, final String prefix, final String ext,
                                          final int save_tree_id) {

    // 生成保存目标文件名
    File result = null;
    final String file_name =
        (TextUtils.isEmpty(prefix) ? getDateTimeString() : prefix + getDateTimeString()) + ext;
    if ((save_tree_id > 0) && SDUtils.hasStorageAccess(context, save_tree_id)) {
      //			result = SDUtils.createStorageFile(context, save_tree_id, "*/*", file_name);
      result = SDUtils.createStorageDir(context, save_tree_id);
      if ((result == null) || !result.canWrite()) {
        Log.w(TAG, "I can not write it.");
        result = null;
      }
      if (result != null) {
        result = new File(result, getDirName());
      }
    }
    if (result == null) {
      // 回退到主外部存储（缺少WRITE_EXTERNAL_STORAGE失败）
      final File dir = getCaptureDir(context, type, 0);
      if (dir != null) {
        dir.mkdirs();
        if (dir.canWrite()) {
          result = dir;
        }
      }
    }
    if (result != null) {
      result = new File(result, file_name);
    }
    //		Log.i(TAG, "getCaptureFile:result=" + result);
    return result;
  }

  @SuppressLint("NewApi")
  public static final File getCaptureDir(final Context context,
                                         final String type, final int save_tree_id) {

    //		Log.i(TAG, "getCaptureDir:save_tree_id=" + save_tree_id + ", context=" + context);
    File result = null;
    if ((save_tree_id > 0) && SDUtils.hasStorageAccess(context, save_tree_id)) {
      result = SDUtils.createStorageDir(context, save_tree_id);
      //			Log.i(TAG, "getCaptureDir:createStorageDir=" + result);
    }
    final File dir = result != null
        ? new File(result, getDirName())
        : new File(Environment.getExternalStoragePublicDirectory(type), getDirName());
    dir.mkdirs();  //在Nexus 5中，如果路径根本不存在，则该值不会正确返回，因此会生成路径
    //		Log.i(TAG, "getCaptureDir:" + result);
    if (dir.canWrite()) {
      return dir;
    }
    return null;
  }

  /**
   * 获取表示当前日期和时间的字符串
   */
  public static final String getDateTimeString() {
    final GregorianCalendar now = new GregorianCalendar();
    return mDateTimeFormat.format(now.getTime());
  }

  public static String getExternalMounts() {
    String externalpath = null;
    String internalpath = "";

    final Runtime runtime = Runtime.getRuntime();
    try {
      String line;
      final Process proc = runtime.exec("mount");
      final BufferedReader br = new BufferedReader(
          new InputStreamReader(proc.getInputStream()));
      while ((line = br.readLine()) != null) {
        //    			Log.i(TAG, "getExternalMounts:" + line);
        if (line.contains("secure")) {
          continue;
        }
        if (line.contains("asec")) {
          continue;
        }

        if (line.contains("fat")) {//external card
          final String columns[] = line.split(" ");
          if (columns.length > 1 && !TextUtils.isEmpty(columns[1])) {
            externalpath = columns[1];
            if (!externalpath.endsWith("/")) {
              externalpath = externalpath + "/";
            }
          }
        } else if (line.contains("fuse")) {//internal storage
          final String columns[] = line.split(" ");
          if (columns.length > 1) {
            internalpath = internalpath.concat("[" + columns[1] + "]");
          }
        }
      }
    } catch (final Exception e) {
      e.printStackTrace();
    }
    //		Log.i(TAG, "Path of sd card external: " + externalpath);
    //		Log.i(TAG, "Path of internal memory: " + internalpath);
    return externalpath;
  }

  // 对外部存储空间的限制（假设为每分钟10 MB，实际为7到8 MB）
  public static float FREE_RATIO = 0.03f;          // 如果可用空间大于3％
  public static float FREE_SIZE_OFFSET = 20 * 1024 * 1024;
  public static float FREE_SIZE = 300 * 1024 * 1024;    // 如果可用空间超过300 MB，则确定
  public static float FREE_SIZE_MINUTE = 40 * 1024 * 1024;  // 每分钟的电影容量（因为它在5 Mbps时约为38 MB）
  public static long CHECK_INTERVAL = 45 * 1000L;  // 可用空间，EOS检查间隔[毫秒]（= 45秒）

  /**
   * 检索存储信息
   *
   * @return 如果无法访问，则为null
   */
  @Nullable
  public static StorageInfo getStorageInfo(final Context context,
                                           @NonNull final String type, final int save_tree_id) {

    if (context != null) {
      try {
        // 可以写入外部存储区域
        // 如果没有外部存储权限，则返回null
        final File dir = getCaptureDir(context, type, save_tree_id);
        //					Log.i(TAG, "checkFreeSpace:dir=" + dir);
        if (dir != null) {
          final float freeSpace = dir.canWrite() ? dir.getUsableSpace() : 0;
          return new StorageInfo(dir.getTotalSpace(), (long) freeSpace);
        }
      } catch (final Exception e) {
        Log.w("getStorageInfo:", e);
      }
    }
    return null;
  }

  /**
   * 检查主外部存储上的可用空间
   * 如果主外部存储空间至少为FREE_RATIO（5％）且高于FREE_SIZE（20 MB），则返回true
   *
   * @return 使用可能であればtrue
   */
  public static final boolean checkFreeSpace(final Context context,
                                             final long max_duration, final long start_time,
                                             final int save_tree_id) {
    //		Log.i(TAG, "checkFreeSpace:save_tree_id=" + save_tree_id + ", context=" + context);
    if (context == null) {
      return false;
    }
    return checkFreeSpace(context, FREE_RATIO,
        max_duration > 0  // 设置最大录制时间时
            ? (max_duration - (System.currentTimeMillis() - start_time)) / 60000.f
            * FREE_SIZE_MINUTE + FREE_SIZE_OFFSET
            : FREE_SIZE, save_tree_id);
  }

  /**
   * 检查主外部存储上的可用空间
   *
   * @param ratio 可用空间百分比（0 - 1）
   * @param minFree 最小可用空间[字节]
   * @return 如果可用则为真
   */
  public static boolean checkFreeSpace(final Context context,
                                       final float ratio, final float minFree,
                                       final int saveTreeId) {


    if (context == null) {
      return false;
    }
    boolean result = false;
    try {
      //			Log.v("checkFreeSpace", "getExternalStorageState=" + Environment.getExternalStorageState());
      //			final String state = Environment.getExternalStorageState();
      //			if (Environment.MEDIA_MOUNTED.equals(state) ||
      //				!Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
      // 可以写入外部存储区域
      // 如果没有外部存储权限，则返回null
      final File dir = getCaptureDir(context, Environment.DIRECTORY_DCIM, saveTreeId);
      //				Log.i(TAG, "checkFreeSpace:dir=" + dir);
      if (dir != null) {
        final float freeSpace = dir.canWrite() ? dir.getUsableSpace() : 0;
        if (dir.getTotalSpace() > 0) {
          result = (freeSpace / dir.getTotalSpace() > ratio) || (freeSpace > minFree);
        }
      }
      //				Log.v("checkFreeSpace:", "freeSpace=" + freeSpace);
      //				Log.v("checkFreeSpace:", "getTotalSpace=" + dir.getTotalSpace());
      //				Log.v("checkFreeSpace:", "result=" + result);
      //			}
    } catch (final Exception e) {
      Log.w("checkFreeSpace:", e);
    }
    return result;
  }

  /**
   * 获得可用空间
   *
   * @param type Environment.DIRECTORY_DCIM等
   */
  public static long getAvailableFreeSpace(final Context context,
                                           final String type, final int saveTreeId) {

    long result = 0;
    if (context != null) {
      final File dir = getCaptureDir(context, type, saveTreeId);
      if (dir != null) {
        result = dir.canWrite() ? dir.getUsableSpace() : 0;
      }
    }
    return result;
  }

  /**
   * 获得可用空间的百分比
   *
   * @param type Environment.DIRECTORY_DCIM等
   */
  public static float getFreeRatio(final Context context,
                                   final String type, final int saveTreeId) {

    if (context != null) {
      final File dir = getCaptureDir(context, type, saveTreeId);
      if (dir != null) {
        final float freeSpace = dir.canWrite() ? dir.getUsableSpace() : 0;
        if (dir.getTotalSpace() > 0) {
          return freeSpace / dir.getTotalSpace();
        }
      }
    }
    return 0;
  }

  /**
   * 删除文件名后缀
   */
  public static String removeFileExtension(final String path) {
    final int ix = !TextUtils.isEmpty(path) ? path.lastIndexOf(".") : -1;
    if (ix > 0) {
      return path.substring(0, ix);
    } else {
      return path;
    }
  }

  /**
   * 替换文件名末尾的后缀
   * 如果path为null或为空，则不执行任何操作
   * 如果没有扩展名，则授予新的扩展名
   *
   * @param newExt 点扩展字符串
   */
  public static String replaceFileExtension(final String path,
                                            @NonNull final String newExt) {
    if (!TextUtils.isEmpty(path)) {
      final int ix = path.lastIndexOf(".");
      if (ix > 0) {
        return path.substring(0, ix) + newExt;
      } else {
        return path + newExt;
      }
    }
    return path;
  }
}
