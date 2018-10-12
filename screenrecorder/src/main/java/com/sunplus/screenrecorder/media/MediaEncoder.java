package com.sunplus.screenrecorder.media;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
public abstract class MediaEncoder implements Runnable {
  private static final String TAG = "MediaEncoder";

  private static final boolean DEBUG = true;

  protected static final int TIMEOUT_USEC = 10000;
  protected static final int MSG_FRAME_AVAILABLE = 1;
  protected static final int MSG_STOP_RECORDING = 9;

  public interface MediaEncoderCallback {
    void onPrepared(MediaEncoder encoder);

    void onStopped(MediaEncoder encoder);
  }

  protected final Object mSync = new Object();

  protected volatile boolean mIsCapturing;

  protected volatile boolean mRequestStop;

  protected boolean mIsEOS;

  protected boolean mMuxerStarted;

  protected int mTrackIndex;

  protected MediaCodec mMediaCodec;

  protected final WeakReference<MediaMuxerWrapper> mWeakMuxer;

  private int mRequestDrain;

  private MediaCodec.BufferInfo mBufferInfo;

  protected final MediaEncoderCallback mListener;

  protected volatile boolean mRequestPause;

  private long mLastPausedTimesUs;

  public MediaEncoder(final MediaMuxerWrapper muxer, final MediaEncoderCallback callback) {
    if (callback == null) {
      throw new NullPointerException("MediaEncoderCallBack is Null");
    }

    if (muxer == null) {
      throw new NullPointerException("MediaMuxerWrapper is null");
    }

    mWeakMuxer = new WeakReference<>(muxer);
    muxer.addEncoder(this);
    mListener = callback;
    synchronized (mSync) {
      mBufferInfo = new MediaCodec.BufferInfo();

      new Thread(this, getClass().getSimpleName()).start();

      try {
        mSync.wait();
        Log.d(TAG, "MediaEncoder: ");
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public String getOutputPath() {
    final MediaMuxerWrapper muxer = mWeakMuxer.get();
    return muxer != null ? muxer.getOutputPath() : null;
  }

  public boolean frameAvailableSoon() {
    if (DEBUG) Log.v(TAG, "frameAvailableSoon");
    synchronized (mSync) {
      if (!mIsCapturing || mRequestStop) {
        return false;
      }
      mRequestDrain++;
      mSync.notifyAll();
    }
    return true;
  }

  /**
   * encoding loop on private thread
   */
  @Override
  public void run() {
    //		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
    synchronized (mSync) {
      mRequestStop = false;
      mRequestDrain = 0;
      mSync.notify();
    }
    final boolean isRunning = true;
    boolean localRequestStop;
    boolean localRequestDrain;
    while (isRunning) {
      synchronized (mSync) {
        localRequestStop = mRequestStop;
        localRequestDrain = (mRequestDrain > 0);
        if (localRequestDrain) {
          mRequestDrain--;
        }
      }
      if (localRequestStop) {
        drain();
        // request stop recording
        signalEndOfInputStream();
        // process output data again for EOS signale
        drain();
        // release all related objects
        release();
        break;
      }
      if (localRequestDrain) {
        drain();
      } else {
        synchronized (mSync) {
          try {
            mSync.wait();
          } catch (final InterruptedException e) {
            break;
          }
        }
      }
    } // end of while
    if (DEBUG) {
      Log.d(TAG, "Encoder thread exiting");
    }
    synchronized (mSync) {
      mRequestStop = true;
      mIsCapturing = false;
    }
  }

  /*
   * preparing method for each sub class
   * this method should be implemented in sub class, so set this as abstract method
   * @throws IOException
   */
  /*package*/
  abstract void prepare() throws IOException;

  /*package*/ void startRecording() {
    if (DEBUG) {
      Log.v(TAG, "startRecording");
    }
    synchronized (mSync) {
      mIsCapturing = true;
      mRequestStop = false;
      mRequestPause = false;
      mSync.notifyAll();
    }
  }

  /**
   * the method to request stop encoding
   */
  /*package*/ void stopRecording() {
    if (DEBUG) {
      Log.d(TAG, "stopRecording");
    }
    synchronized (mSync) {
      if (!mIsCapturing || mRequestStop) {
        return;
      }
      mRequestStop = true;  // for rejecting newer frame
      mSync.notifyAll();
      // We can not know when the encoding and writing finish.
      // so we return immediately after request to avoid delay of caller thread
    }
  }

  /*package*/ void pauseRecording() {
    if (DEBUG) {
      Log.v(TAG, "pauseRecording");
    }
    synchronized (mSync) {
      if (!mIsCapturing || mRequestStop) {
        return;
      }
      mRequestPause = true;
      mLastPausedTimesUs = System.nanoTime() / 1000;
      mSync.notifyAll();
    }
  }

  /*package*/ void resumeRecording() {
    if (DEBUG) {
      Log.v(TAG, "resumeRecording");
    }
    synchronized (mSync) {
      if (!mIsCapturing || mRequestStop) {
        return;
      }
      if (mLastPausedTimesUs != 0) {
        offsetPTSUs = System.nanoTime() / 1000 - mLastPausedTimesUs;
        mLastPausedTimesUs = 0;
      }
      mRequestPause = false;
      mSync.notifyAll();
    }
  }

  //********************************************************************************
  //********************************************************************************

  /**
   * Release all released objects
   */
  protected void release() {
    if (DEBUG) {
      Log.d(TAG, "release:");
    }
    try {
      mListener.onStopped(this);
    } catch (final Exception e) {
      Log.e(TAG, "failed onStopped", e);
    }
    mIsCapturing = false;
    if (mMediaCodec != null) {
      try {
        mMediaCodec.stop();
        mMediaCodec.release();
        mMediaCodec = null;
      } catch (final Exception e) {
        Log.e(TAG, "failed releasing MediaCodec", e);
      }
    }
    if (mMuxerStarted) {
      final MediaMuxerWrapper muxer = mWeakMuxer != null ? mWeakMuxer.get() : null;
      if (muxer != null) {
        try {
          muxer.stop();
        } catch (final Exception e) {
          Log.e(TAG, "failed stopping muxer", e);
        }
      }
    }
    mBufferInfo = null;
  }

  protected void signalEndOfInputStream() {
    if (DEBUG) {
      Log.d(TAG, "sending EOS to encoder");
    }
    // signalEndOfInputStream is only avairable for video encoding with surface
    // and equivalent sending a empty buffer with BUFFER_FLAG_END_OF_STREAM flag.
    //		mMediaCodec.signalEndOfInputStream();	// API >= 18
    encode(null, 0, getPTSUs());
  }

  /**
   * Method to set byte array to the MediaCodec encoder
   *
   * @param length 　length of byte array, zero means EOS.
   */
  protected void encode(final ByteBuffer buffer, final int length, final long presentationTimeUs) {
    if (!mIsCapturing) {
      return;
    }
    final ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
    while (mIsCapturing) {
      final int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
      if (inputBufferIndex >= 0) {
        final ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
        inputBuffer.clear();
        if (buffer != null) {
          inputBuffer.put(buffer);
        }
        //	            if (DEBUG) Log.v(TAG, "encode:queueInputBuffer");
        if (length <= 0) {
          // send EOS
          mIsEOS = true;
          if (DEBUG) {
            Log.i(TAG, "send BUFFER_FLAG_END_OF_STREAM");
          }
          mMediaCodec.queueInputBuffer(inputBufferIndex, 0, 0,
              presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
          break;
        } else {
          mMediaCodec.queueInputBuffer(inputBufferIndex, 0, length,
              presentationTimeUs, 0);
        }
        break;
      } else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
        // wait for MediaCodec encoder is ready to encode
        // nothing to do here because MediaCodec#dequeueInputBuffer(TIMEOUT_USEC)
        // will wait for maximum TIMEOUT_USEC(10msec) on each call
      }
    }
  }

  /**
   * drain encoded data and write them to muxer
   */
  protected void drain() {
    if (mMediaCodec == null) {
      return;
    }
    ByteBuffer[] encoderOutputBuffers = mMediaCodec.getOutputBuffers();
    int encoderStatus, count = 0;
    final MediaMuxerWrapper muxer = mWeakMuxer.get();
    if (muxer == null) {
      //        	throw new NullPointerException("muxer is unexpectedly null");
      Log.w(TAG, "muxer is unexpectedly null");
      return;
    }
    LOOP:
    while (mIsCapturing) {
      // get encoded data with maximum timeout duration of TIMEOUT_USEC(=10[msec])
      encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
      if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
        // wait 5 counts(=TIMEOUT_USEC x 5 = 50msec) until data/EOS come
        if (!mIsEOS) {
          if (++count > 5) {
            break LOOP;    // out of while
          }
        }
      } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
        if (DEBUG) {
          Log.v(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
        }
        // this should not come when encoding
        encoderOutputBuffers = mMediaCodec.getOutputBuffers();
      } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
        if (DEBUG) {
          Log.v(TAG, "INFO_OUTPUT_FORMAT_CHANGED");
        }
        // this status indicate the output format of codec is changed
        // this should come only once before actual encoded data
        // but this status never come on Android4.3 or less
        // and in that case, you should treat when MediaCodec.BUFFER_FLAG_CODEC_CONFIG come.
        if (mMuxerStarted) {  // second time request is error
          throw new RuntimeException("format changed twice");
        }
        // get output format from codec and pass them to muxer
        // getOutputFormat should be called after INFO_OUTPUT_FORMAT_CHANGED otherwise crash.
        final MediaFormat format = mMediaCodec.getOutputFormat(); // API >= 16
        mTrackIndex = muxer.addTrack(format);
        mMuxerStarted = true;
        if (!muxer.start()) {
          // we should wait until muxer is ready
          synchronized (muxer) {
            while (!muxer.isStarted()) {
              try {
                muxer.wait(100);
              } catch (final InterruptedException e) {
                break LOOP;
              }
            }
          }
        }
      } else if (encoderStatus < 0) {
        // unexpected status
        if (DEBUG) {
          Log.w(TAG, "drain:unexpected result from encoder#dequeueOutputBuffer: " + encoderStatus);
        }
      } else {
        final ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
        if (encodedData == null) {
          // this never should come...may be a MediaCodec internal error
          throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
        }
        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
          // You should set output format to muxer here when you target Android4.3 or less
          // but MediaCodec#getOutputFormat can not call here(because INFO_OUTPUT_FORMAT_CHANGED don't come yet)
          // therefor we should expand and prepare output format from buffer data.
          // This sample is for API>=18(>=Android 4.3), just ignore this flag here
          if (DEBUG) {
            Log.d(TAG, "drain:BUFFER_FLAG_CODEC_CONFIG");
          }
          mBufferInfo.size = 0;
        }

        if (mBufferInfo.size != 0) {
          // encoded data is ready, clear waiting counter
          count = 0;
          if (!mMuxerStarted) {
            // muxer is not ready...this will be programing failure.
            throw new RuntimeException("drain:muxer hasn't started");
          }
          // write encoded data to muxer(need to adjust presentationTimeUs.
          if (!mRequestPause) {
            mBufferInfo.presentationTimeUs = getPTSUs();
            muxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
            prevOutputPTSUs = mBufferInfo.presentationTimeUs;
          }
        }
        // return buffer to encoder
        mMediaCodec.releaseOutputBuffer(encoderStatus, false);
        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
          // when EOS come.
          mIsCapturing = false;
          break;      // out of while
        }
      }
    }
  }

  /**
   * previous presentationTimeUs for writing
   */
  private long prevOutputPTSUs = 0;

  private long offsetPTSUs = 0;

  /**
   * get next encoding presentationTimeUs
   */
  protected long getPTSUs() {
    long result;
    synchronized (mSync) {
      result = System.nanoTime() / 1000L - offsetPTSUs;
    }
    // presentationTimeUs should be monotonic
    // otherwise muxer fail to write
    if (result < prevOutputPTSUs) {
      final long offset = prevOutputPTSUs - result;
      offsetPTSUs -= offset;
      result += offset;
    }
    return result;
  }
}
