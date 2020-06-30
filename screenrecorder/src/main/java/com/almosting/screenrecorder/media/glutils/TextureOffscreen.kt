package com.almosting.screenrecorder.media.glutils

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import android.util.Log

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
 */ /**
 * Offscreen class with backing texture using FBO to draw using OpenGL|ES into the texture
 */
class TextureOffscreen(
  texTarget: Int, texUnit: Int, texId: Int,
  width: Int, height: Int,
  useDepthBuffer: Boolean, adjust_power2: Boolean
) {
  private val TEX_TARGET: Int
  private val TEX_UNIT: Int
  private val mHasDepthBuffer: Boolean
  private val mAdjustPower2: Boolean

  /** 绘图区域大小  */
  private var mWidth: Int
  private var mHeight: Int

  /** 纹理尺寸  */
  private var mTexWidth = 0
  private var mTexHeight = 0

  /** 用于屏幕外颜色缓冲区的纹理名称  */
  private var mFBOTextureName = -1

  /** //屏幕外的缓冲区对象  */
  private var mDepthBufferObj = -1
  private var mFrameBufferObj = -1

  /** 纹理坐标变换矩阵  */
  private val mTexMatrix: FloatArray? = FloatArray(16)

  /**
   * 构造函数（GL_TEXTURE_ 2 D），无深度缓冲区
   *     * 纹理单位是GL_TEXTURE 0
   */
  constructor(width: Int, height: Int) : this(
    GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE0, -1,
    width, height, false, DEFAULT_ADJUST_POWER2
  ) {
  }

  /**
   * 构造函数（GL_TEXTURE_ 2 D），无深度缓冲区
   * 纹理单位是GL_TEXTURE 0
   */
  constructor(
    texUnit: Int,
    width: Int, height: Int
  ) : this(
    GLES20.GL_TEXTURE_2D, texUnit, -1,
    width, height,
    false, DEFAULT_ADJUST_POWER2
  ) {
  }

  /**
   * 构造函数（GL_TEXTURE_2D）
   * 纹理单位是GL_TEXTURE 0
   *
   * @param width dimension of offscreen(width)
   * @param height dimension of offscreen(height)
   * @param useDepthBuffer set true if you use depth buffer. the depth is fixed as 16bits
   */
  constructor(
    width: Int, height: Int,
    useDepthBuffer: Boolean
  ) : this(
    GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE0, -1,
    width, height, useDepthBuffer, DEFAULT_ADJUST_POWER2
  ) {
  }

  /**
   * 包装现有纹理的构造函数（GL_TEXTURE_2D）
   * 纹理单位是GL_TEXTURE 0
   */
  constructor(
    texUnit: Int,
    width: Int, height: Int, useDepthBuffer: Boolean
  ) : this(
    GLES20.GL_TEXTURE_2D, texUnit, -1,
    width, height,
    useDepthBuffer, DEFAULT_ADJUST_POWER2
  ) {
  }

  /**
   * 构造函数（GL_TEXTURE_ 2 D)
   * 纹理单位是GL_TEXTURE 0
   */
  constructor(
    width: Int, height: Int,
    useDepthBuffer: Boolean, adjustPower2: Boolean
  ) : this(
    GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE0, -1,
    width, height, useDepthBuffer, adjustPower2
  ) {
  }

  /**
   * 构造函数（GL_TEXTURE_2D）
   */
  constructor(
    texUnit: Int,
    width: Int, height: Int,
    useDepthBuffer: Boolean, adjustPower2: Boolean
  ) : this(
    GLES20.GL_TEXTURE_2D, texUnit, -1,
    width, height, useDepthBuffer, adjustPower2
  ) {
  }

  /**
   * 用于包装现有纹理的构造函数（GL_TEXTURE _ 2 D），没有深度缓冲区
   */
  constructor(
    texUnit: Int, texId: Int,
    width: Int, height: Int
  ) : this(
    GLES20.GL_TEXTURE_2D, texUnit, texId,
    width, height,
    false, DEFAULT_ADJUST_POWER2
  ) {
  }

  /**
   * 用于包装现有纹理的构造函数（GL_TEXTURE_2D）
   */
  constructor(
    texUnit: Int, texId: Int,
    width: Int, height: Int, useDepthBuffer: Boolean
  ) : this(
    GLES20.GL_TEXTURE_2D, texUnit, texId,
    width, height,
    useDepthBuffer, DEFAULT_ADJUST_POWER2
  ) {
  }

  /** 丢弃  */
  fun release() {
    if (DEBUG) {
      Log.v(TAG, "release")
    }
    releaseFrameBuffer()
  }

  /**
   * 切换到渲染缓冲区以进行屏幕外渲染
   * Viewport也将被更改，因此如有必要，请在解除绑定后设置Viewport
   */
  fun bind() {
    GLES20.glActiveTexture(TEX_UNIT)
    GLES20.glBindTexture(TEX_TARGET, mFBOTextureName)
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferObj)
    GLES20.glViewport(0, 0, mWidth, mHeight)
  }

  /**
   * 返回默认渲染缓冲区
   */
  fun unbind() {
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    GLES20.glActiveTexture(TEX_UNIT)
    GLES20.glBindTexture(TEX_TARGET, 0)
  }

  private val mResultMatrix: FloatArray? = FloatArray(16)

  /**
   * get copy of texture matrix
   */
  fun getTexMatrix(): FloatArray? {
    System.arraycopy(mTexMatrix!!, 0, mResultMatrix, 0, 16)
    return mResultMatrix
  }

  /**
   * 获取纹理坐标变换矩阵（因为内部数组直接返回，更改时要小心）
   */
  fun getRawTexMatrix(): FloatArray? {
    return mTexMatrix
  }

  /**
   * 返回纹理变换矩阵的副本
   * 由于我们没有检查过该区域，因此我们必须从偏移位置预留16个或更多
   */
  fun getTexMatrix(matrix: FloatArray?, offset: Int) {
    System.arraycopy(mTexMatrix!!, 0, matrix, offset, mTexMatrix!!.size)
  }

  /**
   * 获取屏幕纹理名称
   * 可以在使用此屏幕上写入的图像作为纹理绘制其他图像时使用
   */
  fun getTexture(): Int {
    return mFBOTextureName
  }

  /** 将指定的纹理指定给此屏幕  */
  fun assignTexture(
    textureName: Int,
    width: Int, height: Int
  ) {
    if (width > mTexWidth || height > mTexHeight) {
      mWidth = width
      mHeight = height
      releaseFrameBuffer()
      createFrameBuffer(width, height)
    }
    mFBOTextureName = textureName
    GLES20.glActiveTexture(TEX_UNIT)
    // 绑定帧缓冲区对象
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferObj)
    GLHelper.checkGlError("glBindFramebuffer $mFrameBufferObj")
    // 将颜色缓冲区（纹理）连接到帧缓冲区
    GLES20.glFramebufferTexture2D(
      GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
      TEX_TARGET, mFBOTextureName, 0
    )
    GLHelper.checkGlError("glFramebufferTexture2D")
    if (mHasDepthBuffer) {
      // 将深度缓冲区连接到帧缓冲区
      GLES20.glFramebufferRenderbuffer(
        GLES20.GL_FRAMEBUFFER,
        GLES20.GL_DEPTH_ATTACHMENT,
        GLES20.GL_RENDERBUFFER,
        mDepthBufferObj
      )
      GLHelper.checkGlError("glFramebufferRenderbuffer")
    }

    // 确认是否正常完成
    val status =
      GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
    if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
      throw RuntimeException("Framebuffer not complete, status=$status")
    }

    // 返回默认帧缓冲区
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)

    // 初始化纹理坐标变换矩阵
    Matrix.setIdentityM(mTexMatrix, 0)
    mTexMatrix?.set(0, width / mTexWidth as Float)
    mTexMatrix?.set(5, height / mTexHeight as Float)
  }

  /** 从Bitmap读取纹理  */
  fun loadBitmap(bitmap: Bitmap?) {
    val width = bitmap!!.getWidth()
    val height = bitmap.getHeight()
    if (width > mTexWidth || height > mTexHeight) {
      mWidth = width
      mHeight = height
      releaseFrameBuffer()
      createFrameBuffer(width, height)
    }
    GLES20.glActiveTexture(TEX_UNIT)
    GLES20.glBindTexture(TEX_TARGET, mFBOTextureName)
    GLUtils.texImage2D(TEX_TARGET, 0, bitmap, 0)
    GLES20.glBindTexture(TEX_TARGET, 0)
    // initialize texture matrix
    Matrix.setIdentityM(mTexMatrix, 0)
    mTexMatrix?.set(0, width / mTexWidth as Float)
    mTexMatrix?.set(5, height / mTexHeight as Float)
  }

  /** 为屏幕外渲染生成帧缓冲区对象  */
  private fun createFrameBuffer(width: Int, height: Int) {
    val ids = IntArray(1)
    if (mAdjustPower2) {
      // 使纹理的大小为2的乘数
      var w = 1
      while (w < width) {
        w = w shl 1
      }
      var h = 1
      while (h < height) {
        h = h shl 1
      }
      if (mTexWidth != w || mTexHeight != h) {
        mTexWidth = w
        mTexHeight = h
      }
    } else {
      mTexWidth = width
      mTexHeight = height
    }
    if (mHasDepthBuffer) {
      // 如果需要深度缓冲区，请创建并初始化渲染缓冲区对象
      GLES20.glGenRenderbuffers(1, ids, 0)
      mDepthBufferObj = ids[0]
      GLES20.glBindRenderbuffer(
        GLES20.GL_RENDERBUFFER,
        mDepthBufferObj
      )
      // 深度缓冲区是16位
      GLES20.glRenderbufferStorage(
        GLES20.GL_RENDERBUFFER,
        GLES20.GL_DEPTH_COMPONENT16, mTexWidth, mTexHeight
      )
    }
    // 创建一个帧缓冲区对象并绑定
    GLES20.glGenFramebuffers(1, ids, 0)
    GLHelper.checkGlError("glGenFramebuffers")
    mFrameBufferObj = ids[0]
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferObj)
    GLHelper.checkGlError("glBindFramebuffer $mFrameBufferObj")

    // 返回默认帧缓冲区
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
  }

  /** 丢弃屏幕外帧缓冲区  */
  private fun releaseFrameBuffer() {
    val names = IntArray(1)
    // 有深度缓冲区时丢弃深度缓冲区
    if (mDepthBufferObj >= 0) {
      names[0] = mDepthBufferObj
      GLES20.glDeleteRenderbuffers(1, names, 0)
      mDepthBufferObj = -1
    }
    // 丢弃屏幕外颜色缓冲区的纹理
    if (mFBOTextureName >= 0) {
      names[0] = mFBOTextureName
      GLES20.glDeleteTextures(1, names, 0)
      mFBOTextureName = -1
    }
    // 丢弃屏幕外帧缓冲对象
    if (mFrameBufferObj >= 0) {
      names[0] = mFrameBufferObj
      GLES20.glDeleteFramebuffers(1, names, 0)
      mFrameBufferObj = -1
    }
  }

  /**
   * get dimension(width) of this offscreen
   */
  fun getWidth(): Int {
    return mWidth
  }

  /**
   * get dimension(height) of this offscreen
   */
  fun getHeight(): Int {
    return mHeight
  }

  /**
   * get backing texture dimension(width) of this offscreen
   */
  fun getTexWidth(): Int {
    return mTexWidth
  }

  /**
   * get backing texture dimension(height) of this offscreen
   */
  fun getTexHeight(): Int {
    return mTexHeight
  }

  fun getTexTarget(): Int {
    return TEX_TARGET
  }

  fun getTexUnit(): Int {
    return TEX_UNIT
  }

  companion object {
    private const val DEBUG = false
    private val TAG: String? = "TextureOffscreen"
    private const val DEFAULT_ADJUST_POWER2 = false

    /**
     * 生成颜色缓冲区的纹理
     */
    private fun genTexture(
      texTarget: Int, texUnit: Int,
      texWidth: Int, texHeight: Int
    ): Int {
      // 生成颜色缓冲区的纹理
      val texName = GLHelper.initTex(
        texTarget,
        texUnit,
        GLES20.GL_LINEAR,
        GLES20.GL_LINEAR,
        GLES20.GL_CLAMP_TO_EDGE
      )
      // 安全的纹理内存区域
      GLES20.glTexImage2D(
        texTarget, 0, GLES20.GL_RGBA, texWidth, texHeight, 0,
        GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null
      )
      GLHelper.checkGlError("glTexImage2D")
      return texName
    }
  }

  /**
   * 用于包装现有纹理的构造函数
   *
   * @param texTarget GL_TEXTURE_2D
   */
  init {
    if (DEBUG) {
      Log.v(TAG, "Constructor")
    }
    TEX_TARGET = texTarget
    TEX_UNIT = texUnit
    mWidth = width
    mHeight = height
    mHasDepthBuffer = useDepthBuffer
    mAdjustPower2 = adjust_power2
    createFrameBuffer(width, height)
    var tex = texId
    if (tex < 0) {
      tex = genTexture(texTarget, texUnit, mTexWidth, mTexHeight)
    }
    assignTexture(tex, width, height)
  }
}