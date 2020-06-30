package com.almosting.toolbox.utils

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION_CODES
import android.os.ParcelFileDescriptor
import android.os.StatFs
import android.provider.DocumentsContract
import android.text.TextUtils
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.FragmentActivity
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.ArrayList
import java.util.Locale

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
object SDUtils {
  private val TAG: String? = "SDUtils"
  fun handleOnResult(
    context: Context,
    requestCode: Int, resultCode: Int,
    data: Intent?, delegater: handleOnResultDelegater
  ): Boolean {
    if (data != null) {
      if (resultCode == Activity.RESULT_OK) {
        val uri = data.data
        if (uri != null) {
          try {
            return delegater.onResult(requestCode, uri, data)
          } catch (e: Exception) {
            Log.w(TAG, e)
          }
        }
      }
    }
    try {
      clearUri(context, getKey(requestCode))
      delegater.onFailed(requestCode, data)
    } catch (e: Exception) {
      Log.w(TAG, e)
    }
    return false
  }

  /**
   * uri从请求代码中，用于保存的共享首选项的密钥名称
   * @param requestCode
   * @return
   */
  private fun getKey(requestCode: Int): String {
    return String.format(Locale.US, "SDUtils-%d", requestCode)
  }

  /**
   * uri保存为共享首选项
   * @param context
   * @param key
   * @param uri
   */
  private fun saveUri(
    context: Context,
    key: String, uri: Uri
  ) {
    val pref =
      context.getSharedPreferences(context.packageName, 0)
    pref?.edit()?.putString(key, uri.toString())?.apply()
  }

  /**
   * 检索已保存的共享首选项的URL
   * @param context
   * @param key
   * @return
   */
  private fun loadUri(context: Context, key: String): Uri? {
    var result: Uri? = null
    val pref =
      context.getSharedPreferences(context.packageName, 0)
    if (pref != null && pref.contains(key)) {
      try {
        result = Uri.parse(pref.getString(key, null))
      } catch (e: Exception) {
        Log.w(TAG, e)
      }
    }
    return result
  }

  /**
   * 删除共享首选项中保存的URL
   * @param context
   * @param key
   */
  private fun clearUri(context: Context, key: String?) {
    val pref =
      context.getSharedPreferences(context.packageName, 0)
    if (pref != null && pref.contains(key)) {
      try {
        pref.edit().remove(key).apply()
      } catch (e: Exception) {
        Log.w(TAG, e)
      }
    }
  }

  /**
   * 请求Uri进行文件读取
   * 在KITKAT之后为每个单独的文件请求权限时
   * @param activity
   * @param mimeType
   * @param requestCode
   */
  @TargetApi(VERSION_CODES.KITKAT) fun requestOpenDocument(
    activity: Activity,
    mimeType: String?, requestCode: Int
  ) {
    if (BuildCheck.isKitKat()) {
      activity.startActivityForResult(prepareOpenDocumentIntent(mimeType!!), requestCode)
    }
  }

  /**
   * 请求Uri进行文件读取
   * 在KITKAT之后为每个单独的文件请求权限时
   * @param activity
   * @param mimeType
   * @param requestCode
   */
  @TargetApi(VERSION_CODES.KITKAT) fun requestOpenDocument(
    activity: FragmentActivity,
    mimeType: String?, requestCode: Int
  ) {
    if (BuildCheck.isKitKat()) {
      activity.startActivityForResult(prepareOpenDocumentIntent(mimeType!!), requestCode)
    }
  }

  /**
   * 请求Uri进行文件读取
   * 在KITKAT之后为每个单独的文件请求权限时
   * @param fragment
   * @param mimeType
   * @param requestCode
   */
  @TargetApi(VERSION_CODES.KITKAT) fun requestOpenDocument(
    fragment: Fragment,
    mimeType: String?, requestCode: Int
  ) {
    if (BuildCheck.isKitKat()) {
      fragment.startActivityForResult(prepareOpenDocumentIntent(mimeType!!), requestCode)
    }
  }

