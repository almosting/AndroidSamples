package com.sunplus.bluetoothhelper.profile;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
public class FwA2dpProfile implements IFwBluetoothProfile {
    private static final String TAG = "FwA2dpProfile";
    private static boolean V = false;

    private Context mContext;
    private BluetoothA2dp mService;
    private boolean mIsProfileReady;

    private final FwBluetoothAdapter mAdapter;

    static final String NAME = "A2DP";
    private final FwBluetoothProfileManager mProfileManager;
    private final FwCachedBluetoothDeviceManager mDeviceManager;
    private static final int ORDINAL = 1;

    private final class A2dpServiceListener implements BluetoothProfile.ServiceListener {

        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (V) {
                Log.d(TAG, "Bluetooth service connected");
            }
            mService = (BluetoothA2dp) proxy;
            List<BluetoothDevice> deviceList = mService.getConnectedDevices();
            while (!deviceList.isEmpty()) {
                BluetoothDevice nextDevice = deviceList.remove(0);
                FwCachedBluetoothDevice device = mDeviceManager.findDevice(nextDevice);

                if (device == null) {
                    Log.w(TAG, "A2dpProfile found new device: " + nextDevice);
                    device = mDeviceManager.addDevice(mAdapter, mProfileManager, nextDevice);
                }

                device.onProfileStateChanged(FwA2dpProfile.this, BluetoothProfile.STATE_CONNECTED);
                device.refresh();
            }

            mIsProfileReady = true;
        }

        @Override
        public void onServiceDisconnected(int profile) {
            if (V) {
                Log.d(TAG, "Bluetooth service disconnected");
            }

            mIsProfileReady = false;
        }
    }

    public FwA2dpProfile(Context context, FwBluetoothAdapter adapter, FwCachedBluetoothDeviceManager deviceManager,
                         FwBluetoothProfileManager profileManager) {
        mContext = context;
        mAdapter = adapter;
        mDeviceManager = deviceManager;
        mProfileManager = profileManager;
        mAdapter.getProfileProxy(context, new A2dpServiceListener(), BluetoothProfile.A2DP);

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
        List<BluetoothDevice> sinks = getConnectedDevices();
        if (sinks != null) {
            for (BluetoothDevice sink : sinks) {
                if (sink.equals(device)) {
                    Log.w(TAG, "Connecting to device " + device + " : disconnect skipped");
                    continue;
                }

                try {
                    ClsUtils.disconnect(mService, device);
                } catch (Exception e) {
                    e.printStackTrace();
                }
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

        return false;
    }

    @Override
    public int getConnectState(BluetoothDevice device) {
        if (mService == null) {
            return BluetoothProfile.STATE_DISCONNECTED;
        }
        return mService.getConnectionState(device);
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

    @Override
    public boolean isProfileReady() {
        return mIsProfileReady;
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


    public boolean isA2dpPlaying() {
        if (mService == null) {
            return false;
        }

        List<BluetoothDevice> sinks = mService.getConnectedDevices();
        return !sinks.isEmpty() && mService.isA2dpPlaying(sinks.get(0));

    }

    @Override
    public String toString() {
        return NAME;
    }

    public int getOrdinal() {
        return ORDINAL;
    }

    @Override
    protected void finalize() {
        if (mService != null) {
            try {
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(BluetoothProfile.A2DP, mService);
                mService = null;
            } catch (Throwable t) {
                Log.w(TAG, "Error cleaning up A2DP proxy", t);
            }
        }
    }
}
