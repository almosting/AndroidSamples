package com.sunplus.bluetoothhelper.profile;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;
import com.sunplus.bluetoothhelper.common.ClsUtils;
import com.sunplus.bluetoothhelper.manager.FwBluetoothAdapter;
import com.sunplus.bluetoothhelper.manager.FwCachedBluetoothDevice;
import com.sunplus.bluetoothhelper.manager.FwCachedBluetoothDeviceManager;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by w.feng on 2018/9/20
 * Email: w.feng@sunmedia.com.cn
 */
public class FwHeadsetProfile implements IFwBluetoothProfile {

    private static final String TAG = "FwHeadsetProfile";

    private static boolean V = true;

    private BluetoothHeadset mService;
    private boolean mIsProfileReady;

    private final FwBluetoothAdapter mLocalAdapter;
    private final FwCachedBluetoothDeviceManager mDeviceManager;
    private final FwBluetoothProfileManager mProfileManager;

    static final String NAME = "HEADSET";

    private static final int ORDINAL = 0;

    private final class HeadsetServiceListener
            implements BluetoothProfile.ServiceListener {

        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (V) {
                Log.d(TAG, "Bluetooth service connected");
            }
            mService = (BluetoothHeadset) proxy;
            // We just bound to the service, so refresh the UI for any connected HFP devices.
            List<BluetoothDevice> deviceList = mService.getConnectedDevices();
            while (!deviceList.isEmpty()) {
                BluetoothDevice nextDevice = deviceList.remove(0);
                FwCachedBluetoothDevice device = mDeviceManager.findDevice(nextDevice);
                // we may add a new device here, but generally this should not happen
                if (device == null) {
                    Log.w(TAG, "HeadsetProfile found new device: " + nextDevice);
                    device = mDeviceManager.addDevice(mLocalAdapter, mProfileManager, nextDevice);
                }
                device.onProfileStateChanged(FwHeadsetProfile.this,
                        BluetoothProfile.STATE_CONNECTED);
                device.refresh();
            }

            mProfileManager.callServiceConnectedListeners();
            mIsProfileReady = true;
        }

        @Override
        public void onServiceDisconnected(int profile) {
            if (V) {
                Log.d(TAG, "Bluetooth service disconnected");
            }
            mProfileManager.callServiceDisconnectedListeners();
            mIsProfileReady = false;
        }
    }

    public FwHeadsetProfile(Context context, FwBluetoothAdapter adapter,
                            FwCachedBluetoothDeviceManager deviceManager,
                            FwBluetoothProfileManager profileManager) {
        mLocalAdapter = adapter;
        mDeviceManager = deviceManager;
        mProfileManager = profileManager;
        mLocalAdapter.getProfileProxy(context, new HeadsetServiceListener(),
                BluetoothProfile.HEADSET);
    }


    @Override
    public boolean isConnectable() {
        return true;
    }

    @Override
    public boolean isAutoConnectable() {
        return true;
    }

    @Override
    public boolean connect(BluetoothDevice device) {
        if (mService == null) {
            return false;
        }
        List<BluetoothDevice> sinks = mService.getConnectedDevices();
        if (sinks != null) {
            for (BluetoothDevice sink : sinks) {
                Log.d(TAG, "Not disconnecting device = " + sink);
            }
        }

        try {
            return ClsUtils.connect(mService, device);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean disconnect(BluetoothDevice device) {
        if (mService == null) {
            return false;
        }
        List<BluetoothDevice> deviceList = mService.getConnectedDevices();
        if (!deviceList.isEmpty()) {
            for (BluetoothDevice dev : deviceList) {
                if (dev.equals(device)) {
                    if (V) {
                        Log.d(TAG, "Downgrade priority as user" +
                                "is disconnecting the headset");
                    }
                    try {
                        if (ClsUtils.getPriority(mService, device) > IFwBluetoothProfile.PRIORITY_ON) {
                            ClsUtils.setPriority(mService, device, IFwBluetoothProfile.PRIORITY_ON);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        return ClsUtils.disconnect(mService, device);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        }
        return false;
    }

    @Override
    public int getConnectState(BluetoothDevice device) {
        if (mService == null) {
            return BluetoothProfile.STATE_DISCONNECTED;
        }
        List<BluetoothDevice> deviceList = mService.getConnectedDevices();
        if (!deviceList.isEmpty()) {
            for (BluetoothDevice dev : deviceList) {
                if (dev.equals(device)) {
                    return mService.getConnectionState(device);
                }
            }
        }
        return BluetoothProfile.STATE_DISCONNECTED;
    }

    @Override
    public boolean isPreferred(BluetoothDevice device) {
        if (mService == null) {
            return false;
        }

        try {
            return ClsUtils.getPriority(mService, device) > IFwBluetoothProfile.PRIORITY_OFF;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public int getPreferred(BluetoothDevice device) {
        if (mService == null) {
            return IFwBluetoothProfile.PRIORITY_OFF;
        }
        try {
            return ClsUtils.getPriority(mService, device);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return IFwBluetoothProfile.PRIORITY_OFF;
    }

    @Override
    public void setPreferred(BluetoothDevice device, boolean preferred) {
        if (mService == null) {
            return;
        }
        try {
            if (preferred) {

                if (ClsUtils.getPriority(mService, device) < IFwBluetoothProfile.PRIORITY_ON) {
                    ClsUtils.setPriority(mService, device, IFwBluetoothProfile.PRIORITY_ON);
                }

            } else {
                ClsUtils.setPriority(mService, device, IFwBluetoothProfile.PRIORITY_OFF);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public List<BluetoothDevice> getConnectedDevices() {
        if (mService == null) {
            return new ArrayList<>(0);
        }
        return mService.getDevicesMatchingConnectionStates(
                new int[]{BluetoothProfile.STATE_CONNECTED,
                        BluetoothProfile.STATE_CONNECTING,
                        BluetoothProfile.STATE_DISCONNECTING});
    }

    @Override
    public String toString() {
        return NAME;
    }

    public int getOrdinal() {
        return ORDINAL;
    }


    @Override
    public boolean isProfileReady() {
        return mIsProfileReady;
    }

    @Override
    protected void finalize() {
        if (V) {
            Log.d(TAG, "finalize()");
        }
        if (mService != null) {
            try {
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(BluetoothProfile.HEADSET,
                        mService);
                mService = null;
            } catch (Throwable t) {
                Log.w(TAG, "Error cleaning up HID proxy", t);
            }
        }
    }
}
