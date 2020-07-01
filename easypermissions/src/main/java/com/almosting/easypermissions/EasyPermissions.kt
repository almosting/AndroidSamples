package com.almosting.easypermissions

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.util.Log
import androidx.annotation.Size
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.almosting.easypermissions.PermissionRequest.Builder
import com.almosting.easypermissions.helper.PermissionHelper
import java.lang.reflect.InvocationTargetException
import java.util.ArrayList

/**
 * Created by w.feng on 2018/10/10
 * Email: fengweisb@gmail.com
 */
object EasyPermissions {
  private const val TAG = "EasyPermissions"
  fun hasPermissions(
    context: Context,
    @Size(min = 1)  perms: Array<String>
  ): Boolean {
    if (VERSION.SDK_INT < VERSION_CODES.M) {
      return true
    }
    Log.i(TAG, "hasPermissions: " + perms.size)
    for (perm in perms) {
      Log.i(TAG, "hasPermissions: $perm")
      if (ContextCompat.checkSelfPermission(
          context,
          perm
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        return false
      }
    }
    return true
  }

  fun requestPermissions(
    host: Activity, rationale: String,
    requestCode: Int,   perms: Array<String>
  ) {
    requestPermissions(
      Builder(host, requestCode, perms)
        .setRationale(rationale).build()
    )
  }

  fun requestPermissions(request: PermissionRequest) {
    if (hasPermissions(request.helper.context!!, request.perms)) {
      notifyAlreadyHasPermissions(
        request.helper.host!!, request.requestCode, request.perms
      )
      return
    }
    request.helper.requestPermissions(
      request.rationale,
      request.positiveButtonText,
      request.negativeButtonText,
      request.theme,
      request.requestCode,
      request.perms
    )
  }

  fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray,
    vararg receivers: Any
  ) {
    val granted: MutableList<String> =
      ArrayList()
    val denied: MutableList<String> =
      ArrayList()
    for (i in permissions.indices) {
      val perm = permissions[i]
      if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
        granted.add(perm)
      } else {
        denied.add(perm)
      }
    }
    for (`object` in receivers) {
      if (granted.isNotEmpty()) {
        if (`object` is PermissionCallbacks) {
          `object`.onPermissionsGranted(requestCode, granted)
        }
      }
      if (!denied.isEmpty()) {
        if (`object` is PermissionCallbacks) {
          `object`.onPermissionsDenied(requestCode, denied)
        }
      }
      if (granted.isNotEmpty() && denied.isEmpty()) {
        runAnnotatedMethods(`object`, requestCode)
      }
    }
  }

  fun somePermissionPermanentlyDenied(
    host: Activity,
    deniedPermissions: List<String>
  ): Boolean {
    return PermissionHelper.Companion.newInstance(host)
      .somePermissionPermanentlyDenied(deniedPermissions)
  }

  fun somePermissionPermanentlyDenied(
    host: Fragment,
    deniedPermissions: List<String>
  ): Boolean {
    return PermissionHelper.Companion.newInstance(host)
      .somePermissionPermanentlyDenied(deniedPermissions)
  }

  fun permissionPermanentlyDenied(
    host: Activity,
    deniedPermission: String
  ): Boolean {
    return PermissionHelper.newInstance(host)
      .permissionPermanentlyDenied(deniedPermission)
  }

  fun permissionPermanentlyDenied(
    host: Fragment,
    deniedPermission: String
  ): Boolean {
    return PermissionHelper.newInstance(host)
      .permissionPermanentlyDenied(deniedPermission)
  }

  fun somePermissionDenied(
    host: Activity,
    perms: Array<String>
  ): Boolean {
    return PermissionHelper.newInstance(host)
      .somePermissionDenied(perms)
  }

  fun somePermissionDenied(
    host: Fragment,
    perms: Array<String>
  ): Boolean {
    return PermissionHelper.newInstance(host)
      .somePermissionDenied(perms)
  }

  private fun notifyAlreadyHasPermissions(
    `object`: Any,
    requestCode: Int,
    perms: Array<String>
  ) {
    val grantResults = IntArray(perms.size)
    for (i in perms.indices) {
      grantResults[i] = PackageManager.PERMISSION_GRANTED
    }
    onRequestPermissionsResult(requestCode, perms, grantResults, `object`)
  }

  private fun runAnnotatedMethods(`object`: Any, requestCode: Int) {
    var clazz: Class<*>? = `object`.javaClass
    if (isUsingAndroidAnnotations(`object`)) {
      clazz = clazz!!.superclass
    }
    while (clazz != null) {
      for (method in clazz.declaredMethods) {
        val granted = method.getAnnotation(
          AfterPermissionGranted::class.java
        )
        if (granted != null) {
          if (granted.value == requestCode) {
            if (method.parameterTypes.isNotEmpty()) {
              throw RuntimeException(
                "Cannot execute method "
                    + method.name
                    + " because it is non-void method and/or has input parameters."
              )
            }
            try {
              if (!method.isAccessible) {
                method.isAccessible = true
              }
              method.invoke(`object`)
            } catch (e: IllegalAccessException) {
              e.printStackTrace()
            } catch (e: InvocationTargetException) {
              e.printStackTrace()
            }
          }
        }
      }
      clazz = clazz.superclass
    }
  }

  private fun isUsingAndroidAnnotations(`object`: Any): Boolean {
    return if (!`object`.javaClass.simpleName.endsWith("_")) {
      false
    } else try {
      val clazz =
        Class.forName("org.androidannotations.api.view.HasViews")
      clazz.isInstance(`object`)
    } catch (e: ClassNotFoundException) {
      false
    }
  }

  interface PermissionCallbacks : OnRequestPermissionsResultCallback {
    fun onPermissionsGranted(
      requestCode: Int,
      perms: List<String>
    )

    fun onPermissionsDenied(
      requestCode: Int,
      perms: List<String>
    )
  }

  interface RationaleCallbacks {
    fun onRationaleAccepted(requestCode: Int)
    fun onRationaleDenied(requestCode: Int)
  }
}