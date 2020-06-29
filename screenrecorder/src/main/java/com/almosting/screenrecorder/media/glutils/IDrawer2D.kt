package com.almosting.screenrecorder.media.glutils

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
interface IDrawer2D {
  fun release()
  fun getMvpMatrix(): FloatArray?
  fun setMvpMatrix(matrix: FloatArray?, offset: Int): IDrawer2D?
  fun getMvpMatrix(matrix: FloatArray?, offset: Int)
  fun draw(texId: Int, texMatrix: FloatArray?, offset: Int)
  fun draw(texture: ITexture?)
  fun draw(offscreen: TextureOffscreen?)
}