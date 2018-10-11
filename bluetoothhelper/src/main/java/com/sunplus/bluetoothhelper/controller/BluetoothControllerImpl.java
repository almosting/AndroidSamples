package com.sunplus.bluetoothhelper.controller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.sunplus.bluetoothhelper.manager.FwBluetoothManager;
import com.sunplus.bluetoothhelper.manager.FwCachedBluetoothDevice;
import com.sunplus.bluetoothhelper.manager.IBluetoothCallback;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.WeakHashMap;

/**
 * Created by w.feng on 2018/9/21
 * Email: w.feng@sunmedia.com.cn
 */
public class BluetoothControllerImpl implements BluetoothController, IBluetoothCallback, FwCachedBluetoothDevice.Callback {
    private static final String TAG = "BluetoothControllerImpl";

    private final FwBluetoothManager mFwBluetoothManager;
    private final WeakHashMap<FwCachedBluetoothDevice, ActuallyCachedState> mCachedState = new WeakHashMap<>();
    private final Handler mBgHandler;

    private boolean mEnabled;
    private int mConnectionState = BluetoothAdapter.STATE_DISCONNECTED;
    private FwCachedBluetoothDevice mLastDevice;

    private final H mHandler = new H(Looper.getMainLooper());

    private int mState;

    public BluetoothControllerImpl(Context context, Looper bgLooper) {
        mFwBluetoothManager = FwBluetoothManager.getInstance(context, null);
        mBgHandler = new Handler(bgLooper);
        if (mFwBluetoothManager != null) {
            mFwBluetoothManager.getEventManager().setReceiverHandler(mBgHandler);
            mFwBluetoothManager.getEventManager().registerCallback(this);
            onBluetoothStateChanged(mFwBluetoothManager.getBluetoothAdapter().getBluetoothState());
        }
    }

    @Override
    public boolean isBluetoothSupported() {
        return mFwBluetoothManager != null;
    }

    @Override
    public boolean isBluetoothEnabled() {
        return mEnabled;
    }

    @Override
    public int getBluetoothState() {
        return mState;
    }

    @Override
    public boolean isBluetoothConnected() {
        return mConnectionState == BluetoothAdapter.STATE_CONNECTED;
    }

    @Override
    public boolean isBluetoothConnecting() {
        return mConnectionState == BluetoothAdapter.STATE_CONNECTING;
    }

    @Override
    public String getLastDeviceName() {
        return mLastDevice != null ? mLastDevice.getName() : null;
    }

    @Override
    public void setBluetoothEnabled(boolean enabled) {
        if (mFwBluetoothManager != null) {
            mFwBluetoothManager.getBluetoothAdapter().setBluetoothEnabled(enabled);
        }
    }

    @Override
    public Collection<FwCachedBluetoothDevice> getDevices() {
        return mFwBluetoothManager != null
                ? mFwBluetoothManager.getCachedDeviceManager().getCachedDevicesCopy()
                : null;
    }

    @Override
    public void connect(FwCachedBluetoothDevice device) {
        if (mFwBluetoothManager == null || device == null) {
            return;
        }
        device.connect(true);
    }

    @Override
    public void disconnect(FwCachedBluetoothDevice device) {
        if (mFwBluetoothManager == null || device == null) {
            return;
        }
        device.disconnect();
    }

    @Override
    public boolean canConfigBluetooth() {
        return false;
    }

    @Override
    public int getMaxConnectionState(FwCachedBluetoothDevice device) {
        return getCachedState(device).mMaxConnectionState;
    }

    @Override
    public int getBondState(FwCachedBluetoothDevice device) {
        return getCachedState(device).mBondState;
    }

    @Override
    public void addCallback(Callback cb) {
        mHandler.obtainMessage(H.MSG_ADD_CALLBACK, cb).sendToTarget();
        mHandler.sendEmptyMessage(H.MSG_STATE_CHANGED);
    }

    @Override
    public void removeCallback(Callback cb) {
        mHandler.obtainMessage(H.MSG_REMOVE_CALLBACK, cb).sendToTarget();
    }


    private void updateConnected() {
        int state = mFwBluetoothManager.getBluetoothAdapter().getConnectionState();
        if (mLastDevice != null && !mLastDevice.isConnected()) {
            mLastDevice = null;
        }

        for (FwCachedBluetoothDevice device : getDevices()) {
            int maxDeviceState = device.getMaxConnectionState();
            if (maxDeviceState > state) {
                state = maxDeviceState;
            }
            if (mLastDevice == null && device.isConnected()) {
                mLastDevice = device;
            }
        }

        if (mLastDevice == null && state == BluetoothAdapter.STATE_CONNECTED) {
            state = BluetoothAdapter.STATE_DISCONNECTED;
        }

        if (state != mConnectionState) {
            mConnectionState = state;
            mHandler.sendEmptyMessage(H.MSG_STATE_CHANGED);
        }

    }


