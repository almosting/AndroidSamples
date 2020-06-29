package com.sunplus.screenrecorder.media.glutils

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
interface IDrawer2dES2 : IDrawer2D {
  fun glGetAttribLocation(name: String?): Int
  fun glGetUniformLocation(name: String?): Int
  fun glUseProgram()
}