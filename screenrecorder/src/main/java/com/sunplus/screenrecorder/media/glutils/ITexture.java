package com.sunplus.screenrecorder.media.glutils;

import android.graphics.Bitmap;
import java.io.IOException;

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
public interface ITexture {
  void release();

  void bind();
  void unbind();

  int getTexTarget();
  int getTexture();

  float[] getTexMatrix();
  void getTexMatrix(float[] matrix, int offset);

  int getTexWidth();
  int getTexHeight();

  void loadTexture(String filePath) throws NullPointerException, IOException;
  void loadTexture(Bitmap bitmap) throws NullPointerException;
}
