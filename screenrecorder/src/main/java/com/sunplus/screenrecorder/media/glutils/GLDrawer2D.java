package com.sunplus.screenrecorder.media.glutils;

import android.opengl.GLES20;
import android.opengl.Matrix;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static com.sunplus.screenrecorder.media.glutils.ShaderConst.FRAGMENT_SHADER_SIMPLE;
import static com.sunplus.screenrecorder.media.glutils.ShaderConst.FRAGMENT_SHADER_SIMPLE_OES;
import static com.sunplus.screenrecorder.media.glutils.ShaderConst.GL_TEXTURE_2D;
import static com.sunplus.screenrecorder.media.glutils.ShaderConst.GL_TEXTURE_EXTERNAL_OES;
import static com.sunplus.screenrecorder.media.glutils.ShaderConst.VERTEX_SHADER;

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
public class GLDrawer2D implements IDrawer2dES2 {

  private static final float[] VERTICES = { 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f };
  private static final float[] TEXCOORD = { 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f };
  private static final int FLOAT_SZ = Float.SIZE / 8;

  private final int VERTEX_NUM;
  private final int VERTEX_SZ;
  private final FloatBuffer pVertex;
  private final FloatBuffer pTexCoord;
  private final int mTexTarget;
  private int hProgram;
  int maPositionLoc;
  int maTextureCoordLoc;
  int muMVPMatrixLoc;
  int muTexMatrixLoc;
  private final float[] mMvpMatrix = new float[16];

  public GLDrawer2D(final boolean isOES) {
    this(VERTICES, TEXCOORD, isOES);
  }

  public GLDrawer2D(final float[] vertices,
                    final float[] texcoord, final boolean isOES) {

    VERTEX_NUM = Math.min(
        vertices != null ? vertices.length : 0,
        texcoord != null ? texcoord.length : 0) / 2;
    VERTEX_SZ = VERTEX_NUM * 2;

    mTexTarget = isOES ? GL_TEXTURE_EXTERNAL_OES : GL_TEXTURE_2D;
    pVertex = ByteBuffer.allocateDirect(VERTEX_SZ * FLOAT_SZ)
        .order(ByteOrder.nativeOrder()).asFloatBuffer();
    pVertex.put(vertices);
    pVertex.flip();
    pTexCoord = ByteBuffer.allocateDirect(VERTEX_SZ * FLOAT_SZ)
        .order(ByteOrder.nativeOrder()).asFloatBuffer();
    pTexCoord.put(texcoord);
    pTexCoord.flip();

    if (isOES) {
      hProgram = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_SIMPLE_OES);
    } else {
      hProgram = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_SIMPLE);
    }
    Matrix.setIdentityM(mMvpMatrix, 0);
    init();
  }

  @Override
  public void release() {
    if (hProgram >= 0) {
      GLES20.glDeleteProgram(hProgram);
    }
    hProgram = -1;
  }

  public boolean isOES() {
    return mTexTarget == GL_TEXTURE_EXTERNAL_OES;
  }

  @Override
  public float[] getMvpMatrix() {
    return mMvpMatrix;
  }

  @Override
  public IDrawer2D setMvpMatrix(final float[] matrix, final int offset) {
    System.arraycopy(matrix, offset, mMvpMatrix, 0, 16);
    return this;
  }

  @Override
  public void getMvpMatrix(final float[] matrix, final int offset) {
    System.arraycopy(mMvpMatrix, 0, matrix, offset, 16);
  }

  @Override
  public synchronized void draw(final int texId,
                                final float[] tex_matrix, final int offset) {

    //		if (DEBUG) Log.v(TAG, "draw");
    if (hProgram < 0) {
      return;
    }
    GLES20.glUseProgram(hProgram);
    if (tex_matrix != null) {
      // 指定纹理变换矩阵时
      GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, tex_matrix, offset);
    }
    // 设置模型视图转换矩阵
    GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mMvpMatrix, 0);
    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
    GLES20.glBindTexture(mTexTarget, texId);
    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTEX_NUM);
    GLES20.glBindTexture(mTexTarget, 0);
    GLES20.glUseProgram(0);
  }

  @Override
  public void draw(final ITexture texture) {
    draw(texture.getTexture(), texture.getTexMatrix(), 0);
  }

  @Override
  public void draw(final TextureOffscreen offscreen) {
    draw(offscreen.getTexture(), offscreen.getTexMatrix(), 0);
  }

  public int initTex() {
    return GLHelper.initTex(mTexTarget, GLES20.GL_NEAREST);
  }

  public void deleteTex(final int hTex) {
    GLHelper.deleteTex(hTex);
  }

  public synchronized void updateShader(final String vs, final String fs) {
    release();
    hProgram = GLHelper.loadShader(vs, fs);
    init();
  }

  public void updateShader(final String fs) {
    updateShader(VERTEX_SHADER, fs);
  }

  public void resetShader() {
    release();
    if (isOES()) {
      hProgram = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_SIMPLE_OES);
    } else {
      hProgram = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_SIMPLE);
    }
    init();
  }

  @Override
  public int glGetAttribLocation(final String name) {
    GLES20.glUseProgram(hProgram);
    return GLES20.glGetAttribLocation(hProgram, name);
  }

  @Override
  public int glGetUniformLocation(final String name) {
    GLES20.glUseProgram(hProgram);
    return GLES20.glGetUniformLocation(hProgram, name);
  }

  @Override
  public void glUseProgram() {
    GLES20.glUseProgram(hProgram);
  }

  private void init() {
    GLES20.glUseProgram(hProgram);
    maPositionLoc = GLES20.glGetAttribLocation(hProgram, "aPosition");
    maTextureCoordLoc = GLES20.glGetAttribLocation(hProgram, "aTextureCoord");
    muMVPMatrixLoc = GLES20.glGetUniformLocation(hProgram, "uMVPMatrix");
    muTexMatrixLoc = GLES20.glGetUniformLocation(hProgram, "uTexMatrix");
    //
    GLES20.glUniformMatrix4fv(muMVPMatrixLoc,
        1, false, mMvpMatrix, 0);
    GLES20.glUniformMatrix4fv(muTexMatrixLoc,
        1, false, mMvpMatrix, 0);
    GLES20.glVertexAttribPointer(maPositionLoc,
        2, GLES20.GL_FLOAT, false, VERTEX_SZ, pVertex);
    GLES20.glVertexAttribPointer(maTextureCoordLoc,
        2, GLES20.GL_FLOAT, false, VERTEX_SZ, pTexCoord);
    GLES20.glEnableVertexAttribArray(maPositionLoc);
    GLES20.glEnableVertexAttribArray(maTextureCoordLoc);
  }
}
