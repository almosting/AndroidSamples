package com.sunplus.toolbox.utils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.StatFs;
import android.provider.DocumentsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.provider.DocumentFile;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
public class SDUtils {
  private static final String TAG = "SDUtils";
  public interface handleOnResultDelegater {
    public boolean onResult(final int requestCode, final Uri uri, final Intent data);
    public void onFailed(final int requestCode, final Intent data);
  }


  public static boolean handleOnResult(@NonNull final Context context,
                                       final int requestCode, final int resultCode,
                                       final Intent data, @NonNull final handleOnResultDelegater delegater) {

    if (data != null) {
      if (resultCode == Activity.RESULT_OK) {
        final Uri uri = data.getData();
        if (uri != null) {
          try {
            return delegater.onResult(requestCode, uri, data);
          } catch (final Exception e) {
            Log.w(TAG, e);
          }
        }
      }
    }
    try {
      clearUri(context, getKey(requestCode));
      delegater.onFailed(requestCode, data);
    } catch (final Exception e) {
      Log.w(TAG, e);
    }
    return false;
  }

  /**
   * uri从请求代码中，用于保存的共享首选项的密钥名称
   * @param requestCode
   * @return
   */
  @NonNull
  private static String getKey(final int requestCode) {
    return String.format(Locale.US, "SDUtils-%d", requestCode);
  }

  /**
   * uri保存为共享首选项
   * @param context
   * @param key
   * @param uri
   */
  private static void saveUri(@NonNull final Context context,
                              @NonNull final String key, @NonNull final Uri uri) {
    final SharedPreferences pref = context.getSharedPreferences(context.getPackageName(), 0);
    if (pref != null) {
      pref.edit().putString(key, uri.toString()).apply();
    }
  }

  /**
   * 检索已保存的共享首选项的URL
   * @param context
   * @param key
   * @return
   */
  @Nullable
  private static Uri loadUri(@NonNull final Context context, @NonNull final String key) {
    Uri result = null;
    final SharedPreferences pref = context.getSharedPreferences(context.getPackageName(), 0);
    if ((pref != null) && pref.contains(key)) {
      try {
        result = Uri.parse(pref.getString(key, null));
      } catch (final Exception e) {
        Log.w(TAG, e);
      }
    }
    return result;
  }

