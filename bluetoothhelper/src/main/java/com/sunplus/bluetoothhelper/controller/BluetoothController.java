package com.sunplus.bluetoothhelper.controller;

import com.sunplus.bluetoothhelper.manager.FwCachedBluetoothDevice;
import java.util.Collection;

/**
 * Created by w.feng on 2018/9/21
 * Email: w.feng@sunmedia.com.cn
 */
public interface BluetoothController extends CallbackController<BluetoothController.Callback> {

    boolean isBluetoothSupported();

    boolean isBluetoothEnabled();

    int getBluetoothState();

    boolean isBluetoothConnected();

    boolean isBluetoothConnecting();

    String getLastDeviceName();

    void setBluetoothEnabled(boolean enabled);

    Collection<FwCachedBluetoothDevice> getDevices();

    void connect(FwCachedBluetoothDevice device);

    void disconnect(FwCachedBluetoothDevice device);

    boolean canConfigBluetooth();

    int getMaxConnectionState(FwCachedBluetoothDevice device);

    int getBondState(FwCachedBluetoothDevice device);

    public interface Callback {
        void onBluetoothStateChange(boolean enabled);

        void onBluetoothDevicesChanged();
    }
}
