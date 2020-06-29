package com.almosting.screenrecorder.media.glutils

import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.opengl.GLES10
import android.opengl.GLES20
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceHolder.Callback
import android.view.SurfaceView
import com.almosting.toolbox.utils.BuildCheck
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.egl.EGLSurface

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
class EGLBaseLow(
  maxClientVersion: Int,
  sharedContext: Context?,
  withDepthBuffer: Boolean,
  stencilBits: Int,
  isRecordable: Boolean
) : EGLBase() {
  private var mEgl: EGL10? = null
  private var mEglDisplay: EGLDisplay? = null
  private var mEglConfig: Config? = null
  private var mGlVersion = 2
  private var mContext: Context =
    EGL_NO_CONTEXT!!

  class Context(val eglContext: EGLContext?) :
    IContext() {
    override fun getNativeHandle(): Long {
      return 0L
    }

    override fun getEGLContext(): Any? {
      return eglContext
    }
  }

  class Config(val eglConfig: EGLConfig?) :
    IConfig()

  class MySurfaceHolder(private val surface: Surface?) : SurfaceHolder {
    override fun getSurface(): Surface? {
      return surface
    }

    override fun addCallback(callback: Callback?) {}
    override fun removeCallback(callback: Callback?) {}
    override fun isCreating(): Boolean {
      return false
    }

    override fun setType(type: Int) {}
    override fun setFixedSize(width: Int, height: Int) {}
    override fun setSizeFromLayout() {}
    override fun setFormat(format: Int) {}
    override fun setKeepScreenOn(screenOn: Boolean) {}
    override fun lockCanvas(): Canvas? {
      return null
    }

    override fun lockCanvas(dirty: Rect?): Canvas? {
      return null
    }

    override fun unlockCanvasAndPost(canvas: Canvas?) {}
    override fun getSurfaceFrame(): Rect? {
      return null
    }
  }

  class EglSurface : IEglSurface {
    private val mEglBase: EGLBaseLow?
    private var mEglSurface =
      EGL10.EGL_NO_SURFACE

    /**
     * 与Surface（Surface / SurfaceTexture / SurfaceHolder）相关的EglSurface
     */
    constructor(eglBase: EGLBaseLow?, surface: Any?) {

      //			if (DEBUG) Log.v(TAG, "EglSurface:");
      mEglBase = eglBase
      mEglSurface = if (surface is Surface && !BuildCheck.isAndroid42()) {
        mEglBase!!.createWindowSurface(
          MySurfaceHolder(surface as Surface?)
        )
      } else if (surface is Surface
        || surface is SurfaceHolder
        || surface is SurfaceTexture
        || surface is SurfaceView
      ) {
        mEglBase!!.createWindowSurface(surface)
      } else {
        throw IllegalArgumentException("unsupported surface")
      }
    }

    constructor(eglBase: EGLBaseLow?, width: Int, height: Int) {
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
          mEglBase.getSurfaceWidth(mEglSurface), mEglBase.getSurfaceHeight(mEglSurface)
        )
      } else {
        GLES10.glViewport(
          0, 0,
          mEglBase.getSurfaceWidth(mEglSurface), mEglBase.getSurfaceHeight(mEglSurface)
        )
      }
    }

    override fun swap() {
      mEglBase!!.swap(mEglSurface)
    }

    override fun swap(presentationTimeNs: Long) {
      mEglBase!!.swap(mEglSurface, presentationTimeNs)
    }

    override fun getContext(): IContext? {
      return mEglBase!!.getContext()
    }

    fun setPresentationTime(presentationTimeNs: Long) {
      //			EGLExt.eglPresentationTimeANDROID(mEglBase.mEglDisplay,
      // 				mEglSurface, presentationTimeNs);
    }

    override fun isValid(): Boolean {
      return (mEglSurface != null
          && mEglSurface !== EGL10.EGL_NO_SURFACE
          && mEglBase!!.getSurfaceWidth(mEglSurface) > 0
          && mEglBase.getSurfaceHeight(mEglSurface) > 0)
    }

    override fun release() {
      mEglBase!!.makeDefault()
      mEglBase.destroyWindowSurface(mEglSurface)
      mEglSurface = EGL10.EGL_NO_SURFACE
    }
  }

  override fun release() {
    destroyContext()
    mContext = EGL_NO_CONTEXT!!
    if (mEgl == null) {
      return
    }
    mEgl!!.eglMakeCurrent(
      mEglDisplay,
      EGL10.EGL_NO_SURFACE,
      EGL10.EGL_NO_SURFACE,
      EGL10.EGL_NO_CONTEXT
    )
    //		mEgl.eglReleaseThread();	// XXX これを入れるとハングアップする機種がある
    mEgl!!.eglTerminate(mEglDisplay)
    mEglDisplay = null
    mEglConfig = null
    mEgl = null
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

  override fun getContext(): Context? {
    return mContext
  }

  override fun getConfig(): Config? {
    return mEglConfig
  }

  override fun makeDefault() {
    if (!mEgl!!.eglMakeCurrent(
        mEglDisplay,
        EGL10.EGL_NO_SURFACE,
        EGL10.EGL_NO_SURFACE,
        EGL10.EGL_NO_CONTEXT
      )
    ) {
      Log.w(
        TAG,
        "makeDefault:eglMakeCurrent:err=" + mEgl!!.eglGetError()
      )
    }
  }

  override fun sync() {
    mEgl!!.eglWaitGL() // 效果类似于GLES20.glFinish（）
    mEgl!!.eglWaitNative(EGL10.EGL_CORE_NATIVE_ENGINE, null)
  }

  override fun queryString(what: Int): String? {
    return mEgl!!.eglQueryString(mEglDisplay, what)
  }

  override fun getGlVersion(): Int {
    return mGlVersion
  }

  private fun init(
    maxClientVersion: Int,
    sharedContext: Context?,
    withDepthBuffer: Boolean, stencilBits: Int,
    isRecordable: Boolean
  ) {
    var sharedContext = sharedContext
    sharedContext =
      sharedContext ?: EGL_NO_CONTEXT
    if (mEgl == null) {
      mEgl =
        EGLContext.getEGL() as EGL10
      mEglDisplay = mEgl!!.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
      if (mEglDisplay === EGL10.EGL_NO_DISPLAY) {
        throw RuntimeException("eglGetDisplay failed")
      }
      val version = IntArray(2)
      if (!mEgl!!.eglInitialize(mEglDisplay, version)) {
        mEglDisplay = null
        throw RuntimeException("eglInitialize failed")
      }
    }
    var config: EGLConfig?
    if (maxClientVersion >= 3) {
      config = getConfig(3, withDepthBuffer, stencilBits, isRecordable)
      if (config != null) {
        val context =
          createContext(sharedContext!!, config, 3)
        if (mEgl!!.eglGetError() == EGL10.EGL_SUCCESS) {
          mEglConfig = Config(config)
          mContext = Context(context)
          mGlVersion = 3
        }
      }
    }
    if (maxClientVersion >= 2 && mContext.eglContext === EGL10.EGL_NO_CONTEXT) {
      config = getConfig(2, withDepthBuffer, stencilBits, isRecordable)
      if (config == null) {
        throw RuntimeException("chooseConfig failed")
      }
      try {
        // create EGL rendering context
        val context =
          createContext(sharedContext!!, config, 2)
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
          val context =
            createContext(sharedContext!!, config, 2)
          checkEglError("eglCreateContext")
          mEglConfig = Config(config)
          mContext = Context(context)
          mGlVersion = 2
        }
      }
    }
    if (mContext.eglContext === EGL10.EGL_NO_CONTEXT) {
      config = getConfig(1, withDepthBuffer, stencilBits, isRecordable)
      if (config == null) {
        throw RuntimeException("chooseConfig failed")
      }
      // create EGL rendering context
      val context =
        createContext(sharedContext!!, config, 1)
      checkEglError("eglCreateContext")
      mEglConfig = Config(config)
      mContext = Context(context)
      mGlVersion = 1
    }
    // confirm whether the EGL rendering context is successfully created
    val values = IntArray(1)
    mEgl!!.eglQueryContext(
      mEglDisplay,
      mContext.eglContext, EGLBase.Companion.EGL_CONTEXT_CLIENT_VERSION, values
    )
    Log.d(
      TAG,
      "EGLContext created, client version " + values[0]
    )
    makeDefault()
  }

  /**
   * change context to draw this window surface
   */
  private fun makeCurrent(surface: EGLSurface?): Boolean {
    if (surface == null || surface === EGL10.EGL_NO_SURFACE) {
      val error = mEgl!!.eglGetError()
      if (error == EGL10.EGL_BAD_NATIVE_WINDOW) {
        Log.e(TAG, "makeCurrent:EGL_BAD_NATIVE_WINDOW")
      }
      return false
    }
    // attach EGL rendering context to specific EGL window surface
    if (!mEgl!!.eglMakeCurrent(mEglDisplay, surface, surface, mContext.eglContext)) {
      Log.w("TAG", "eglMakeCurrent" + mEgl!!.eglGetError())
      return false
    }
    return true
  }

  private fun swap(surface: EGLSurface?): Int {
    //		if (DEBUG) Log.v(TAG, "swap:");
    return if (!mEgl!!.eglSwapBuffers(mEglDisplay, surface)) {
      //        	if (DEBUG) Log.w(TAG, "swap:err=" + err);
      mEgl!!.eglGetError()
    } else EGL10.EGL_SUCCESS
  }

  /**
   * swap rendering buffer with presentation time[ns]
   * presentationTimeNs is ignored on this method
   */
  private fun swap(surface: EGLSurface?, ignored: Long): Int {
    //		if (DEBUG) Log.v(TAG, "swap:");
    //		EGLExt.eglPresentationTimeANDROID(mEglDisplay, surface, presentationTimeNs);
    return if (!mEgl!!.eglSwapBuffers(mEglDisplay, surface)) {
      //        	if (DEBUG) Log.w(TAG, "swap:err=" + err);
      mEgl!!.eglGetError()
    } else EGL10.EGL_SUCCESS
  }

  private fun createContext(
    sharedContext: Context,
    config: EGLConfig?, version: Int
  ): EGLContext? {
    val attributeList = intArrayOf(
      EGLBase.Companion.EGL_CONTEXT_CLIENT_VERSION, version,
      EGL10.EGL_NONE
    )
    //		checkEglError("eglCreateContext");
    return mEgl!!.eglCreateContext(
      mEglDisplay, config, sharedContext.eglContext, attributeList
    )
  }

  private fun destroyContext() {
    if (!mEgl!!.eglDestroyContext(mEglDisplay, mContext.eglContext)) {
      Log.e(
        "destroyContext", "display:" + mEglDisplay
            + " context: " + mContext.eglContext
      )
      Log.e(TAG, "eglDestroyContext:" + mEgl!!.eglGetError())
    }
    mContext = EGL_NO_CONTEXT!!
  }

  private fun getSurfaceWidth(surface: EGLSurface?): Int {
    val value = IntArray(1)
    val ret = mEgl!!.eglQuerySurface(
      mEglDisplay,
      surface, EGL10.EGL_WIDTH, value
    )
    if (!ret) {
      value[0] = 0
    }
    return value[0]
  }

  private fun getSurfaceHeight(surface: EGLSurface?): Int {
    val value = IntArray(1)
    val ret = mEgl!!.eglQuerySurface(
      mEglDisplay,
      surface, EGL10.EGL_HEIGHT, value
    )
    if (!ret) {
      value[0] = 0
    }
    return value[0]
  }

  /**
   * nativeWindow should be one of the SurfaceView, Surface, SurfaceHolder and SurfaceTexture
   */
  private fun createWindowSurface(nativeWindow: Any?): EGLSurface? {
    val surfaceAttributes = intArrayOf(
      EGL10.EGL_NONE
    )
    var result: EGLSurface? = null
    try {
      result = mEgl!!.eglCreateWindowSurface(
        mEglDisplay,
        mEglConfig!!.eglConfig, nativeWindow, surfaceAttributes
      )
      if (result == null || result === EGL10.EGL_NO_SURFACE) {
        val error = mEgl!!.eglGetError()
        if (error == EGL10.EGL_BAD_NATIVE_WINDOW) {
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
  private fun createOffscreenSurface(
    width: Int,
    height: Int
  ): EGLSurface? {
    val surfaceAttributes = intArrayOf(
      EGL10.EGL_WIDTH, width,
      EGL10.EGL_HEIGHT, height,
      EGL10.EGL_NONE
    )
    mEgl!!.eglWaitGL()
    var result: EGLSurface? = null
    try {
      result = mEgl!!.eglCreatePbufferSurface(
        mEglDisplay,
        mEglConfig!!.eglConfig, surfaceAttributes
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
    var surface = surface
    if (surface !== EGL10.EGL_NO_SURFACE) {
      mEgl!!.eglMakeCurrent(
        mEglDisplay,
        EGL10.EGL_NO_SURFACE,
        EGL10.EGL_NO_SURFACE,
        EGL10.EGL_NO_CONTEXT
      )
      mEgl!!.eglDestroySurface(mEglDisplay, surface)
    }
    surface = EGL10.EGL_NO_SURFACE
    //		if (DEBUG) Log.v(TAG, "destroySurface:finished");
  }

  private fun checkEglError(msg: String?) {
    var error: Int
    if (mEgl!!.eglGetError()
        .also { error = it } != EGL10.EGL_SUCCESS
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
    //		final int swapBehavior = dirtyRegions ? EGL_SWAP_BEHAVIOR_PRESERVED_BIT : 0;
    val attributeList = intArrayOf(
      EGL10.EGL_RENDERABLE_TYPE,
      readableType,
      EGL10.EGL_RED_SIZE,
      8,
      EGL10.EGL_GREEN_SIZE,
      8,
      EGL10.EGL_BLUE_SIZE,
      8,
      EGL10.EGL_ALPHA_SIZE,
      8,  //        	EGL10.EGL_SURFACE_TYPE, EGL10.EGL_WINDOW_BIT | swapBehavior,
      //EGL10.EGL_STENCIL_SIZE, 8,
      EGL10.EGL_NONE,
      EGL10.EGL_NONE,  // this flag need to recording of MediaCodec
      // EGL_RECORDABLE_ANDROID, 1,
      EGL10.EGL_NONE,
      EGL10.EGL_NONE,
      EGL10.EGL_NONE,
      EGL10.EGL_NONE,  // with_depth_buffer ? EGL10.EGL_DEPTH_SIZE : EGL10.EGL_NONE,
      // with_depth_buffer ? 16 : 0,
      EGL10.EGL_NONE
    )
    var offset = 10
    if (stencilBits > 0) {
      attributeList[offset++] = EGL10.EGL_STENCIL_SIZE
      attributeList[offset++] = 8
    }
    if (hasDepthBuffer) {
      attributeList[offset++] = EGL10.EGL_DEPTH_SIZE
      attributeList[offset++] = 16
    }
    if (isRecordable && BuildCheck.isAndroid43()) {
      // 如果是Surface输入MediaCodec
      // A-1000F（Android 4.1.2）不适用于此标志
      attributeList[offset++] = EGLBase.Companion.EGL_RECORDABLE_ANDROID
      attributeList[offset++] = 1
    }
    for (i in attributeList.size - 1 downTo offset) {
      attributeList[i] = EGL10.EGL_NONE
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
              attributeList[j] = EGL10.EGL_NONE
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
    return if (!mEgl!!.eglChooseConfig(
        mEglDisplay,
        attributeList, configs, configs.size, numConfigs
      )
    ) {
      null
    } else configs[0]
  }

  companion object {
    private val TAG: String? = "EGLBaseLow"
    private val EGL_NO_CONTEXT: Context? =
      Context(EGL10.EGL_NO_CONTEXT)
  }

  init {
    init(maxClientVersion, sharedContext, withDepthBuffer, stencilBits, isRecordable)
  }
}