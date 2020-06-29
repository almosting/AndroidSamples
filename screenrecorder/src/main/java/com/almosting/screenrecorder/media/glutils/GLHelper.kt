package com.almosting.screenrecorder.media.glutils

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources.Theme
import android.graphics.Bitmap
import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLUtils
import android.util.Log
import com.almosting.toolbox.utils.AssetsHelper
import com.almosting.toolbox.utils.BuildCheck
import java.io.IOException

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
object GLHelper {
  private val TAG: String? = "GLHelper"
  fun checkGlError(op: String?) {
    val error = GLES20.glGetError()
    if (error != GLES20.GL_NO_ERROR) {
      val msg = op + ": glError 0x" + Integer.toHexString(error)
      Log.e(TAG, msg)
      Throwable(msg).printStackTrace()
    }
  }

  fun initTex(texTarget: Int, filterParam: Int): Int {
    return initTex(
      texTarget, GLES20.GL_TEXTURE0,
      filterParam, filterParam, GLES20.GL_CLAMP_TO_EDGE
    )
  }

  fun initTex(
    texTarget: Int, texUnit: Int,
    minFilter: Int, magFilter: Int, wrap: Int
  ): Int {
    val tex = IntArray(1)
    GLES20.glActiveTexture(texUnit)
    GLES20.glGenTextures(1, tex, 0)
    GLES20.glBindTexture(texTarget, tex[0])
    GLES20.glTexParameteri(texTarget, GLES20.GL_TEXTURE_WRAP_S, wrap)
    GLES20.glTexParameteri(texTarget, GLES20.GL_TEXTURE_WRAP_T, wrap)
    GLES20.glTexParameteri(
      texTarget,
      GLES20.GL_TEXTURE_MIN_FILTER,
      minFilter
    )
    GLES20.glTexParameteri(
      texTarget,
      GLES20.GL_TEXTURE_MAG_FILTER,
      magFilter
    )
    return tex[0]
  }

  fun initTexes(
    n: Int,
    texTarget: Int, filterParam: Int
  ): IntArray? {
    return initTexes(
      IntArray(n), texTarget,
      filterParam, filterParam, GLES20.GL_CLAMP_TO_EDGE
    )
  }

  fun initTexes(
    texIds: IntArray,
    texTarget: Int, filterParam: Int
  ): IntArray? {
    return initTexes(
      texIds, texTarget,
      filterParam, filterParam, GLES20.GL_CLAMP_TO_EDGE
    )
  }

  fun initTexes(
    n: Int,
    texTarget: Int, minFilter: Int, magFilter: Int,
    wrap: Int
  ): IntArray? {
    return initTexes(IntArray(n), texTarget, minFilter, magFilter, wrap)
  }

  fun initTexes(
    texIds: IntArray,
    texTarget: Int, minFilter: Int, magFilter: Int,
    wrap: Int
  ): IntArray? {
    val textureUnits = IntArray(1)
    GLES20.glGetIntegerv(
      GLES20.GL_MAX_TEXTURE_IMAGE_UNITS,
      textureUnits,
      0
    )
    Log.v(TAG, "GL_MAX_TEXTURE_IMAGE_UNITS=" + textureUnits[0])
    val n = if (texIds.size > textureUnits[0]) textureUnits[0] else texIds.size
    for (i in 0 until n) {
      texIds[i] = initTex(
        texTarget, ShaderConst.TEX_NUMBERS!![i],
        minFilter, magFilter, wrap
      )
    }
    return texIds
  }

  fun initTexes(
    n: Int,
    texTarget: Int, texUnit: Int,
    minFilter: Int, magFilter: Int, wrap: Int
  ): IntArray? {
    return initTexes(
      IntArray(n), texTarget, texUnit,
      minFilter, magFilter, wrap
    )
  }

  fun initTexes(
    texIds: IntArray,
    texTarget: Int, texUnit: Int, filterParam: Int
  ): IntArray? {
    return initTexes(
      texIds, texTarget, texUnit,
      filterParam, filterParam, GLES20.GL_CLAMP_TO_EDGE
    )
  }

