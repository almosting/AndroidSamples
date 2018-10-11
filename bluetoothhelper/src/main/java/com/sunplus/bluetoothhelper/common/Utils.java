package com.sunplus.bluetoothhelper.common;

import android.bluetooth.BluetoothProfile;
import android.content.Context;

/**
 * Created by w.feng on 2018/9/20
 * Email: w.feng@sunmedia.com.cn
 */
public class Utils {
    public static final boolean V = false;
    public static final boolean D = true;

    private static ErrorListener sErrorListener;

    public static int getConnectionStateSummary(int connectionState) {
        switch (connectionState) {
            case BluetoothProfile.STATE_CONNECTED:
                return 0;
            case BluetoothProfile.STATE_CONNECTING:
                return 0;
            case BluetoothProfile.STATE_DISCONNECTED:
                return 0;
            case BluetoothProfile.STATE_DISCONNECTING:
                return 0;
            default:
                return 0;
        }
    }

    public static void showError(Context context, String name, int messageResId) {
        if (sErrorListener != null) {
            sErrorListener.onShowError(context, name, messageResId);
        }
    }

    public static void setErrorListener(ErrorListener listener) {
        sErrorListener = listener;
    }

    public interface ErrorListener {
        void onShowError(Context context, String name, int messageResId);
    }

}
