package com.sunplus.screenrecorder.media.glutils

import android.opengl.GLES20

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
object ShaderConst {
  const val GL_TEXTURE_EXTERNAL_OES = 0x8D65
  const val GL_TEXTURE_2D = 0x0DE1
  val SHADER_VERSION: String? = "#version 100\n"
  val HEADER_2D: String? = ""
  val SAMPLER_2D: String? = "sampler2D"
  val HEADER_OES: String? = "#extension GL_OES_EGL_image_external : require\n"
  val SAMPLER_OES: String? = "samplerExternalOES"
  const val KERNEL_SIZE3x3 = 9
  const val KERNEL_SIZE5x５ = 25
  const val NO_TEXTURE = -1
  val TEX_NUMBERS: IntArray? = intArrayOf(
    GLES20.GL_TEXTURE0, GLES20.GL_TEXTURE1,
    GLES20.GL_TEXTURE2, GLES20.GL_TEXTURE3,
    GLES20.GL_TEXTURE4, GLES20.GL_TEXTURE5,
    GLES20.GL_TEXTURE6, GLES20.GL_TEXTURE7,
    GLES20.GL_TEXTURE8, GLES20.GL_TEXTURE9,
    GLES20.GL_TEXTURE10, GLES20.GL_TEXTURE11,
    GLES20.GL_TEXTURE12, GLES20.GL_TEXTURE13,
    GLES20.GL_TEXTURE14, GLES20.GL_TEXTURE15,
    GLES20.GL_TEXTURE16, GLES20.GL_TEXTURE17,
    GLES20.GL_TEXTURE18, GLES20.GL_TEXTURE19,
    GLES20.GL_TEXTURE20, GLES20.GL_TEXTURE21,
    GLES20.GL_TEXTURE22, GLES20.GL_TEXTURE23,
    GLES20.GL_TEXTURE24, GLES20.GL_TEXTURE25,
    GLES20.GL_TEXTURE26, GLES20.GL_TEXTURE27,
    GLES20.GL_TEXTURE28, GLES20.GL_TEXTURE29,
    GLES20.GL_TEXTURE30, GLES20.GL_TEXTURE31
  )
  // 函数字符串定义
  /**
   * 将RGB转换为HSV
   * {R[0.0-1.0], G[0.0-1.0], B([0.0-1.0]} => {H[0.0-1.0], S[0.0-1.0], V[0.0-1.0]}
   */
  val FUNC_RGB2HSV: String? = """
    vec3 rgb2hsv(vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
    }

    """.trimIndent()

  /**
   * 将HSV转换为RGB
   * {H[0.0-1.0], S[0.0-1.0], V[0.0-1.0]} => {R[0.0-1.0], G[0.0-1.0], B([0.0-1.0]}
   */
  val FUNC_HSV2RGB: String? = """
    vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
    }

    """.trimIndent()

  /**
   * 获得RGB亮度
   * 只需使用转换因子计算内积
   * 系数是(0.2125, 0.7154, 0.0721)
   */
  val FUNC_GET_INTENSITY: String? =
    """
    const highp vec3 luminanceWeighting = vec3(0.2125, 0.7154, 0.0721);
    highp float getIntensity(vec3 c) {
    return dot(c.rgb, luminanceWeighting);
    }

    """.trimIndent()
  // 顶点着色器
  /**
   * 模型视图变换矩阵和纹理变换矩阵顶点着色器只是为了应用
   */
  val VERTEX_SHADER: String? = """${SHADER_VERSION}uniform mat4 uMVPMatrix;
uniform mat4 uTexMatrix;
attribute highp vec4 aPosition;
attribute highp vec4 aTextureCoord;
varying highp vec2 vTextureCoord;
void main() {
    gl_Position = uMVPMatrix * aPosition;
    vTextureCoord = (uTexMatrix * aTextureCoord).xy;
}
"""

