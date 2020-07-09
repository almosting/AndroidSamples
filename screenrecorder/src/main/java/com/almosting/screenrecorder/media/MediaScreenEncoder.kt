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
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

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
  private val lock = ReentrantLock()
  private val condition = lock.newCondition()
  override fun release() {
    mHandler!!.looper.quit()
    super.release()
  }

  @Throws(IOException::class) override fun prepare() {
    Log.i(TAG, "prepare: ")
    mSurface = prepareSurfaceEncoder(MIME_TYPE, fps, bitrate)
    mMediaCodec!!.start()
    mIsRecording = true
    Thread(mScreenCaptureTask, "ScreenCaptureThread").start()
    Log.i(TAG, "prepare finishing")
    if (mListener != null) {
      try {
        mListener.onPrepared(this)
      } catch (e: Exception) {
        Log.e(TAG, "prepare:", e)
      }
    }
  }

  override fun stopRecording() {
    Log.v(TAG, "stopRecording:")
    lock.withLock {
      mIsRecording = false
      condition.signalAll()
      return@withLock
    }
    super.stopRecording()
  }


  @Volatile
  private var mIsRecording = false
  private var requestDraw = false
  private val mScreenCaptureTask: DrawTask? = DrawTask(null, 0)

  private inner class DrawTask(sharedContext: IContext?, flags: Int) :
    EglTask(sharedContext, flags) {
    private lateinit var display: VirtualDisplay
    private var intervals: Long = 0
    private var mTexId = 0
    private lateinit var mSourceTexture: SurfaceTexture
    private lateinit var mSourceSurface: Surface
    private lateinit var mEncoderSurface: IEglSurface
    private lateinit var mDrawer: GLDrawer2D
    private val mTexMatrix: FloatArray = FloatArray(16)
    override fun onStart() {
      Log.d(TAG, "mScreenCaptureTask#onStart:")
      mDrawer = GLDrawer2D(true)
      mTexId = mDrawer.initTex()
      mSourceTexture = SurfaceTexture(mTexId)
      mSourceTexture.setDefaultBufferSize(mWidth, mHeight)
      mSourceSurface = Surface(mSourceTexture)
      mSourceTexture.setOnFrameAvailableListener(mOnFrameAvailableListener, mHandler)
      mEncoderSurface = getEgl()?.createFromSurface(mSurface)!!
      Log.d(TAG, "setup VirtualDisplay")
      intervals = (1000f / fps).toLong()
      display = mMediaProjection!!.createVirtualDisplay(
        "Capturing Display",
        mWidth, mHeight, mDensity,
        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
        mSourceSurface, mCallback, mHandler
      )
      Log.v(
        TAG,
        "screen capture loop:display=$display"
      )
      queueEvent(mDrawTask)
    }

    override fun onStop() {
      mDrawer.release()
      mSourceSurface.release()
      mSourceTexture.release()
      mEncoderSurface.release()
      makeCurrent()
      Log.v(TAG, "mScreenCaptureTask#onStop:")
      Log.v(TAG, "release VirtualDisplay")
      display.release()
      Log.v(TAG, "tear down MediaProjection")
      mMediaProjection?.stop()
    }

    override fun onError(e: Exception?): Boolean {
      Log.w(TAG, "mScreenCaptureTask:", e)
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
        Log.v(
          TAG,
          "onFrameAvailable:mIsRecording=$mIsRecording"
        )
        if (mIsRecording) {
          lock.withLock {
            requestDraw = true
            condition.signalAll()
          }
        }
      }
    private val mDrawTask: Runnable? = object : Runnable {
      override fun run() {
        Log.v(TAG, "draw:")
        var localRequestDraw = false
        lock.withLock {
          localRequestDraw = requestDraw
          requestDraw = false
          if (!localRequestDraw) {
            condition.await(intervals, java.util.concurrent.TimeUnit.MILLISECONDS)
            localRequestDraw = requestDraw
            requestDraw = false
            return@withLock
          }

        }
        if (mIsRecording) {
          if (localRequestDraw) {
            mSourceTexture.updateTexImage()
            mSourceTexture.getTransformMatrix(mTexMatrix)
          }
          // 在Surface上绘制SurfaceTexture接收的图像以输入MediaCodec
          mEncoderSurface.makeCurrent()
          mDrawer.draw(mTexId, mTexMatrix, 0)
          mEncoderSurface.swap()
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

  private val mCallback: Callback =
    object : Callback() {
      /**
       * Called when the virtual display video projection has been
       * paused by the system or when the surface has been detached
       * by the application by calling setSurface(null).
       * The surface will not receive any more buffers while paused.
       */
      override fun onPaused() {
        Log.v(TAG, "Callback#onPaused:")
      }

      /**
       * Called when the virtual display video projection has been
       * resumed after having been paused.
       */
      override fun onResumed() {
        Log.v(TAG, "Callback#onResumed:")
      }

      /**
       * Called when the virtual display video projection has been
       * stopped by the system.  It will no longer receive frames
       * and it will never be resumed.  It is still the responsibility
       * of the application to release() the virtual display.
       */
      override fun onStopped() {
        Log.v(TAG, "Callback#onStopped:")
      }
    }

  companion object {
    private const val TAG: String = "MediaScreenEncoder"
    private const val MIME_TYPE: String = MediaFormat.MIMETYPE_VIDEO_AVC
    private const val FRAME_RATE = 25
  }

  init {
    this.fps = if (fps in 1..30) fps else FRAME_RATE
    this.bitrate = if (bitrate > 0) bitrate else calcBitRate(fps)
    val thread = HandlerThread(TAG)
    thread.start()
    mHandler = Handler(thread.looper)
  }
}