package com.sunplus.screenrecorder.media

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec.BufferInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaMuxer.OutputFormat
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import com.sunplus.toolbox.utils.FileUtils
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
class MediaMuxerWrapper(context: Context?, ext: String?) {
  private var mOutputPath: String? = null
  private val mMediaMuxer: MediaMuxer?
  private var mEncoderCount: Int
  private var mStartedCount: Int
  private var mIsStarted: Boolean

  @Volatile
  private var mIsPaused = false
  private var mVideoEncoder: MediaEncoder? = null
  private var mAudioEncoder: MediaEncoder? = null
  fun getOutputPath(): String? {
    return mOutputPath
  }

  @Synchronized @Throws(IOException::class) fun prepare() {
    Log.i(ContentValues.TAG, "prepare: ")
    if (mVideoEncoder != null) {
      mVideoEncoder!!.prepare()
    }
    if (mAudioEncoder != null) {
      mAudioEncoder!!.prepare()
    }
  }

  @Synchronized fun startRecording() {
    if (mVideoEncoder != null) {
      mVideoEncoder!!.startRecording()
    }
    if (mAudioEncoder != null) {
      mAudioEncoder!!.startRecording()
    }
  }

  @Synchronized fun stopRecording() {
    if (mVideoEncoder != null) {
      mVideoEncoder!!.stopRecording()
    }
    mVideoEncoder = null
    if (mAudioEncoder != null) {
      mAudioEncoder!!.stopRecording()
    }
    mAudioEncoder = null
  }

  @Synchronized fun isStarted(): Boolean {
    return mIsStarted
  }

  @Synchronized fun pauseRecording() {
    mIsPaused = true
    if (mVideoEncoder != null) {
      mVideoEncoder!!.pauseRecording()
    }
    if (mAudioEncoder != null) {
      mAudioEncoder!!.pauseRecording()
    }
  }

  @Synchronized fun resumeRecording() {
    if (mVideoEncoder != null) {
      mVideoEncoder!!.resumeRecording()
    }
    if (mAudioEncoder != null) {
      mAudioEncoder!!.resumeRecording()
    }
    mIsPaused = false
  }

  @Synchronized fun isPaused(): Boolean {
    return mIsPaused
  }

  fun addEncoder(encoder: MediaEncoder?) {
    if (encoder is MediaVideoEncoderBase) {
      require(mVideoEncoder == null) { "Video encoder already added." }
      mVideoEncoder = encoder
    } else if (encoder is MediaAudioEncoder) {
      require(mAudioEncoder == null) { "Video encoder already added." }
      mAudioEncoder = encoder
    } else {
      throw IllegalArgumentException("unsupported encoder")
    }
    mEncoderCount = (if (mVideoEncoder != null) 1 else 0) + if (mAudioEncoder != null) 1 else 0
  }

  @Synchronized fun start(): Boolean {
    if (DEBUG) {
      Log.v(ContentValues.TAG, "start:")
    }
    mStartedCount++
    if (mEncoderCount > 0 && mStartedCount == mEncoderCount) {
      mMediaMuxer!!.start()
      mIsStarted = true
      (this as Object).notifyAll()
      if (DEBUG) {
        Log.v(ContentValues.TAG, "MediaMuxer started:")
      }
    }
    return mIsStarted
  }

  @Synchronized fun stop() {
    if (DEBUG) {
      Log.v(ContentValues.TAG, "stop:mStatredCount=$mStartedCount")
    }
    mStartedCount--
    if (mEncoderCount > 0 && mStartedCount <= 0) {
      mMediaMuxer!!.stop()
      mMediaMuxer.release()
      mIsStarted = false
      if (DEBUG) {
        Log.v(ContentValues.TAG, "MediaMuxer stopped:")
      }
    }
  }

  @Synchronized fun addTrack(format: MediaFormat?): Int {
    check(!mIsStarted) { "muxer already started" }
    val trackIx = mMediaMuxer!!.addTrack(format)
    if (DEBUG) {
      Log.i(
        ContentValues.TAG,
        "addTrack:trackNum=$mEncoderCount,trackIx=$trackIx,format=$format"
      )
    }
    return trackIx
  }

  @Synchronized fun writeSampleData(
    trackIndex: Int, byteBuf: ByteBuffer?,
    bufferInfo: BufferInfo?
  ) {
    if (mStartedCount > 0) {
      mMediaMuxer!!.writeSampleData(trackIndex, byteBuf, bufferInfo)
    }
  }

  companion object {
    private const val DEBUG = false
  }

  init {
    var extName = ext
    if (TextUtils.isEmpty(extName)) {
      extName = ".mp4"
    }
    mOutputPath = try {
      FileUtils.getCaptureFile(
        context,
        Environment.DIRECTORY_MOVIES,
        ext,
        0
      ).toString()
    } catch (e: NullPointerException) {
      throw RuntimeException("This app has no permission of writing external storage")
    }
    mMediaMuxer = MediaMuxer(
      mOutputPath,
      OutputFormat.MUXER_OUTPUT_MPEG_4
    )
    mStartedCount = 0
    mEncoderCount = mStartedCount
    mIsStarted = false
  }
}