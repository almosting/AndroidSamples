package com.sunplus.screenrecorder.media.glutils;

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
public abstract class EGLBase {
  public static final Object EGL_LOCK = new Object();
  public static final int EGL_RECORDABLE_ANDROID = 0x3142;
  public static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
  public static final int EGL_OPENGL_ES2_BIT = 4;
  public static final int EGL_OPENGL_ES3_BIT_KHR = 0x0040;

  public static EGLBase createFrom(final IContext sharedContext,
                                   final boolean withDepthBuffer, final boolean isRecordable) {

    return createFrom(3, sharedContext, withDepthBuffer, 0, isRecordable);
  }

  public static EGLBase createFrom(final IContext sharedContext,
                                   final boolean withDepthBuffer, final int stencilBits,
                                   final boolean isRecordable) {

    return createFrom(3, sharedContext,
        withDepthBuffer, stencilBits, isRecordable);
  }

  public static EGLBase createFrom(final int maxClientVersion,
                                   final IContext sharedContext, final boolean withDepthBuffer,
                                   final int stencilBits, final boolean isRecordable) {

    if (isEGL14Supported() && ((sharedContext == null)
        || (sharedContext instanceof EGLBaseHigh.Context))) {

      return new EGLBaseHigh(maxClientVersion,
          (EGLBaseHigh.Context) sharedContext,
          withDepthBuffer, stencilBits, isRecordable);
    } else {
      return new EGLBaseLow(maxClientVersion,
          (EGLBaseLow.Context) sharedContext,
          withDepthBuffer, stencilBits, isRecordable);
    }
  }

  public static abstract class IContext {
    public abstract long getNativeHandle();

    public abstract Object getEGLContext();
  }

  public static abstract class IConfig {
  }

  public interface IEglSurface {
    public void makeCurrent();

    public void swap();

    public IContext getContext();

    /**
     * swap with presentation time[ns]
     * only works well now when using EGLBase14
     */
    public void swap(final long presentationTimeNs);

    public void release();

    public boolean isValid();
  }

  public static boolean isEGL14Supported() {
    return true;
  }

  public abstract void release();

  public abstract String queryString(final int what);

  public abstract int getGlVersion();

  public abstract IContext getContext();

  public abstract IConfig getConfig();

  public abstract IEglSurface createFromSurface(final Object nativeWindow);

  public abstract IEglSurface createOffscreen(final int width, final int height);

  public abstract void makeDefault();

  public abstract void sync();
}
