package com.sunplus.bluetoothhelper.profile;

import android.bluetooth.BluetoothDevice;

/**
 * Created by w.feng on 2018/9/20
 * Email: w.feng@sunmedia.com.cn
 */
public interface IFwBluetoothProfile {
    int PRIORITY_ON = 100;

    int PRIORITY_OFF = 0;

    boolean isConnectable();

    boolean isAutoConnectable();

    boolean connect(BluetoothDevice device);

    boolean disconnect(BluetoothDevice device);

    int getConnectState(BluetoothDevice device);

    boolean isPreferred(BluetoothDevice device);

    int getPreferred(BluetoothDevice device);

    void setPreferred(BluetoothDevice device, boolean preferred);

    boolean isProfileReady();



}
