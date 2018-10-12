package com.sunplus.screenrecorder.media.glutils;

import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.opengl.GLES10;
import android.opengl.GLES20;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.sunplus.toolbox.utils.BuildCheck;

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
public class EGLBaseHigh extends EGLBase {
  private static final String TAG = "EGLBaseHigh";

  private static final Context EGL_NO_CONTEXT = new Context(EGL14.EGL_NO_CONTEXT);
  private Config mEglConfig = null;
  @NonNull
  private Context mContext = EGL_NO_CONTEXT;
  private EGLDisplay mEglDisplay = EGL14.EGL_NO_DISPLAY;
  private EGLContext mDefaultContext = EGL14.EGL_NO_CONTEXT;
  private int mGlVersion = 2;

  public static class Context extends IContext {
    public final EGLContext eglContext;

    private Context(final EGLContext context) {
      eglContext = context;
    }

    @Override
    @SuppressLint("NewApi")
    public long getNativeHandle() {
      return eglContext != null ?
          (BuildCheck.isLollipop()
              ? eglContext.getNativeHandle() : eglContext.getHandle()) : 0L;
    }

    @Override
    public Object getEGLContext() {
      return eglContext;
    }
  }

  public static class Config extends IConfig {
    public final EGLConfig eglConfig;

    private Config(final EGLConfig eglConfig) {
      this.eglConfig = eglConfig;
    }
  }

  public static class EglSurface implements IEglSurface {
    private final EGLBaseHigh mEglBase;
    private EGLSurface mEglSurface;

    private EglSurface(final EGLBaseHigh eglBase, final Object surface)
        throws IllegalArgumentException {


      mEglBase = eglBase;
      if ((surface instanceof Surface)
          || (surface instanceof SurfaceHolder)
          || (surface instanceof SurfaceTexture)
          || (surface instanceof SurfaceView)) {
        mEglSurface = mEglBase.createWindowSurface(surface);
      } else {
        throw new IllegalArgumentException("unsupported surface");
      }
    }

    private EglSurface(final EGLBaseHigh eglBase,
                       final int width, final int height) {


      mEglBase = eglBase;
      if ((width <= 0) || (height <= 0)) {
        mEglSurface = mEglBase.createOffscreenSurface(1, 1);
      } else {
        mEglSurface = mEglBase.createOffscreenSurface(width, height);
      }
    }

    @Override
    public void makeCurrent() {
      mEglBase.makeCurrent(mEglSurface);
      if (mEglBase.getGlVersion() >= 2) {
        GLES20.glViewport(0, 0,
            mEglBase.getSurfaceWidth(mEglSurface),
            mEglBase.getSurfaceHeight(mEglSurface));
      } else {
        GLES10.glViewport(0, 0,
            mEglBase.getSurfaceWidth(mEglSurface),
            mEglBase.getSurfaceHeight(mEglSurface));
      }
    }

    @Override
    public void swap() {
      mEglBase.swap(mEglSurface);
    }

    @Override
    public void swap(final long presentationTimeNs) {
      mEglBase.swap(mEglSurface, presentationTimeNs);
    }

    public void setPresentationTime(final long presentationTimeNs) {
      EGLExt.eglPresentationTimeANDROID(mEglBase.mEglDisplay,
          mEglSurface, presentationTimeNs);
    }

    @Override
    public IContext getContext() {
      return mEglBase.getContext();
    }

    @Override
    public boolean isValid() {
      return (mEglSurface != null)
          && (mEglSurface != EGL14.EGL_NO_SURFACE)
          && (mEglBase.getSurfaceWidth(mEglSurface) > 0)
          && (mEglBase.getSurfaceHeight(mEglSurface) > 0);
    }

    @Override
    public void release() {

      mEglBase.makeDefault();
      mEglBase.destroyWindowSurface(mEglSurface);
      mEglSurface = EGL14.EGL_NO_SURFACE;
    }
  }

  public EGLBaseHigh(final int maxClientVersion,
                     final Context sharedContext, final boolean withDepthBuffer,
                     final int stencilBits, final boolean isRecordable) {


    init(maxClientVersion, sharedContext, withDepthBuffer, stencilBits, isRecordable);
  }

  @Override
  public void release() {

    if (mEglDisplay != EGL14.EGL_NO_DISPLAY) {
      destroyContext();
      EGL14.eglTerminate(mEglDisplay);
      EGL14.eglReleaseThread();
    }
    mEglDisplay = EGL14.EGL_NO_DISPLAY;
    mContext = EGL_NO_CONTEXT;
  }

  @Override
  public EglSurface createFromSurface(final Object nativeWindow) {
    final EglSurface eglSurface = new EglSurface(this, nativeWindow);
    eglSurface.makeCurrent();
    return eglSurface;
  }

