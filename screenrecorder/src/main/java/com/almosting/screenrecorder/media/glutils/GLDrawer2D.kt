package com.almosting.screenrecorder.media.glutils

import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
class GLDrawer2D(
  vertices: FloatArray?,
  texcoord: FloatArray?, isOES: Boolean
) : IDrawer2dES2 {
  private val VERTEX_NUM: Int
  private val VERTEX_SZ: Int
  private val pVertex: FloatBuffer?
  private val pTexCoord: FloatBuffer?
  private val mTexTarget: Int
  private var hProgram = 0
  var maPositionLoc = 0
  var maTextureCoordLoc = 0
  var muMVPMatrixLoc = 0
  var muTexMatrixLoc = 0
  private val mMvpMatrix: FloatArray? = FloatArray(16)

  constructor(isOES: Boolean) : this(
    VERTICES,
    TEXCOORD,
    isOES
  ) {
  }

  override fun release() {
    if (hProgram >= 0) {
      GLES20.glDeleteProgram(hProgram)
    }
    hProgram = -1
  }

  fun isOES(): Boolean {
    return mTexTarget == ShaderConst.GL_TEXTURE_EXTERNAL_OES
  }

  override fun getMvpMatrix(): FloatArray? {
    return mMvpMatrix
  }

  override fun setMvpMatrix(matrix: FloatArray?, offset: Int): IDrawer2D? {
    System.arraycopy(matrix, offset, mMvpMatrix, 0, 16)
    return this
  }

  override fun getMvpMatrix(matrix: FloatArray?, offset: Int) {
    System.arraycopy(mMvpMatrix, 0, matrix, offset, 16)
  }

  @Synchronized override fun draw(
    texId: Int,
    texMatrix: FloatArray?, offset: Int
  ) {
    if (hProgram < 0) {
      return
    }
    GLES20.glUseProgram(hProgram)
    if (texMatrix != null) {
      // 指定纹理变换矩阵时
      GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, offset)
    }
    // 设置模型视图转换矩阵
    GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mMvpMatrix, 0)
    GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
    GLES20.glBindTexture(mTexTarget, texId)
    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTEX_NUM)
    GLES20.glBindTexture(mTexTarget, 0)
    GLES20.glUseProgram(0)
  }

  override fun draw(texture: ITexture?) {
    draw(texture!!.getTexture(), texture.getTexMatrix(), 0)
  }

  override fun draw(offscreen: TextureOffscreen?) {
    draw(offscreen!!.getTexture(), offscreen.getTexMatrix(), 0)
  }

  fun initTex(): Int {
    return GLHelper.initTex(mTexTarget, GLES20.GL_NEAREST)
  }

  fun deleteTex(hTex: Int) {
    GLHelper.deleteTex(hTex)
  }

  @Synchronized fun updateShader(vs: String?, fs: String?) {
    release()
    hProgram = GLHelper.loadShader(vs, fs)
    init()
  }

  fun updateShader(fs: String?) {
    updateShader(ShaderConst.VERTEX_SHADER, fs)
  }

  fun resetShader() {
    release()
    hProgram = if (isOES()) {
      GLHelper.loadShader(ShaderConst.VERTEX_SHADER, ShaderConst.FRAGMENT_SHADER_SIMPLE_OES)
    } else {
      GLHelper.loadShader(ShaderConst.VERTEX_SHADER, ShaderConst.FRAGMENT_SHADER_SIMPLE)
    }
    init()
  }

  override fun glGetAttribLocation(name: String?): Int {
    GLES20.glUseProgram(hProgram)
    return GLES20.glGetAttribLocation(hProgram, name)
  }

  override fun glGetUniformLocation(name: String?): Int {
    GLES20.glUseProgram(hProgram)
    return GLES20.glGetUniformLocation(hProgram, name)
  }

  override fun glUseProgram() {
    GLES20.glUseProgram(hProgram)
  }

  private fun init() {
    GLES20.glUseProgram(hProgram)
    maPositionLoc = GLES20.glGetAttribLocation(hProgram, "aPosition")
    maTextureCoordLoc = GLES20.glGetAttribLocation(hProgram, "aTextureCoord")
    muMVPMatrixLoc = GLES20.glGetUniformLocation(hProgram, "uMVPMatrix")
    muTexMatrixLoc = GLES20.glGetUniformLocation(hProgram, "uTexMatrix")
    //
    GLES20.glUniformMatrix4fv(
      muMVPMatrixLoc,
      1, false, mMvpMatrix, 0
    )
    GLES20.glUniformMatrix4fv(
      muTexMatrixLoc,
      1, false, mMvpMatrix, 0
    )
    GLES20.glVertexAttribPointer(
      maPositionLoc,
      2, GLES20.GL_FLOAT, false, VERTEX_SZ, pVertex
    )
    GLES20.glVertexAttribPointer(
      maTextureCoordLoc,
      2, GLES20.GL_FLOAT, false, VERTEX_SZ, pTexCoord
    )
    GLES20.glEnableVertexAttribArray(maPositionLoc)
    GLES20.glEnableVertexAttribArray(maTextureCoordLoc)
  }

  companion object {
    private val VERTICES: FloatArray? =
      floatArrayOf(1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f)
    private val TEXCOORD: FloatArray? =
      floatArrayOf(1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f)
    private const val FLOAT_SZ = java.lang.Float.SIZE / 8
  }

  init {
    VERTEX_NUM = Math.min(
      vertices?.size ?: 0,
      texcoord?.size ?: 0
    ) / 2
    VERTEX_SZ = VERTEX_NUM * 2
    mTexTarget = if (isOES) ShaderConst.GL_TEXTURE_EXTERNAL_OES else ShaderConst.GL_TEXTURE_2D
    pVertex = ByteBuffer.allocateDirect(VERTEX_SZ * FLOAT_SZ)
      .order(ByteOrder.nativeOrder()).asFloatBuffer()
    pVertex.put(vertices)
    pVertex.flip()
    pTexCoord = ByteBuffer.allocateDirect(VERTEX_SZ * FLOAT_SZ)
      .order(ByteOrder.nativeOrder()).asFloatBuffer()
    pTexCoord.put(texcoord)
    pTexCoord.flip()
    hProgram = if (isOES) {
      GLHelper.loadShader(ShaderConst.VERTEX_SHADER, ShaderConst.FRAGMENT_SHADER_SIMPLE_OES)
    } else {
      GLHelper.loadShader(ShaderConst.VERTEX_SHADER, ShaderConst.FRAGMENT_SHADER_SIMPLE)
    }
    Matrix.setIdentityM(mMvpMatrix, 0)
    init()
  }
}