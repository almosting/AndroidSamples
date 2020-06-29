package com.almosting.toolbox.utils

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build.VERSION_CODES
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore.Audio
import android.provider.MediaStore.Images.Media
import android.provider.MediaStore.Video
import android.text.TextUtils
import android.util.Log
import java.io.File

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
object UriHelper {
  private const val DEBUG = false
  private val TAG = UriHelper::class.java.simpleName

  /**
   * 从Uri到Path的转换过程
   *
   * @return String 如果找不到路径，则为空
   */
  fun getAbsolutePath(cr: ContentResolver?, uri: Uri?): String? {
    var path: String? = null
    if (uri != null) {
      try {
        val columns =
          arrayOf<String?>(Media.DATA)
        val cursor = cr!!.query(uri, columns, null, null, null)
        if (cursor != null) {
          try {
            if (cursor.moveToFirst()) {
              path = cursor.getString(0)
            }
          } finally {
            cursor.close()
          }
        }
      } catch (e: Exception) {
        //				if (DEBUG) Log.w(TAG, e);
      }
    }
    return path
  }

  val STANDARD_DIRECTORIES: Array<String?>?
  fun isStandardDirectory(dir: String): Boolean {
    try {
      if (STANDARD_DIRECTORIES != null) {
        for (valid in STANDARD_DIRECTORIES) {
          if (valid == dir) {
            return true
          }
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, e)
    }
    return false
  }

  /**
   * 如果Uri可以转换为本地路径，则返回路径
   * 在二级存储的情况下，没有许可
   * 由于它不应该直接访问，只需要显示一个字符串
   *
   * @param context The context.
   * @param uri The Uri to query.
   * @author paulburke
   */
  @SuppressLint("NewApi")
  @TargetApi(VERSION_CODES.KITKAT)
  fun getPath(context: Context?, uri: Uri?): String? {
    if (DEBUG) {
      Log.i(TAG, "getPath:uri=$uri")
    }
    if (BuildCheck.isKitKat() && DocumentsContract.isDocumentUri(context, uri)) {
      // DocumentProvider
      if (DEBUG) {
        Log.i(
          TAG,
          "getPath:isDocumentUri,getAuthority=" + uri!!.getAuthority()
        )
      }
      // ExternalStorageProvider
      if (isExternalStorageDocument(uri!!)) {
        val docId = DocumentsContract.getDocumentId(uri)
        if (DEBUG) {
          Log.i(TAG, "getPath:isDocumentUri,docId=$docId")
        }
        if (BuildCheck.isLollipop() && DEBUG) {
          Log.i(
            TAG, "getPath:isDocumentUri,getTreeDocumentId="
                + DocumentsContract.getTreeDocumentId(uri)
          )
        }
        val split: Array<String?> = docId.split(":".toRegex()).toTypedArray()
        val type = split[0]
        if (DEBUG) {
          Log.i(TAG, "getPath:type=$type")
        }
        if (type != null) {
          if ("primary".equals(type, ignoreCase = true)) {
            val path =
              Environment.getExternalStorageDirectory().toString() + "/"
            return if (split.size > 1) path + split[1] else path
          } else if ("home".equals(type, ignoreCase = true)) {
            if (split.size > 1 && isStandardDirectory(split[1]!!)) {
              return Environment.getExternalStoragePublicDirectory(
                split[1]
              ).toString() + "/"
            }
            val path = Environment.getExternalStoragePublicDirectory(
              Environment.DIRECTORY_DOCUMENTS
            ).toString() + "/"
            return if (split.size > 1) path + split[1] else path
          } else {
            // 当它不是主存储时，它从前面按顺序搜索
            val primary =
              Environment.getExternalStorageDirectory().absolutePath
            if (DEBUG) {
              Log.i(TAG, "getPath:primary=$primary")
            }
            val dirs = context!!.getExternalFilesDirs(null)
            val n = dirs?.size ?: 0
            val sb = StringBuilder()
            for (i in 0 until n) {
              val dir = dirs[i]
              if (DEBUG) {
                Log.i(TAG, "getPath:$i)dir=$dir")
              }
              if (dir != null && dir.absolutePath.startsWith(primary)) {
                // 跳过主存储
                continue
              }
              val dirPath = dir?.absolutePath
              if (!TextUtils.isEmpty(dirPath)) {
                val dirElements: Array<String?> =
                  dirPath!!.split("/".toRegex()).toTypedArray()
                val m = dirElements.size
                if (m > 2 && "storage".equals(dirElements[1], ignoreCase = true)
                  && type.equals(dirElements[2], ignoreCase = true)
                ) {
                  var found = false
                  sb.setLength(0)
                  sb.append('/').append(dirElements[1])
                  for (j in 2 until m) {
                    if ("Android".equals(dirElements[j], ignoreCase = true)) {
                      found = true
                      break
                    }
                    sb.append('/').append(dirElements[j])
                  }
                  if (found) {
                    val path =
                      File(File(sb.toString()), split[1])
                    if (DEBUG) {
                      Log.i(TAG, "getPath:path=$path")
                    }
                    // 它不知道是否可以读取，读取或写入找到的路径
                    return path.absolutePath
                  }
                }
              }
            }
          }
        } else {
          Log.w(TAG, "unexpectedly type is null")
        }
      } else if (isDownloadsDocument(uri)) {
        // DownloadsProvider
        val id = DocumentsContract.getDocumentId(uri)
        val contentUri = ContentUris.withAppendedId(
          Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(id)
        )
        return getDataColumn(context!!, contentUri, null, null)
      } else if (isMediaDocument(uri)) {
        // MediaProvider
        val docId = DocumentsContract.getDocumentId(uri)
        val split: Array<String?> = docId.split(":".toRegex()).toTypedArray()
        val type = split[0]
        var contentUri: Uri? = null
        if ("image" == type) {
          contentUri = Media.EXTERNAL_CONTENT_URI
        } else if ("video" == type) {
          contentUri = Video.Media.EXTERNAL_CONTENT_URI
        } else if ("audio" == type) {
          contentUri = Audio.Media.EXTERNAL_CONTENT_URI
        }
        if (contentUri != null) {
          val selection = "_id=?"
          val selectionArgs = arrayOf(split[1])
          return getDataColumn(context!!, contentUri, selection, selectionArgs)
        }
      }
    } else if (uri != null) {
      if ("content".equals(uri.scheme, ignoreCase = true)) {
        // MediaStore (and general)
        return if (isGooglePhotosUri(uri)) {
          uri.lastPathSegment
        } else getDataColumn(context!!, uri, null, null)
      } else if ("file".equals(uri.scheme, ignoreCase = true)) {
        // File
        return uri.path
      }
    }
    Log.w(TAG, "unexpectedly not found,uri=$uri")
    return null
  }

  /**
   * Get the value of the data column for this Uri. This is useful for
   * MediaStore Uris, and other file-based ContentProviders.
   *
   * @param context The context.
   * @param uri The Uri to query.
   * @param selection (Optional) Filter used in the query.
   * @param selectionArgs (Optional) Selection arguments used in the query.
   * @return The value of the _data column, which is typically a file path.
   */
  fun getDataColumn(
    context: Context,
    uri: Uri, selection: String?,
    selectionArgs: Array<String?>?
  ): String? {
    var cursor: Cursor? = null
    val column = "_data"
    val projection = arrayOf<String?>(column)
    try {
      cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
      if (cursor != null && cursor.moveToFirst()) {
        val columnIndex = cursor.getColumnIndexOrThrow(column)
        return cursor.getString(columnIndex)
      }
    } finally {
      cursor?.close()
    }
    return null
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is ExternalStorageProvider.
   */
  fun isExternalStorageDocument(uri: Uri): Boolean {
    return "com.android.externalstorage.documents" == uri.authority
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is DownloadsProvider.
   */
  fun isDownloadsDocument(uri: Uri): Boolean {
    return "com.android.providers.downloads.documents" == uri.authority
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is MediaProvider.
   */
  fun isMediaDocument(uri: Uri): Boolean {
    return "com.android.providers.media.documents" == uri.authority
  }

  fun isGooglePhotosUri(uri: Uri): Boolean {
    return "com.google.android.apps.photos.content" == uri.authority
  }

  init {
    STANDARD_DIRECTORIES = arrayOf(
      Environment.DIRECTORY_MUSIC,
      Environment.DIRECTORY_PODCASTS,
      Environment.DIRECTORY_RINGTONES,
      Environment.DIRECTORY_ALARMS,
      Environment.DIRECTORY_NOTIFICATIONS,
      Environment.DIRECTORY_PICTURES,
      Environment.DIRECTORY_MOVIES,
      Environment.DIRECTORY_DOWNLOADS,
      Environment.DIRECTORY_DCIM,
      Environment.DIRECTORY_DOCUMENTS
    )
  }
}