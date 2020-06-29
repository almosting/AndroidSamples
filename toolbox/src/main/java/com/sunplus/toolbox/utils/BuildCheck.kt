package com.sunplus.toolbox.utils

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
object BuildCheck {
  private fun check(value: Int): Boolean {
    return VERSION.SDK_INT >= value
  }

  /**
   * Magic version number for a current development build,
   * which has not yet turned into an official release. API=10000
   */
  fun isCurrentDevelopment(): Boolean {
    return VERSION.SDK_INT == VERSION_CODES.CUR_DEVELOPMENT
  }

  /**
   * October 2011: Android 4.0., API>=14
   */
  fun isIcecreamSandwich(): Boolean {
    return check(VERSION_CODES.ICE_CREAM_SANDWICH)
  }

  /**
   * October 2011: Android 4.0., API>=14
   */
  fun isAndroid4(): Boolean {
    return check(VERSION_CODES.ICE_CREAM_SANDWICH)
  }

  /**
   * December 2011: Android 4.0.3., API>=15
   */
  fun isIcecreamSandwichMR1(): Boolean {
    return check(VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
  }

  /**
   * December 2011: Android 4.0.3., API>=15
   */
  fun isAndroid403(): Boolean {
    return check(VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
  }

  /**
   * June 2012: Android 4.1., API>=16
   */
  fun isJellyBean(): Boolean {
    return check(VERSION_CODES.JELLY_BEAN)
  }

  /**
   * June 2012: Android 4.1., API>=16
   */
  fun isAndroid41(): Boolean {
    return check(VERSION_CODES.JELLY_BEAN)
  }

  /**
   * November 2012: Android 4.2, Moar jelly beans!, API>=17
   */
  fun isJellyBeanMr1(): Boolean {
    return check(VERSION_CODES.JELLY_BEAN_MR1)
  }

  /**
   * November 2012: Android 4.2, Moar jelly beans!, API>=17
   */
  fun isAndroid42(): Boolean {
    return check(VERSION_CODES.JELLY_BEAN_MR1)
  }

  /**
   * July 2013: Android 4.3, the revenge of the beans., API>=18
   */
  fun isJellyBeanMR2(): Boolean {
    return check(VERSION_CODES.JELLY_BEAN_MR2)
  }

  /**
   * July 2013: Android 4.3, the revenge of the beans., API>=18
   */
  fun isAndroid43(): Boolean {
    return check(VERSION_CODES.JELLY_BEAN_MR2)
  }

  /**
   * October 2013: Android 4.4, KitKat, another tasty treat., API>=19
   */
  fun isKitKat(): Boolean {
    return check(VERSION_CODES.KITKAT)
  }

  /**
   * October 2013: Android 4.4, KitKat, another tasty treat., API>=19
   */
  fun isAndroid44(): Boolean {
    return check(VERSION_CODES.KITKAT)
  }

  /**
   * Android 4.4W: KitKat for watches, snacks on the run., API>=20
   */
  fun isKitKatWatch(): Boolean {
    return VERSION.SDK_INT >= VERSION_CODES.KITKAT_WATCH
  }

  /**
   * Lollipop.  A flat one with beautiful shadows.  But still tasty., API>=21
   */
  fun isL(): Boolean {
    return VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP
  }

  /**
   * Lollipop.  A flat one with beautiful shadows.  But still tasty., API>=21
   */
  @JvmStatic fun isLollipop(): Boolean {
    return VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP
  }

  /**
   * Lollipop.  A flat one with beautiful shadows.  But still tasty., API>=21
   */
  fun isAndroid5(): Boolean {
    return check(VERSION_CODES.LOLLIPOP)
  }

  /**
   * Lollipop with an extra sugar coating on the outside!, API>=22
   */
  fun isLollipopMR1(): Boolean {
    return VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP_MR1
  }

  /**
   * Marshmallow.  A flat one with beautiful shadows.  But still tasty., API>=23
   */
  fun isM(): Boolean {
    return check(VERSION_CODES.M)
  }

  /**
   * Marshmallow.  A flat one with beautiful shadows.  But still tasty., API>=23
   */
  fun isMarshmallow(): Boolean {
    return check(VERSION_CODES.M)
  }

  /**
   * Marshmallow.  A flat one with beautiful shadows.  But still tasty., API>=23
   */
  fun isAndroid6(): Boolean {
    return check(VERSION_CODES.M)
  }

  fun isN(): Boolean {
    return check(VERSION_CODES.N)
  }

  fun isNougat(): Boolean {
    return check(VERSION_CODES.N)
  }

  fun isAndroid7(): Boolean {
    return check(VERSION_CODES.N)
  }

  fun isNMR1(): Boolean {
    return check(VERSION_CODES.N_MR1)
  }

  fun isNougatMR1(): Boolean {
    return check(VERSION_CODES.N_MR1)
  }

  fun isO(): Boolean {
    return check(VERSION_CODES.O)
  }

  fun isOreo(): Boolean {
    return check(VERSION_CODES.O)
  }

  fun isAndroid8(): Boolean {
    return check(VERSION_CODES.O)
  }

  fun isOMR1(): Boolean {
    return check(VERSION_CODES.O_MR1)
  }

  fun isOreoMR1(): Boolean {
    return check(VERSION_CODES.O_MR1)
  }
}