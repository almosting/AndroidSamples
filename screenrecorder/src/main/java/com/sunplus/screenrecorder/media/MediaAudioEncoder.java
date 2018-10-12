package com.sunplus.screenrecorder.media;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
public class MediaAudioEncoder extends MediaEncoder {

  private static final String TAG = "MediaAudioEncoder";
  private static final boolean DEBUG = true;
  private static final String MIME_TYPE = "audio/mp4a-latm";
  private static final int SAMPLE_RATE = 44100;
  private static final int BIT_RATE = 64000;
  public static final int SAMPLES_PER_FRAME = 1024;
  public static final int FRAMES_PER_BUFFER = 25;

  private AudioThread mAudioThread = null;

  public MediaAudioEncoder(MediaMuxerWrapper muxer, MediaEncoderCallback callback) {
    super(muxer, callback);
  }

  @Override
  protected void prepare() throws IOException {
    if (DEBUG) {
      Log.v(TAG, "prepare:");
    }
    mTrackIndex = -1;
    mMuxerStarted = mIsEOS = false;
    // prepare MediaCodec for AAC encoding of audio data from inernal mic.
    final MediaCodecInfo audioCodecInfo = selectAudioCodec(MIME_TYPE);
    if (audioCodecInfo == null) {
      Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
      return;
    }
    if (DEBUG) {
      Log.i(TAG, "selected codec: " + audioCodecInfo.getName());
    }

    final MediaFormat audioFormat = MediaFormat.createAudioFormat(MIME_TYPE, SAMPLE_RATE, 1);
    audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
    audioFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_MONO);
    audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
    audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
    //		audioFormat.setLong(MediaFormat.KEY_MAX_INPUT_SIZE, inputFile.length());
    //      audioFormat.setLong(MediaFormat.KEY_DURATION, (long)durationInMs );
    if (DEBUG) {
      Log.i(TAG, "format: " + audioFormat);
    }
    mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
    mMediaCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    mMediaCodec.start();
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
  protected void startRecording() {
    super.startRecording();
    // create and execute audio capturing thread using internal mic
    if (mAudioThread == null) {
      mAudioThread = new AudioThread();
      mAudioThread.start();
    }
  }

  @Override
  protected void release() {
    mAudioThread = null;
    super.release();
  }

  private static final int[] AUDIO_SOURCES = new int[] {
      MediaRecorder.AudioSource.CAMCORDER,
      MediaRecorder.AudioSource.MIC,
      MediaRecorder.AudioSource.DEFAULT,
      MediaRecorder.AudioSource.VOICE_COMMUNICATION,
      MediaRecorder.AudioSource.VOICE_RECOGNITION,
  };

  /**
   * Thread to capture audio data from internal mic as uncompressed 16bit PCM data
   * and write them to the MediaCodec encoder
   */
  private class AudioThread extends Thread {
    @Override
    public void run() {
      android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
      try {
        final int minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT);
        int bufferSize = SAMPLES_PER_FRAME * FRAMES_PER_BUFFER;
        if (bufferSize < minBufferSize) {
          bufferSize = ((minBufferSize / SAMPLES_PER_FRAME) + 1) * SAMPLES_PER_FRAME * 2;
        }

        AudioRecord audioRecord = null;
        for (final int source : AUDIO_SOURCES) {
          try {
            audioRecord = new AudioRecord(
                source, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
              audioRecord.release();
              audioRecord = null;
            }
          } catch (final Exception e) {
            audioRecord = null;
          }
          if (audioRecord != null) {
            break;
          }
        }
        if (audioRecord != null) {
          try {
            if (mIsCapturing) {
              if (DEBUG) {
                Log.v(TAG, "AudioThread:start audio recording");
              }
              final ByteBuffer buf = ByteBuffer.allocateDirect(SAMPLES_PER_FRAME);
              int readBytes;
              audioRecord.startRecording();
              try {
                for (; mIsCapturing && !mRequestStop && !mIsEOS ;) {
                  // read audio data from internal mic
                  buf.clear();
                  readBytes = audioRecord.read(buf, SAMPLES_PER_FRAME);
                  if (readBytes > 0) {
                    // set audio data to encoder
                    buf.position(readBytes);
                    buf.flip();
                    encode(buf, readBytes, getPTSUs());
                    frameAvailableSoon();
                  }
                }
                frameAvailableSoon();
              } finally {
                audioRecord.stop();
              }
            }
          } finally {
            audioRecord.release();
          }
        } else {
          Log.e(TAG, "failed to initialize AudioRecord");
        }
      } catch (final Exception e) {
        Log.e(TAG, "AudioThread#run", e);
      }
      if (DEBUG) {
        Log.v(TAG, "AudioThread:finished");
      }
    }
  }

  /**
   * select the first codec that match a specific MIME type
   * @param mimeType
   * @return
   */
  private static MediaCodecInfo selectAudioCodec(final String mimeType) {
    if (DEBUG) {
      Log.v(TAG, "selectAudioCodec:");
    }

    MediaCodecInfo result = null;
    // get the list of available codecs
    final int numCodecs = MediaCodecList.getCodecCount();
    LOOP:	for (int i = 0; i < numCodecs; i++) {
      final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
      if (!codecInfo.isEncoder()) {
        continue;
      }
      final String[] types = codecInfo.getSupportedTypes();
      for (String type : types) {
        if (DEBUG) {
          Log.i(TAG, "supportedType:" + codecInfo.getName() + ",MIME=" + type);
        }
        if (type.equalsIgnoreCase(mimeType)) {
          result = codecInfo;
          break LOOP;
        }
      }
    }
    return result;
  }
}