  // 片段着色器
  val FRAGMENT_SHADER_SIMPLE_OES: String? = """$SHADER_VERSION${HEADER_OES}precision mediump float;
uniform samplerExternalOES sTexture;
varying highp vec2 vTextureCoord;
void main() {
  gl_FragColor = texture2D(sTexture, vTextureCoord);
}"""
  val FRAGMENT_SHADER_SIMPLE: String? = """$SHADER_VERSION${HEADER_2D}precision mediump float;
uniform sampler2D sTexture;
varying highp vec2 vTextureCoord;
void main() {
  gl_FragColor = texture2D(sTexture, vTextureCoord);
}"""

  // Simple fragment shader for use with "normal" 2D textures.
  private val FRAGMENT_SHADER_BASE: String? = """$SHADER_VERSION%sprecision mediump float;
varying vec2 vTextureCoord;
uniform %s sTexture;
void main() {
    gl_FragColor = texture2D(sTexture, vTextureCoord);
}
"""
  val FRAGMENT_SHADER_2D: String? = String.format(
    FRAGMENT_SHADER_BASE!!,
    HEADER_2D,
    SAMPLER_2D
  )
  val FRAGMENT_SHADER_EXT: String? = String.format(
    FRAGMENT_SHADER_BASE!!,
    HEADER_OES,
    SAMPLER_OES
  )

  // Fragment shader that converts color to black & white with a simple transformation.
  private val FRAGMENT_SHADER_BW_BASE: String? = """$SHADER_VERSION%sprecision mediump float;
varying vec2 vTextureCoord;
uniform %s sTexture;
void main() {
    vec4 tc = texture2D(sTexture, vTextureCoord);
    float color = tc.r * 0.3 + tc.g * 0.59 + tc.b * 0.11;
    gl_FragColor = vec4(color, color, color, 1.0);
}
"""
  val FRAGMENT_SHADER_BW: String? = String.format(
    FRAGMENT_SHADER_BW_BASE!!,
    HEADER_2D,
    SAMPLER_2D
  )
  val FRAGMENT_SHADER_EXT_BW: String? = String.format(
    FRAGMENT_SHADER_BW_BASE!!,
    HEADER_OES,
    SAMPLER_OES
  )

  // Fragment shader that attempts to produce a high contrast image
  private val FRAGMENT_SHADER_NIGHT_BASE: String? = """$SHADER_VERSION%sprecision mediump float;
varying vec2 vTextureCoord;
uniform %s sTexture;
void main() {
    vec4 tc = texture2D(sTexture, vTextureCoord);
    float color = ((tc.r * 0.3 + tc.g * 0.59 + tc.b * 0.11) - 0.5 * 1.5) + 0.8;
    gl_FragColor = vec4(color, color + 0.15, color, 1.0);
}
"""
  val FRAGMENT_SHADER_NIGHT: String? = String.format(
    FRAGMENT_SHADER_NIGHT_BASE!!,
    HEADER_2D,
    SAMPLER_2D
  )
  val FRAGMENT_SHADER_EXT_NIGHT: String? = String.format(
    FRAGMENT_SHADER_NIGHT_BASE!!,
    HEADER_OES,
    SAMPLER_OES
  )

