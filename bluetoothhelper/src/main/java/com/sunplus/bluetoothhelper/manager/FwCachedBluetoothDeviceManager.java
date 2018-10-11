package com.sunplus.bluetoothhelper.manager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;



import com.sunplus.bluetoothhelper.common.Utils;
import com.sunplus.bluetoothhelper.profile.FwBluetoothProfileManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created by w.feng on 2018/9/20
 * Email: w.feng@sunmedia.com.cn
 */
public class FwCachedBluetoothDeviceManager {
    private static final String TAG = "BluetoothDeviceManager";
    private static final boolean DEBUG = Utils.D;

    private Context mContext;
    private final List<FwCachedBluetoothDevice> mCachedDevices =
            new ArrayList<>();
    private final FwBluetoothManager mBtManager;

    FwCachedBluetoothDeviceManager(Context context, FwBluetoothManager localBtManager) {
        mContext = context;
        mBtManager = localBtManager;
    }

    public synchronized Collection<FwCachedBluetoothDevice> getCachedDevicesCopy() {
        return new ArrayList<>(mCachedDevices);
    }

    public static boolean onDeviceDisappeared(FwCachedBluetoothDevice cachedDevice) {
        cachedDevice.setVisible(false);
        return cachedDevice.getBondState() == BluetoothDevice.BOND_NONE;
    }

    public void onDeviceNameUpdated(BluetoothDevice device) {
        FwCachedBluetoothDevice cachedDevice = findDevice(device);
        if (cachedDevice != null) {
            Log.d(TAG, "name changed:" + device.getName());
        }
    }


    public FwCachedBluetoothDevice findDevice(BluetoothDevice device) {
        for (FwCachedBluetoothDevice cachedDevice : mCachedDevices) {
            if (cachedDevice.getDevice().equals(device)) {
                return cachedDevice;
            }
        }
        return null;
    }


    public FwCachedBluetoothDevice addDevice(FwBluetoothAdapter adapter,
                                             FwBluetoothProfileManager profileManager,
                                             BluetoothDevice device) {
        FwCachedBluetoothDevice newDevice = new FwCachedBluetoothDevice(mContext, adapter,
                profileManager, device);
        synchronized (mCachedDevices) {
            mCachedDevices.add(newDevice);
            mBtManager.getEventManager().dispatchDeviceAdded(newDevice);
        }
        return newDevice;
    }


    public String getName(BluetoothDevice device) {
        FwCachedBluetoothDevice cachedDevice = findDevice(device);
        if (cachedDevice != null && cachedDevice.getName() != null) {
            return cachedDevice.getName();
        }

        String name = device.getName();
        if (name != null) {
            return name;
        }

        return device.getAddress();
    }

    public synchronized void clearNonBondedDevices() {
        for (int i = mCachedDevices.size() - 1; i >= 0; i--) {
            FwCachedBluetoothDevice cachedDevice = mCachedDevices.get(i);
            if (cachedDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
                mCachedDevices.remove(i);
            }
        }
    }

    public synchronized void onScanningStateChanged(boolean started) {
        if (!started) {
            return;
        }
        for (int i = mCachedDevices.size() - 1; i >= 0; i--) {
            FwCachedBluetoothDevice cachedDevice = mCachedDevices.get(i);
            cachedDevice.setVisible(false);
        }
    }

    public synchronized void onBtClassChanged(BluetoothDevice device) {
        FwCachedBluetoothDevice cachedDevice = findDevice(device);
        if (cachedDevice != null) {
            cachedDevice.refreshBtClass();
        }
    }

    public synchronized void onUuidChanged(BluetoothDevice device) {
        FwCachedBluetoothDevice cachedDevice = findDevice(device);
        if (cachedDevice != null) {
            Log.d(TAG, "onUuidChanged: " + Arrays.toString(device.getUuids()));
        }
    }

    public synchronized void onBluetoothStateChanged(int bluetoothState) {
        if (bluetoothState == BluetoothAdapter.STATE_TURNING_OFF) {
            for (int i = mCachedDevices.size() - 1; i >= 0; i--) {
                FwCachedBluetoothDevice cachedDevice = mCachedDevices.get(i);
                if (cachedDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
                    cachedDevice.setVisible(false);
                    mCachedDevices.remove(i);
                } else {
                    cachedDevice.clearProfileConnectionState();
                }
            }
        }
    }

    private void log(String msg) {
        if (DEBUG) {
            Log.d(TAG, msg);
        }
    }
}
