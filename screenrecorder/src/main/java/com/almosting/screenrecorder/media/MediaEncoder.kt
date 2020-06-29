package com.almosting.screenrecorder.media

import android.media.MediaCodec
import android.media.MediaCodec.BufferInfo
import android.util.Log
import java.io.IOException
import java.lang.ref.WeakReference
import java.nio.ByteBuffer

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
abstract class MediaEncoder internal constructor(
  muxer: MediaMuxerWrapper?,
  callback: MediaEncoderCallback?
) : Runnable {
  interface MediaEncoderCallback {
    fun onPrepared(encoder: MediaEncoder?)
    fun onStopped(encoder: MediaEncoder?)
  }

  private val mSync: Object? = Object()

  @Volatile
  var mIsCapturing = false

  @Volatile
  var mRequestStop = false
  var mIsEOS = false
  var mMuxerStarted = false
  var mTrackIndex = 0
  var mMediaCodec: MediaCodec? = null
  private val mWeakMuxer: WeakReference<MediaMuxerWrapper?>?
  private var mRequestDrain = 0
  private var mBufferInfo: BufferInfo? = null
  val mListener: MediaEncoderCallback?

  @Volatile
  private var mRequestPause = false
  private var mLastPausedTimesUs: Long = 0
  fun getOutputPath(): String? {
    val muxer = mWeakMuxer!!.get()
    return muxer?.getOutputPath()
  }

  fun frameAvailableSoon(): Boolean {
    if (DEBUG) {
      Log.v(TAG, "frameAvailableSoon")
    }
    synchronized(mSync as Object) {
      if (!mIsCapturing || mRequestStop) {
        return false
      }
      mRequestDrain++
      mSync.notifyAll()
    }
    return true
  }

  /**
   * encoding loop on private thread
   */
  override fun run() {
    //		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
    synchronized(mSync as Object) {
      mRequestStop = false
      mRequestDrain = 0
      mSync.notify()
    }
    var localRequestStop: Boolean
    var localRequestDrain: Boolean
    while (true) {
      synchronized(mSync) {
        localRequestStop = mRequestStop
        localRequestDrain = mRequestDrain > 0
        if (localRequestDrain) {
          mRequestDrain--
        }
      }
      if (localRequestStop) {
        drain()
        // request stop recording
        signalEndOfInputStream()
        // process output data again for EOS signale
        drain()
        // release all related objects
        release()
        break
      }
      if (localRequestDrain) {
        drain()
      } else {
        synchronized(mSync) {
          try {
            mSync.wait()
          } catch (e: InterruptedException) {
            return
          }
        }
      }
    } // end of while
    if (DEBUG) {
      Log.d(TAG, "Encoder thread exiting")
    }
    synchronized(mSync) {
      mRequestStop = true
      mIsCapturing = false
    }
  }

  /*
   * preparing method for each sub class
   * this method should be implemented in sub class, so set this as abstract method
   * @throws IOException
   */
  /*package*/
  @Throws(IOException::class) abstract fun prepare()

  /*package*/
  open fun startRecording() {
    if (DEBUG) {
      Log.v(TAG, "startRecording")
    }
    synchronized(mSync as Object) {
      mIsCapturing = true
      mRequestStop = false
      mRequestPause = false
      mSync.notifyAll()
    }
  }

  /**
   * the method to request stop encoding
   */
  /*package*/
  open fun stopRecording() {
    if (DEBUG) {
      Log.d(TAG, "stopRecording")
    }
    synchronized(mSync as Object) {
      if (!mIsCapturing || mRequestStop) {
        return
      }
      // for rejecting newer frame
      mRequestStop = true
      mSync.notifyAll()
    }
  }

  fun pauseRecording() {
    if (DEBUG) {
      Log.v(TAG, "pauseRecording")
    }
    synchronized(mSync as Object) {
      if (!mIsCapturing || mRequestStop) {
        return
      }
      mRequestPause = true
      mLastPausedTimesUs = System.nanoTime() / 1000
      mSync.notifyAll()
    }
  }

  fun resumeRecording() {
    if (DEBUG) {
      Log.v(TAG, "resumeRecording")
    }
    synchronized(mSync as Object) {
      if (!mIsCapturing || mRequestStop) {
        return
      }
      if (mLastPausedTimesUs != 0L) {
        offsetPTSUs = System.nanoTime() / 1000 - mLastPausedTimesUs
        mLastPausedTimesUs = 0
      }
      mRequestPause = false
      mSync.notifyAll()
    }
  }
  //********************************************************************************
  //********************************************************************************
  /**
   * Release all released objects
   */
  protected open fun release() {
    if (DEBUG) {
      Log.d(TAG, "release:")
    }
    try {
      mListener!!.onStopped(this)
    } catch (e: Exception) {
      Log.e(TAG, "failed onStopped", e)
    }
    mIsCapturing = false
    if (mMediaCodec != null) {
      try {
        mMediaCodec!!.stop()
        mMediaCodec!!.release()
        mMediaCodec = null
      } catch (e: Exception) {
        Log.e(TAG, "failed releasing MediaCodec", e)
      }
    }
    if (mMuxerStarted) {
      val muxer = mWeakMuxer?.get()
      if (muxer != null) {
        try {
          muxer.stop()
        } catch (e: Exception) {
          Log.e(TAG, "failed stopping muxer", e)
        }
      }
    }
    mBufferInfo = null
  }

  protected open fun signalEndOfInputStream() {
    if (DEBUG) {
      Log.d(TAG, "sending EOS to encoder")
    }
    // signalEndOfInputStream is only avairable for video encoding with surface
    // and equivalent sending a empty buffer with BUFFER_FLAG_END_OF_STREAM flag.
    //		mMediaCodec.signalEndOfInputStream();	// API >= 18
    encode(null, 0, getPTSUs())
  }

  /**
   * Method to set byte array to the MediaCodec encoder
   *
   * @param length 　length of byte array, zero means EOS.
   */
  fun encode(
    buffer: ByteBuffer?,
    length: Int,
    presentationTimeUs: Long
  ) {
    if (!mIsCapturing) {
      return
    }
    val inputBuffers = mMediaCodec!!.getInputBuffers()
    while (mIsCapturing) {
      val inputBufferIndex =
        mMediaCodec!!.dequeueInputBuffer(TIMEOUT_USEC.toLong())
      if (inputBufferIndex >= 0) {
        val inputBuffer = inputBuffers[inputBufferIndex]
        inputBuffer.clear()
        if (buffer != null) {
          inputBuffer.put(buffer)
        }
        //	            if (DEBUG) Log.v(TAG, "encode:queueInputBuffer");
        if (length <= 0) {
          // send EOS
          mIsEOS = true
          if (DEBUG) {
            Log.i(TAG, "send BUFFER_FLAG_END_OF_STREAM")
          }
          mMediaCodec!!.queueInputBuffer(
            inputBufferIndex, 0, 0,
            presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM
          )
          break
        } else {
          mMediaCodec!!.queueInputBuffer(
            inputBufferIndex, 0, length,
            presentationTimeUs, 0
          )
        }
        break
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
  private fun drain() {
    if (mMediaCodec == null) {
      return
    }
    var encoderOutputBuffers = mMediaCodec!!.getOutputBuffers()
    var encoderStatus: Int
    var count = 0
    val muxer = mWeakMuxer!!.get()
    if (muxer == null) {
      //        	throw new NullPointerException("muxer is unexpectedly null");
      Log.w(TAG, "muxer is unexpectedly null")
      return
    }
    LOOP@ while (mIsCapturing) {
      // get encoded data with maximum timeout duration of TIMEOUT_USEC(=10[msec])
      encoderStatus =
        mMediaCodec!!.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC.toLong())
      if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
        // wait 5 counts(=TIMEOUT_USEC x 5 = 50msec) until data/EOS come
        if (!mIsEOS) {
          if (++count > 5) {
            break@LOOP  // out of while
          }
        }
      } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
        if (DEBUG) {
          Log.v(TAG, "INFO_OUTPUT_BUFFERS_CHANGED")
        }
        // this should not come when encoding
        encoderOutputBuffers = mMediaCodec!!.getOutputBuffers()
      } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
        if (DEBUG) {
          Log.v(TAG, "INFO_OUTPUT_FORMAT_CHANGED")
        }
        // this status indicate the output format of codec is changed
        // this should come only once before actual encoded data
        // but this status never come on Android4.3 or less
        // and in that case, you should treat when MediaCodec.BUFFER_FLAG_CODEC_CONFIG come.
        // second time request is error
        if (mMuxerStarted) {
          throw RuntimeException("format changed twice")
        }
        // get output format from codec and pass them to muxer
        // getOutputFormat should be called after INFO_OUTPUT_FORMAT_CHANGED otherwise crash.
        val format = mMediaCodec!!.getOutputFormat()
        mTrackIndex = muxer.addTrack(format)
        mMuxerStarted = true
        if (!muxer.start()) {
          // we should wait until muxer is ready
          synchronized(muxer as Object) {
            while (!muxer.isStarted()) {
              try {
                muxer.wait(100)
              } catch (e: InterruptedException) {
                break
              }
            }
          }
        }
      } else if (encoderStatus < 0) {
        // unexpected status
        if (DEBUG) {
          Log.w(
            TAG,
            "drain:unexpected result from encoder#dequeueOutputBuffer: $encoderStatus"
          )
        }
      } else {
        val encodedData = encoderOutputBuffers[encoderStatus]
          ?: // this never should come...may be a MediaCodec internal error
          throw RuntimeException("encoderOutputBuffer $encoderStatus was null")
        if (mBufferInfo!!.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
          // You should set output format to muxer here when you target Android4.3 or less
          // but MediaCodec#getOutputFormat can not call here(because INFO_OUTPUT_FORMAT_CHANGED don't come yet)
          // therefor we should expand and prepare output format from buffer data.
          // This sample is for API>=18(>=Android 4.3), just ignore this flag here
          if (DEBUG) {
            Log.d(TAG, "drain:BUFFER_FLAG_CODEC_CONFIG")
          }
          mBufferInfo!!.size = 0
        }
        if (mBufferInfo!!.size != 0) {
          // encoded data is ready, clear waiting counter
          count = 0
          if (!mMuxerStarted) {
            // muxer is not ready...this will be programing failure.
            throw RuntimeException("drain:muxer hasn't started")
          }
          // write encoded data to muxer(need to adjust presentationTimeUs.
          if (!mRequestPause) {
            mBufferInfo!!.presentationTimeUs = getPTSUs()
            muxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo)
            prevOutputPTSUs = mBufferInfo!!.presentationTimeUs
          }
        }
        // return buffer to encoder
        mMediaCodec!!.releaseOutputBuffer(encoderStatus, false)
        if (mBufferInfo!!.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
          // when EOS come.
          mIsCapturing = false
          break // out of while
        }
      }
    }
  }

  /**
   * previous presentationTimeUs for writing
   */
  private var prevOutputPTSUs: Long = 0
  private var offsetPTSUs: Long = 0

  /**
   * get next encoding presentationTimeUs
   */
  fun getPTSUs(): Long {
    var result: Long
    synchronized(mSync as Object) { result = System.nanoTime() / 1000L - offsetPTSUs }
    // presentationTimeUs should be monotonic
    // otherwise muxer fail to write
    if (result < prevOutputPTSUs) {
      val offset = prevOutputPTSUs - result
      offsetPTSUs -= offset
      result += offset
    }
    return result
  }

  companion object {
    private val TAG: String? = "MediaEncoder"
    private const val DEBUG = true
    protected const val TIMEOUT_USEC = 10000
    protected const val MSG_FRAME_AVAILABLE = 1
    protected const val MSG_STOP_RECORDING = 9
  }

  init {
    if (callback == null) {
      throw NullPointerException("MediaEncoderCallBack is Null")
    }
    if (muxer == null) {
      throw NullPointerException("MediaMuxerWrapper is null")
    }
    mWeakMuxer = WeakReference(muxer)
    muxer.addEncoder(this)
    mListener = callback
    synchronized(mSync as Object) {
      mBufferInfo = BufferInfo()
      Thread(this, javaClass.simpleName).start()
      try {
        mSync.wait()
        Log.d(TAG, "MediaEncoder: ")
      } catch (e: InterruptedException) {
        e.printStackTrace()
      }
    }
  }
}