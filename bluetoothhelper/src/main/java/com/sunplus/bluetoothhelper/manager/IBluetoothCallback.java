package com.sunplus.bluetoothhelper.manager;

/**
 * Created by w.feng on 2018/9/20
 * Email: w.feng@sunmedia.com.cn
 */
public interface IBluetoothCallback {
    void onBluetoothStateChanged(int bluetoothState);

    void onScanningStateChanged(boolean started);

    void onDeviceAdded(FwCachedBluetoothDevice cachedDevice);

    void onDeviceDeleted(FwCachedBluetoothDevice cachedDevice);

    void onDeviceBondStateChanged(FwCachedBluetoothDevice cachedDevice, int bondState);

    void onConnectionStateChanged(FwCachedBluetoothDevice cachedDevice, int state);
}
