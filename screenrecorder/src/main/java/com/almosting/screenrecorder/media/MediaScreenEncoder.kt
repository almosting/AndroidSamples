package com.almosting.screenrecorder.media

import android.graphics.SurfaceTexture
import android.graphics.SurfaceTexture.OnFrameAvailableListener
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.hardware.display.VirtualDisplay.Callback
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import com.almosting.screenrecorder.media.glutils.EGLBase.IContext
import com.almosting.screenrecorder.media.glutils.EGLBase.IEglSurface
import com.almosting.screenrecorder.media.glutils.EglTask
import com.almosting.screenrecorder.media.glutils.GLDrawer2D
import java.io.IOException

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
class MediaScreenEncoder(
  muxer: MediaMuxerWrapper?, listener: MediaEncoderCallback?,
  private var mMediaProjection: MediaProjection?, width: Int, height: Int,
  private val mDensity: Int,
  bitrate: Int, fps: Int
) : MediaVideoEncoderBase(muxer, listener, width, height) {
  private val bitrate: Int
  private val fps: Int
  private var mSurface: Surface? = null
  private val mHandler: Handler?
  override fun release() {
    mHandler!!.getLooper().quit()
    super.release()
  }

  @Throws(IOException::class) public override fun prepare() {
    if (DEBUG) {
      Log.i(TAG, "prepare: ")
    }
    mSurface = prepareSurfaceEncoder(MIME_TYPE, fps, bitrate)
    mMediaCodec!!.start()
    mIsRecording = true
    Thread(mScreenCaptureTask, "ScreenCaptureThread").start()
    if (DEBUG) {
      Log.i(TAG, "prepare finishing")
    }
    if (mListener != null) {
      try {
        mListener.onPrepared(this)
      } catch (e: Exception) {
        Log.e(TAG, "prepare:", e)
      }
    }
  }

  public override fun stopRecording() {
    if (DEBUG) {
      Log.v(TAG, "stopRecording:")
    }
    synchronized(mSync as Object) {
      mIsRecording = false
      mSync.notifyAll()
    }
    super.stopRecording()
  }

  private val mSync: Object? = Object()

  @Volatile
  private var mIsRecording = false
  private var requestDraw = false
  private val mScreenCaptureTask: DrawTask? = DrawTask(null, 0)

  private inner class DrawTask(sharedContext: IContext?, flags: Int) :
    EglTask(sharedContext, flags) {
    private var display: VirtualDisplay? = null
    private var intervals: Long = 0
    private var mTexId = 0
    private var mSourceTexture: SurfaceTexture? = null
    private var mSourceSurface: Surface? = null
    private var mEncoderSurface: IEglSurface? = null
    private var mDrawer: GLDrawer2D? = null
    private val mTexMatrix: FloatArray? = FloatArray(16)
    override fun onStart() {
      if (DEBUG) {
        Log.d(TAG, "mScreenCaptureTask#onStart:")
      }
      mDrawer = GLDrawer2D(true)
      mTexId = mDrawer!!.initTex()
      mSourceTexture = SurfaceTexture(mTexId)
      mSourceTexture!!.setDefaultBufferSize(mWidth, mHeight)
      mSourceSurface = Surface(mSourceTexture)
      mSourceTexture!!.setOnFrameAvailableListener(mOnFrameAvailableListener, mHandler)
      mEncoderSurface = getEgl()!!.createFromSurface(mSurface)
      if (DEBUG) {
        Log.d(TAG, "setup VirtualDisplay")
      }
      intervals = (1000f / fps) as Long
      display = mMediaProjection!!.createVirtualDisplay(
        "Capturing Display",
        mWidth, mHeight, mDensity,
        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
        mSourceSurface, mCallback, mHandler
      )
      if (DEBUG) {
        Log.v(
          TAG,
          "screen capture loop:display=$display"
        )
      }
      queueEvent(mDrawTask)
    }

    override fun onStop() {
      if (mDrawer != null) {
        mDrawer!!.release()
        mDrawer = null
      }
      if (mSourceSurface != null) {
        mSourceSurface!!.release()
        mSourceSurface = null
      }
      if (mSourceTexture != null) {
        mSourceTexture!!.release()
        mSourceTexture = null
      }
      if (mEncoderSurface != null) {
        mEncoderSurface!!.release()
        mEncoderSurface = null
      }
      makeCurrent()
      if (DEBUG) {
        Log.v(TAG, "mScreenCaptureTask#onStop:")
      }
      if (display != null) {
        if (DEBUG) {
          Log.v(TAG, "release VirtualDisplay")
        }
        display!!.release()
      }
      if (DEBUG) {
        Log.v(TAG, "tear down MediaProjection")
      }
      if (mMediaProjection != null) {
        mMediaProjection!!.stop()
        mMediaProjection = null
      }
    }

    override fun onError(e: Exception?): Boolean {
      if (DEBUG) {
        Log.w(TAG, "mScreenCaptureTask:", e)
      }
      return false
    }

    override fun processRequest(
      request: Int, arg1: Int, arg2: Int,
      obj: Object?
    ): Object? {
      return null
    }

    private val mOnFrameAvailableListener: OnFrameAvailableListener? =
      OnFrameAvailableListener {
        if (DEBUG) {
          Log.v(
            TAG,
            "onFrameAvailable:mIsRecording=$mIsRecording"
          )
        }
        if (mIsRecording) {
          synchronized(mSync as Object) {
            requestDraw = true
            mSync.notifyAll()
          }
        }
      }
    private val mDrawTask: Runnable? = object : Runnable {
      override fun run() {
        if (DEBUG) {
          Log.v(TAG, "draw:")
        }
        var local_request_draw: Boolean
        synchronized(mSync as Object) {
          local_request_draw = requestDraw
          requestDraw = false
          if (!local_request_draw) {
            try {
              mSync!!.wait(intervals)
              local_request_draw = requestDraw
              requestDraw = false
            } catch (e: InterruptedException) {
              return
            }
          }
        }
        if (mIsRecording) {
          if (local_request_draw) {
            mSourceTexture!!.updateTexImage()
            mSourceTexture!!.getTransformMatrix(mTexMatrix)
          }
          // 在Surface上绘制SurfaceTexture接收的图像以输入MediaCodec
          mEncoderSurface!!.makeCurrent()
          mDrawer!!.draw(mTexId, mTexMatrix, 0)
          mEncoderSurface!!.swap()
          makeCurrent()
          GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
          GLES20.glFlush()
          frameAvailableSoon()
          queueEvent(this)
        } else {
          releaseSelf()
        }
        //				if (DEBUG) Log.v(TAG, "draw:finished");
      }
    }
  }

  private val mCallback: Callback? =
    object : Callback() {
      /**
       * Called when the virtual display video projection has been
       * paused by the system or when the surface has been detached
       * by the application by calling setSurface(null).
       * The surface will not receive any more buffers while paused.
       */
      override fun onPaused() {
        if (DEBUG) {
          Log.v(TAG, "Callback#onPaused:")
        }
      }

      /**
       * Called when the virtual display video projection has been
       * resumed after having been paused.
       */
      override fun onResumed() {
        if (DEBUG) {
          Log.v(TAG, "Callback#onResumed:")
        }
      }

      /**
       * Called when the virtual display video projection has been
       * stopped by the system.  It will no longer receive frames
       * and it will never be resumed.  It is still the responsibility
       * of the application to release() the virtual display.
       */
      override fun onStopped() {
        if (DEBUG) {
          Log.v(TAG, "Callback#onStopped:")
        }
      }
    }

  companion object {
    private val TAG: String? = "MediaScreenEncoder"
    private const val DEBUG = true
    private val MIME_TYPE: String? = MediaFormat.MIMETYPE_VIDEO_AVC
    private const val FRAME_RATE = 25
  }

  init {
    this.fps = if (fps > 0 && fps <= 30) fps else FRAME_RATE
    this.bitrate = if (bitrate > 0) bitrate else calcBitRate(fps)
    val thread =
      HandlerThread(TAG)
    thread.start()
    mHandler = Handler(thread.looper)
  }
}