  @Override
  public EglSurface createOffscreen(final int width, final int height) {
    final EglSurface eglSurface = new EglSurface(this, width, height);
    eglSurface.makeCurrent();
    return eglSurface;
  }

  @Override
  public String queryString(final int what) {
    return EGL14.eglQueryString(mEglDisplay, what);
  }

  @Override
  public int getGlVersion() {
    return mGlVersion;
  }

  @NonNull
  @Override
  public Context getContext() {
    return mContext;
  }

  @Override
  public Config getConfig() {
    return mEglConfig;
  }

  /**
   * 取消链接EGL渲染上下文和线程
   */
  @Override
  public void makeDefault() {

    if (!EGL14.eglMakeCurrent(mEglDisplay,
        EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)) {

      Log.w("TAG", "makeDefault" + EGL14.eglGetError());
    }
  }

  @Override
  public void sync() {
    EGL14.eglWaitGL();  // 效果类似于GLES20.glFinish（）
    EGL14.eglWaitNative(EGL14.EGL_CORE_NATIVE_ENGINE);
  }

  private void init(final int maxClientVersion, Context sharedContext,
                    final boolean withDepthBuffer, final int stencilBits,
                    final boolean isRecordable) {

    if (mEglDisplay != EGL14.EGL_NO_DISPLAY) {
      throw new RuntimeException("EGL already set up");
    }

    mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
    if (mEglDisplay == EGL14.EGL_NO_DISPLAY) {
      throw new RuntimeException("eglGetDisplay failed");
    }
    // 获取EGL的版本
    final int[] version = new int[2];
    if (!EGL14.eglInitialize(mEglDisplay, version, 0, version, 1)) {
      mEglDisplay = null;
      throw new RuntimeException("eglInitialize failed");
    }

    sharedContext = (sharedContext != null) ? sharedContext : EGL_NO_CONTEXT;

    EGLConfig config;
    if (maxClientVersion >= 3) {
      // 试着看看它是否可以用GLES 3获得
      config = getConfig(3, withDepthBuffer, stencilBits, isRecordable);
      if (config != null) {
        final EGLContext context = createContext(sharedContext, config, 3);
        if (EGL14.eglGetError() == EGL14.EGL_SUCCESS) {
          // 由于我不想在这里生成异常，因此请检查它自己而不是checkEglError
          mEglConfig = new Config(config);
          mContext = new Context(context);
          mGlVersion = 3;
        }
      }
    }
    //使用GLES 3无法获取GLES 2
    if (maxClientVersion >= 2 && mContext.eglContext == EGL14.EGL_NO_CONTEXT) {

      config = getConfig(2, withDepthBuffer, stencilBits, isRecordable);
      if (config == null) {
        throw new RuntimeException("chooseConfig failed");
      }
      try {
        // create EGL rendering context
        final EGLContext context = createContext(sharedContext, config, 2);
        checkEglError("eglCreateContext");
        mEglConfig = new Config(config);
        mContext = new Context(context);
        mGlVersion = 2;
      } catch (final Exception e) {
        if (isRecordable) {
          config = getConfig(2, withDepthBuffer, stencilBits, false);
          if (config == null) {
            throw new RuntimeException("chooseConfig failed");
          }
          // create EGL rendering context
          final EGLContext context = createContext(sharedContext, config, 2);
          checkEglError("eglCreateContext");
          mEglConfig = new Config(config);
          mContext = new Context(context);
          mGlVersion = 2;
        }
      }
    }
    if (mContext.eglContext == EGL14.EGL_NO_CONTEXT) {
      config = getConfig(1, withDepthBuffer, stencilBits, isRecordable);
      if (config == null) {
        throw new RuntimeException("chooseConfig failed");
      }
      // create EGL rendering context
      final EGLContext context = createContext(sharedContext, config, 1);
      checkEglError("eglCreateContext");
      mEglConfig = new Config(config);
      mContext = new Context(context);
      mGlVersion = 1;
    }
    // confirm whether the EGL rendering context is successfully created
    final int[] values = new int[1];
    EGL14.eglQueryContext(mEglDisplay,
        mContext.eglContext, EGL14.EGL_CONTEXT_CLIENT_VERSION, values, 0);
    Log.d(TAG, "EGLContext created, client version " + values[0]);
    makeDefault();  // makeCurrent(EGL14.EGL_NO_SURFACE);
  }