  fun initTexes(
    texIds: IntArray,
    texTarget: Int, texUnit: Int,
    minFilter: Int, magFilter: Int, wrap: Int
  ): IntArray? {
    val textureUnits = IntArray(1)
    GLES20.glGetIntegerv(
      GLES20.GL_MAX_TEXTURE_IMAGE_UNITS,
      textureUnits,
      0
    )
    val n = if (texIds.size > textureUnits[0]) textureUnits[0] else texIds.size
    for (i in 0 until n) {
      texIds[i] = initTex(
        texTarget, texUnit,
        minFilter, magFilter, wrap
      )
    }
    return texIds
  }

  fun deleteTex(hTex: Int) {
    val tex = intArrayOf(hTex)
    GLES20.glDeleteTextures(1, tex, 0)
  }

  fun deleteTex(tex: IntArray) {
    GLES20.glDeleteTextures(tex.size, tex, 0)
  }

  fun loadTextureFromResource(context: Context?, resId: Int): Int {
    return loadTextureFromResource(context, resId, null)
  }

  @SuppressLint("NewApi") fun loadTextureFromResource(
    context: Context?, resId: Int,
    theme: Theme?
  ): Int {
    // Create an empty, mutable bitmap
    val bitmap =
      Bitmap.createBitmap(256, 256, ARGB_8888)
    // get a canvas to paint over the bitmap
    val canvas = Canvas(bitmap)
    canvas.drawARGB(0, 0, 255, 0)

    // get a background image from resources
    // note the image format must match the bitmap format
    val background: Drawable? = if (BuildCheck.isAndroid5()) {
      context!!.resources.getDrawable(resId, theme)
    } else {
      context!!.resources.getDrawable(resId)
    }
    background!!.setBounds(0, 0, 256, 256)
    background.draw(canvas)
    val textures = IntArray(1)

    //Generate one texture pointer...
    GLES20.glGenTextures(1, textures, 0)
    //...and bind it to our array
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])

    //Create Nearest Filtered Texture
    GLES20.glTexParameterf(
      GLES20.GL_TEXTURE_2D,
      GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST.toFloat()
    )
    GLES20.glTexParameterf(
      GLES20.GL_TEXTURE_2D,
      GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat()
    )

    //Different possible texture parameters, e.g. GLES20.GL_CLAMP_TO_EDGE
    GLES20.glTexParameterf(
      GLES20.GL_TEXTURE_2D,
      GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT.toFloat()
    )
    GLES20.glTexParameterf(
      GLES20.GL_TEXTURE_2D,
      GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT.toFloat()
    )

    //Use the Android GLUtils to specify a two-dimensional texture image from our bitmap
    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
    //Clean up
    bitmap.recycle()
    return textures[0]
  }

  fun createTextureWithTextContent(text: String?): Int {
    // Create an empty, mutable bitmap
    val bitmap =
      Bitmap.createBitmap(256, 256, ARGB_8888)
    // get a canvas to paint over the bitmap
    val canvas = Canvas(bitmap)
    canvas.drawARGB(0, 0, 255, 0)

    // Draw the text
    val textPaint = Paint()
    textPaint.textSize = 32f
    textPaint.isAntiAlias = true
    textPaint.setARGB(0xff, 0xff, 0xff, 0xff)
    // draw the text centered
    canvas.drawText(text, 16f, 112f, textPaint)
    val texture = initTex(
      GLES20.GL_TEXTURE_2D,
      GLES20.GL_TEXTURE0,
      GLES20.GL_NEAREST,
      GLES20.GL_LINEAR,
      GLES20.GL_REPEAT
    )

    // Alpha blending
    // GLES20.glEnable(GLES20.GL_BLEND);
    // GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

    // Use the Android GLUtils to specify a two-dimensional texture image from our bitmap
    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
    // Clean up
    bitmap.recycle()
    return texture
  }

  /**
   * load, compile and link shader from Assets files
   *
   * @param vssAsset source file name in Assets of vertex shader
   */
  fun loadShader(
    context: Context,
    vssAsset: String?
  ): Int {
    var program = 0
    try {
      val vss = AssetsHelper.loadString(context.assets, vssAsset)
      val fss = AssetsHelper.loadString(context.assets, vssAsset)
      program = loadShader(vss, fss)
    } catch (e: IOException) {
      e.printStackTrace()
    }
    return program
  }

  /**
   * load, compile and link shader
   *
   * @param vss source of vertex shader
   * @param fss source of fragment shader
   */
  fun loadShader(vss: String?, fss: String?): Int {
    val compiled = IntArray(1)
    // 编译顶点着色器
    val vs = loadShader(GLES20.GL_VERTEX_SHADER, vss)
    if (vs == 0) {
      return 0
    }
    // 编译片段着色器
    val fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fss)
    if (fs == 0) {
      return 0
    }
    // 链接
    val program = GLES20.glCreateProgram()
    checkGlError("glCreateProgram")
    if (program == 0) {
      Log.e(TAG, "Could not create program")
    }
    GLES20.glAttachShader(program, vs)
    checkGlError("glAttachShader")
    GLES20.glAttachShader(program, fs)
    checkGlError("glAttachShader")
    GLES20.glLinkProgram(program)
    val linkStatus = IntArray(1)
    GLES20.glGetProgramiv(
      program,
      GLES20.GL_LINK_STATUS,
      linkStatus,
      0
    )
    if (linkStatus[0] != GLES20.GL_TRUE) {
      Log.e(TAG, "Could not link program: ")
      Log.e(TAG, GLES20.glGetProgramInfoLog(program))
      GLES20.glDeleteProgram(program)
      return 0
    }
    return program
  }

  /**
   * Compiles the provided shader source.
   *
   * @return A handle to the shader, or 0 on failure.
   */
  fun loadShader(shaderType: Int, source: String?): Int {
    var shader = GLES20.glCreateShader(shaderType)
    checkGlError("glCreateShader type=$shaderType")
    GLES20.glShaderSource(shader, source)
    GLES20.glCompileShader(shader)
    val compiled = IntArray(1)
    GLES20.glGetShaderiv(
      shader,
      GLES20.GL_COMPILE_STATUS,
      compiled,
      0
    )
    if (compiled[0] == 0) {
      Log.e(TAG, "Could not compile shader $shaderType:")
      Log.e(TAG, " " + GLES20.glGetShaderInfoLog(shader))
      GLES20.glDeleteShader(shader)
      shader = 0
    }
    return shader
  }

  /**
   * Checks to see if the location we obtained is valid.  GLES returns -1 if a label
   * could not be found, but does not set the GL error.
   *
   *
   * Throws a RuntimeException if the location is invalid.
   */
  fun checkLocation(location: Int, label: String?) {
    if (location < 0) {
      throw RuntimeException("Unable to locate '$label' in program")
    }
  }

  /**
   * Writes GL version info to the log.
   */
  @SuppressLint("InlinedApi") fun logVersionInfo() {
    Log.i(
      TAG,
      "vendor  : " + GLES20.glGetString(GLES20.GL_VENDOR)
    )
    Log.i(
      TAG,
      "renderer: " + GLES20.glGetString(GLES20.GL_RENDERER)
    )
    Log.i(
      TAG,
      "version : " + GLES20.glGetString(GLES20.GL_VERSION)
    )
    if (BuildCheck.isAndroid43()) {
      val values = IntArray(1)
      GLES30.glGetIntegerv(GLES30.GL_MAJOR_VERSION, values, 0)
      val majorVersion = values[0]
      GLES30.glGetIntegerv(GLES30.GL_MINOR_VERSION, values, 0)
      val minorVersion = values[0]
      if (GLES30.glGetError() == GLES30.GL_NO_ERROR) {
        Log.i(TAG, "version: $majorVersion.$minorVersion")
      }
    }
  }
}