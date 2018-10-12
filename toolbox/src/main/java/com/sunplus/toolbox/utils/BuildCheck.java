package com.sunplus.toolbox.utils;

import android.os.Build;

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */

public final class BuildCheck {

  private static boolean check(final int value) {
    return (Build.VERSION.SDK_INT >= value);
  }

  /**
   * Magic version number for a current development build,
   * which has not yet turned into an official release. API=10000
   */
  public static boolean isCurrentDevelopment() {
    return (Build.VERSION.SDK_INT == Build.VERSION_CODES.CUR_DEVELOPMENT);
  }

  /**
   * October 2011: Android 4.0., API>=14
   */
  public static boolean isIcecreamSandwich() {
    return check(Build.VERSION_CODES.ICE_CREAM_SANDWICH);
  }

  /**
   * October 2011: Android 4.0., API>=14
   */
  public static boolean isAndroid4() {
    return check(Build.VERSION_CODES.ICE_CREAM_SANDWICH);
  }

  /**
   * December 2011: Android 4.0.3., API>=15
   */
  public static boolean isIcecreamSandwichMR1() {
    return check(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1);
  }

  /**
   * December 2011: Android 4.0.3., API>=15
   */
  public static boolean isAndroid403() {
    return check(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1);
  }

  /**
   * June 2012: Android 4.1., API>=16
   */
  public static boolean isJellyBean() {
    return check(Build.VERSION_CODES.JELLY_BEAN);
  }

  /**
   * June 2012: Android 4.1., API>=16
   */
  public static boolean isAndroid41() {
    return check(Build.VERSION_CODES.JELLY_BEAN);
  }

  /**
   * November 2012: Android 4.2, Moar jelly beans!, API>=17
   */
  public static boolean isJellyBeanMr1() {
    return check(Build.VERSION_CODES.JELLY_BEAN_MR1);
  }

  /**
   * November 2012: Android 4.2, Moar jelly beans!, API>=17
   */
  public static boolean isAndroid42() {
    return check(Build.VERSION_CODES.JELLY_BEAN_MR1);
  }

  /**
   * July 2013: Android 4.3, the revenge of the beans., API>=18
   */
  public static boolean isJellyBeanMR2() {
    return check(Build.VERSION_CODES.JELLY_BEAN_MR2);
  }

  /**
   * July 2013: Android 4.3, the revenge of the beans., API>=18
   */
  public static boolean isAndroid43() {
    return check(Build.VERSION_CODES.JELLY_BEAN_MR2);
  }

  /**
   * October 2013: Android 4.4, KitKat, another tasty treat., API>=19
   */
  public static boolean isKitKat() {
    return check(Build.VERSION_CODES.KITKAT);
  }

  /**
   * October 2013: Android 4.4, KitKat, another tasty treat., API>=19
   */
  public static boolean isAndroid44() {
    return check(Build.VERSION_CODES.KITKAT);
  }

  /**
   * Android 4.4W: KitKat for watches, snacks on the run., API>=20
   */
  public static boolean isKitKatWatch() {
    return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH);
  }

  /**
   * Lollipop.  A flat one with beautiful shadows.  But still tasty., API>=21
   */
  public static boolean isL() {
    return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
  }

  /**
   * Lollipop.  A flat one with beautiful shadows.  But still tasty., API>=21
   */
  public static boolean isLollipop() {
    return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
  }

  /**
   * Lollipop.  A flat one with beautiful shadows.  But still tasty., API>=21
   */
  public static boolean isAndroid5() {
    return check(Build.VERSION_CODES.LOLLIPOP);
  }

  /**
   * Lollipop with an extra sugar coating on the outside!, API>=22
   */
  public static boolean isLollipopMR1() {
    return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1);
  }

  /**
   * Marshmallow.  A flat one with beautiful shadows.  But still tasty., API>=23
   */
  public static boolean isM() {
    return check(Build.VERSION_CODES.M);
  }

  /**
   * Marshmallow.  A flat one with beautiful shadows.  But still tasty., API>=23
   */
  public static boolean isMarshmallow() {
    return check(Build.VERSION_CODES.M);
  }

  /**
   * Marshmallow.  A flat one with beautiful shadows.  But still tasty., API>=23
   */
  public static boolean isAndroid6() {
    return check(Build.VERSION_CODES.M);
  }


  public static boolean isN() {
    return check(Build.VERSION_CODES.N);
  }


  public static boolean isNougat() {
    return check(Build.VERSION_CODES.N);
  }


  public static boolean isAndroid7() {
    return check(Build.VERSION_CODES.N);
  }


  public static boolean isNMR1() {
    return check(Build.VERSION_CODES.N_MR1);
  }


  public static boolean isNougatMR1() {
    return check(Build.VERSION_CODES.N_MR1);
  }


  public static boolean isO() {
    return check(Build.VERSION_CODES.O);
  }


  public static boolean isOreo() {
    return check(Build.VERSION_CODES.O);
  }

  public static boolean isAndroid8() {
    return check(Build.VERSION_CODES.O);
  }

  public static boolean isOMR1() {
    return check(Build.VERSION_CODES.O_MR1);
  }


  public static boolean isOreoMR1() {
    return check((Build.VERSION_CODES.O_MR1));
  }
}