  /**
   * 请求Uri进行文件读取
   * 在KITKAT之后为每个单独的文件请求权限时
   */
  @TargetApi(VERSION_CODES.KITKAT) fun requestOpenDocument(
    fragment: androidx.fragment.app.Fragment,
    mimeType: String?, requestCode: Int
  ) {
    if (BuildCheck.isKitKat()) {
      fragment.startActivityForResult(prepareOpenDocumentIntent(mimeType!!), requestCode)
    }
  }

  /**
   * 请求Uri读取文件的Helper方法
   * 在KITKAT之后为每个单独的文件请求权限时
   * @param mimeType
   * @return
   */
  @TargetApi(VERSION_CODES.KITKAT)
  private fun prepareOpenDocumentIntent(mimeType: String): Intent? {
    val intent =
      Intent(Intent.ACTION_OPEN_DOCUMENT)
    intent.type = mimeType
    return intent
  }

  /**
   * 请求Uri保存文件
   * 在KITKAT之后为每个单独的文件请求权限时
   * @param activity
   * @param mimeType
   * @param requestCode
   */
  @TargetApi(VERSION_CODES.KITKAT) fun requestCreateDocument(
    activity: Activity,
    mimeType: String?, requestCode: Int
  ) {
    if (BuildCheck.isKitKat()) {
      activity.startActivityForResult(prepareCreateDocument(mimeType, null), requestCode)
    }
  }

  /**
   * 请求Uri保存文件
   * 在KITKAT之后为每个单独的文件请求权限时
   * @param activity
   * @param mimeType
   * @param defaultName
   * @param requestCode
   */
  @TargetApi(VERSION_CODES.KITKAT) fun requestCreateDocument(
    activity: Activity,
    mimeType: String?, defaultName: String?, requestCode: Int
  ) {
    if (BuildCheck.isKitKat()) {
      activity.startActivityForResult(
        prepareCreateDocument(mimeType, defaultName),
        requestCode
      )
    }
  }

  /**
   * 请求Uri保存文件
   * 在KITKAT之后为每个单独的文件请求权限时
   * @param activity
   * @param mimeType
   * @param requestCode
   */
  @TargetApi(VERSION_CODES.KITKAT) fun requestCreateDocument(
    activity: FragmentActivity,
    mimeType: String?, requestCode: Int
  ) {
    if (BuildCheck.isKitKat()) {
      activity.startActivityForResult(prepareCreateDocument(mimeType, null), requestCode)
    }
  }

  /**
   * 请求Uri保存文件
   * 在KITKAT之后为每个单独的文件请求权限时
   * @param activity
   * @param mimeType
   * @param defaultName
   * @param requestCode
   */
  @TargetApi(VERSION_CODES.KITKAT) fun requestCreateDocument(
    activity: FragmentActivity,
    mimeType: String?, defaultName: String?, requestCode: Int
  ) {
    if (BuildCheck.isKitKat()) {
      activity.startActivityForResult(
        prepareCreateDocument(mimeType, defaultName),
        requestCode
      )
    }
  }

  /**
   * 请求Uri保存文件
   * 在KITKAT之后为每个单独的文件请求权限时
   * @param fragment
   * @param mimeType
   * @param requestCode
   */
  @TargetApi(VERSION_CODES.KITKAT) fun requestCreateDocument(
    fragment: Fragment,
    mimeType: String?, requestCode: Int
  ) {
    if (BuildCheck.isKitKat()) {
      fragment.startActivityForResult(prepareCreateDocument(mimeType, null), requestCode)
    }
  }

  /**
   * 请求Uri保存文件
   * 在KITKAT之后为每个单独的文件请求权限时
   * @param fragment
   * @param mimeType
   * @param defaultName
   * @param requestCode
   */
  @TargetApi(VERSION_CODES.KITKAT) fun requestCreateDocument(
    fragment: Fragment,
    mimeType: String?, defaultName: String?, requestCode: Int
  ) {
    if (BuildCheck.isKitKat()) {
      fragment.startActivityForResult(
        prepareCreateDocument(mimeType, defaultName),
        requestCode
      )
    }
  }

  /**
   * 请求Uri保存文件
   * 在KITKAT之后为每个单独的文件请求权限时
   * @param fragment
   * @param mimeType
   * @param requestCode
   */
  @TargetApi(VERSION_CODES.KITKAT) fun requestCreateDocument(
    fragment: androidx.fragment.app.Fragment,
    mimeType: String?, requestCode: Int
  ) {
    if (BuildCheck.isKitKat()) {
      fragment.startActivityForResult(prepareCreateDocument(mimeType, null), requestCode)
    }
  }

