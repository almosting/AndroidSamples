package com.sunplus.screenrecorder.media;

import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;
import com.sunplus.screenrecorder.media.glutils.EGLBase;
import com.sunplus.screenrecorder.media.glutils.EglTask;
import com.sunplus.screenrecorder.media.glutils.GLDrawer2D;
import java.io.IOException;

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
public class MediaScreenEncoder extends MediaVideoEncoderBase {
  private static final String TAG = "MediaScreenEncoder";
  private static final boolean DEBUG = true;
  private static final String MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
  private static final int FRAME_RATE = 25;
  private MediaProjection mMediaProjection;
  private final int mDensity;
  private final int bitrate, fps;
  private Surface mSurface;
  private final Handler mHandler;

  public MediaScreenEncoder(final MediaMuxerWrapper muxer, final MediaEncoderCallback listener,
                            final MediaProjection projection, final int width, final int height,
                            final int density,
                            final int _bitrate, final int _fps) {

    super(muxer, listener, width, height);
    mMediaProjection = projection;
    mDensity = density;
    fps = (_fps > 0 && _fps <= 30) ? _fps : FRAME_RATE;
    bitrate = (_bitrate > 0) ? _bitrate : calcBitRate(_fps);
    final HandlerThread thread = new HandlerThread(TAG);
    thread.start();
    mHandler = new Handler(thread.getLooper());
  }

  @Override
  protected void release() {
    mHandler.getLooper().quit();
    super.release();
  }

  @Override
  void prepare() throws IOException {
    if (DEBUG) {
      Log.i(TAG, "prepare: ");
    }
    mSurface = prepareSurfaceEncoder(MIME_TYPE, fps, bitrate);
    mMediaCodec.start();
    mIsRecording = true;
    new Thread(mScreenCaptureTask, "ScreenCaptureThread").start();
    if (DEBUG) {
      Log.i(TAG, "prepare finishing");
    }
    if (mListener != null) {
      try {
        mListener.onPrepared(this);
      } catch (final Exception e) {
        Log.e(TAG, "prepare:", e);
      }
    }
  }

  @Override
  void stopRecording() {
    if (DEBUG) {
      Log.v(TAG, "stopRecording:");
    }
    synchronized (mSync) {
      mIsRecording = false;
      mSync.notifyAll();
    }
    super.stopRecording();
  }

  private final Object mSync = new Object();
  private volatile boolean mIsRecording;

  private boolean requestDraw;
  private final DrawTask mScreenCaptureTask = new DrawTask(null, 0);

  private final class DrawTask extends EglTask {
    private VirtualDisplay display;
    private long intervals;
    private int mTexId;
    private SurfaceTexture mSourceTexture;
    private Surface mSourceSurface;
    private EGLBase.IEglSurface mEncoderSurface;
    private GLDrawer2D mDrawer;
    private final float[] mTexMatrix = new float[16];

    public DrawTask(final EGLBase.IContext sharedContext, final int flags) {
      super(sharedContext, flags);
    }

    @Override
    protected void onStart() {
      if (DEBUG) {
        Log.d(TAG, "mScreenCaptureTask#onStart:");
      }
      mDrawer = new GLDrawer2D(true);
      mTexId = mDrawer.initTex();
      mSourceTexture = new SurfaceTexture(mTexId);
      mSourceTexture.setDefaultBufferSize(mWidth, mHeight);
      mSourceSurface = new Surface(mSourceTexture);
      mSourceTexture.setOnFrameAvailableListener(mOnFrameAvailableListener, mHandler);
      mEncoderSurface = getEgl().createFromSurface(mSurface);

      if (DEBUG) {
        Log.d(TAG, "setup VirtualDisplay");
      }
      intervals = (long) (1000f / fps);
      display = mMediaProjection.createVirtualDisplay(
          "Capturing Display",
          mWidth, mHeight, mDensity,
          DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
          mSourceSurface, mCallback, mHandler);
      if (DEBUG) {
        Log.v(TAG, "screen capture loop:display=" + display);
      }
      queueEvent(mDrawTask);
    }

