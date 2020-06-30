package com.almosting.screenrecorder.media.glutils

import com.almosting.screenrecorder.media.glutils.EGLBase.IConfig
import com.almosting.screenrecorder.media.glutils.EGLBase.IContext
import com.almosting.screenrecorder.media.glutils.EGLBase.IEglSurface
import com.almosting.toolbox.utils.MessageTask

/*
 * Copyright (c) 2015 saki t_saki@serenegiant.com
 *
 * File name: EglTask.java
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
 */ abstract class EglTask : MessageTask {
  private var mEgl: EGLBase? = null
  private var mEglHolder: IEglSurface? = null

  constructor(sharedContext: IContext?, flags: Int) {
    init(flags, 3, sharedContext as Object)
  }

  constructor(
    maxClientVersion: Int,
    sharedContext: IContext?, flags: Int
  ) {
    init(flags, maxClientVersion, sharedContext as Object)
  }

  /**
   * @param arg1
   * @param arg2
   * @param sharedContext
   */
  override fun onInit(
    arg1: Int,
    arg2: Int, sharedContext: Any?
  ) {
    if (sharedContext == null
      || sharedContext is IContext
    ) {
      val stencilBits =
        if (arg1 and EGL_FLAG_STENCIL_1BIT == EGL_FLAG_STENCIL_1BIT) 1 else if (arg1 and EGL_FLAG_STENCIL_8BIT == EGL_FLAG_STENCIL_8BIT) 8 else 0
      mEgl = EGLBase.createFrom(
        arg2, sharedContext as IContext?,
        arg1 and EGL_FLAG_DEPTH_BUFFER == EGL_FLAG_DEPTH_BUFFER,
        stencilBits,
        arg1 and EGL_FLAG_RECORDABLE == EGL_FLAG_RECORDABLE
      )
    }
    if (mEgl == null) {
      callOnError(RuntimeException("failed to create EglCore"))
      releaseSelf()
    } else {
      mEglHolder = mEgl!!.createOffscreen(1, 1)
      mEglHolder!!.makeCurrent()
    }
  }

  @Throws(InterruptedException::class)
  override fun takeRequest(): Request? {
    val result = super.takeRequest()
    mEglHolder!!.makeCurrent()
    return result
  }

  override fun onBeforeStop() {
    mEglHolder!!.makeCurrent()
  }

  override fun onRelease() {
    mEglHolder!!.release()
    mEgl!!.release()
  }

  protected fun getEgl(): EGLBase? {
    return mEgl
  }

  protected fun getEGLContext(): IContext? {
    return mEgl!!.getContext()
  }

  protected fun getConfig(): IConfig? {
    return mEgl!!.getConfig()
  }

  protected fun getContext(): IContext? {
    return if (mEgl != null) mEgl!!.getContext() else null
  }

  protected fun makeCurrent() {
    mEglHolder!!.makeCurrent()
  }

  protected fun isGLES3(): Boolean {
    return mEgl != null && mEgl!!.getGlVersion() > 2
  }

  companion object {
    private val TAG: String? = "EglTask"
    const val EGL_FLAG_DEPTH_BUFFER = 0x01
    const val EGL_FLAG_RECORDABLE = 0x02
    const val EGL_FLAG_STENCIL_1BIT = 0x04
    const val EGL_FLAG_STENCIL_8BIT = 0x20
  }
}