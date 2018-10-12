package com.sunplus.screenrecorder.media;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import com.sunplus.toolbox.utils.FileUtils;
import java.io.IOException;
import java.nio.ByteBuffer;

import static android.content.ContentValues.TAG;

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
public class MediaMuxerWrapper {

  private String mOutputPath;
  private final MediaMuxer mMediaMuxer;
  private int mEncoderCount;
  private int mStartedCount;
  private boolean mIsStarted;
  private static final boolean DEBUG = false;
  private volatile boolean mIsPaused;
  private MediaEncoder mVideoEncoder, mAudioEncoder;

  public MediaMuxerWrapper(final Context context, final String ext) throws IOException {
    String extName = ext;
    if (TextUtils.isEmpty(extName)) {
      extName = ".mp4";
    }
    try {
      mOutputPath =
          FileUtils.getCaptureFile(context, Environment.DIRECTORY_MOVIES, ext, 0).toString();
    } catch (final NullPointerException e) {
      throw new RuntimeException("This app has no permission of writing external storage");
    }

    mMediaMuxer = new MediaMuxer(mOutputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
    mEncoderCount = mStartedCount = 0;
    mIsStarted = false;
  }

  public String getOutputPath() {
    return mOutputPath;
  }

  public synchronized void prepare() throws IOException {
    Log.i(TAG, "prepare: ");
    if (mVideoEncoder != null) {
      mVideoEncoder.prepare();
    }
    if (mAudioEncoder != null) {
      mAudioEncoder.prepare();
    }
  }

  public synchronized void startRecording() {
    if (mVideoEncoder != null) {
      mVideoEncoder.startRecording();
    }
    if (mAudioEncoder != null) {
      mAudioEncoder.startRecording();
    }
  }

  public synchronized void stopRecording() {
    if (mVideoEncoder != null) {
      mVideoEncoder.stopRecording();
    }
    mVideoEncoder = null;
    if (mAudioEncoder != null) {
      mAudioEncoder.stopRecording();
    }
    mAudioEncoder = null;
  }

  public synchronized boolean isStarted() {
    return mIsStarted;
  }

  public synchronized void pauseRecording() {
    mIsPaused = true;
    if (mVideoEncoder != null) {
      mVideoEncoder.pauseRecording();
    }
    if (mAudioEncoder != null) {
      mAudioEncoder.pauseRecording();
    }
  }

  public synchronized void resumeRecording() {
    if (mVideoEncoder != null) {
      mVideoEncoder.resumeRecording();
    }
    if (mAudioEncoder != null) {
      mAudioEncoder.resumeRecording();
    }
    mIsPaused = false;
  }

  public synchronized boolean isPaused() {
    return mIsPaused;
  }

  void addEncoder(final MediaEncoder encoder) {
    if (encoder instanceof MediaVideoEncoderBase) {
      if (mVideoEncoder != null) {
        throw new IllegalArgumentException("Video encoder already added.");
      }
      mVideoEncoder = encoder;
    } else if (encoder instanceof MediaAudioEncoder) {
      if (mAudioEncoder != null) {
        throw new IllegalArgumentException("Video encoder already added.");
      }
      mAudioEncoder = encoder;
    } else {
      throw new IllegalArgumentException("unsupported encoder");
    }
    mEncoderCount = (mVideoEncoder != null ? 1 : 0) + (mAudioEncoder != null ? 1 : 0);
  }

  synchronized boolean start() {
    if (DEBUG) {
      Log.v(TAG, "start:");
    }
    mStartedCount++;
    if ((mEncoderCount > 0) && (mStartedCount == mEncoderCount)) {
      mMediaMuxer.start();
      mIsStarted = true;
      notifyAll();
      if (DEBUG) {
        Log.v(TAG, "MediaMuxer started:");
      }
    }
    return mIsStarted;
  }

  synchronized void stop() {
    if (DEBUG) {
      Log.v(TAG, "stop:mStatredCount=" + mStartedCount);
    }
    mStartedCount--;
    if ((mEncoderCount > 0) && (mStartedCount <= 0)) {
      mMediaMuxer.stop();
      mMediaMuxer.release();
      mIsStarted = false;
      if (DEBUG) {
        Log.v(TAG, "MediaMuxer stopped:");
      }
    }
  }

  synchronized int addTrack(final MediaFormat format) {
    if (mIsStarted) {
      throw new IllegalStateException("muxer already started");
    }
    final int trackIx = mMediaMuxer.addTrack(format);
    if (DEBUG) {
      Log.i(TAG,
          "addTrack:trackNum=" + mEncoderCount + ",trackIx=" + trackIx + ",format=" + format);
    }
    return trackIx;
  }

  synchronized void writeSampleData(final int trackIndex, final ByteBuffer byteBuf,
                                    final MediaCodec.BufferInfo bufferInfo) {
    if (mStartedCount > 0) {
      mMediaMuxer.writeSampleData(trackIndex, byteBuf, bufferInfo);
    }
  }
}