  /**
   * 请求Uri保存文件
   * 在KITKAT之后为每个单独的文件请求权限时
   * @param fragment
   * @param mimeType
   * @param defaultName
   * @param requestCode
   */
  @TargetApi(VERSION_CODES.KITKAT) fun requestCreateDocument(
    fragment: androidx.fragment.app.Fragment,
    mimeType: String?, defaultName: String?, requestCode: Int
  ) {
    if (BuildCheck.isKitKat()) {
      fragment.startActivityForResult(
        prepareCreateDocument(mimeType, defaultName),
        requestCode
      )
    }
  }

  /**
   * 请求Uri保存文件的Helper方法
   * 在KITKAT之后为每个单独的文件请求权限时
   * @param mimeType
   * @param defaultName
   * @return
   */
  @TargetApi(VERSION_CODES.KITKAT)
  private fun prepareCreateDocument(
    mimeType: String?,
    defaultName: String?
  ): Intent? {
    val intent =
      Intent(Intent.ACTION_OPEN_DOCUMENT)
    intent.type = mimeType
    if (!TextUtils.isEmpty(defaultName)) {
      intent.putExtra(Intent.EXTRA_TITLE, defaultName)
    }
    return intent
  }

  /**
   * 文件删除请求
   * 在KITKAT之后为每个单独的文件请求权限时
   * @param context
   * @param uri
   * @return
   */
  @TargetApi(VERSION_CODES.KITKAT) fun requestDeleteDocument(
    context: Context,
    uri: Uri?
  ): Boolean {
    return try {
      (BuildCheck.isKitKat()
          && DocumentsContract.deleteDocument(context.contentResolver, uri))
    } catch (e: FileNotFoundException) {
      false
    }
  }

  /**
   * 是否可以访问对应于request_code的Uri
   * @param context
   * @param requestCode
   * @return
   */
  @TargetApi(VERSION_CODES.KITKAT) fun hasStorageAccess(
    context: Context,
    requestCode: Int
  ): Boolean {
    var found = false
    if (BuildCheck.isLollipop()) {
      val uri = loadUri(context, getKey(requestCode))
      if (uri != null) {
        // 获取永久存储的Uri权限列表
        val list =
          context.contentResolver.persistedUriPermissions
        for (item in list) {
          if (item.uri == uri) {
            // 永久保持对应于request_code的Uri的权限
            found = true
            break
          }
        }
      }
    }
    return found
  }

  /**
   * 请求访问与request_code相对应的Uri
   * @param activity
   * @param requestCode
   * @return 如果已经存在与request_code相对应的Uri，则返回它，如果它不存在，则发出权限请求并返回null
   */
  @TargetApi(VERSION_CODES.LOLLIPOP) fun requestStorageAccess(
    activity: Activity,
    requestCode: Int
  ): Uri? {
    if (BuildCheck.isLollipop()) {
      val uri = getStorageUri(activity, requestCode)
      if (uri == null) {
        // 请求并返回null，如果它没有对request_code对应的Uri的权限
        activity.startActivityForResult(prepareStorageAccessPermission(), requestCode)
      }
      return uri
    }
    return null
  }

  /**
   * 请求访问与request_code相对应的Uri
   * @param activity
   * @param requestCode
   * @return 如果已经存在与request_code相对应的Uri，则返回它，如果它不存在，则发出权限请求并返回null
   */
  @TargetApi(VERSION_CODES.LOLLIPOP) fun requestStorageAccess(
    activity: FragmentActivity,
    requestCode: Int
  ): Uri? {
    if (BuildCheck.isLollipop()) {
      val uri = getStorageUri(activity, requestCode)
      if (uri == null) {
        // 请求并返回null，如果它没有对request_code对应的Uri的权限
        activity.startActivityForResult(prepareStorageAccessPermission(), requestCode)
      }
      return uri
    }
    return null
  }

