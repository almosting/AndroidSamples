package com.almosting.toolbox.utils

import android.content.res.AssetManager
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
object AssetsHelper {
  @Throws(IOException::class)
  fun loadString(assets: AssetManager?, name: String?): String? {
    val sb = StringBuffer()
    val buf = CharArray(1024)
    val reader = BufferedReader(InputStreamReader(assets?.open(name!!)))
    var r = reader.read(buf)
    while (r > 0) {
      sb.append(buf, 0, r)
      r = reader.read(buf)
    }
    return sb.toString()
  }
}