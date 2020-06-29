package com.almosting.easypermissions;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Size;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;
import androidx.fragment.app.Fragment;
import com.almosting.easypermissions.helper.PermissionHelper;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by w.feng on 2018/10/10
 * Email: fengweisb@gmail.com
 */
public class EasyPermissions {
  public interface PermissionCallbacks extends ActivityCompat.OnRequestPermissionsResultCallback {
    void onPermissionsGranted(int requestCode, @NonNull List<String> perms);

    void onPermissionsDenied(int requestCode, @NonNull List<String> perms);
  }

  public interface RationaleCallbacks {
    void onRationaleAccepted(int requestCode);

    void onRationaleDenied(int requestCode);
  }

  private static final String TAG = "EasyPermissions";

  public static boolean hasPermissions(@NonNull Context context,
                                       @Size(min = 1) @NonNull String... perms) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      return true;
    }
    Log.i(TAG, "hasPermissions: " + perms.length);
    for (String perm : perms) {
      Log.i(TAG, "hasPermissions: " + perm);
      if (ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED) {
        return false;
      }
    }
    return true;
  }

  public static void requestPermissions(@NonNull Activity host, @NonNull String rationale,
                                        int requestCode, @Size(min = 1) @NonNull String... perms) {
    requestPermissions(new PermissionRequest.Builder(host, requestCode, perms)
        .setRationale(rationale).build());
  }

  public static void requestPermissions(PermissionRequest request) {

    if (hasPermissions(request.getHelper().getContext(), request.getPerms())) {
      notifyAlreadyHasPermissions(
          request.getHelper().getHost(), request.getRequestCode(), request.getPerms());
      return;
    }

    request.getHelper().requestPermissions(
        request.getRationale(),
        request.getPositiveButtonText(),
        request.getNegativeButtonText(),
        request.getTheme(),
        request.getRequestCode(),
        request.getPerms());
  }

  public static void onRequestPermissionsResult(int requestCode,
                                                @NonNull String[] permissions,
                                                @NonNull int[] grantResults,
                                                @NonNull Object... receivers) {
    List<String> granted = new ArrayList<>();
    List<String> denied = new ArrayList<>();
    for (int i = 0; i < permissions.length; i++) {
      String perm = permissions[i];
      if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
        granted.add(perm);
      } else {
        denied.add(perm);
      }
    }

    for (Object object : receivers) {
      if (!granted.isEmpty()) {
        if (object instanceof PermissionCallbacks) {
          ((PermissionCallbacks) object).onPermissionsGranted(requestCode, granted);
        }
      }

      if (!denied.isEmpty()) {
        if (object instanceof PermissionCallbacks) {
          ((PermissionCallbacks) object).onPermissionsDenied(requestCode, denied);
        }
      }

      if (!granted.isEmpty() && denied.isEmpty()) {
        runAnnotatedMethods(object, requestCode);
      }
    }
  }

  public static boolean somePermissionPermanentlyDenied(@NonNull Activity host,
                                                        @NonNull List<String> deniedPermissions) {
    return PermissionHelper.newInstance(host)
        .somePermissionPermanentlyDenied(deniedPermissions);
  }

  public static boolean somePermissionPermanentlyDenied(@NonNull Fragment host,
                                                        @NonNull List<String> deniedPermissions) {
    return PermissionHelper.newInstance(host)
        .somePermissionPermanentlyDenied(deniedPermissions);
  }

  public static boolean permissionPermanentlyDenied(@NonNull Activity host,
                                                    @NonNull String deniedPermission) {
    return PermissionHelper.newInstance(host).permissionPermanentlyDenied(deniedPermission);
  }

  public static boolean permissionPermanentlyDenied(@NonNull Fragment host,
                                                    @NonNull String deniedPermission) {
    return PermissionHelper.newInstance(host).permissionPermanentlyDenied(deniedPermission);
  }

  public static boolean somePermissionDenied(@NonNull Activity host,
                                             @NonNull String... perms) {
    return PermissionHelper.newInstance(host).somePermissionDenied(perms);
  }

  public static boolean somePermissionDenied(@NonNull Fragment host,
                                             @NonNull String... perms) {
    return PermissionHelper.newInstance(host).somePermissionDenied(perms);
  }

  private static void notifyAlreadyHasPermissions(@NonNull Object object,
                                                  int requestCode,
                                                  @NonNull String[] perms) {
    int[] grantResults = new int[perms.length];
    for (int i = 0; i < perms.length; i++) {
      grantResults[i] = PackageManager.PERMISSION_GRANTED;
    }

    onRequestPermissionsResult(requestCode, perms, grantResults, object);
  }

  private static void runAnnotatedMethods(@NonNull Object object, int requestCode) {
    Class clazz = object.getClass();
    if (isUsingAndroidAnnotations(object)) {
      clazz = clazz.getSuperclass();
    }

    while (clazz != null) {
      for (Method method : clazz.getDeclaredMethods()) {
        AfterPermissionGranted granted = method.getAnnotation(AfterPermissionGranted.class);
        if (granted != null) {
          if (granted.value() == requestCode) {
            if (method.getParameterTypes().length > 0) {
              throw new RuntimeException(
                  "Cannot execute method "
                      + method.getName()
                      + " because it is non-void method and/or has input parameters.");
            }

            try {
              if (!method.isAccessible()) {
                method.setAccessible(true);
              }
              method.invoke(object);
            } catch (IllegalAccessException | InvocationTargetException e) {
              e.printStackTrace();
            }
          }
        }
      }
      clazz = clazz.getSuperclass();
    }
  }

  private static boolean isUsingAndroidAnnotations(@NonNull Object object) {
    if (!object.getClass().getSimpleName().endsWith("_")) {
      return false;
    }
    try {
      Class clazz = Class.forName("org.androidannotations.api.view.HasViews");
      return clazz.isInstance(object);
    } catch (ClassNotFoundException e) {
      return false;
    }
  }
}
