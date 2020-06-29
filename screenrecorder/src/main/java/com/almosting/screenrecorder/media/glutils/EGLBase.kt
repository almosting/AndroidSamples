package com.almosting.screenrecorder.media.glutils

import com.almosting.screenrecorder.media.glutils.EGLBaseHigh.Context

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
abstract class EGLBase {
  abstract class IContext {
    abstract fun getNativeHandle(): Long
    abstract fun getEGLContext(): Any?
  }

  abstract class IConfig
  interface IEglSurface {
    fun makeCurrent()
    fun swap()
    fun getContext(): IContext?

    /**
     * swap with presentation time[ns]
     * only works well now when using EGLBase14
     */
    fun swap(presentationTimeNs: Long)
    fun release()
    fun isValid(): Boolean
  }

  abstract fun release()
  abstract fun queryString(what: Int): String?
  abstract fun getGlVersion(): Int
  abstract fun getContext(): IContext?
  abstract fun getConfig(): IConfig?
  abstract fun createFromSurface(nativeWindow: Any?): IEglSurface?
  abstract fun createOffscreen(width: Int, height: Int): IEglSurface?
  abstract fun makeDefault()
  abstract fun sync()

  companion object {
    val EGL_LOCK: Any? = Any()
    const val EGL_RECORDABLE_ANDROID = 0x3142
    const val EGL_CONTEXT_CLIENT_VERSION = 0x3098
    const val EGL_OPENGL_ES2_BIT = 4
    const val EGL_OPENGL_ES3_BIT_KHR = 0x0040
    fun createFrom(
      sharedContext: IContext?,
      withDepthBuffer: Boolean, isRecordable: Boolean
    ): EGLBase? {
      return createFrom(3, sharedContext, withDepthBuffer, 0, isRecordable)
    }

    fun createFrom(
      sharedContext: IContext?,
      withDepthBuffer: Boolean, stencilBits: Int,
      isRecordable: Boolean
    ): EGLBase? {
      return createFrom(
        3, sharedContext,
        withDepthBuffer, stencilBits, isRecordable
      )
    }

    fun createFrom(
      maxClientVersion: Int,
      sharedContext: IContext?, withDepthBuffer: Boolean,
      stencilBits: Int, isRecordable: Boolean
    ): EGLBase? {
      return if (isEGL14Supported() && (sharedContext == null
            || sharedContext is Context)
      ) {
        EGLBaseHigh(
          maxClientVersion,
          sharedContext as Context?,
          withDepthBuffer, stencilBits, isRecordable
        )
      } else {
        EGLBaseLow(
          maxClientVersion,
          sharedContext as EGLBaseLow.Context?,
          withDepthBuffer, stencilBits, isRecordable
        )
      }
    }

    fun isEGL14Supported(): Boolean {
      return true
    }
  }
}