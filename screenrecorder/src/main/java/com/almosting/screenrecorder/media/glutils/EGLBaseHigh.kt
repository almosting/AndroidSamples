package com.almosting.screenrecorder.media.glutils

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLES10
import android.opengl.GLES20
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.almosting.toolbox.utils.BuildCheck

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
class EGLBaseHigh(
  maxClientVersion: Int,
  sharedContext: Context?,
  withDepthBuffer: Boolean,
  stencilBits: Int,
  isRecordable: Boolean
) : EGLBase() {
  private var mEglConfig: Config? = null
  private var mContext: Context =
    EGL_NO_CONTEXT!!
  private var mEglDisplay = EGL14.EGL_NO_DISPLAY
  private var mDefaultContext = EGL14.EGL_NO_CONTEXT
  private var mGlVersion = 2

  class Context(val eglContext: EGLContext?) : IContext() {
    @SuppressLint("NewApi") override fun getNativeHandle(): Long {
      return if (eglContext != null) if (BuildCheck.isLollipop()) eglContext.nativeHandle else eglContext.handle.toLong() else 0L
    }

    override fun getEGLContext(): Any? {
      return eglContext
    }
  }

  class Config(val eglConfig: EGLConfig?) : IConfig()

  class EglSurface : IEglSurface {
    private val mEglBase: EGLBaseHigh?
    private var mEglSurface: EGLSurface? = null

    constructor(eglBase: EGLBaseHigh?, surface: Any?) {
      mEglBase = eglBase
      mEglSurface = if (surface is Surface
        || surface is SurfaceHolder
        || surface is SurfaceTexture
        || surface is SurfaceView
      ) {
        mEglBase!!.createWindowSurface(surface)
      } else {
        throw IllegalArgumentException("unsupported surface")
      }
    }

    constructor(
      eglBase: EGLBaseHigh?,
      width: Int, height: Int
    ) {
      mEglBase = eglBase
      mEglSurface = if (width <= 0 || height <= 0) {
        mEglBase!!.createOffscreenSurface(1, 1)
      } else {
        mEglBase!!.createOffscreenSurface(width, height)
      }
    }

    override fun makeCurrent() {
      mEglBase!!.makeCurrent(mEglSurface)
      if (mEglBase.getGlVersion() >= 2) {
        GLES20.glViewport(
          0, 0,
          mEglBase.getSurfaceWidth(mEglSurface),
          mEglBase.getSurfaceHeight(mEglSurface)
        )
      } else {
        GLES10.glViewport(
          0, 0,
          mEglBase.getSurfaceWidth(mEglSurface),
          mEglBase.getSurfaceHeight(mEglSurface)
        )
      }
    }

    override fun swap() {
      mEglBase!!.swap(mEglSurface)
    }

    override fun swap(presentationTimeNs: Long) {
      mEglBase!!.swap(mEglSurface, presentationTimeNs)
    }

    fun setPresentationTime(presentationTimeNs: Long) {
      EGLExt.eglPresentationTimeANDROID(
        mEglBase!!.mEglDisplay,
        mEglSurface, presentationTimeNs
      )
    }

    override fun getContext(): IContext? {
      return mEglBase!!.getContext()
    }

    override fun isValid(): Boolean {
      return (mEglSurface != null
          && mEglSurface !== EGL14.EGL_NO_SURFACE
          && mEglBase!!.getSurfaceWidth(mEglSurface) > 0
          && mEglBase.getSurfaceHeight(mEglSurface) > 0)
    }

    override fun release() {
      mEglBase!!.makeDefault()
      mEglBase.destroyWindowSurface(mEglSurface)
      mEglSurface = EGL14.EGL_NO_SURFACE
    }
  }

  override fun release() {
    if (mEglDisplay !== EGL14.EGL_NO_DISPLAY) {
      destroyContext()
      EGL14.eglTerminate(mEglDisplay)
      EGL14.eglReleaseThread()
    }
    mEglDisplay = EGL14.EGL_NO_DISPLAY
    mContext = EGL_NO_CONTEXT!!
  }

  override fun createFromSurface(nativeWindow: Any?): EglSurface? {
    val eglSurface =
      EglSurface(this, nativeWindow)
    eglSurface.makeCurrent()
    return eglSurface
  }

  override fun createOffscreen(
    width: Int,
    height: Int
  ): EglSurface? {
    val eglSurface =
      EglSurface(this, width, height)
    eglSurface.makeCurrent()
    return eglSurface
  }

  override fun queryString(what: Int): String? {
    return EGL14.eglQueryString(mEglDisplay, what)
  }

  override fun getGlVersion(): Int {
    return mGlVersion
  }

  override fun getContext(): Context {
    return mContext
  }

  override fun getConfig(): Config? {
    return mEglConfig
  }

  /**
   * 取消链接EGL渲染上下文和线程
   */
  override fun makeDefault() {
    if (!EGL14.eglMakeCurrent(
        mEglDisplay,
        EGL14.EGL_NO_SURFACE,
        EGL14.EGL_NO_SURFACE,
        EGL14.EGL_NO_CONTEXT
      )
    ) {
      Log.w("TAG", "makeDefault" + EGL14.eglGetError())
    }
  }

  override fun sync() {
    EGL14.eglWaitGL() // 效果类似于GLES20.glFinish（）
    EGL14.eglWaitNative(EGL14.EGL_CORE_NATIVE_ENGINE)
  }

  private fun init(
    maxClientVersion: Int,
    sharedContext: Context?,
    withDepthBuffer: Boolean,
    stencilBits: Int,
    isRecordable: Boolean
  ) {
    var sharedContext = sharedContext
    if (mEglDisplay !== EGL14.EGL_NO_DISPLAY) {
      throw RuntimeException("EGL already set up")
    }
    mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
    if (mEglDisplay === EGL14.EGL_NO_DISPLAY) {
      throw RuntimeException("eglGetDisplay failed")
    }
    // 获取EGL的版本
    val version = IntArray(2)
    if (!EGL14.eglInitialize(mEglDisplay, version, 0, version, 1)) {
      mEglDisplay = null
      throw RuntimeException("eglInitialize failed")
    }
    sharedContext =
      sharedContext ?: EGL_NO_CONTEXT
    var config: EGLConfig?
    if (maxClientVersion >= 3) {
      // 试着看看它是否可以用GLES 3获得
      config = getConfig(3, withDepthBuffer, stencilBits, isRecordable)
      if (config != null) {
        val context = createContext(sharedContext, config, 3)
        if (EGL14.eglGetError() == EGL14.EGL_SUCCESS) {
          // 由于我不想在这里生成异常，因此请检查它自己而不是checkEglError
          mEglConfig = Config(config)
          mContext = Context(context)
          mGlVersion = 3
        }
      }
    }
    //使用GLES 3无法获取GLES 2
    if (maxClientVersion >= 2 && mContext.eglContext === EGL14.EGL_NO_CONTEXT) {
      config = getConfig(2, withDepthBuffer, stencilBits, isRecordable)
      if (config == null) {
        throw RuntimeException("chooseConfig failed")
      }
      try {
        // create EGL rendering context
        val context = createContext(sharedContext, config, 2)
        checkEglError("eglCreateContext")
        mEglConfig = Config(config)
        mContext = Context(context)
        mGlVersion = 2
      } catch (e: Exception) {
        if (isRecordable) {
          config = getConfig(2, withDepthBuffer, stencilBits, false)
          if (config == null) {
            throw RuntimeException("chooseConfig failed")
          }
          // create EGL rendering context
          val context = createContext(sharedContext, config, 2)
          checkEglError("eglCreateContext")
          mEglConfig = Config(config)
          mContext = Context(context)
          mGlVersion = 2
        }
      }
    }
    if (mContext.eglContext === EGL14.EGL_NO_CONTEXT) {
      config = getConfig(1, withDepthBuffer, stencilBits, isRecordable)
      if (config == null) {
        throw RuntimeException("chooseConfig failed")
      }
      // create EGL rendering context
      val context = createContext(sharedContext, config, 1)
      checkEglError("eglCreateContext")
      mEglConfig = Config(config)
      mContext = Context(context)
      mGlVersion = 1
    }
    // confirm whether the EGL rendering context is successfully created
    val values = IntArray(1)
    EGL14.eglQueryContext(
      mEglDisplay,
      mContext.eglContext, EGL14.EGL_CONTEXT_CLIENT_VERSION, values, 0
    )
    Log.d(
      TAG,
      "EGLContext created, client version " + values[0]
    )
    makeDefault() // makeCurrent(EGL14.EGL_NO_SURFACE);
  }

  private fun makeCurrent(surface: EGLSurface?): Boolean {
    if (surface == null || surface === EGL14.EGL_NO_SURFACE) {
      val error = EGL14.eglGetError()
      if (error == EGL14.EGL_BAD_NATIVE_WINDOW) {
        Log.e(TAG, "makeCurrent:returned EGL_BAD_NATIVE_WINDOW.")
      }
      return false
    }
    // attach EGL rendering context to specific EGL window surface
    if (!EGL14.eglMakeCurrent(mEglDisplay, surface, surface, mContext.eglContext)) {
      Log.w("TAG", "eglMakeCurrent" + EGL14.eglGetError())
      return false
    }
    return true
  }

  private fun swap(surface: EGLSurface?): Int {
    //		if (DEBUG) Log.v(TAG, "swap:");
    return if (!EGL14.eglSwapBuffers(mEglDisplay, surface)) {
      //        	if (DEBUG) Log.w(TAG, "swap:err=" + err);
      EGL14.eglGetError()
    } else EGL14.EGL_SUCCESS
  }

  private fun swap(surface: EGLSurface?, presentationTimeNs: Long): Int {
    //		if (DEBUG) Log.v(TAG, "swap:");
    EGLExt.eglPresentationTimeANDROID(mEglDisplay, surface, presentationTimeNs)
    return if (!EGL14.eglSwapBuffers(mEglDisplay, surface)) {
      //        	if (DEBUG) Log.w(TAG, "swap:err=" + err);
      EGL14.eglGetError()
    } else EGL14.EGL_SUCCESS
  }

  private fun createContext(
    sharedContext: Context?,
    config: EGLConfig?, version: Int
  ): EGLContext? {

    //		if (DEBUG) Log.v(TAG, "createContext:");
    val attribList = intArrayOf(
      EGL14.EGL_CONTEXT_CLIENT_VERSION, version,
      EGL14.EGL_NONE
    )
    //		checkEglError("eglCreateContext");
    return EGL14.eglCreateContext(
      mEglDisplay,
      config, sharedContext!!.eglContext, attribList, 0
    )
  }

  private fun destroyContext() {
    //		if (DEBUG) Log.v(TAG, "destroyContext:");
    if (!EGL14.eglDestroyContext(mEglDisplay, mContext.eglContext)) {
      Log.e(
        "destroyContext", "display:" + mEglDisplay
            + " context: " + mContext.eglContext
      )
      Log.e(
        TAG,
        "eglDestroyContext:" + EGL14.eglGetError()
      )
    }
    mContext = EGL_NO_CONTEXT!!
    if (mDefaultContext !== EGL14.EGL_NO_CONTEXT) {
      if (!EGL14.eglDestroyContext(mEglDisplay, mDefaultContext)) {
        Log.e(
          "destroyContext", "display:" + mEglDisplay
              + " context: " + mDefaultContext
        )
        Log.e(
          TAG,
          "eglDestroyContext:" + EGL14.eglGetError()
        )
      }
      mDefaultContext = EGL14.EGL_NO_CONTEXT
    }
  }

  private val mSurfaceDimension: IntArray? = IntArray(2)
  private fun getSurfaceWidth(surface: EGLSurface?): Int {
    val ret = EGL14.eglQuerySurface(
      mEglDisplay,
      surface, EGL14.EGL_WIDTH, mSurfaceDimension, 0
    )
    if (!ret) {
      mSurfaceDimension?.set(0, 0)
    }
    return mSurfaceDimension!![0]
  }

  private fun getSurfaceHeight(surface: EGLSurface?): Int {
    val ret = EGL14.eglQuerySurface(
      mEglDisplay,
      surface, EGL14.EGL_HEIGHT, mSurfaceDimension, 1
    )
    if (!ret) {
      mSurfaceDimension?.set(1, 0)
    }
    return mSurfaceDimension!![1]
  }

  /**
   * nativeWindow should be one of the Surface, SurfaceHolder and SurfaceTexture
   */
  private fun createWindowSurface(nativeWindow: Any?): EGLSurface? {
    //		if (DEBUG) Log.v(TAG, "createWindowSurface:nativeWindow=" + nativeWindow);
    val surfaceAttribs = intArrayOf(
      EGL14.EGL_NONE
    )
    var result: EGLSurface?
    try {
      result = EGL14.eglCreateWindowSurface(
        mEglDisplay,
        mEglConfig!!.eglConfig, nativeWindow, surfaceAttribs, 0
      )
      if (result == null || result === EGL14.EGL_NO_SURFACE) {
        val error = EGL14.eglGetError()
        if (error == EGL14.EGL_BAD_NATIVE_WINDOW) {
          Log.e(
            TAG,
            "createWindowSurface returned EGL_BAD_NATIVE_WINDOW."
          )
        }
        throw RuntimeException("createWindowSurface failed error=$error")
      }
      makeCurrent(result)
      // 获取屏幕尺寸/格式
    } catch (e: Exception) {
      Log.e(TAG, "eglCreateWindowSurface", e)
      throw IllegalArgumentException(e)
    }
    return result
  }

  /**
   * Creates an EGL surface associated with an offscreen buffer.
   */
  private fun createOffscreenSurface(width: Int, height: Int): EGLSurface? {
    //		if (DEBUG) Log.v(TAG, "createOffscreenSurface:");
    val surfaceAttributes = intArrayOf(
      EGL14.EGL_WIDTH, width,
      EGL14.EGL_HEIGHT, height,
      EGL14.EGL_NONE
    )
    var result: EGLSurface? = null
    try {
      result = EGL14.eglCreatePbufferSurface(
        mEglDisplay,
        mEglConfig!!.eglConfig, surfaceAttributes, 0
      )
      checkEglError("eglCreatePbufferSurface")
      if (result == null) {
        throw RuntimeException("surface was null")
      }
    } catch (e: RuntimeException) {
      Log.e(TAG, "createOffscreenSurface", e)
    }
    return result
  }

  private fun destroyWindowSurface(surface: EGLSurface?) {
    //		if (DEBUG) Log.v(TAG, "destroySurface:");
    if (surface !== EGL14.EGL_NO_SURFACE) {
      EGL14.eglMakeCurrent(
        mEglDisplay,
        EGL14.EGL_NO_SURFACE,
        EGL14.EGL_NO_SURFACE,
        EGL14.EGL_NO_CONTEXT
      )
      EGL14.eglDestroySurface(mEglDisplay, surface)
    }
  }

  private fun checkEglError(msg: String?) {
    var error: Int
    if (EGL14.eglGetError()
        .also { error = it } != EGL14.EGL_SUCCESS
    ) {
      throw RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error))
    }
  }

  private fun getConfig(
    version: Int,
    hasDepthBuffer: Boolean, stencilBits: Int,
    isRecordable: Boolean
  ): EGLConfig? {
    var readableType: Int = EGLBase.Companion.EGL_OPENGL_ES2_BIT
    if (version >= 3) {
      readableType = readableType or EGLBase.Companion.EGL_OPENGL_ES3_BIT_KHR
    }
    val attributeList = intArrayOf(
      EGL14.EGL_RENDERABLE_TYPE,
      readableType,
      EGL14.EGL_RED_SIZE,
      8,
      EGL14.EGL_GREEN_SIZE,
      8,
      EGL14.EGL_BLUE_SIZE,
      8,
      EGL14.EGL_ALPHA_SIZE,
      8,  //        	EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT | swapBehavior,
      //EGL14.EGL_STENCIL_SIZE, 8,
      EGL14.EGL_NONE,
      EGL14.EGL_NONE,  // this flag need to recording of MediaCodec
      //EGL_RECORDABLE_ANDROID, 1,
      EGL14.EGL_NONE,
      EGL14.EGL_NONE,
      EGL14.EGL_NONE,
      EGL14.EGL_NONE,  //	with_depth_buffer ? EGL14.EGL_DEPTH_SIZE : EGL14.EGL_NONE,
      // with_depth_buffer ? 16 : 0,
      EGL14.EGL_NONE
    )
    var offset = 10
    // 模板缓冲区（始终未使用）
    if (stencilBits > 0) {
      attributeList[offset++] = EGL14.EGL_STENCIL_SIZE
      attributeList[offset++] = stencilBits
    }
    //深度缓冲区
    if (hasDepthBuffer) {
      attributeList[offset++] = EGL14.EGL_DEPTH_SIZE
      attributeList[offset++] = 16
    }
    //如果是Surface输入MediaCodec
    if (isRecordable && BuildCheck.isAndroid43()) {
      attributeList[offset++] = EGLBase.Companion.EGL_RECORDABLE_ANDROID
      attributeList[offset++] = 1
    }
    for (i in attributeList.size - 1 downTo offset) {
      attributeList[i] = EGL14.EGL_NONE
    }
    var config = internalGetConfig(attributeList)
    if (config == null && version == 2) {
      if (isRecordable) {
        // 如果添加EGL_RECORDABLE_ANDROID，有些模型会失败，因此请将其删除
        val n = attributeList.size
        var i = 10
        while (i < n - 1) {
          if (attributeList[i] == EGLBase.Companion.EGL_RECORDABLE_ANDROID) {
            for (j in i until n) {
              attributeList[j] = EGL14.EGL_NONE
            }
            break
          }
          i += 2
        }
        config = internalGetConfig(attributeList)
      }
    }
    if (config == null) {
      Log.w(TAG, "try to fallback to RGB565")
      attributeList[3] = 5
      attributeList[5] = 6
      attributeList[7] = 5
      config = internalGetConfig(attributeList)
    }
    return config
  }

  private fun internalGetConfig(attributeList: IntArray?): EGLConfig? {
    val configs =
      arrayOfNulls<EGLConfig?>(1)
    val numConfigs = IntArray(1)
    return if (!EGL14.eglChooseConfig(
        mEglDisplay,
        attributeList, 0, configs, 0, configs.size, numConfigs, 0
      )
    ) {
      null
    } else configs[0]
  }

  companion object {
    private val TAG: String? = "EGLBaseHigh"
    private val EGL_NO_CONTEXT: Context? =
      Context(EGL14.EGL_NO_CONTEXT)
  }

  init {
    init(maxClientVersion, sharedContext, withDepthBuffer, stencilBits, isRecordable)
  }
}