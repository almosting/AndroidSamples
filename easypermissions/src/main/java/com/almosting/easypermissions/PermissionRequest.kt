package com.almosting.easypermissions

import android.R
import android.app.Activity
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.annotation.Size
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.fragment.app.Fragment
import com.almosting.easypermissions.R.string
import com.almosting.easypermissions.helper.PermissionHelper
import java.util.Arrays

/**
 * Created by w.feng on 2018/10/10
 * Email: fengweisb@gmail.com
 */
class PermissionRequest private constructor(
  @get:RestrictTo(LIBRARY_GROUP) val helper: PermissionHelper<*>,
  perms: Array<String>,
  requestCode: Int,
  rationale: String,
  positiveButtonText: String,
  negativeButtonText: String,
  theme: Int
) {
  private val mPerms: Array<String>
  val requestCode: Int
  val rationale: String
  val positiveButtonText: String
  val negativeButtonText: String
  @get:StyleRes val theme: Int

  val perms: Array<String>
    get() = mPerms.clone()

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val request =
      other as PermissionRequest
    return Arrays.equals(mPerms, request.mPerms) && requestCode == request.requestCode
  }

  override fun hashCode(): Int {
    var result = Arrays.hashCode(mPerms)
    result = 31 * result + requestCode
    return result
  }

  override fun toString(): String {
    return "PermissionRequest{" +
        "mHelper=" + helper +
        ", mPerms=" + Arrays.toString(mPerms) +
        ", mRequestCode=" + requestCode +
        ", mRationale='" + rationale + '\'' +
        ", mPositiveButtonText='" + positiveButtonText + '\'' +
        ", mNegativeButtonText='" + negativeButtonText + '\'' +
        ", mTheme=" + theme +
        '}'
  }

  class Builder {
    private val mHelper: PermissionHelper<*>
    private val mRequestCode: Int
    private val mPerms: Array<String>
    private var mRationale: String? = null
    private var mPositiveButtonText: String? = null
    private var mNegativeButtonText: String? = null
    private var mTheme = -1

    constructor(
      activity: Activity, requestCode: Int,
      @Size(min = 1) vararg perms: String
    ) {
      mHelper =
        PermissionHelper.newInstance(activity)
      mRequestCode = requestCode
      mPerms = perms as Array<String>
    }

    constructor(
      fragment: Fragment, requestCode: Int,
      @Size(min = 1) vararg perms: String
    ) {
      mHelper =
        PermissionHelper.Companion.newInstance(fragment)
      mRequestCode = requestCode
      mPerms = perms as Array<String>
    }

    fun setRationale(rationale: String?): Builder {
      mRationale = rationale
      return this
    }

    fun setRationale(
      @StringRes resId: Int
    ): Builder {
      mRationale = mHelper.context!!.getString(resId)
      return this
    }

    fun setPositiveButtonText(positiveButtonText: String?): Builder {
      mPositiveButtonText = positiveButtonText
      return this
    }

    fun setPositiveButtonText(
      @StringRes resId: Int
    ): Builder {
      mPositiveButtonText = mHelper.context!!.getString(resId)
      return this
    }

    fun setNegativeButtonText(negativeButtonText: String?): Builder {
      mNegativeButtonText = negativeButtonText
      return this
    }

    fun setNegativeButtonText(
      @StringRes resId: Int
    ): Builder {
      mNegativeButtonText = mHelper.context?.getString(resId)
      return this
    }

    fun setTheme(@StyleRes theme: Int): Builder {
      mTheme = theme
      return this
    }

    fun build(): PermissionRequest {
      if (mRationale == null) {
        mRationale =
          mHelper.context!!.getString(string.rationale_ask)
      }
      if (mPositiveButtonText == null) {
        mPositiveButtonText = mHelper.context?.getString(R.string.ok)
      }
      if (mNegativeButtonText == null) {
        mNegativeButtonText = mHelper.context?.getString(R.string.cancel)
      }
      return PermissionRequest(
        mHelper,
        mPerms,
        mRequestCode,
        mRationale!!,
        mPositiveButtonText!!,
        mNegativeButtonText!!,
        mTheme
      )
    }
  }

  init {
    mPerms = perms.clone()
    this.requestCode = requestCode
    this.rationale = rationale
    this.positiveButtonText = positiveButtonText
    this.negativeButtonText = negativeButtonText
    this.theme = theme
  }
}