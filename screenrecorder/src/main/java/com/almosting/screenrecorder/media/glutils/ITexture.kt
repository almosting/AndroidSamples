package com.almosting.screenrecorder.media.glutils

import android.graphics.Bitmap
import java.io.IOException

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
interface ITexture {
  fun release()
  fun bind()
  fun unbind()
  fun getTexTarget(): Int
  fun getTexture(): Int
  fun getTexMatrix(): FloatArray?
  fun getTexMatrix(matrix: FloatArray?, offset: Int)
  fun getTexWidth(): Int
  fun getTexHeight(): Int
  @Throws(NullPointerException::class, IOException::class) open fun loadTexture(
    filePath: String?
  )

  @Throws(NullPointerException::class) fun loadTexture(bitmap: Bitmap?)
}