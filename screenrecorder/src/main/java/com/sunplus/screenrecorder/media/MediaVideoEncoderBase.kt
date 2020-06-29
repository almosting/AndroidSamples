package com.sunplus.screenrecorder.media

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecInfo.CodecCapabilities
import android.media.MediaCodecList
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import java.io.IOException

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
abstract class MediaVideoEncoderBase internal constructor(
  muxer: MediaMuxerWrapper?, listener: MediaEncoderCallback?,
  val mWidth: Int, val mHeight: Int
) : MediaEncoder(muxer, listener) {
  private fun createEncoderFormat(
    mime: String?, frameRate: Int,
    bitrate: Int
  ): MediaFormat? {
    if (DEBUG) {
      Log.v(
        TAG, String.format(
          "createEncoderFormat:(%d,%d),mime=%s,frameRate=%d,bitrate=%d", mWidth,
          mHeight, mime, frameRate, bitrate
        )
      )
    }
    val format =
      MediaFormat.createVideoFormat(mime, mWidth, mHeight)
    format.setInteger(
      MediaFormat.KEY_COLOR_FORMAT,
      CodecCapabilities.COLOR_FormatSurface
    )
    format.setInteger(
      MediaFormat.KEY_BIT_RATE,
      if (bitrate > 0) bitrate else calcBitRate(frameRate)
    )
    format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
    format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10)
    return format
  }

  @Throws(IOException::class, IllegalArgumentException::class)
  fun prepareSurfaceEncoder(
    mime: String?, frameRate: Int,
    bitrate: Int
  ): Surface? {
    if (DEBUG) {
      Log.v(
        TAG, String.format(
          "prepareSurfaceEncoder:(%d,%d),mime=%s,frameRate=%d,bitrate=%d", mWidth,
          mHeight, mime, frameRate, bitrate
        )
      )
    }
    mTrackIndex = -1
    mIsEOS = false
    mMuxerStarted = mIsEOS
    val videoCodecInfo =
      selectVideoCodec(mime)
        ?: throw IllegalArgumentException("Unable to find an appropriate codec for $mime")
    if (DEBUG) {
      Log.i(
        TAG,
        "selected codec: " + videoCodecInfo.name
      )
    }
    val format = createEncoderFormat(mime, frameRate, bitrate)
    if (DEBUG) {
      Log.i(TAG, "format: $format")
    }
    mMediaCodec = MediaCodec.createEncoderByType(mime)
    mMediaCodec!!.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
    return mMediaCodec!!.createInputSurface()
  }

  fun calcBitRate(frameRate: Int): Int {
    val bitrate = (BPP * frameRate * mWidth * mHeight) as Int
    Log.i(
      TAG,
      String.format("bitrate=%5.2f[Mbps]", bitrate / 1024f / 1024f)
    )
    return bitrate
  }

  companion object {
    private const val DEBUG = false
    private val TAG: String? = "MediaVideoEncoderBase"
    private const val BPP = 0.25f

    /**
     * select the first codec that match a specific MIME type
     *
     * @return null if no codec matched
     */
    private fun selectVideoCodec(mimeType: String?): MediaCodecInfo? {
      if (DEBUG) {
        Log.v(TAG, "selectVideoCodec:")
      }

      // get the list of available codecs
      val numCodecs = MediaCodecList.getCodecCount()
      for (i in 0 until numCodecs) {
        val codecInfo =
          MediaCodecList.getCodecInfoAt(i)
        if (!codecInfo.isEncoder) {
          continue
        }
        // select first codec that match a specific MIME type and color format
        val types = codecInfo.supportedTypes
        for (type in types) {
          if (type.equals(mimeType, ignoreCase = true)) {
            if (DEBUG) {
              Log.i(
                TAG,
                "codec:" + codecInfo.name + ",MIME=" + type
              )
            }
            val format = selectColorFormat(codecInfo, mimeType)
            if (format > 0) {
              return codecInfo
            }
          }
        }
      }
      return null
    }

    fun selectColorFormat(
      codecInfo: MediaCodecInfo?,
      mimeType: String?
    ): Int {
      if (DEBUG) {
        Log.i(TAG, "selectColorFormat: ")
      }
      var result = 0
      val caps: CodecCapabilities?
      try {
        Thread.currentThread().priority = Thread.MAX_PRIORITY
        caps = codecInfo!!.getCapabilitiesForType(mimeType)
      } finally {
        Thread.currentThread().priority = Thread.NORM_PRIORITY
      }
      var colorFormat: Int
      for (i in caps!!.colorFormats.indices) {
        colorFormat = caps.colorFormats[i]
        if (isRecognizedViewoFormat(colorFormat)) {
          result = colorFormat
          break
        }
      }
      if (result == 0) {
        Log.e(
          TAG,
          "couldn't find a good color format for " + codecInfo!!.getName() + " / " + mimeType
        )
      }
      return result
    }

    /**
     * color formats that we can use in this class
     */
    private val recognizedFormats: IntArray?
    private fun isRecognizedViewoFormat(colorFormat: Int): Boolean {
      if (DEBUG) {
        Log.i(
          TAG,
          "isRecognizedViewoFormat:colorFormat=$colorFormat"
        )
      }
      val n =
        recognizedFormats?.size ?: 0
      for (i in 0 until n) {
        if (recognizedFormats!!.get(i) == colorFormat) {
          return true
        }
      }
      return false
    }

    init {
      recognizedFormats =
        intArrayOf( //        	MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
          //        	MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
          //        	MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar,
          CodecCapabilities.COLOR_FormatSurface
        )
    }
  }

  override fun signalEndOfInputStream() {
    if (DEBUG) {
      Log.d(TAG, "sending EOS to encoder")
    }
    mMediaCodec!!.signalEndOfInputStream() // API >= 18
    mIsEOS = true
  }
}