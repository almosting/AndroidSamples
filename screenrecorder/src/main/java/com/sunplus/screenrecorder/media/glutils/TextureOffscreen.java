package com.sunplus.screenrecorder.media.glutils;

/*
 * Copyright (c) 2015 saki t_saki@serenegiant.com
 *
 * File name: TextureOffscreen.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

/**
 * Offscreen class with backing texture using FBO to draw using OpenGL|ES into the texture
 */
public class TextureOffscreen {
  private static final boolean DEBUG = false;
  private static final String TAG = "TextureOffscreen";

  private static final boolean DEFAULT_ADJUST_POWER2 = false;

  private final int TEX_TARGET;
  private final int TEX_UNIT;
  private final boolean mHasDepthBuffer, mAdjustPower2;
  /** 绘图区域大小 */
  private int mWidth, mHeight;
  /** 纹理尺寸 */
  private int mTexWidth, mTexHeight;
  /** 用于屏幕外颜色缓冲区的纹理名称 */
  private int mFBOTextureName = -1;
  /** //屏幕外的缓冲区对象 */
  private int mDepthBufferObj = -1, mFrameBufferObj = -1;
  /** 纹理坐标变换矩阵 */
  private final float[] mTexMatrix = new float[16];

  /**
   * 构造函数（GL_TEXTURE_ 2 D），无深度缓冲区
   *     * 纹理单位是GL_TEXTURE 0
   */
  public TextureOffscreen(final int width, final int height) {
    this(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE0, -1,
        width, height, false, DEFAULT_ADJUST_POWER2);
  }

  /**
   * 构造函数（GL_TEXTURE_ 2 D），无深度缓冲区
   * 纹理单位是GL_TEXTURE 0
   */
  public TextureOffscreen(final int tex_unit,
                          final int width, final int height) {

    this(GLES20.GL_TEXTURE_2D, tex_unit, -1,
        width, height,
        false, DEFAULT_ADJUST_POWER2);
  }

  /**
   * 构造函数（GL_TEXTURE_2D）
   * 纹理单位是GL_TEXTURE 0
   *
   * @param width dimension of offscreen(width)
   * @param height dimension of offscreen(height)
   * @param use_depth_buffer set true if you use depth buffer. the depth is fixed as 16bits
   */
  public TextureOffscreen(final int width, final int height,
                          final boolean use_depth_buffer) {

    this(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE0, -1,
        width, height, use_depth_buffer, DEFAULT_ADJUST_POWER2);
  }

  /**
   * 包装现有纹理的构造函数（GL_TEXTURE_2D）
   * 纹理单位是GL_TEXTURE 0
   */
  public TextureOffscreen(final int tex_unit,
                          final int width, final int height, final boolean use_depth_buffer) {

    this(GLES20.GL_TEXTURE_2D, tex_unit, -1,
        width, height,
        use_depth_buffer, DEFAULT_ADJUST_POWER2);
  }

  /**
   * 构造函数（GL_TEXTURE_ 2 D)
   * 纹理单位是GL_TEXTURE 0
   */
  public TextureOffscreen(final int width, final int height,
                          final boolean use_depth_buffer, final boolean adjust_power2) {

    this(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE0, -1,
        width, height, use_depth_buffer, adjust_power2);
  }

  /**
   * 构造函数（GL_TEXTURE_2D）
   */
  public TextureOffscreen(final int tex_unit,
                          final int width, final int height,
                          final boolean use_depth_buffer, final boolean adjust_power2) {

    this(GLES20.GL_TEXTURE_2D, tex_unit, -1,
        width, height, use_depth_buffer, adjust_power2);
  }

  /**
   * 用于包装现有纹理的构造函数（GL_TEXTURE _ 2 D），没有深度缓冲区
   */
  public TextureOffscreen(final int tex_unit, final int tex_id,
                          final int width, final int height) {

    this(GLES20.GL_TEXTURE_2D, tex_unit, tex_id,
        width, height,
        false, DEFAULT_ADJUST_POWER2);
  }

  /**
   * 用于包装现有纹理的构造函数（GL_TEXTURE_2D）
   */
  public TextureOffscreen(final int tex_unit, final int tex_id,
                          final int width, final int height, final boolean use_depth_buffer) {

    this(GLES20.GL_TEXTURE_2D, tex_unit, tex_id,
        width, height,
        use_depth_buffer, DEFAULT_ADJUST_POWER2);
  }