  // Fragment shader that applies a Chroma Key effect, making green pixels transparent
  private val FRAGMENT_SHADER_CHROMA_KEY_BASE: String? =
    """$SHADER_VERSION%sprecision mediump float;
varying vec2 vTextureCoord;
uniform %s sTexture;
void main() {
    vec4 tc = texture2D(sTexture, vTextureCoord);
    float color = ((tc.r * 0.3 + tc.g * 0.59 + tc.b * 0.11) - 0.5 * 1.5) + 0.8;
    if(tc.g > 0.6 && tc.b < 0.6 && tc.r < 0.6){
        gl_FragColor = vec4(0, 0, 0, 0.0);
    }else{
        gl_FragColor = texture2D(sTexture, vTextureCoord);
    }
}
"""
  val FRAGMENT_SHADER_CHROMA_KEY: String? = String.format(
    FRAGMENT_SHADER_CHROMA_KEY_BASE!!,
    HEADER_2D,
    SAMPLER_2D
  )
  val FRAGMENT_SHADER_EXT_CHROMA_KEY: String? = String.format(
    FRAGMENT_SHADER_CHROMA_KEY_BASE!!,
    HEADER_OES,
    SAMPLER_OES
  )
  private val FRAGMENT_SHADER_SQUEEZE_BASE: String? = """$SHADER_VERSION%sprecision mediump float;
varying vec2 vTextureCoord;
uniform %s sTexture;
uniform vec2 uPosition;
void main() {
    vec2 texCoord = vTextureCoord.xy;
    vec2 normCoord = 2.0 * texCoord - 1.0;
    float r = length(normCoord); // to polar coords
    float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x); // to polar coords
    r = pow(r, 1.0/1.8) * 0.8;
    normCoord.x = r * cos(phi);
    normCoord.y = r * sin(phi);
    texCoord = normCoord / 2.0 + 0.5;
    gl_FragColor = texture2D(sTexture, texCoord);
}
"""
  val FRAGMENT_SHADER_SQUEEZE: String? = String.format(
    FRAGMENT_SHADER_SQUEEZE_BASE!!,
    HEADER_2D,
    SAMPLER_2D
  )
  val FRAGMENT_SHADER_EXT_SQUEEZE: String? = String.format(
    FRAGMENT_SHADER_SQUEEZE_BASE!!,
    HEADER_OES,
    SAMPLER_OES
  )
  val FRAGMENT_SHADER_EXT_TWIRL: String? = """#version 100
#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 vTextureCoord;
uniform samplerExternalOES sTexture;
uniform vec2 uPosition;
void main() {
    vec2 texCoord = vTextureCoord.xy;
    vec2 normCoord = 2.0 * texCoord - 1.0;
    float r = length(normCoord); // to polar coords
    float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x); // to polar coords
    phi = phi + (1.0 - smoothstep(-0.5, 0.5, r)) * 4.0;
    normCoord.x = r * cos(phi);
    normCoord.y = r * sin(phi);
    texCoord = normCoord / 2.0 + 0.5;
    gl_FragColor = texture2D(sTexture, texCoord);
}
"""
  val FRAGMENT_SHADER_EXT_TUNNEL: String? =
    """$SHADER_VERSION#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 vTextureCoord;
uniform samplerExternalOES sTexture;
uniform vec2 uPosition;
void main() {
    vec2 texCoord = vTextureCoord.xy;
    vec2 normCoord = 2.0 * texCoord - 1.0;
    float r = length(normCoord); // to polar coords
    float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x); // to polar coords
    if (r > 0.5) r = 0.5;
    normCoord.x = r * cos(phi);
    normCoord.y = r * sin(phi);
    texCoord = normCoord / 2.0 + 0.5;
    gl_FragColor = texture2D(sTexture, texCoord);
}
"""
  val FRAGMENT_SHADER_EXT_BULGE: String? =
    """$SHADER_VERSION#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 vTextureCoord;
uniform samplerExternalOES sTexture;
uniform vec2 uPosition;
void main() {
    vec2 texCoord = vTextureCoord.xy;
    vec2 normCoord = 2.0 * texCoord - 1.0;
    float r = length(normCoord); // to polar coords
    float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x); // to polar coords
    r = r * smoothstep(-0.1, 0.5, r);
    normCoord.x = r * cos(phi);
    normCoord.y = r * sin(phi);
    texCoord = normCoord / 2.0 + 0.5;
    gl_FragColor = texture2D(sTexture, texCoord);
}
"""
  val FRAGMENT_SHADER_EXT_DENT: String? =
    """$SHADER_VERSION#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 vTextureCoord;
uniform samplerExternalOES sTexture;
uniform vec2 uPosition;
void main() {
    vec2 texCoord = vTextureCoord.xy;
    vec2 normCoord = 2.0 * texCoord - 1.0;
    float r = length(normCoord); // to polar coords
    float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x); // to polar coords
    r = 2.0 * r - r * smoothstep(0.0, 0.7, r);
    normCoord.x = r * cos(phi);
    normCoord.y = r * sin(phi);
    texCoord = normCoord / 2.0 + 0.5;
    gl_FragColor = texture2D(sTexture, texCoord);
}
"""
  val FRAGMENT_SHADER_EXT_FISHEYE: String? =
    """$SHADER_VERSION#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 vTextureCoord;
uniform samplerExternalOES sTexture;
uniform vec2 uPosition;
void main() {
    vec2 texCoord = vTextureCoord.xy;
    vec2 normCoord = 2.0 * texCoord - 1.0;
    float r = length(normCoord); // to polar coords
    float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x); // to polar coords
    r = r * r / sqrt(2.0);
    normCoord.x = r * cos(phi);
    normCoord.y = r * sin(phi);
    texCoord = normCoord / 2.0 + 0.5;
    gl_FragColor = texture2D(sTexture, texCoord);
}
"""
  val FRAGMENT_SHADER_EXT_STRETCH: String? =
    """$SHADER_VERSION#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 vTextureCoord;
uniform samplerExternalOES sTexture;
uniform vec2 uPosition;
void main() {
    vec2 texCoord = vTextureCoord.xy;
    vec2 normCoord = 2.0 * texCoord - 1.0;
    vec2 s = sign(normCoord + uPosition);
    normCoord = abs(normCoord);
    normCoord = 0.5 * normCoord + 0.5 * smoothstep(0.25, 0.5, normCoord) * normCoord;
    normCoord = s * normCoord;
    texCoord = normCoord / 2.0 + 0.5;
    gl_FragColor = texture2D(sTexture, texCoord);
}
"""
  val FRAGMENT_SHADER_EXT_MIRROR: String? =
    """$SHADER_VERSION#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 vTextureCoord;
uniform samplerExternalOES sTexture;
uniform vec2 uPosition;
void main() {
    vec2 texCoord = vTextureCoord.xy;
    vec2 normCoord = 2.0 * texCoord - 1.0;
    normCoord.x = normCoord.x * sign(normCoord.x + uPosition.x);
    texCoord = normCoord / 2.0 + 0.5;
    gl_FragColor = texture2D(sTexture, texCoord);
}
"""
  val FRAGMENT_SHADER_SOBEL_BASE: String? =
    """$SHADER_VERSION%s#define KERNEL_SIZE3x3 $KERNEL_SIZE3x3
precision highp float;
varying       vec2 vTextureCoord;
uniform %s    sTexture;
uniform float uKernel[18];
uniform vec2  uTexOffset[KERNEL_SIZE3x3];
uniform float uColorAdjust;
void main() {
    vec3 t0 = texture2D(sTexture, vTextureCoord + uTexOffset[0]).rgb;
    vec3 t1 = texture2D(sTexture, vTextureCoord + uTexOffset[1]).rgb;
    vec3 t2 = texture2D(sTexture, vTextureCoord + uTexOffset[2]).rgb;
    vec3 t3 = texture2D(sTexture, vTextureCoord + uTexOffset[3]).rgb;
    vec3 t4 = texture2D(sTexture, vTextureCoord + uTexOffset[4]).rgb;
    vec3 t5 = texture2D(sTexture, vTextureCoord + uTexOffset[5]).rgb;
    vec3 t6 = texture2D(sTexture, vTextureCoord + uTexOffset[6]).rgb;
    vec3 t7 = texture2D(sTexture, vTextureCoord + uTexOffset[7]).rgb;
    vec3 t8 = texture2D(sTexture, vTextureCoord + uTexOffset[8]).rgb;
    vec3 sumH = t0 * uKernel[0] + t1 * uKernel[1] + t2 * uKernel[2]
              + t3 * uKernel[3] + t4 * uKernel[4] + t5 * uKernel[5]
              + t6 * uKernel[6] + t7 * uKernel[7] + t8 * uKernel[8];
    float mag = length(sumH);
    gl_FragColor = vec4(vec3(mag), 1.0);
}
"""
  val FRAGMENT_SHADER_SOBEL: String? = String.format(
    FRAGMENT_SHADER_SOBEL_BASE!!,
    HEADER_2D,
    SAMPLER_2D
  )
  val FRAGMENT_SHADER_EXT_SOBEL: String? = String.format(
    FRAGMENT_SHADER_SOBEL_BASE!!,
    HEADER_OES,
    SAMPLER_OES
  )
  val KERNEL_NULL: FloatArray? = floatArrayOf(0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f)
  val KERNEL_SOBEL_H: FloatArray? = floatArrayOf(1f, 0f, -1f, 2f, 0f, -2f, 1f, 0f, -1f)