    @Override
    public void onDeviceAttributesChanged() {
        updateConnected();
        mHandler.sendEmptyMessage(H.MSG_PAIRED_DEVICES_CHANGED);
    }

    @Override
    public void onBluetoothStateChanged(int bluetoothState) {
        mEnabled = bluetoothState == BluetoothAdapter.STATE_ON
                || bluetoothState == BluetoothAdapter.STATE_TURNING_ON;
        mState = bluetoothState;
        mHandler.sendEmptyMessage(H.MSG_STATE_CHANGED);
    }

    @Override
    public void onScanningStateChanged(boolean started) {
        //TODO
    }

    @Override
    public void onDeviceAdded(FwCachedBluetoothDevice cachedDevice) {
        cachedDevice.registerCallback(this);
        updateConnected();
        mHandler.sendEmptyMessage(H.MSG_PAIRED_DEVICES_CHANGED);
    }


    @Override
    public void onDeviceDeleted(FwCachedBluetoothDevice cachedDevice) {
        mCachedState.remove(cachedDevice);
        updateConnected();
        mHandler.sendEmptyMessage(H.MSG_PAIRED_DEVICES_CHANGED);
    }

    @Override
    public void onDeviceBondStateChanged(FwCachedBluetoothDevice cachedDevice, int bondState) {
        mCachedState.remove(cachedDevice);
        updateConnected();
        mHandler.sendEmptyMessage(H.MSG_PAIRED_DEVICES_CHANGED);
    }

    @Override
    public void onConnectionStateChanged(FwCachedBluetoothDevice cachedDevice, int state) {
        mCachedState.remove(cachedDevice);
        mLastDevice = cachedDevice;
        updateConnected();
        mHandler.sendEmptyMessage(H.MSG_STATE_CHANGED);
    }

    private ActuallyCachedState getCachedState(FwCachedBluetoothDevice device) {
        ActuallyCachedState state = mCachedState.get(device);
        if (state == null) {
            state = new ActuallyCachedState(device, mHandler);
            mBgHandler.post(state);
            mCachedState.put(device, state);
            return state;
        }
        return state;
    }


    private static class ActuallyCachedState implements Runnable {

        private final WeakReference<FwCachedBluetoothDevice> mDevice;
        private final Handler mUiHandler;
        private int mBondState = BluetoothDevice.BOND_NONE;
        private int mMaxConnectionState = BluetoothProfile.STATE_DISCONNECTED;

        private ActuallyCachedState(FwCachedBluetoothDevice device, Handler uiHandler) {
            mDevice = new WeakReference<>(device);
            mUiHandler = uiHandler;
        }

        @Override
        public void run() {
            mBondState = mDevice.get().getBondState();
            mMaxConnectionState = mDevice.get().getMaxConnectionState();
            mUiHandler.removeMessages(H.MSG_PAIRED_DEVICES_CHANGED);
            mUiHandler.sendEmptyMessage(H.MSG_PAIRED_DEVICES_CHANGED);
        }
    }


    private final class H extends Handler {
        private final ArrayList<BluetoothController.Callback> mCallbacks = new ArrayList<>();

        private static final int MSG_PAIRED_DEVICES_CHANGED = 1;
        private static final int MSG_STATE_CHANGED = 2;
        private static final int MSG_ADD_CALLBACK = 3;
        private static final int MSG_REMOVE_CALLBACK = 4;

        public H(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PAIRED_DEVICES_CHANGED:
                    firePairedDevicesChanged();
                    break;
                case MSG_STATE_CHANGED:
                    fireStateChange();
                    break;
                case MSG_ADD_CALLBACK:
                    mCallbacks.add((BluetoothController.Callback) msg.obj);
                    break;
                case MSG_REMOVE_CALLBACK:
                    mCallbacks.remove((BluetoothController.Callback) msg.obj);
                    break;
                default:
                    break;
            }
        }

        private void firePairedDevicesChanged() {
            for (BluetoothController.Callback cb : mCallbacks) {
                cb.onBluetoothDevicesChanged();
            }
        }

        private void fireStateChange() {
            for (BluetoothController.Callback cb : mCallbacks) {
                fireStateChange(cb);
            }
        }

        private void fireStateChange(BluetoothController.Callback cb) {
            cb.onBluetoothStateChange(mEnabled);
        }
    }

}
