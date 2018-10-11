package com.sunplus.bluetoothhelper.manager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.util.Log;

import com.sunplus.bluetoothhelper.common.Utils;
import com.sunplus.bluetoothhelper.profile.FwA2dpProfile;
import com.sunplus.bluetoothhelper.profile.FwBluetoothProfileManager;
import java.util.Set;

/**
 * Created by w.feng on 2018/9/20
 * Email: w.feng@sunmedia.com.cn
 */
public class FwBluetoothAdapter {
    private static final String TAG = "FwBluetoothAdapter";

    private final BluetoothAdapter mAdapter;

    private static FwBluetoothAdapter sInstance;

    private FwBluetoothProfileManager mProfileManager;

    private int mState = BluetoothAdapter.ERROR;

    private static final int SCAN_EXPIRATION_MS = 5 * 60 * 1000;

    private long mLastScan;

    private FwBluetoothAdapter(BluetoothAdapter adapter) {
        mAdapter = adapter;
    }

    public void setProfileManager(FwBluetoothProfileManager manager) {
        mProfileManager = manager;
    }

    static synchronized FwBluetoothAdapter getInstance() {
        if (sInstance == null) {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null) {
                sInstance = new FwBluetoothAdapter(adapter);
            }
        }

        return sInstance;
    }

    public void cancelDiscovery() {
        mAdapter.cancelDiscovery();
    }

    public boolean enable() {
        return mAdapter.enable();
    }

    public void getProfileProxy(Context context,
                                BluetoothProfile.ServiceListener listener, int profile) {
        mAdapter.getProfileProxy(context, listener, profile);
    }

    public Set<BluetoothDevice> getBondedDevices() {
        return mAdapter.getBondedDevices();
    }

    public String getName() {
        return mAdapter.getName();
    }

    public int getScanMode() {
        return mAdapter.getScanMode();
    }

    public BluetoothLeScanner getBluetoothLeScanner() {
        return mAdapter.getBluetoothLeScanner();
    }

    public int getState() {
        return mAdapter.getState();
    }

    public boolean isDiscovering() {
        return mAdapter.isDiscovering();
    }

    public boolean isEnabled() {
        return mAdapter.isEnabled();
    }

    public int getConnectionState() {
        return 0;
    }


    public void setName(String name) {
        mAdapter.setName(name);
    }

    public void startScanning(boolean force) {
        if (!mAdapter.isDiscovering()) {
            if (!force) {
                if (mLastScan + SCAN_EXPIRATION_MS > System.currentTimeMillis()) {
                    return;
                }

                FwA2dpProfile a2dp = mProfileManager.getA2dpProfile();
                if (a2dp != null && a2dp.isA2dpPlaying()) {
                    return;

                }

                if (mAdapter.startDiscovery()) {
                    mLastScan = System.currentTimeMillis();
                }
            }
        }
    }


    public void stopScanning() {
        if (mAdapter.isDiscovering()) {
            mAdapter.cancelDiscovery();
        }
    }

    public synchronized int getBluetoothState() {
        syncBluetoothState();
        return mState;
    }

    synchronized void setBluetoothStateInt(int state) {
        mState = state;

        if (state == BluetoothAdapter.STATE_ON) {
            if (mProfileManager != null) {
                mProfileManager.setBluetoothStateOn();
            }
        }
    }

    boolean syncBluetoothState() {
        int currentState = mAdapter.getState();
        if (currentState != mState) {
            setBluetoothStateInt(mAdapter.getState());
            return true;
        }

        return false;
    }

    public boolean setBluetoothEnabled(boolean enabled) {
        boolean success = enabled
                ? mAdapter.enable()
                : mAdapter.disable();

        if (success) {
            setBluetoothStateInt(enabled
                    ? BluetoothAdapter.STATE_TURNING_ON
                    : BluetoothAdapter.STATE_TURNING_OFF);
        } else {
            if (Utils.V) {
                Log.v(TAG, "setBluetoothEnabled call, manager didn't return " +
                        "success for enabled: " + enabled);
            }

            syncBluetoothState();
        }
        return success;
    }

    public BluetoothDevice getRemoteDevice(String address) {
        return mAdapter.getRemoteDevice(address);
    }


}