    @Override
    protected void onStop() {
      if (mDrawer != null) {
        mDrawer.release();
        mDrawer = null;
      }
      if (mSourceSurface != null) {
        mSourceSurface.release();
        mSourceSurface = null;
      }
      if (mSourceTexture != null) {
        mSourceTexture.release();
        mSourceTexture = null;
      }
      if (mEncoderSurface != null) {
        mEncoderSurface.release();
        mEncoderSurface = null;
      }
      makeCurrent();
      if (DEBUG) {
        Log.v(TAG, "mScreenCaptureTask#onStop:");
      }
      if (display != null) {
        if (DEBUG) {
          Log.v(TAG, "release VirtualDisplay");
        }
        display.release();
      }
      if (DEBUG) {
        Log.v(TAG, "tear down MediaProjection");
      }
      if (mMediaProjection != null) {
        mMediaProjection.stop();
        mMediaProjection = null;
      }
    }

    @Override
    protected boolean onError(final Exception e) {
      if (DEBUG) {
        Log.w(TAG, "mScreenCaptureTask:", e);
      }
      return false;
    }

    @Override
    protected Object processRequest(final int request, final int arg1, final int arg2,
                                    final Object obj) {
      return null;
    }

    private final SurfaceTexture.OnFrameAvailableListener mOnFrameAvailableListener =
        new SurfaceTexture.OnFrameAvailableListener() {
          @Override
          public void onFrameAvailable(final SurfaceTexture surfaceTexture) {
            if (DEBUG) {
              Log.v(TAG, "onFrameAvailable:mIsRecording=" + mIsRecording);
            }
            if (mIsRecording) {
              synchronized (mSync) {
                requestDraw = true;
                mSync.notifyAll();
              }
            }
          }
        };

    private final Runnable mDrawTask = new Runnable() {
      @Override
      public void run() {
        if (DEBUG) {
          Log.v(TAG, "draw:");
        }
        boolean local_request_draw;
        synchronized (mSync) {
          local_request_draw = requestDraw;
          requestDraw = false;
          if (!local_request_draw) {
            try {
              mSync.wait(intervals);
              local_request_draw = requestDraw;
              requestDraw = false;
            } catch (final InterruptedException e) {
              return;
            }
          }
        }
        if (mIsRecording) {
          if (local_request_draw) {
            mSourceTexture.updateTexImage();
            mSourceTexture.getTransformMatrix(mTexMatrix);
          }
          // 在Surface上绘制SurfaceTexture接收的图像以输入MediaCodec
          mEncoderSurface.makeCurrent();
          mDrawer.draw(mTexId, mTexMatrix, 0);
          mEncoderSurface.swap();
          makeCurrent();
          GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
          GLES20.glFlush();
          frameAvailableSoon();
          queueEvent(this);
        } else {
          releaseSelf();
        }
        //				if (DEBUG) Log.v(TAG, "draw:finished");
      }
    };
  }

  private final VirtualDisplay.Callback mCallback = new VirtualDisplay.Callback() {
    /**
     * Called when the virtual display video projection has been
     * paused by the system or when the surface has been detached
     * by the application by calling setSurface(null).
     * The surface will not receive any more buffers while paused.
     */
    @Override
    public void onPaused() {
      if (DEBUG) {
        Log.v(TAG, "Callback#onPaused:");
      }
    }

    /**
     * Called when the virtual display video projection has been
     * resumed after having been paused.
     */
    @Override
    public void onResumed() {
      if (DEBUG) {
        Log.v(TAG, "Callback#onResumed:");
      }
    }

    /**
     * Called when the virtual display video projection has been
     * stopped by the system.  It will no longer receive frames
     * and it will never be resumed.  It is still the responsibility
     * of the application to release() the virtual display.
     */
    @Override
    public void onStopped() {
      if (DEBUG) {
        Log.v(TAG, "Callback#onStopped:");
      }
    }
  };
}
