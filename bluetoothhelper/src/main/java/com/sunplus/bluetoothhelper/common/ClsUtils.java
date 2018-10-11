package com.sunplus.bluetoothhelper.common;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.util.Log;

import com.sunplus.bluetoothhelper.profile.IFwBluetoothProfile;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ClsUtils {

  public static boolean createBond(Class<?> btClass, BluetoothDevice btDevice) throws Exception {
    Method createBondMethod = btClass.getMethod("createBond");
    Boolean returnValue = (Boolean) createBondMethod.invoke(btDevice);
    return returnValue;
  }

  public static boolean removeBond(Class<?> btClass, BluetoothDevice btDevice) throws Exception {
    Method removeBondMethod = btClass.getMethod("removeBond");
    return (Boolean) removeBondMethod.invoke(btDevice);
  }

  public static boolean setPin(Class<? extends BluetoothDevice> btClass, BluetoothDevice
      btDevice, String str)
      throws Exception {
    try {
      Method removeBondMethod = btClass.getDeclaredMethod("setPin", byte[]
          .class);
      Boolean returnValue = (Boolean) removeBondMethod.invoke(btDevice, new Object[] {
          str
              .getBytes()
      });
      Log.e("returnValue", "" + returnValue);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return true;
  }

  public static boolean cancelPairingUserInput(Class<?> btClass, BluetoothDevice device) throws
      Exception {
    Method createBondMethod = btClass.getMethod("cancelPairingUserInput");
    // cancelBondProcess(btClass, device);
    Boolean returnValue = (Boolean) createBondMethod.invoke(device);
    return returnValue;
  }

  public static boolean cancelBondProcess(Class<?> btClass, BluetoothDevice device)

      throws Exception {
    Method createBondMethod = btClass.getMethod("cancelBondProcess");
    Boolean returnValue = (Boolean) createBondMethod.invoke(device);
    return returnValue;
  }

  public static void setPairingConfirmation(Class<?> btClass, BluetoothDevice device, boolean
      isConfirm)
      throws Exception {
    Method setPairingConfirmation = btClass.getDeclaredMethod("setPairingConfirmation",
        boolean.class);
    setPairingConfirmation.invoke(device, isConfirm);
  }

  /**
   * @param clsShow
   */
  public static void printAllInform(Class<?> clsShow) {
    try {

      Method[] hideMethod = clsShow.getMethods();
      int i = 0;
      for (; i < hideMethod.length; i++) {
        Log.e("method name", hideMethod[i].getName() + ";and the i is:" + i);
      }

      Field[] allFields = clsShow.getFields();
      for (i = 0; i < allFields.length; i++) {
        Log.e("Field name", allFields[i].getName());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static boolean connect(BluetoothProfile profile, BluetoothDevice device) throws Exception {
    try {
      Class<?> clazz = Class.forName(profile.getClass().getName());
      Method method = clazz.getMethod("connect", BluetoothDevice.class);
      Object o = method.invoke(clazz, device);
      return o != null && (boolean) o;
    } catch (Exception e) {
      e.printStackTrace();
    }

    return false;
  }

  public static boolean disconnect(BluetoothProfile profile, BluetoothDevice device)
      throws Exception {
    try {
      Class<?> clazz = Class.forName(profile.getClass().getName());
      Method method = clazz.getMethod("disconnect", BluetoothDevice.class);
      Object o = method.invoke(clazz, device);
      return o != null && (boolean) o;
    } catch (Exception e) {
      e.printStackTrace();
    }

    return false;
  }

  public static boolean setPriority(BluetoothProfile profile, BluetoothDevice device, int priority)
      throws Exception {
    try {
      Class<?> clazz = Class.forName(profile.getClass().getName());
      Method method = clazz.getMethod("setPriority", BluetoothDevice.class, int.class);
      Object[] objects = new Object[] { device, priority };
      Object o = method.invoke(clazz, objects);
      return o != null && (boolean) o;
    } catch (Exception e) {
      e.printStackTrace();
    }

    return false;
  }

  public static int getPriority(BluetoothProfile profile, BluetoothDevice device) throws Exception {
    try {
      Class<?> clazz = Class.forName(profile.getClass().getName());
      Method method = clazz.getMethod("getPriority", BluetoothDevice.class);
      Object o = method.invoke(clazz, device);
      return (o instanceof Integer) ? (Integer) o : IFwBluetoothProfile.PRIORITY_OFF;
    } catch (Exception e) {
      e.printStackTrace();
    }

    return IFwBluetoothProfile.PRIORITY_OFF;
  }
}