  private boolean makeCurrent(final EGLSurface surface) {

    if (surface == null || surface == EGL14.EGL_NO_SURFACE) {
      final int error = EGL14.eglGetError();
      if (error == EGL14.EGL_BAD_NATIVE_WINDOW) {
        Log.e(TAG, "makeCurrent:returned EGL_BAD_NATIVE_WINDOW.");
      }
      return false;
    }
    // attach EGL rendering context to specific EGL window surface
    if (!EGL14.eglMakeCurrent(mEglDisplay, surface, surface, mContext.eglContext)) {
      Log.w("TAG", "eglMakeCurrent" + EGL14.eglGetError());
      return false;
    }
    return true;
  }

  private int swap(final EGLSurface surface) {
    //		if (DEBUG) Log.v(TAG, "swap:");
    if (!EGL14.eglSwapBuffers(mEglDisplay, surface)) {
      //        	if (DEBUG) Log.w(TAG, "swap:err=" + err);
      return EGL14.eglGetError();
    }
    return EGL14.EGL_SUCCESS;
  }

  private int swap(final EGLSurface surface, final long presentationTimeNs) {
    //		if (DEBUG) Log.v(TAG, "swap:");
    EGLExt.eglPresentationTimeANDROID(mEglDisplay, surface, presentationTimeNs);
    if (!EGL14.eglSwapBuffers(mEglDisplay, surface)) {
      //        	if (DEBUG) Log.w(TAG, "swap:err=" + err);
      return EGL14.eglGetError();
    }
    return EGL14.EGL_SUCCESS;
  }

  private EGLContext createContext(final Context sharedContext,
                                   final EGLConfig config, final int version) {

    //		if (DEBUG) Log.v(TAG, "createContext:");

    final int[] attribList = {
        EGL14.EGL_CONTEXT_CLIENT_VERSION, version,
        EGL14.EGL_NONE
    };
    //		checkEglError("eglCreateContext");
    return EGL14.eglCreateContext(mEglDisplay,
        config, sharedContext.eglContext, attribList, 0);
  }

  private void destroyContext() {
    //		if (DEBUG) Log.v(TAG, "destroyContext:");

    if (!EGL14.eglDestroyContext(mEglDisplay, mContext.eglContext)) {
      Log.e("destroyContext", "display:" + mEglDisplay
          + " context: " + mContext.eglContext);
      Log.e(TAG, "eglDestroyContext:" + EGL14.eglGetError());
    }
    mContext = EGL_NO_CONTEXT;
    if (mDefaultContext != EGL14.EGL_NO_CONTEXT) {
      if (!EGL14.eglDestroyContext(mEglDisplay, mDefaultContext)) {
        Log.e("destroyContext", "display:" + mEglDisplay
            + " context: " + mDefaultContext);
        Log.e(TAG, "eglDestroyContext:" + EGL14.eglGetError());
      }
      mDefaultContext = EGL14.EGL_NO_CONTEXT;
    }
  }

  private final int[] mSurfaceDimension = new int[2];

  private int getSurfaceWidth(final EGLSurface surface) {
    final boolean ret = EGL14.eglQuerySurface(mEglDisplay,
        surface, EGL14.EGL_WIDTH, mSurfaceDimension, 0);
    if (!ret) {
      mSurfaceDimension[0] = 0;
    }
    return mSurfaceDimension[0];
  }

  private int getSurfaceHeight(final EGLSurface surface) {
    final boolean ret = EGL14.eglQuerySurface(mEglDisplay,
        surface, EGL14.EGL_HEIGHT, mSurfaceDimension, 1);
    if (!ret) {
      mSurfaceDimension[1] = 0;
    }
    return mSurfaceDimension[1];
  }

  /**
   * nativeWindow should be one of the Surface, SurfaceHolder and SurfaceTexture
   */
  private EGLSurface createWindowSurface(final Object nativeWindow) {
    //		if (DEBUG) Log.v(TAG, "createWindowSurface:nativeWindow=" + nativeWindow);

    final int[] surfaceAttribs = {
        EGL14.EGL_NONE
    };
    EGLSurface result = null;
    try {
      result = EGL14.eglCreateWindowSurface(mEglDisplay,
          mEglConfig.eglConfig, nativeWindow, surfaceAttribs, 0);
      if (result == null || result == EGL14.EGL_NO_SURFACE) {
        final int error = EGL14.eglGetError();
        if (error == EGL14.EGL_BAD_NATIVE_WINDOW) {
          Log.e(TAG, "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.");
        }
        throw new RuntimeException("createWindowSurface failed error=" + error);
      }
      makeCurrent(result);
      // 获取屏幕尺寸/格式
    } catch (final Exception e) {
      Log.e(TAG, "eglCreateWindowSurface", e);
      throw new IllegalArgumentException(e);
    }
    return result;
  }