  /**
   * 用于包装现有纹理的构造函数
   *
   * @param tex_target GL_TEXTURE_2D
   */
  public TextureOffscreen(final int tex_target, final int tex_unit, final int tex_id,
                          final int width, final int height,
                          final boolean use_depth_buffer, final boolean adjust_power2) {

    if (DEBUG) {
      Log.v(TAG, "Constructor");
    }
    TEX_TARGET = tex_target;
    TEX_UNIT = tex_unit;
    mWidth = width;
    mHeight = height;
    mHasDepthBuffer = use_depth_buffer;
    mAdjustPower2 = adjust_power2;

    createFrameBuffer(width, height);
    int tex = tex_id;
    if (tex < 0) {
      tex = genTexture(tex_target, tex_unit, mTexWidth, mTexHeight);
    }
    assignTexture(tex, width, height);
  }

  /** 丢弃 */
  public void release() {
    if (DEBUG) {
      Log.v(TAG, "release");
    }
    releaseFrameBuffer();
  }

  /**
   * 切换到渲染缓冲区以进行屏幕外渲染
   * Viewport也将被更改，因此如有必要，请在解除绑定后设置Viewport
   */
  public void bind() {
    //		if (DEBUG) Log.v(TAG, "bind:");
    GLES20.glActiveTexture(TEX_UNIT);
    GLES20.glBindTexture(TEX_TARGET, mFBOTextureName);
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferObj);
    GLES20.glViewport(0, 0, mWidth, mHeight);
  }

  /**
   * 返回默认渲染缓冲区
   */
  public void unbind() {
    //		if (DEBUG) Log.v(TAG, "unbind:");
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    GLES20.glActiveTexture(TEX_UNIT);
    GLES20.glBindTexture(TEX_TARGET, 0);
  }

  private final float[] mResultMatrix = new float[16];

  /**
   * get copy of texture matrix
   */
  public float[] getTexMatrix() {
    System.arraycopy(mTexMatrix, 0, mResultMatrix, 0, 16);
    return mResultMatrix;
  }

  /**
   * 获取纹理坐标变换矩阵（因为内部数组直接返回，更改时要小心）
   */
  public float[] getRawTexMatrix() {
    return mTexMatrix;
  }

  /**
   * 返回纹理变换矩阵的副本
   * 由于我们没有检查过该区域，因此我们必须从偏移位置预留16个或更多
   */
  public void getTexMatrix(final float[] matrix, final int offset) {
    System.arraycopy(mTexMatrix, 0, matrix, offset, mTexMatrix.length);
  }

  /**
   * 获取屏幕纹理名称
   * 可以在使用此屏幕上写入的图像作为纹理绘制其他图像时使用
   */
  public int getTexture() {
    return mFBOTextureName;
  }

  /** 将指定的纹理指定给此屏幕 */
  public void assignTexture(final int texture_name,
                            final int width, final int height) {

    if ((width > mTexWidth) || (height > mTexHeight)) {
      mWidth = width;
      mHeight = height;
      releaseFrameBuffer();
      createFrameBuffer(width, height);
    }
    mFBOTextureName = texture_name;
    GLES20.glActiveTexture(TEX_UNIT);
    // 绑定帧缓冲区对象
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferObj);
    GLHelper.checkGlError("glBindFramebuffer " + mFrameBufferObj);
    // 将颜色缓冲区（纹理）连接到帧缓冲区
    GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
        TEX_TARGET, mFBOTextureName, 0);
    GLHelper.checkGlError("glFramebufferTexture2D");

    if (mHasDepthBuffer) {
      // 将深度缓冲区连接到帧缓冲区
      GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER,
          GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, mDepthBufferObj);
      GLHelper.checkGlError("glFramebufferRenderbuffer");
    }

    // 确认是否正常完成
    final int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
    if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
      throw new RuntimeException("Framebuffer not complete, status=" + status);
    }

    // 返回默认帧缓冲区
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

    // 初始化纹理坐标变换矩阵
    Matrix.setIdentityM(mTexMatrix, 0);
    mTexMatrix[0] = width / (float) mTexWidth;
    mTexMatrix[5] = height / (float) mTexHeight;
  }

  /** 从Bitmap读取纹理 */
  public void loadBitmap(final Bitmap bitmap) {
    final int width = bitmap.getWidth();
    final int height = bitmap.getHeight();
    if ((width > mTexWidth) || (height > mTexHeight)) {
      mWidth = width;
      mHeight = height;
      releaseFrameBuffer();
      createFrameBuffer(width, height);
    }
    GLES20.glActiveTexture(TEX_UNIT);
    GLES20.glBindTexture(TEX_TARGET, mFBOTextureName);
    GLUtils.texImage2D(TEX_TARGET, 0, bitmap, 0);
    GLES20.glBindTexture(TEX_TARGET, 0);
    // initialize texture matrix
    Matrix.setIdentityM(mTexMatrix, 0);
    mTexMatrix[0] = width / (float) mTexWidth;
    mTexMatrix[5] = height / (float) mTexHeight;
  }

  /**
   * 生成颜色缓冲区的纹理
   */
  private static int genTexture(final int tex_target, final int tex_unit,
                                final int tex_width, final int tex_height) {
    // 生成颜色缓冲区的纹理
    final int tex_name = GLHelper.initTex(tex_target, tex_unit,
        GLES20.GL_LINEAR, GLES20.GL_LINEAR, GLES20.GL_CLAMP_TO_EDGE);
    // 安全的纹理内存区域
    GLES20.glTexImage2D(tex_target, 0, GLES20.GL_RGBA, tex_width, tex_height, 0,
        GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
    GLHelper.checkGlError("glTexImage2D");
    return tex_name;
  }

  /** 为屏幕外渲染生成帧缓冲区对象 */
  private final void createFrameBuffer(final int width, final int height) {
    final int[] ids = new int[1];

    if (mAdjustPower2) {
      // 使纹理的大小为2的乘数
      int w = 1;
      for (; w < width; w <<= 1) {
        ;
      }
      int h = 1;
      for (; h < height; h <<= 1) {
        ;
      }
      if (mTexWidth != w || mTexHeight != h) {
        mTexWidth = w;
        mTexHeight = h;
      }
    } else {
      mTexWidth = width;
      mTexHeight = height;
    }

    if (mHasDepthBuffer) {
      // 如果需要深度缓冲区，请创建并初始化渲染缓冲区对象
      GLES20.glGenRenderbuffers(1, ids, 0);
      mDepthBufferObj = ids[0];
      GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, mDepthBufferObj);
      // 深度缓冲区是16位
      GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER,
          GLES20.GL_DEPTH_COMPONENT16, mTexWidth, mTexHeight);
    }
    // 创建一个帧缓冲区对象并绑定
    GLES20.glGenFramebuffers(1, ids, 0);
    GLHelper.checkGlError("glGenFramebuffers");
    mFrameBufferObj = ids[0];
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferObj);
    GLHelper.checkGlError("glBindFramebuffer " + mFrameBufferObj);

    // 返回默认帧缓冲区
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
  }

  /** 丢弃屏幕外帧缓冲区 */
  private final void releaseFrameBuffer() {
    final int[] names = new int[1];
    // 有深度缓冲区时丢弃深度缓冲区
    if (mDepthBufferObj >= 0) {
      names[0] = mDepthBufferObj;
      GLES20.glDeleteRenderbuffers(1, names, 0);
      mDepthBufferObj = -1;
    }
    // 丢弃屏幕外颜色缓冲区的纹理
    if (mFBOTextureName >= 0) {
      names[0] = mFBOTextureName;
      GLES20.glDeleteTextures(1, names, 0);
      mFBOTextureName = -1;
    }
    // 丢弃屏幕外帧缓冲对象
    if (mFrameBufferObj >= 0) {
      names[0] = mFrameBufferObj;
      GLES20.glDeleteFramebuffers(1, names, 0);
      mFrameBufferObj = -1;
    }
  }

  /**
   * get dimension(width) of this offscreen
   */
  public int getWidth() {
    return mWidth;
  }

  /**
   * get dimension(height) of this offscreen
   */
  public int getHeight() {
    return mHeight;
  }

  /**
   * get backing texture dimension(width) of this offscreen
   */
  public int getTexWidth() {
    return mTexWidth;
  }

  /**
   * get backing texture dimension(height) of this offscreen
   */
  public int getTexHeight() {
    return mTexHeight;
  }

  public int getTexTarget() {
    return TEX_TARGET;
  }

  public int getTexUnit() {
    return TEX_UNIT;
  }
}