  // 索贝尔（一阶导数）
  val KERNEL_SOBEL_V: FloatArray? = floatArrayOf(1f, 2f, 1f, 0f, 0f, 0f, -1f, -2f, -1f)
  val KERNEL_SOBEL2_H: FloatArray? = floatArrayOf(3f, 0f, -3f, 10f, 0f, -10f, 3f, 0f, -3f)
  val KERNEL_SOBEL2_V: FloatArray? = floatArrayOf(3f, 10f, 3f, 0f, 0f, 0f, -3f, -10f, -3f)
  val KERNEL_SHARPNESS: FloatArray? = floatArrayOf(0f, -1f, 0f, -1f, 5f, -1f, 0f, -1f, 0f)

  // 锐度
  val KERNEL_EDGE_DETECT: FloatArray? =
    floatArrayOf(-1f, -1f, -1f, -1f, 8f, -1f, -1f, -1f, -1f)

  // 边缘检测
  val KERNEL_EMBOSS: FloatArray? = floatArrayOf(2f, 0f, 0f, 0f, -1f, 0f, 0f, 0f, -1f)

  // 浮雕，偏移0.5 f
  val KERNEL_SMOOTH: FloatArray? =
    floatArrayOf(1 / 9f, 1 / 9f, 1 / 9f, 1 / 9f, 1 / 9f, 1 / 9f, 1 / 9f, 1 / 9f, 1 / 9f) // 移動平均
  val KERNEL_GAUSSIAN: FloatArray? =
    floatArrayOf(1 / 16f, 2 / 16f, 1 / 16f, 2 / 16f, 4 / 16f, 2 / 16f, 1 / 16f, 2 / 16f, 1 / 16f)