  /**
   * Creates an EGL surface associated with an offscreen buffer.
   */
  private EGLSurface createOffscreenSurface(final int width, final int height) {
    //		if (DEBUG) Log.v(TAG, "createOffscreenSurface:");
    final int[] surfaceAttributes = {
        EGL14.EGL_WIDTH, width,
        EGL14.EGL_HEIGHT, height,
        EGL14.EGL_NONE
    };
    EGLSurface result = null;
    try {
      result = EGL14.eglCreatePbufferSurface(mEglDisplay,
          mEglConfig.eglConfig, surfaceAttributes, 0);
      checkEglError("eglCreatePbufferSurface");
      if (result == null) {
        throw new RuntimeException("surface was null");
      }
    } catch (final RuntimeException e) {
      Log.e(TAG, "createOffscreenSurface", e);
    }
    return result;
  }

  private void destroyWindowSurface(EGLSurface surface) {
    //		if (DEBUG) Log.v(TAG, "destroySurface:");

    if (surface != EGL14.EGL_NO_SURFACE) {
      EGL14.eglMakeCurrent(mEglDisplay,
          EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
      EGL14.eglDestroySurface(mEglDisplay, surface);
    }
    surface = EGL14.EGL_NO_SURFACE;
    //		if (DEBUG) Log.v(TAG, "destroySurface:finished");
  }

  private void checkEglError(final String msg) {
    int error;
    if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
      throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
    }
  }

  private EGLConfig getConfig(final int version,
                              final boolean hasDepthBuffer, final int stencilBits,
                              final boolean isRecordable) {

    int readableType = EGL_OPENGL_ES2_BIT;
    if (version >= 3) {
      readableType |= EGL_OPENGL_ES3_BIT_KHR;
    }
    final int[] attributeList = {
        EGL14.EGL_RENDERABLE_TYPE, readableType,
        EGL14.EGL_RED_SIZE, 8,
        EGL14.EGL_GREEN_SIZE, 8,
        EGL14.EGL_BLUE_SIZE, 8,
        EGL14.EGL_ALPHA_SIZE, 8,
        //        	EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT | swapBehavior,
        //EGL14.EGL_STENCIL_SIZE, 8,
        EGL14.EGL_NONE, EGL14.EGL_NONE,

        // this flag need to recording of MediaCodec
        //EGL_RECORDABLE_ANDROID, 1,
        EGL14.EGL_NONE, EGL14.EGL_NONE,
        EGL14.EGL_NONE, EGL14.EGL_NONE,
        //	with_depth_buffer ? EGL14.EGL_DEPTH_SIZE : EGL14.EGL_NONE,
        // with_depth_buffer ? 16 : 0,
        EGL14.EGL_NONE
    };
    int offset = 10;
    // 模板缓冲区（始终未使用）
    if (stencilBits > 0) {
      attributeList[offset++] = EGL14.EGL_STENCIL_SIZE;
      attributeList[offset++] = stencilBits;
    }
    //深度缓冲区
    if (hasDepthBuffer) {
      attributeList[offset++] = EGL14.EGL_DEPTH_SIZE;
      attributeList[offset++] = 16;
    }
    //如果是Surface输入MediaCodec
    if (isRecordable && BuildCheck.isAndroid43()) {
      attributeList[offset++] = EGL_RECORDABLE_ANDROID;
      attributeList[offset++] = 1;
    }
    for (int i = attributeList.length - 1; i >= offset; i--) {
      attributeList[i] = EGL14.EGL_NONE;
    }
    EGLConfig config = internalGetConfig(attributeList);
    if ((config == null) && (version == 2)) {
      if (isRecordable) {
        // 如果添加EGL_RECORDABLE_ANDROID，有些模型会失败，因此请将其删除
        final int n = attributeList.length;
        for (int i = 10; i < n - 1; i += 2) {
          if (attributeList[i] == EGL_RECORDABLE_ANDROID) {
            for (int j = i; j < n; j++) {
              attributeList[j] = EGL14.EGL_NONE;
            }
            break;
          }
        }
        config = internalGetConfig(attributeList);
      }
    }
    if (config == null) {
      Log.w(TAG, "try to fallback to RGB565");
      attributeList[3] = 5;
      attributeList[5] = 6;
      attributeList[7] = 5;
      config = internalGetConfig(attributeList);
    }
    return config;
  }

  private EGLConfig internalGetConfig(final int[] attributeList) {
    final EGLConfig[] configs = new EGLConfig[1];
    final int[] numConfigs = new int[1];
    if (!EGL14.eglChooseConfig(mEglDisplay,
        attributeList, 0, configs, 0, configs.length, numConfigs, 0)) {
      return null;
    }
    return configs[0];
  }
}
