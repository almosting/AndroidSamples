package com.sunplus.screenrecorder.media;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;
import java.io.IOException;

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
public abstract class MediaVideoEncoderBase extends MediaEncoder {

  private static final boolean DEBUG = false;
  private static final String TAG = "MediaVideoEncoderBase";
  private static final float BPP = 0.25f;
  protected final int mWidth;
  protected final int mHeight;

  public MediaVideoEncoderBase(final MediaMuxerWrapper muxer, final MediaEncoderCallback listener,
                               final int width, final int height) {
    super(muxer, listener);
    mWidth = width;
    mHeight = height;
  }

  protected MediaFormat createEncoderFormat(final String mime, final int frameRate,
                                            final int bitrate) {
    if (DEBUG) {
      Log.v(TAG,
          String.format("createEncoderFormat:(%d,%d),mime=%s,frameRate=%d,bitrate=%d", mWidth,
              mHeight, mime, frameRate, bitrate));
    }
    final MediaFormat format = MediaFormat.createVideoFormat(mime, mWidth, mHeight);
    format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
        MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);  // API >= 18
    format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate > 0 ? bitrate : calcBitRate(frameRate));
    format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
    format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);
    return format;
  }

  protected Surface prepareSurfaceEncoder(final String mime, final int frameRate,
                                          final int bitrate)
      throws IOException, IllegalArgumentException {

    if (DEBUG) {
      Log.v(TAG,
          String.format("prepareSurfaceEncoder:(%d,%d),mime=%s,frameRate=%d,bitrate=%d", mWidth,
              mHeight, mime, frameRate, bitrate));
    }

    mTrackIndex = -1;
    mMuxerStarted = mIsEOS = false;

    final MediaCodecInfo videoCodecInfo = selectVideoCodec(mime);
    if (videoCodecInfo == null) {
      throw new IllegalArgumentException("Unable to find an appropriate codec for " + mime);
    }
    if (DEBUG) {
      Log.i(TAG, "selected codec: " + videoCodecInfo.getName());
    }

    final MediaFormat format = createEncoderFormat(mime, frameRate, bitrate);
    if (DEBUG) {
      Log.i(TAG, "format: " + format);
    }

    mMediaCodec = MediaCodec.createEncoderByType(mime);
    mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    return mMediaCodec.createInputSurface();
  }

  protected int calcBitRate(final int frameRate) {
    final int bitrate = (int) (BPP * frameRate * mWidth * mHeight);
    Log.i(TAG, String.format("bitrate=%5.2f[Mbps]", bitrate / 1024f / 1024f));
    return bitrate;
  }

  /**
   * select the first codec that match a specific MIME type
   *
   * @return null if no codec matched
   */
  @SuppressWarnings("deprecation")
  protected static final MediaCodecInfo selectVideoCodec(final String mimeType) {
    if (DEBUG) {
      Log.v(TAG, "selectVideoCodec:");
    }

    // get the list of available codecs
    final int numCodecs = MediaCodecList.getCodecCount();
    for (int i = 0; i < numCodecs; i++) {
      final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

      if (!codecInfo.isEncoder()) {
        continue;
      }
      // select first codec that match a specific MIME type and color format
      final String[] types = codecInfo.getSupportedTypes();
      for (String type : types) {
        if (type.equalsIgnoreCase(mimeType)) {
          if (DEBUG) {
            Log.i(TAG, "codec:" + codecInfo.getName() + ",MIME=" + type);
          }
          final int format = selectColorFormat(codecInfo, mimeType);
          if (format > 0) {
            return codecInfo;
          }
        }
      }
    }
    return null;
  }

  protected static final int selectColorFormat(final MediaCodecInfo codecInfo,
                                               final String mimeType) {
    if (DEBUG) {
      Log.i(TAG, "selectColorFormat: ");
    }
    int result = 0;
    final MediaCodecInfo.CodecCapabilities caps;
    try {
      Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
      caps = codecInfo.getCapabilitiesForType(mimeType);
    } finally {
      Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
    }
    int colorFormat;
    for (int i = 0; i < caps.colorFormats.length; i++) {
      colorFormat = caps.colorFormats[i];
      if (isRecognizedViewoFormat(colorFormat)) {
        result = colorFormat;
        break;
      }
    }
    if (result == 0) {
      Log.e(TAG, "couldn't find a good color format for " + codecInfo.getName() + " / " + mimeType);
    }
    return result;
  }

  /**
   * color formats that we can use in this class
   */
  protected static int[] recognizedFormats;

  static {
    recognizedFormats = new int[] {
        //        	MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
        //        	MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
        //        	MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar,
        MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface,
    };
  }

  protected static final boolean isRecognizedViewoFormat(final int colorFormat) {
    if (DEBUG) {
      Log.i(TAG, "isRecognizedViewoFormat:colorFormat=" + colorFormat);
    }
    final int n = recognizedFormats != null ? recognizedFormats.length : 0;
    for (int i = 0; i < n; i++) {
      if (recognizedFormats[i] == colorFormat) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected void signalEndOfInputStream() {
    if (DEBUG) {
      Log.d(TAG, "sending EOS to encoder");
    }
    mMediaCodec.signalEndOfInputStream();  // API >= 18
    mIsEOS = true;
  }
}