  /**
   * 请求访问与request_code相对应的Uri
   * @param fragment
   * @param requestCode
   * @return 如果已经存在与request_code相对应的Uri，则返回它，如果它不存在，则发出权限请求并返回null
   */
  @TargetApi(VERSION_CODES.LOLLIPOP) fun requestStorageAccess(
    fragment: Fragment,
    requestCode: Int
  ): Uri? {
    val uri = getStorageUri(fragment.activity, requestCode)
    if (uri == null) {
      // 请求并返回null，如果它没有对request_code对应的Uri的权限
      fragment.startActivityForResult(prepareStorageAccessPermission(), requestCode)
    }
    return uri
  }

  /**
   * 请求访问与request_code相对应的Uri
   * @param fragment
   * @param requestCode
   * @return 如果已经存在与request_code相对应的Uri，则返回它，如果它不存在，则发出权限请求并返回null
   */
  @TargetApi(VERSION_CODES.LOLLIPOP) fun requestStorageAccess(
    fragment: androidx.fragment.app.Fragment,
    requestCode: Int
  ): Uri? {
    if (BuildCheck.isLollipop()) {
      val activity: Activity? = fragment.activity
      val uri =
        activity?.let { getStorageUri(it, requestCode) }
      if (uri == null) {
        // 请求并返回null，如果它没有对request_code对应的Uri的权限
        fragment.startActivityForResult(prepareStorageAccessPermission(), requestCode)
      }
      return uri
    }
    return null
  }

  /**
   * 如果存在与request_code对应的Uri，如果有永久权限，则返回它，否则返回null
   * @param context
   * @param requestCode
   * @return
   */
  @TargetApi(VERSION_CODES.LOLLIPOP) fun getStorageUri(
    context: Context,
    requestCode: Int
  ): Uri? {
    if (BuildCheck.isLollipop()) {
      val uri = loadUri(context, getKey(requestCode))
      if (uri != null) {
        var found = false
        // 获取永久存储的Uri权限列表
        val list =
          context.contentResolver.persistedUriPermissions
        for (item in list) {
          if (item.uri == uri) {
            // 永久保持对应于request_code的Uri的权限
            found = true
            break
          }
        }
        if (found) {
          return uri
        }
      }
    }
    return null
  }

  /**
   * requestStorageAccess
   * @return
   */
  @TargetApi(VERSION_CODES.LOLLIPOP)
  private fun prepareStorageAccessPermission(): Intent? {
    return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
  }

  /**
   * 要求永久访问权限
   * @param context
   * @param treeUri
   * @return
   */
  @TargetApi(VERSION_CODES.LOLLIPOP)
  fun requestStorageAccessPermission(
    context: Context,
    requestCode: Int, treeUri: Uri?
  ): Uri? {
    return requestStorageAccessPermission(
      context,
      requestCode, treeUri,
      Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    )
  }

  /**
   * 要求永久访问权限
   * @param context
   * @param treeUri
   * @param flags
   * @return
   */
  @TargetApi(VERSION_CODES.LOLLIPOP)
  fun requestStorageAccessPermission(
    context: Context,
    requestCode: Int, treeUri: Uri?, flags: Int
  ): Uri? {
    return if (BuildCheck.isLollipop()) {
      context.contentResolver.takePersistableUriPermission(treeUri!!, flags)
      saveUri(context, getKey(requestCode), treeUri)
      treeUri
    } else {
      null
    }
  }