  /**
   * 删除共享首选项中保存的URL
   * @param context
   * @param key
   */
  private static void clearUri(@NonNull final Context context, @Nullable final String key) {
    final SharedPreferences pref = context.getSharedPreferences(context.getPackageName(), 0);
    if ((pref != null) && pref.contains(key)) {
      try {
        pref.edit().remove(key).apply();
      } catch (final Exception e) {
        Log.w(TAG, e);
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
  @TargetApi(Build.VERSION_CODES.KITKAT)
  public static void requestOpenDocument(@NonNull final Activity activity,
                                         final String mimeType, final int requestCode) {

    if (BuildCheck.isKitKat()) {
      activity.startActivityForResult(prepareOpenDocumentIntent(mimeType), requestCode);
    }
  }

  /**
   * 请求Uri进行文件读取
   * 在KITKAT之后为每个单独的文件请求权限时
   * @param activity
   * @param mimeType
   * @param requestCode
   */
  @TargetApi(Build.VERSION_CODES.KITKAT)
  public static void requestOpenDocument(@NonNull final FragmentActivity activity,
                                         final String mimeType, final int requestCode) {

    if (BuildCheck.isKitKat()) {
      activity.startActivityForResult(prepareOpenDocumentIntent(mimeType), requestCode);
    }
  }

  /**
   * 请求Uri进行文件读取
   * 在KITKAT之后为每个单独的文件请求权限时
   * @param fragment
   * @param mimeType
   * @param requestCode
   */
  @TargetApi(Build.VERSION_CODES.KITKAT)
  public static void requestOpenDocument(@NonNull final android.app.Fragment fragment,
                                         final String mimeType, final int requestCode) {

    if (BuildCheck.isKitKat()) {
      fragment.startActivityForResult(prepareOpenDocumentIntent(mimeType), requestCode);
    }
  }

  /**
   * 请求Uri进行文件读取
   * 在KITKAT之后为每个单独的文件请求权限时
   * @param fragment
   * @param mimeType
   * @param requestCode
   */
  @TargetApi(Build.VERSION_CODES.KITKAT)
  public static void requestOpenDocument(@NonNull final android.support.v4.app.Fragment fragment,
                                         final String mimeType, final int requestCode) {

    if (BuildCheck.isKitKat()) {
      fragment.startActivityForResult(prepareOpenDocumentIntent(mimeType), requestCode);
    }
  }

  /**
   * 请求Uri读取文件的Helper方法
   * 在KITKAT之后为每个单独的文件请求权限时
   * @param mimeType
   * @return
   */
  @TargetApi(Build.VERSION_CODES.KITKAT)
  private static Intent prepareOpenDocumentIntent(@NonNull final String mimeType) {
    final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.setType(mimeType);
    return intent;
  }

  /**
   * 请求Uri保存文件
   * 在KITKAT之后为每个单独的文件请求权限时
   * @param activity
   * @param mimeType
   * @param requestCode
   */
  @TargetApi(Build.VERSION_CODES.KITKAT)
  public static void requestCreateDocument(@NonNull final Activity activity,
                                           final String mimeType, final int requestCode) {

    if (BuildCheck.isKitKat()) {
      activity.startActivityForResult(prepareCreateDocument(mimeType, null), requestCode);
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
  @TargetApi(Build.VERSION_CODES.KITKAT)
  public static void requestCreateDocument(@NonNull final Activity activity,
                                           final String mimeType, final String defaultName, final int requestCode) {

    if (BuildCheck.isKitKat()) {
      activity.startActivityForResult(prepareCreateDocument(mimeType, defaultName), requestCode);
    }
  }

  /**
   * 请求Uri保存文件
   * 在KITKAT之后为每个单独的文件请求权限时
   * @param activity
   * @param mimeType
   * @param requestCode
   */
  @TargetApi(Build.VERSION_CODES.KITKAT)
  public static void requestCreateDocument(@NonNull final FragmentActivity activity,
                                           final String mimeType, final int requestCode) {

    if (BuildCheck.isKitKat()) {
      activity.startActivityForResult(prepareCreateDocument(mimeType, null), requestCode);
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
  @TargetApi(Build.VERSION_CODES.KITKAT)
  public static void requestCreateDocument(@NonNull final FragmentActivity activity,
                                           final String mimeType, final String defaultName, final int requestCode) {

    if (BuildCheck.isKitKat()) {
      activity.startActivityForResult(prepareCreateDocument(mimeType, defaultName), requestCode);
    }
  }

  /**
   * 请求Uri保存文件
   * 在KITKAT之后为每个单独的文件请求权限时
   * @param fragment
   * @param mimeType
   * @param requestCode
   */
  @TargetApi(Build.VERSION_CODES.KITKAT)
  public static void requestCreateDocument(@NonNull final android.app.Fragment fragment,
                                           final String mimeType, final int requestCode) {

    if (BuildCheck.isKitKat()) {
      fragment.startActivityForResult(prepareCreateDocument(mimeType, null), requestCode);
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
  @TargetApi(Build.VERSION_CODES.KITKAT)
  public static void requestCreateDocument(@NonNull final android.app.Fragment fragment,
                                           final String mimeType, final String defaultName, final int requestCode) {

    if (BuildCheck.isKitKat()) {
      fragment.startActivityForResult(prepareCreateDocument(mimeType, defaultName), requestCode);
    }
  }

  /**
   * 请求Uri保存文件
   * 在KITKAT之后为每个单独的文件请求权限时
   * @param fragment
   * @param mimeType
   * @param requestCode
   */
  @TargetApi(Build.VERSION_CODES.KITKAT)
  public static void requestCreateDocument(@NonNull final android.support.v4.app.Fragment fragment,
                                           final String mimeType, final int requestCode) {

    if (BuildCheck.isKitKat()) {
      fragment.startActivityForResult(prepareCreateDocument(mimeType, null), requestCode);
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
  @TargetApi(Build.VERSION_CODES.KITKAT)
  public static void requestCreateDocument(@NonNull final android.support.v4.app.Fragment fragment,
                                           final String mimeType, final String defaultName, final int requestCode) {

    if (BuildCheck.isKitKat()) {
      fragment.startActivityForResult(prepareCreateDocument(mimeType, defaultName), requestCode);
    }
  }

  /**
   * 请求Uri保存文件的Helper方法
   * 在KITKAT之后为每个单独的文件请求权限时
   * @param mimeType
   * @param defaultName
   * @return
   */
  @TargetApi(Build.VERSION_CODES.KITKAT)
  private static Intent prepareCreateDocument(final String mimeType, final String defaultName) {
    final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.setType(mimeType);
    if (!TextUtils.isEmpty(defaultName)) {
      intent.putExtra(Intent.EXTRA_TITLE, defaultName);
    }
    return intent;
  }

  /**
   * 文件删除请求
   * 在KITKAT之后为每个单独的文件请求权限时
   * @param context
   * @param uri
   * @return
   */
  @TargetApi(Build.VERSION_CODES.KITKAT)
  public static boolean requestDeleteDocument(@NonNull final Context context, final Uri uri) {
    try {
      return BuildCheck.isKitKat()
          && DocumentsContract.deleteDocument(context.getContentResolver(), uri);
    } catch (final FileNotFoundException e) {
      return false;
    }
  }

  /**
   * 是否可以访问对应于request_code的Uri
   * @param context
   * @param requestCode
   * @return
   */
  @TargetApi(Build.VERSION_CODES.KITKAT)
  public static boolean hasStorageAccess(@NonNull final Context context,
                                         final int requestCode) {

    boolean found = false;
    if (BuildCheck.isLollipop()) {
      final Uri uri = loadUri(context, getKey(requestCode));
      if (uri != null) {
        // 获取永久存储的Uri权限列表
        final List<UriPermission> list
            = context.getContentResolver().getPersistedUriPermissions();
        for (final UriPermission item: list) {
          if (item.getUri().equals(uri)) {
            // 永久保持对应于request_code的Uri的权限
            found = true;
            break;
          }
        }
      }
    }
    return found;
  }

  /**
   * 请求访问与request_code相对应的Uri
   * @param activity
   * @param requestCode
   * @return 如果已经存在与request_code相对应的Uri，则返回它，如果它不存在，则发出权限请求并返回null
   */
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public static Uri requestStorageAccess(@NonNull final Activity activity,
                                         final int requestCode) {

    if (BuildCheck.isLollipop()) {
      final Uri uri = getStorageUri(activity, requestCode);
      if (uri == null) {
        // 请求并返回null，如果它没有对request_code对应的Uri的权限
        activity.startActivityForResult(prepareStorageAccessPermission(), requestCode);
      }
      return uri;
    }
    return null;
  }

  /**
   * 请求访问与request_code相对应的Uri
   * @param activity
   * @param requestCode
   * @return 如果已经存在与request_code相对应的Uri，则返回它，如果它不存在，则发出权限请求并返回null
   */
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public static Uri requestStorageAccess(@NonNull final FragmentActivity activity,
                                         final int requestCode) {

    if (BuildCheck.isLollipop()) {
      final Uri uri = getStorageUri(activity, requestCode);
      if (uri == null) {
        // 请求并返回null，如果它没有对request_code对应的Uri的权限
        activity.startActivityForResult(prepareStorageAccessPermission(), requestCode);
      }
      return uri;
    }
    return null;
  }

  /**
   * 请求访问与request_code相对应的Uri
   * @param fragment
   * @param requestCode
   * @return 如果已经存在与request_code相对应的Uri，则返回它，如果它不存在，则发出权限请求并返回null
   */
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public static Uri requestStorageAccess(@NonNull final android.app.Fragment fragment,
                                         final int requestCode) {

    final Uri uri = getStorageUri(fragment.getActivity(), requestCode);
    if (uri == null) {
      // 请求并返回null，如果它没有对request_code对应的Uri的权限
      fragment.startActivityForResult(prepareStorageAccessPermission(), requestCode);
    }
    return uri;
  }

  /**
   * 请求访问与request_code相对应的Uri
   * @param fragment
   * @param requestCode
   * @return 如果已经存在与request_code相对应的Uri，则返回它，如果它不存在，则发出权限请求并返回null
   */
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public static Uri requestStorageAccess(@NonNull final android.support.v4.app.Fragment fragment,
                                         final int requestCode) {

    if (BuildCheck.isLollipop()) {
      final Activity activity = fragment.getActivity();
      final Uri uri = activity != null ? getStorageUri(activity, requestCode) : null;
      if (uri == null) {
        // 请求并返回null，如果它没有对request_code对应的Uri的权限
        fragment.startActivityForResult(prepareStorageAccessPermission(), requestCode);
      }
      return uri;
    }
    return null;
  }

  /**
   * 如果存在与request_code对应的Uri，如果有永久权限，则返回它，否则返回null
   * @param context
   * @param requestCode
   * @return
   */
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  @Nullable
  public static Uri getStorageUri(@NonNull final Context context,
                                  final int requestCode) {

    if (BuildCheck.isLollipop()) {
      final Uri uri = loadUri(context, getKey(requestCode));
      if (uri != null) {
        boolean found = false;
        // 获取永久存储的Uri权限列表
        final List<UriPermission> list
            = context.getContentResolver().getPersistedUriPermissions();
        for (final UriPermission item: list) {
          if (item.getUri().equals(uri)) {
            // 永久保持对应于request_code的Uri的权限
            found = true;
            break;
          }
        }
        if (found) {
          return uri;
        }
      }
    }
    return null;
  }

  /**
   * requestStorageAccess
   * @return
   */
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  private static Intent prepareStorageAccessPermission() {
    return new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
  }

  /**
   * 要求永久访问权限
   * @param context
   * @param treeUri
   * @return
   */
  @Nullable
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public static Uri requestStorageAccessPermission(@NonNull final Context context,
                                                   final int requestCode, final Uri treeUri) {

    return requestStorageAccessPermission(context,
        requestCode, treeUri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
  }

  /**
   * 要求永久访问权限
   * @param context
   * @param treeUri
   * @param flags
   * @return
   */
  @Nullable
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public static Uri requestStorageAccessPermission(@NonNull final Context context,
                                                   final int requestCode, final Uri treeUri, final int flags) {

    if (BuildCheck.isLollipop()) {
      context.getContentResolver().takePersistableUriPermission(treeUri, flags);
      saveUri(context, getKey(requestCode), treeUri);
      return treeUri;
    } else {
      return null;
    }
  }

  /**
   * 要求永久访问权限
   * @param context
   * @param requestCode
   */
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public static void releaseStorageAccessPermission(@NonNull final Context context,
                                                    final int requestCode) {

    if (BuildCheck.isLollipop()) {
      final String key = getKey(requestCode);
      final Uri uri = loadUri(context, key);
      if (uri != null) {
        context.getContentResolver().releasePersistableUriPermission(uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        clearUri(context, key);
      }
    }
  }

  //================================================================================
  public interface FileFilter {
    public boolean accept(@NonNull final DocumentFile file);
  }

  /**
   * 当存在与指定id对应的Uri时，返回相应的DocumentFile
   * @param context
   * @param treeId
   * @return
   */
  @Nullable
  public static DocumentFile getStorage(@NonNull final Context context,
                                        final int treeId) throws IOException {

    return getStorage(context, treeId, null);
  }

  /**
   * 如果存在与指定的id相对应的Uri并且它是可写的，则在其下创建目录
   * 返回指示该目录的DocumentFile对象
   * @param context
   * @param treeId
   * @param dirs 由斜杠（`/`）分隔的路径字符串
   * @return 对应于底层目录的DocumentFile，Uri不存在，如果无法写入则为null
   */
  @Nullable
  public static DocumentFile getStorage(@NonNull final Context context,
                                        final int treeId, @Nullable final String dirs) throws IOException {

    if (BuildCheck.isLollipop()) {
      final Uri treeUri = getStorageUri(context, treeId);
      if (treeUri != null) {
        DocumentFile tree = DocumentFile.fromTreeUri(context, treeUri);
        if (!TextUtils.isEmpty(dirs)) {
          final String[] dir = dirs.split("/");
          for (final String d: dir) {
            if (!TextUtils.isEmpty(d)) {
              final DocumentFile t = tree.findFile(d);
              if ((t != null) && t.isDirectory()) {
                // 它已经存在时我什么都不做
                tree = t;
              } else if (t == null) {
                if (tree.canWrite()) {
                  // 当目录不存在时生成目录
                  tree = tree.createDirectory(d);
                } else {
                  throw new IOException("can't create directory");
                }
              } else {
                throw new IOException("can't create directory, file with same name already exists");
              }
            }
          }
        }
        return tree;
      }
    }
    return null;
  }

  /**
   * 如果存在与指定的id相对应的Uri并且它是可写的，则在其下创建目录
   * 返回指示该目录的DocumentFile对象
   * @param context
   * @param parent
   * @param dirs
   * @return 如果无法写入，则为null
   */
  public static DocumentFile getStorage(@NonNull final Context context,
                                        @NonNull final DocumentFile parent, @Nullable final String dirs)
      throws IOException {

    DocumentFile tree = parent;
    if (!TextUtils.isEmpty(dirs)) {
      final String[] dir = dirs.split("/");
      for (final String d: dir) {
        if (!TextUtils.isEmpty(d)) {
          final DocumentFile t = tree.findFile(d);
          if ((t != null) && t.isDirectory()) {
            // 它已经存在时我什么都不做
            tree = t;
          } else if (t == null) {
            if (tree.canWrite()) {
              // 当目录不存在时生成目录
              tree = tree.createDirectory(d);
            } else {
              throw new IOException("can't create directory");
            }
          } else {
            throw new IOException("can't create directory, file with same name already exists");
          }
        }
      }
    }
    return tree;
  }

  /**
   * 获取指定目录下的文件列表
   * @param context
   * @param dir
   * @param filter 如果为null，则添加所有现有文件
   * @return
   * @throws IOException
   */
  @NonNull
  public static Collection<DocumentFile> listFiles(@NonNull final Context context,
                                                   @NonNull final DocumentFile dir,
                                                   @Nullable final FileFilter filter) throws IOException {

    final Collection<DocumentFile> result = new ArrayList<DocumentFile>();
    if (dir.isDirectory()) {
      final DocumentFile[] files = dir.listFiles();
      for (final DocumentFile file: files) {
        if ((filter == null) || (filter.accept(file))) {
          result.add(file);
        }
      }
    }
    return result;
  }

  /**
   * 返回满负荷和可用空间
   * 如果它不是目录或无法访问，则返回null
   * @param context
   * @param dir
   * @return
   */
  @SuppressLint("NewApi")
  @Nullable
  public static StorageInfo getStorageInfo(@NonNull final Context context,
                                           @NonNull final DocumentFile dir) {

    try {
      final String path = UriHelper.getPath(context, dir.getUri());
      if (path != null) {
        final File file = new File(path);
        if (file.isDirectory() && file.canRead()) {
          final long total = file.getTotalSpace();
          long free = file.getFreeSpace();
          if (free < file.getUsableSpace()) {
            free = file.getUsableSpace();
          }
          return new StorageInfo(total, free);
        }
      }
    } catch (final Exception e) {
      // ignore
    }
    if (BuildCheck.isJellyBeanMR2()) {
      try {
        final String path = UriHelper.getPath(context, dir.getUri());
        final StatFs fs = new StatFs(path);
        return new StorageInfo(fs.getTotalBytes(), fs.getAvailableBytes());
      } catch (final Exception e) {
        // ignore
      }
    }
    return null;
  }

  /**
   * 当存在指定的Uri时，它会创建一个DocumentFile对象以引用相应的文件
   * @param context
   * @param treeId
   * @param mime
   * @param name
   * @return
   */
  @Nullable
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public static DocumentFile getStorageFile(@NonNull final Context context,
                                            final int treeId, final String mime, final String name) throws IOException {

    return getStorageFile(context, treeId, null, mime, name);
  }

  /**
   * 当存在指定的Uri时，它会创建一个DocumentFile对象以引用相应的文件
   * @param context
   * @param treeId
   * @param mime
   * @param name
   * @return
   */
  @Nullable
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public static DocumentFile getStorageFile(@NonNull final Context context,
                                            final int treeId, @Nullable final String dirs,
                                            final String mime, final String name) throws IOException {

    if (BuildCheck.isLollipop()) {
      final DocumentFile tree = getStorage(context, treeId, dirs);
      if (tree != null) {
        final DocumentFile file = tree.findFile(name);
        if (file != null) {
          if (file.isFile()) {
            return file;
          } else {
            throw new IOException("directory with same name already exists");
          }
        } else {
          return tree.createFile(mime, name);
        }
      }
    }
    return null;
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
  public static DocumentFile getStorageFile(@NonNull final Context context,
                                            @NonNull final DocumentFile parent, @Nullable final String dirs,
                                            final String mime, final String name) throws IOException {

    final DocumentFile tree = getStorage(context, parent, dirs);
    if (tree != null) {
      final DocumentFile file = tree.findFile(name);
      if (file != null) {
        if (file.isFile()) {
          return file;
        } else {
          throw new IOException("directory with same name already exists");
        }
      } else {
        return tree.createFile(mime, name);
      }
    }
    return null;
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
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public static OutputStream getStorageOutputStream(@NonNull final Context context,
                                                    final int treeId,
                                                    final String mime, final String name) throws IOException {

    return getStorageOutputStream(context, treeId, null, mime, name);
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
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public static OutputStream getStorageOutputStream(@NonNull final Context context,
                                                    final int treeId, @Nullable final String dirs,
                                                    final String mime, final String name) throws IOException {

    if (BuildCheck.isLollipop()) {
      final DocumentFile tree = getStorage(context, treeId, dirs);
      if (tree != null) {
        final DocumentFile file = tree.findFile(name);
        if (file != null) {
          if (file.isFile()) {
            return context.getContentResolver().openOutputStream(
                file.getUri());
          } else {
            throw new IOException("directory with same name already exists");
          }
        } else {
          return context.getContentResolver().openOutputStream(
              tree.createFile(mime, name).getUri());
        }
      }
    }
    throw new FileNotFoundException();
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
  public static OutputStream getStorageOutputStream(@NonNull final Context context,
                                                    @NonNull final DocumentFile parent, @Nullable final String dirs,
                                                    final String mime, final String name) throws IOException {

    final DocumentFile tree = getStorage(context, parent, dirs);
    if (tree != null) {
      final DocumentFile file = tree.findFile(name);
      if (file != null) {
        if (file.isFile()) {
          return context.getContentResolver().openOutputStream(
              file.getUri());
        } else {
          throw new IOException("directory with same name already exists");
        }
      } else {
        return context.getContentResolver().openOutputStream(
            tree.createFile(mime, name).getUri());
      }
    }
    throw new FileNotFoundException();
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
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public static InputStream getStorageInputStream(@NonNull final Context context,
                                                  final int treeId,
                                                  final String mime, final String name) throws IOException {

    return getStorageInputStream(context, treeId, null, mime, name);
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
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public static InputStream getStorageInputStream(@NonNull final Context context,
                                                  final int treeId, @Nullable final String dirs,
                                                  final String mime, final String name) throws IOException {

    if (BuildCheck.isLollipop()) {
      final DocumentFile tree = getStorage(context, treeId, dirs);
      if (tree != null) {
        final DocumentFile file = tree.findFile(name);
        if (file != null) {
          if (file.isFile()) {
            return context.getContentResolver().openInputStream(
                file.getUri());
          } else {
            throw new IOException("directory with same name already exists");
          }
        }
      }
    }
    throw new FileNotFoundException();
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
  public static InputStream getStorageInputStream(@NonNull final Context context,
                                                  @NonNull final DocumentFile parent, @Nullable final String dirs,
                                                  final String mime, final String name) throws IOException {

    final DocumentFile tree = getStorage(context, parent, dirs);
    if (tree != null) {
      final DocumentFile file = tree.findFile(name);
      if (file != null) {
        if (file.isFile()) {
          return context.getContentResolver().openInputStream(
              file.getUri());
        } else {
          throw new IOException("directory with same name already exists");
        }
      }
    }
    throw new FileNotFoundException();
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
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public static ParcelFileDescriptor getStorageFileFD(@NonNull final Context context,
                                                      final int treeId, @Nullable final String dirs,
                                                      final String mime, final String name) throws IOException {

    if (BuildCheck.isLollipop()) {
      final DocumentFile tree = getStorage(context, treeId, dirs);
      if (tree != null) {
        final DocumentFile file = tree.findFile(name);
        if (file != null) {
          if (file.isFile()) {
            return context.getContentResolver().openFileDescriptor(
                file.getUri(), "rw");
          } else {
            throw new IOException("directory with same name already exists");
          }
        } else {
          return context.getContentResolver().openFileDescriptor(
              tree.createFile(mime, name).getUri(), "rw");
        }
      }
    }
    throw new FileNotFoundException();
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
  public static ParcelFileDescriptor getStorageFileFD(@NonNull final Context context,
                                                      @NonNull final DocumentFile parent, @Nullable final String dirs,
                                                      final String mime, final String name) throws IOException {

    final DocumentFile tree = getStorage(context, parent, dirs);
    if (tree != null) {
      final DocumentFile file = tree.findFile(name);
      if (file != null) {
        if (file.isFile()) {
          return context.getContentResolver().openFileDescriptor(
              file.getUri(), "rw");
        } else {
          throw new IOException("directory with same name already exists");
        }
      } else {
        return context.getContentResolver().openFileDescriptor(
            tree.createFile(mime, name).getUri(), "rw");
      }
    }
    throw new FileNotFoundException();
  }

  //================================================================================
  /**
   * 当存在与指定id对应的Uri时，它返回用于在其下创建文件的路径
   * @param context
   * @param treeId
   * @return
   */
  @Nullable
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public static File createStorageDir(@NonNull final Context context,
                                      final int treeId) {

    if (BuildCheck.isLollipop()) {
      final Uri treeUri = getStorageUri(context, treeId);
      if (treeUri != null) {
        final DocumentFile saveTree = DocumentFile.fromTreeUri(context, treeUri);
        final String path = UriHelper.getPath(context, saveTree.getUri());
        if (!TextUtils.isEmpty(path)) {
          return new File(path);
        }
      }
    }
    return null;
  }

  /**
   * 当存在与指定id相对应的Uri时，它会创建并返回其下面指定的File
   * @param context
   * @param treeId
   * @param mime
   * @param fileName
   * @return
   */
  @Nullable
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public static File createStorageFile(@NonNull final Context context,
                                       final int treeId, final String mime, final String fileName) {

    return createStorageFile(context, getStorageUri(context, treeId), mime, fileName);
  }

  /**
   * 返回在指定的Uri存在时创建文件的路径
   * @param context
   * @param treeUri
   * @param mime
   * @param fileName
   * @return
   */
  @Nullable
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public static File createStorageFile(@NonNull final Context context,
                                       final Uri treeUri, final String mime, final String fileName) {
    Log.i(TAG, "createStorageFile:" + fileName);

    if (BuildCheck.isLollipop()) {
      if ((treeUri != null) && !TextUtils.isEmpty(fileName)) {
        final DocumentFile saveTree = DocumentFile.fromTreeUri(context, treeUri);
        final DocumentFile target = saveTree.createFile(mime, fileName);
        final String path = UriHelper.getPath(context, target.getUri());
        if (!TextUtils.isEmpty(path)) {
          return new File(path);
        }
      }
    }
    return null;
  }

  /**
   * 当存在与指定id相对应的Uri时，它返回其下生成的文件的原始文件描述符
   * @param context
   * @param treeId
   * @param mime
   * @param fileName
   * @return
   */
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public static int createStorageFileFD(@NonNull final Context context,
                                        final int treeId, final String mime, final String fileName) {

    Log.i(TAG, "createStorageFileFD:" + fileName);
    return createStorageFileFD(context, getStorageUri(context, treeId), mime, fileName);
  }

  /**
   * 当存在与指定id相对应的Uri时，它返回其下生成的文件的原始文件描述符
   * @param context
   * @param treeUri
   * @param mime
   * @param fileName
   * @return
   */
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public static int createStorageFileFD(@NonNull final Context context,
                                        final Uri treeUri, final String mime, final String fileName) {

    Log.i(TAG, "createStorageFileFD:" + fileName);
    if (BuildCheck.isLollipop()) {
      if ((treeUri != null) && !TextUtils.isEmpty(fileName)) {
        final DocumentFile saveTree = DocumentFile.fromTreeUri(context, treeUri);
        final DocumentFile target = saveTree.createFile(mime, fileName);
        try {
          assert target != null;
          final ParcelFileDescriptor fd
              = context.getContentResolver().openFileDescriptor(target.getUri(), "rw");
          return fd != null ? fd.getFd() : 0;
        } catch (final FileNotFoundException e) {
          Log.w(TAG, e);
        }
      }
    }
    return 0;
  }
}
