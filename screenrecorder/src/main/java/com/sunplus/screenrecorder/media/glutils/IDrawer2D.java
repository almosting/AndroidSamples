package com.sunplus.screenrecorder.media.glutils;

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
public interface IDrawer2D {
  void release();

  float[] getMvpMatrix();

  IDrawer2D setMvpMatrix(final float[] matrix, final int offset);

  void getMvpMatrix(final float[] matrix, final int offset);

  void draw(final int texId, final float[] texMatrix, final int offset);

  void draw(final ITexture texture);

  void draw(final TextureOffscreen offscreen);
}