  // 高斯（去噪/）
  val KERNEL_BRIGHTEN: FloatArray? = floatArrayOf(1f, 1f, 1f, 1f, 2f, 1f, 1f, 1f, 1f)
  val KERNEL_LAPLACIAN: FloatArray? = floatArrayOf(1f, 1f, 1f, 1f, -8f, 1f, 1f, 1f, 1f)

  // 拉普拉斯（二阶导数）
  private val FRAGMENT_SHADER_FILT3x3_BASE: String? =
    """$SHADER_VERSION%s#define KERNEL_SIZE3x3 $KERNEL_SIZE3x3
precision highp float;
varying       vec2 vTextureCoord;
uniform %s    sTexture;
uniform float uKernel[18];
uniform vec2  uTexOffset[KERNEL_SIZE3x3];
uniform float uColorAdjust;
void main() {
    vec4 sum = vec4(0.0);
    sum += texture2D(sTexture, vTextureCoord + uTexOffset[0]) * uKernel[0];
    sum += texture2D(sTexture, vTextureCoord + uTexOffset[1]) * uKernel[1];
    sum += texture2D(sTexture, vTextureCoord + uTexOffset[2]) * uKernel[2];
    sum += texture2D(sTexture, vTextureCoord + uTexOffset[3]) * uKernel[3];
    sum += texture2D(sTexture, vTextureCoord + uTexOffset[4]) * uKernel[4];
    sum += texture2D(sTexture, vTextureCoord + uTexOffset[5]) * uKernel[5];
    sum += texture2D(sTexture, vTextureCoord + uTexOffset[6]) * uKernel[6];
    sum += texture2D(sTexture, vTextureCoord + uTexOffset[7]) * uKernel[7];
    sum += texture2D(sTexture, vTextureCoord + uTexOffset[8]) * uKernel[8];
    gl_FragColor = sum + uColorAdjust;
}
"""
  val FRAGMENT_SHADER_FILT3x3: String? = String.format(
    FRAGMENT_SHADER_FILT3x3_BASE!!,
    HEADER_2D,
    SAMPLER_2D
  )
  val FRAGMENT_SHADER_EXT_FILT3x3: String? = String.format(
    FRAGMENT_SHADER_FILT3x3_BASE!!,
    HEADER_OES,
    SAMPLER_OES
  )
}