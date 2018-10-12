package com.sunplus.screenrecorder.media.glutils;

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
public interface IDrawer2dES2 extends IDrawer2D{
  int glGetAttribLocation(final String name);
  int glGetUniformLocation(final String name);
  void glUseProgram();
}