  /**
   * 要求永久访问权限
   * @param context
   * @param requestCode
   */
  @TargetApi(VERSION_CODES.LOLLIPOP)
  fun releaseStorageAccessPermission(
    context: Context,
    requestCode: Int
  ) {
    if (BuildCheck.isLollipop()) {
      val key = getKey(requestCode)
      val uri = loadUri(context, key)
      if (uri != null) {
        context.contentResolver.releasePersistableUriPermission(
          uri,
          Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        clearUri(context, key)
      }
    }
  }

  /**
   * 当存在与指定id对应的Uri时，返回相应的DocumentFile
   * @param context
   * @param treeId
   * @return
   */
  @Throws(IOException::class) fun getStorage(
    context: Context,
    treeId: Int
  ): DocumentFile? {
    return getStorage(context, treeId, null)
  }

  /**
   * 如果存在与指定的id相对应的Uri并且它是可写的，则在其下创建目录
   * 返回指示该目录的DocumentFile对象
   * @param context
   * @param treeId
   * @param dirs 由斜杠（`/`）分隔的路径字符串
   * @return 对应于底层目录的DocumentFile，Uri不存在，如果无法写入则为null
   */
  @Throws(IOException::class) fun getStorage(
    context: Context,
    treeId: Int, dirs: String?
  ): DocumentFile? {
    if (BuildCheck.isLollipop()) {
      val treeUri = getStorageUri(context, treeId)
      if (treeUri != null) {
        var tree = DocumentFile.fromTreeUri(context, treeUri)
        if (!TextUtils.isEmpty(dirs)) {
          val dir: Array<String?> = dirs!!.split("/".toRegex()).toTypedArray()
          for (d in dir) {
            if (!TextUtils.isEmpty(d)) {
              val t = tree!!.findFile(d!!)
              tree = if (t != null && t.isDirectory) {
                // 它已经存在时我什么都不做
                t
              } else if (t == null) {
                if (tree.canWrite()) {
                  // 当目录不存在时生成目录
                  tree.createDirectory(d)
                } else {
                  throw IOException("can't create directory")
                }
              } else {
                throw IOException("can't create directory, file with same name already exists")
              }
            }
          }
        }
        return tree
      }
    }
    return null
  }

  /**
   * 如果存在与指定的id相对应的Uri并且它是可写的，则在其下创建目录
   * 返回指示该目录的DocumentFile对象
   * @param context
   * @param parent
   * @param dirs
   * @return 如果无法写入，则为null
   */
  @Throws(IOException::class) fun getStorage(
    context: Context,
    parent: DocumentFile, dirs: String?
  ): DocumentFile? {
    var tree: DocumentFile? = parent
    if (!TextUtils.isEmpty(dirs)) {
      val dir: Array<String?> = dirs!!.split("/".toRegex()).toTypedArray()
      for (d in dir) {
        if (!TextUtils.isEmpty(d)) {
          val t = tree!!.findFile(d!!)
          tree = if (t != null && t.isDirectory) {
            // 它已经存在时我什么都不做
            t
          } else if (t == null) {
            if (tree.canWrite()) {
              // 当目录不存在时生成目录
              tree.createDirectory(d)
            } else {
              throw IOException("can't create directory")
            }
          } else {
            throw IOException("can't create directory, file with same name already exists")
          }
        }
      }
    }
    return tree
  }

  /**
   * 获取指定目录下的文件列表
   * @param context
   * @param dir
   * @param filter 如果为null，则添加所有现有文件
   * @return
   * @throws IOException
   */
  @Throws(IOException::class) fun listFiles(
    context: Context,
    dir: DocumentFile,
    filter: FileFilter?
  ): MutableCollection<DocumentFile?> {
    val result: MutableCollection<DocumentFile?> =
      ArrayList()
    if (dir.isDirectory) {
      val files = dir.listFiles()
      for (file in files) {
        if (filter == null || filter.accept(file)) {
          result.add(file)
        }
      }
    }
    return result
  }

  /**
   * 返回满负荷和可用空间
   * 如果它不是目录或无法访问，则返回null
   * @param context
   * @param dir
   * @return
   */
  @SuppressLint("NewApi") fun getStorageInfo(
    context: Context,
    dir: DocumentFile
  ): StorageInfo? {
    try {
      val path = UriHelper.getPath(context, dir.uri)
      if (path != null) {
        val file = File(path)
        if (file.isDirectory && file.canRead()) {
          val total = file.totalSpace
          var free = file.freeSpace
          if (free < file.usableSpace) {
            free = file.usableSpace
          }
          return StorageInfo(total, free)
        }
      }
    } catch (e: Exception) {
      // ignore
    }
    if (BuildCheck.isJellyBeanMR2()) {
      try {
        val path = UriHelper.getPath(context, dir.uri)
        val fs = StatFs(path)
        return StorageInfo(fs.totalBytes, fs.availableBytes)
      } catch (e: Exception) {
        // ignore
      }
    }
    return null
  }

  /**
   * 当存在指定的Uri时，它会创建一个DocumentFile对象以引用相应的文件
   * @param context
   * @param treeId
   * @param mime
   * @param name
   * @return
   */
  @TargetApi(VERSION_CODES.LOLLIPOP)
  @Throws(IOException::class) fun getStorageFile(
    context: Context,
    treeId: Int, mime: String?, name: String?
  ): DocumentFile? {
    return getStorageFile(context, treeId, null, mime, name)
  }

  /**
   * 当存在指定的Uri时，它会创建一个DocumentFile对象以引用相应的文件
   * @param context
   * @param treeId
   * @param mime
   * @param name
   * @return
   */
  @TargetApi(VERSION_CODES.LOLLIPOP)
  @Throws(IOException::class) fun getStorageFile(
    context: Context,
    treeId: Int, dirs: String?,
    mime: String?, name: String?
  ): DocumentFile? {
    if (BuildCheck.isLollipop()) {
      val tree = getStorage(context, treeId, dirs)
      if (tree != null) {
        val file = tree.findFile(name!!)
        return if (file != null) {
          if (file.isFile) {
            file
          } else {
            throw IOException("directory with same name already exists")
          }
        } else {
          tree.createFile(mime!!, name)
        }
      }
    }
    return null
  }

  /**
   * 在指定的DocumentFile下生成文件
   * 如果dirs为null或为空字符串，则与调用DocumentFile＃createFile相同
   * @param context
   * @param parent
   * @param dirs
   * @param mime
   * @param name
   * @return
   */
  @Throws(IOException::class) fun getStorageFile(
    context: Context,
    parent: DocumentFile, dirs: String?,
    mime: String?, name: String?
  ): DocumentFile? {
    val tree = getStorage(context, parent, dirs)
    if (tree != null) {
      val file = tree.findFile(name!!)
      return if (file != null) {
        if (file.isFile) {
          file
        } else {
          throw IOException("directory with same name already exists")
        }
      } else {
        tree.createFile(mime!!, name)
      }
    }
    return null
  }

  /**
   * 当指定的Uri存在时，它会在其下创建一个输出文件并将其作为OutputStream返回
   * @param context
   * @param treeId
   * @param mime
   * @param name
   * @return
   * @throws FileNotFoundException
   */
  @TargetApi(VERSION_CODES.LOLLIPOP)
  @Throws(IOException::class) fun getStorageOutputStream(
    context: Context,
    treeId: Int,
    mime: String?, name: String?
  ): OutputStream? {
    return getStorageOutputStream(context, treeId, null, mime, name)
  }

  /**
   * 当指定的Uri存在时，它会在其下创建一个输出文件并将其作为OutputStream返回
   * @param context
   * @param treeId
   * @param dirs
   * @param mime
   * @param name
   * @return
   * @throws FileNotFoundException
   */
  @TargetApi(VERSION_CODES.LOLLIPOP)
  @Throws(IOException::class) fun getStorageOutputStream(
    context: Context,
    treeId: Int, dirs: String?,
    mime: String?, name: String?
  ): OutputStream? {
    if (BuildCheck.isLollipop()) {
      val tree = getStorage(context, treeId, dirs)
      if (tree != null) {
        val file = tree.findFile(name!!)
        return if (file != null) {
          if (file.isFile) {
            context.contentResolver.openOutputStream(
              file.uri
            )
          } else {
            throw IOException("directory with same name already exists")
          }
        } else {
          context.contentResolver.openOutputStream(
            tree.createFile(mime!!, name)!!.getUri()
          )
        }
      }
    }
    throw FileNotFoundException()
  }

  /**
   * 当指定的Uri存在时，它会在其下创建一个输出文件并将其作为OutputStream返回
   * @param context
   * @param parent
   * @param dirs
   * @param mime
   * @param name
   * @return
   * @throws FileNotFoundException
   */
  @Throws(IOException::class) fun getStorageOutputStream(
    context: Context,
    parent: DocumentFile, dirs: String?,
    mime: String?, name: String?
  ): OutputStream? {
    val tree = getStorage(context, parent, dirs)
    if (tree != null) {
      val file = tree.findFile(name!!)
      return if (file != null) {
        if (file.isFile) {
          context.contentResolver.openOutputStream(
            file.uri
          )
        } else {
          throw IOException("directory with same name already exists")
        }
      } else {
        context.contentResolver.openOutputStream(
          tree.createFile(mime!!, name)!!.getUri()
        )
      }
    }
    throw FileNotFoundException()
  }

  /**
   * 当指定的Uri存在时，它会在其下创建一个输出文件并将其作为OutputStream返回
   * @param context
   * @param treeId
   * @param mime
   * @param name
   * @return
   * @throws FileNotFoundException
   */
  @TargetApi(VERSION_CODES.LOLLIPOP)
  @Throws(IOException::class) fun getStorageInputStream(
    context: Context,
    treeId: Int,
    mime: String?, name: String?
  ): InputStream? {
    return getStorageInputStream(context, treeId, null, mime, name)
  }

  /**
   * 当指定的Uri存在时，它将输入文件作为InputStream返回
   * @param context
   * @param treeId
   * @param dirs
   * @param mime
   * @param name
   * @return
   * @throws FileNotFoundException
   */
  @TargetApi(VERSION_CODES.LOLLIPOP)
  @Throws(IOException::class) fun getStorageInputStream(
    context: Context,
    treeId: Int, dirs: String?,
    mime: String?, name: String?
  ): InputStream? {
    if (BuildCheck.isLollipop()) {
      val tree = getStorage(context, treeId, dirs)
      if (tree != null) {
        val file = tree.findFile(name!!)
        if (file != null) {
          return if (file.isFile) {
            context.contentResolver.openInputStream(
              file.uri
            )
          } else {
            throw IOException("directory with same name already exists")
          }
        }
      }
    }
    throw FileNotFoundException()
  }

  /**
   * 当指定的Uri存在时，它会在其下创建一个输出文件并将其作为OutputStream返回
   * @param context
   * @param parent
   * @param dirs
   * @param mime
   * @param name
   * @return
   * @throws FileNotFoundException
   */
  @Throws(IOException::class) fun getStorageInputStream(
    context: Context,
    parent: DocumentFile, dirs: String?,
    mime: String?, name: String?
  ): InputStream? {
    val tree = getStorage(context, parent, dirs)
    if (tree != null) {
      val file = tree.findFile(name!!)
      if (file != null) {
        return if (file.isFile) {
          context.contentResolver.openInputStream(
            file.uri
          )
        } else {
          throw IOException("directory with same name already exists")
        }
      }
    }
    throw FileNotFoundException()
  }

  /**
   * 当指定的Uri存在时，它会在其下创建一个输入文件，并返回输入/输出的文件描述符
   * @param context
   * @param treeId
   * @param dirs
   * @param mime
   * @param name
   * @return
   * @throws FileNotFoundException
   */
  @TargetApi(VERSION_CODES.LOLLIPOP)
  @Throws(IOException::class) fun getStorageFileFD(
    context: Context,
    treeId: Int, dirs: String?,
    mime: String?, name: String?
  ): ParcelFileDescriptor? {
    if (BuildCheck.isLollipop()) {
      val tree = getStorage(context, treeId, dirs)
      if (tree != null) {
        val file = tree.findFile(name!!)
        return if (file != null) {
          if (file.isFile) {
            context.contentResolver.openFileDescriptor(
              file.uri, "rw"
            )
          } else {
            throw IOException("directory with same name already exists")
          }
        } else {
          context.contentResolver.openFileDescriptor(
            tree.createFile(mime!!, name)!!.getUri(), "rw"
          )
        }
      }
    }
    throw FileNotFoundException()
  }

  /**
   * 如果指定的DocumentFile指示的目录存在，则返回输入和输出的文件描述符
   * @param context
   * @param parent
   * @param dirs
   * @param mime
   * @param name
   * @return
   * @throws IOException
   */
  @Throws(IOException::class) fun getStorageFileFD(
    context: Context,
    parent: DocumentFile, dirs: String?,
    mime: String?, name: String?
  ): ParcelFileDescriptor? {
    val tree = getStorage(context, parent, dirs)
    if (tree != null) {
      val file = tree.findFile(name!!)
      return if (file != null) {
        if (file.isFile) {
          context.contentResolver.openFileDescriptor(
            file.uri, "rw"
          )
        } else {
          throw IOException("directory with same name already exists")
        }
      } else {
        context.contentResolver.openFileDescriptor(
          tree.createFile(mime!!, name)!!.getUri(), "rw"
        )
      }
    }
    throw FileNotFoundException()
  }
  //================================================================================
  /**
   * 当存在与指定id对应的Uri时，它返回用于在其下创建文件的路径
   * @param context
   * @param treeId
   * @return
   */
  @TargetApi(VERSION_CODES.LOLLIPOP) fun createStorageDir(
    context: Context,
    treeId: Int
  ): File? {
    if (BuildCheck.isLollipop()) {
      val treeUri = getStorageUri(context, treeId)
      if (treeUri != null) {
        val saveTree = DocumentFile.fromTreeUri(context, treeUri)
        val path = UriHelper.getPath(context, saveTree!!.getUri())
        if (!TextUtils.isEmpty(path)) {
          return File(path)
        }
      }
    }
    return null
  }

  /**
   * 当存在与指定id相对应的Uri时，它会创建并返回其下面指定的File
   * @param context
   * @param treeId
   * @param mime
   * @param fileName
   * @return
   */
  @TargetApi(VERSION_CODES.LOLLIPOP) fun createStorageFile(
    context: Context,
    treeId: Int, mime: String?, fileName: String?
  ): File? {
    return createStorageFile(
      context,
      getStorageUri(context, treeId),
      mime,
      fileName
    )
  }

  /**
   * 返回在指定的Uri存在时创建文件的路径
   * @param context
   * @param treeUri
   * @param mime
   * @param fileName
   * @return
   */
  @TargetApi(VERSION_CODES.LOLLIPOP) fun createStorageFile(
    context: Context,
    treeUri: Uri?, mime: String?, fileName: String?
  ): File? {
    Log.i(TAG, "createStorageFile:$fileName")
    if (BuildCheck.isLollipop()) {
      if (treeUri != null && !TextUtils.isEmpty(fileName)) {
        val saveTree = DocumentFile.fromTreeUri(context, treeUri)
        val target = saveTree!!.createFile(mime!!, fileName!!)
        val path = UriHelper.getPath(context, target!!.getUri())
        if (!TextUtils.isEmpty(path)) {
          return File(path)
        }
      }
    }
    return null
  }

  /**
   * 当存在与指定id相对应的Uri时，它返回其下生成的文件的原始文件描述符
   * @param context
   * @param treeId
   * @param mime
   * @param fileName
   * @return
   */
  @TargetApi(VERSION_CODES.LOLLIPOP) fun createStorageFileFD(
    context: Context,
    treeId: Int, mime: String?, fileName: String?
  ): Int {
    Log.i(TAG, "createStorageFileFD:$fileName")
    return createStorageFileFD(
      context,
      getStorageUri(context, treeId),
      mime,
      fileName
    )
  }

  /**
   * 当存在与指定id相对应的Uri时，它返回其下生成的文件的原始文件描述符
   * @param context
   * @param treeUri
   * @param mime
   * @param fileName
   * @return
   */
  @TargetApi(VERSION_CODES.LOLLIPOP) fun createStorageFileFD(
    context: Context,
    treeUri: Uri?, mime: String?, fileName: String?
  ): Int {
    Log.i(TAG, "createStorageFileFD:$fileName")
    if (BuildCheck.isLollipop()) {
      if (treeUri != null && !TextUtils.isEmpty(fileName)) {
        val saveTree = DocumentFile.fromTreeUri(context, treeUri)
        val target = saveTree!!.createFile(mime!!, fileName!!)
        try {
          assert(target != null)
          val fd =
            context.contentResolver.openFileDescriptor(target!!.getUri(), "rw")
          return fd?.fd ?: 0
        } catch (e: FileNotFoundException) {
          Log.w(TAG, e)
        }
      }
    }
    return 0
  }

  interface handleOnResultDelegater {
    open fun onResult(
      requestCode: Int,
      uri: Uri?,
      data: Intent?
    ): Boolean

    open fun onFailed(requestCode: Int, data: Intent?)
  }

  //================================================================================
  interface FileFilter {
    open fun accept(file: DocumentFile): Boolean
  }
}