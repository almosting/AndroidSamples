package com.sunplus.screenrecorder.media

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecInfo.CodecProfileLevel
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaRecorder.AudioSource
import android.os.Process
import android.util.Log
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
class MediaAudioEncoder(muxer: MediaMuxerWrapper?, callback: MediaEncoderCallback?) :
  MediaEncoder(muxer, callback) {
  private var mAudioThread: AudioThread? = null
  @Throws(IOException::class) override fun prepare() {
    if (DEBUG) {
      Log.v(TAG, "prepare:")
    }
    mTrackIndex = -1
    mIsEOS = false
    mMuxerStarted = mIsEOS
    // prepare MediaCodec for AAC encoding of audio data from inernal mic.
    val audioCodecInfo =
      selectAudioCodec(MIME_TYPE)
    if (audioCodecInfo == null) {
      Log.e(
        TAG,
        "Unable to find an appropriate codec for $MIME_TYPE"
      )
      return
    }
    if (DEBUG) {
      Log.i(
        TAG,
        "selected codec: " + audioCodecInfo.name
      )
    }
    val audioFormat = MediaFormat.createAudioFormat(
      MIME_TYPE,
      SAMPLE_RATE,
      1
    )
    audioFormat.setInteger(
      MediaFormat.KEY_AAC_PROFILE,
      CodecProfileLevel.AACObjectLC
    )
    audioFormat.setInteger(
      MediaFormat.KEY_CHANNEL_MASK,
      AudioFormat.CHANNEL_IN_MONO
    )
    audioFormat.setInteger(
      MediaFormat.KEY_BIT_RATE,
      BIT_RATE
    )
    audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1)
    //		audioFormat.setLong(MediaFormat.KEY_MAX_INPUT_SIZE, inputFile.length());
    //      audioFormat.setLong(MediaFormat.KEY_DURATION, (long)durationInMs );
    if (DEBUG) {
      Log.i(TAG, "format: $audioFormat")
    }
    mMediaCodec =
      MediaCodec.createEncoderByType(MIME_TYPE)
    mMediaCodec!!.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
    mMediaCodec!!.start()
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

  override fun startRecording() {
    super.startRecording()
    // create and execute audio capturing thread using internal mic
    if (mAudioThread == null) {
      mAudioThread = AudioThread()
      mAudioThread!!.start()
    }
  }

  override fun release() {
    mAudioThread = null
    super.release()
  }

  /**
   * Thread to capture audio data from internal mic as uncompressed 16bit PCM data
   * and write them to the MediaCodec encoder
   */
  private inner class AudioThread : Thread() {
    override fun run() {
      Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
      try {
        val minBufferSize = AudioRecord.getMinBufferSize(
          SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
          AudioFormat.ENCODING_PCM_16BIT
        )
        var bufferSize =
          SAMPLES_PER_FRAME * FRAMES_PER_BUFFER
        if (bufferSize < minBufferSize) {
          bufferSize =
            (minBufferSize / SAMPLES_PER_FRAME + 1) * SAMPLES_PER_FRAME * 2
        }
        var audioRecord: AudioRecord? = null
        if (AUDIO_SOURCES != null) {
          for (source in AUDIO_SOURCES) {
            try {
              audioRecord = AudioRecord(
                source,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
              )
              if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord.release()
                audioRecord = null
              }
            } catch (e: Exception) {
              audioRecord = null
            }
            if (audioRecord != null) {
              break
            }
          }
        }
        if (audioRecord != null) {
          try {
            if (mIsCapturing) {
              if (DEBUG) {
                Log.v(
                  TAG,
                  "AudioThread:start audio recording"
                )
              }
              val buf =
                ByteBuffer.allocateDirect(SAMPLES_PER_FRAME)
              var readBytes: Int
              audioRecord.startRecording()
              try {
                while (mIsCapturing && !mRequestStop && !mIsEOS) {

                  // read audio data from internal mic
                  buf.clear()
                  readBytes = audioRecord.read(buf, SAMPLES_PER_FRAME)
                  if (readBytes > 0) {
                    // set audio data to encoder
                    buf.position(readBytes)
                    buf.flip()
                    encode(buf, readBytes, getPTSUs())
                    frameAvailableSoon()
                  }
                }
                frameAvailableSoon()
              } finally {
                audioRecord.stop()
              }
            }
          } finally {
            audioRecord.release()
          }
        } else {
          Log.e(TAG, "failed to initialize AudioRecord")
        }
      } catch (e: Exception) {
        Log.e(TAG, "AudioThread#run", e)
      }
      if (DEBUG) {
        Log.v(TAG, "AudioThread:finished")
      }
    }
  }

  companion object {
    private val TAG: String? = "MediaAudioEncoder"
    private const val DEBUG = true
    private val MIME_TYPE: String? = "audio/mp4a-latm"
    private const val SAMPLE_RATE = 44100
    private const val BIT_RATE = 64000
    const val SAMPLES_PER_FRAME = 1024
    const val FRAMES_PER_BUFFER = 25
    private val AUDIO_SOURCES: IntArray? = intArrayOf(
      AudioSource.CAMCORDER,
      AudioSource.MIC,
      AudioSource.DEFAULT,
      AudioSource.VOICE_COMMUNICATION,
      AudioSource.VOICE_RECOGNITION
    )

    /**
     * select the first codec that match a specific MIME type
     * @param mimeType
     * @return
     */
    private fun selectAudioCodec(mimeType: String?): MediaCodecInfo? {
      if (DEBUG) {
        Log.v(TAG, "selectAudioCodec:")
      }
      var result: MediaCodecInfo? = null
      // get the list of available codecs
      val numCodecs = MediaCodecList.getCodecCount()
      LOOP@ for (i in 0 until numCodecs) {
        val codecInfo =
          MediaCodecList.getCodecInfoAt(i)
        if (!codecInfo.isEncoder) {
          continue
        }
        val types = codecInfo.supportedTypes
        for (type in types) {
          if (DEBUG) {
            Log.i(
              TAG,
              "supportedType:" + codecInfo.name + ",MIME=" + type
            )
          }
          if (type.equals(mimeType, ignoreCase = true)) {
            result = codecInfo
            break@LOOP
          }
        }
      }
      return result
    }
  }
}