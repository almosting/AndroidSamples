package com.sunplus.bluetoothhelper.manager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.sunplus.bluetoothhelper.common.Utils;
import com.sunplus.bluetoothhelper.profile.FwBluetoothProfileManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by w.feng on 2018/9/20
 * Email: w.feng@sunmedia.com.cn
 */
public class FwBluetoothEventManager {
    private static final String TAG = "FwBluetoothEventManager";

    private final FwBluetoothAdapter mLocalAdapter;
    private final FwCachedBluetoothDeviceManager mDeviceManager;
    private FwBluetoothProfileManager mProfileManager;
    private final IntentFilter mAdapterIntentFilter, mProfileIntentFilter;
    private final Map<String, Handler> mHandlerMap;
    private Context mContext;

    private final Collection<IBluetoothCallback> mCallbacks =
            new ArrayList<>();

    private android.os.Handler mReceiverHandler;

    public interface Handler {
        void onReceive(Context context, Intent intent, BluetoothDevice device);
    }

    private void addHandler(String action, Handler handler) {
        mHandlerMap.put(action, handler);
        mAdapterIntentFilter.addAction(action);
    }

    public void addProfileHandler(String action, Handler handler) {
        mHandlerMap.put(action, handler);
        mProfileIntentFilter.addAction(action);
    }

    public void setProfileManager(FwBluetoothProfileManager manager) {
        mProfileManager = manager;
    }

    FwBluetoothEventManager(FwBluetoothAdapter adapter, FwCachedBluetoothDeviceManager deviceManager, Context context) {
        mLocalAdapter = adapter;
        mDeviceManager = deviceManager;
        mAdapterIntentFilter = new IntentFilter();
        mProfileIntentFilter = new IntentFilter();
        mHandlerMap = new HashMap<>();
        mContext = context;
        // Bluetooth on/off broadcasts
        addHandler(BluetoothAdapter.ACTION_STATE_CHANGED, new AdapterStateChangedHandler());
        // Generic connected/not broadcast
        addHandler(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED,
                new ConnectionStateChangedHandler());

        // Discovery broadcasts
        addHandler(BluetoothAdapter.ACTION_DISCOVERY_STARTED, new ScanningStateChangedHandler(true));
        addHandler(BluetoothAdapter.ACTION_DISCOVERY_FINISHED, new ScanningStateChangedHandler(false));
        addHandler(BluetoothDevice.ACTION_FOUND, new DeviceFoundHandler());
        addHandler(BluetoothDevice.ACTION_NAME_CHANGED, new NameChangedHandler());

        // Pairing broadcasts
        addHandler(BluetoothDevice.ACTION_BOND_STATE_CHANGED, new BondStateChangedHandler());

        // Fine-grained state broadcasts
        addHandler(BluetoothDevice.ACTION_CLASS_CHANGED, new ClassChangedHandler());
        addHandler(BluetoothDevice.ACTION_UUID, new UuidChangedHandler());

        // Dock event broadcasts
        addHandler(Intent.ACTION_DOCK_EVENT, new DockEventHandler());

        mContext.registerReceiver(mBroadcastReceiver, mAdapterIntentFilter, null, mReceiverHandler);
    }

    public void registerProfileIntentReceiver() {
        mContext.registerReceiver(mBroadcastReceiver, mProfileIntentFilter, null, mReceiverHandler);
    }

    public void setReceiverHandler(android.os.Handler handler) {
        mContext.unregisterReceiver(mBroadcastReceiver);
        mReceiverHandler = handler;
        mContext.registerReceiver(mBroadcastReceiver, mAdapterIntentFilter, null, mReceiverHandler);
        registerProfileIntentReceiver();
    }

    public void registerCallback(IBluetoothCallback callback) {
        synchronized (mCallbacks) {
            mCallbacks.add(callback);
        }
    }

    public void unregisterCallback(IBluetoothCallback callback) {
        synchronized (mCallbacks) {
            mCallbacks.remove(callback);
        }
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent
                    .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            Handler handler = mHandlerMap.get(action);
            if (handler != null) {
                handler.onReceive(context, intent, device);
            }
        }
    };

    private class AdapterStateChangedHandler implements Handler {
        @Override
        public void onReceive(Context context, Intent intent,
                              BluetoothDevice device) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR);
            // update local profiles and get paired devices
            mLocalAdapter.setBluetoothStateInt(state);
            // send callback to update UI and possibly start scanning
            synchronized (mCallbacks) {
                for (IBluetoothCallback callback : mCallbacks) {
                    callback.onBluetoothStateChanged(state);
                }
            }
            // Inform CachedDeviceManager that the adapter state has changed
            mDeviceManager.onBluetoothStateChanged(state);
        }
    }

    private class ScanningStateChangedHandler implements Handler {
        private final boolean mStarted;

        ScanningStateChangedHandler(boolean started) {
            mStarted = started;
        }

        @Override
        public void onReceive(Context context, Intent intent,
                              BluetoothDevice device) {
            synchronized (mCallbacks) {
                for (IBluetoothCallback callback : mCallbacks) {
                    callback.onScanningStateChanged(mStarted);
                }
            }
            mDeviceManager.onScanningStateChanged(mStarted);
        }
    }

    private class DeviceFoundHandler implements Handler {
        @Override
        public void onReceive(Context context, Intent intent,
                              BluetoothDevice device) {
            short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
            BluetoothClass btClass = intent.getParcelableExtra(BluetoothDevice.EXTRA_CLASS);
            String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
            // TODO Pick up UUID. They should be available for 2.1 devices.
            // Skip for now, there's a bluez problem and we are not getting uuids even for 2.1.
            FwCachedBluetoothDevice cachedDevice = mDeviceManager.findDevice(device);
            if (cachedDevice == null) {
                cachedDevice = mDeviceManager.addDevice(mLocalAdapter, mProfileManager, device);
                Log.d(TAG, "DeviceFoundHandler created new CachedBluetoothDevice: "
                        + cachedDevice);
            }
            cachedDevice.setRssi(rssi);
            cachedDevice.setBtClass(btClass);
            cachedDevice.setNewName(name);
            cachedDevice.setVisible(true);
        }
    }


    private class ConnectionStateChangedHandler implements Handler {
        @Override
        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            FwCachedBluetoothDevice cachedDevice = mDeviceManager.findDevice(device);
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE,
                    BluetoothAdapter.ERROR);
            dispatchConnectionStateChanged(cachedDevice, state);
        }
    }


    private void dispatchConnectionStateChanged(FwCachedBluetoothDevice cachedDevice, int state) {
        synchronized (mCallbacks) {
            for (IBluetoothCallback callback : mCallbacks) {
                callback.onConnectionStateChanged(cachedDevice, state);
            }
        }
    }

    public void dispatchDeviceAdded(FwCachedBluetoothDevice cachedDevice) {
        synchronized (mCallbacks) {
            for (IBluetoothCallback callback : mCallbacks) {
                callback.onDeviceAdded(cachedDevice);
            }
        }
    }

    private class NameChangedHandler implements Handler {
        @Override
        public void onReceive(Context context, Intent intent,
                              BluetoothDevice device) {
            mDeviceManager.onDeviceNameUpdated(device);
        }
    }

    private class BondStateChangedHandler implements Handler {
        @Override
        public void onReceive(Context context, Intent intent,
                              BluetoothDevice device) {
            if (device == null) {
                Log.e(TAG, "ACTION_BOND_STATE_CHANGED with no EXTRA_DEVICE");
                return;
            }
            int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                    BluetoothDevice.ERROR);
            FwCachedBluetoothDevice cachedDevice = mDeviceManager.findDevice(device);
            if (cachedDevice == null) {
                Log.w(TAG, "CachedBluetoothDevice for device " + device +
                        " not found, calling readPairedDevices().");
                if (readPairedDevices()) {
                    cachedDevice = mDeviceManager.findDevice(device);
                }

                if (cachedDevice == null) {
                    Log.w(TAG, "Got bonding state changed for " + device +
                            ", but we have no record of that device.");

                    cachedDevice = mDeviceManager.addDevice(mLocalAdapter, mProfileManager, device);
                    dispatchDeviceAdded(cachedDevice);
                }
            }

            synchronized (mCallbacks) {
                for (IBluetoothCallback callback : mCallbacks) {
                    callback.onDeviceBondStateChanged(cachedDevice, bondState);
                }
            }
            cachedDevice.onBondingStateChanged(bondState);

            if (bondState == BluetoothDevice.BOND_NONE) {
                int reason = intent.getIntExtra("android.bluetooth.device.extra.REASON",
                        BluetoothDevice.ERROR);

                showUnbondMessage(context, cachedDevice.getName(), reason);
            }
        }

        private void showUnbondMessage(Context context, String name, int reason) {
            int errorMsg;


            Log.w(TAG, "showUnbondMessage: Not displaying any message for reason: " + reason);

            Utils.showError(context, name, reason);
        }
    }

    private class ClassChangedHandler implements Handler {
        @Override
        public void onReceive(Context context, Intent intent,
                              BluetoothDevice device) {
            mDeviceManager.onBtClassChanged(device);
        }
    }

    private class UuidChangedHandler implements Handler {
        @Override
        public void onReceive(Context context, Intent intent,
                              BluetoothDevice device) {
            mDeviceManager.onUuidChanged(device);
        }
    }


    private class DockEventHandler implements Handler {
        @Override
        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            // Remove if unpair device upon undocking
            int anythingButUnDocked = Intent.EXTRA_DOCK_STATE_UNDOCKED + 1;
            int state = intent.getIntExtra(Intent.EXTRA_DOCK_STATE, anythingButUnDocked);
            if (state == Intent.EXTRA_DOCK_STATE_UNDOCKED) {
                if (device != null && device.getBondState() == BluetoothDevice.BOND_NONE) {
                    FwCachedBluetoothDevice cachedDevice = mDeviceManager.findDevice(device);
                    if (cachedDevice != null) {
                        cachedDevice.setVisible(false);
                    }
                }
            }
        }
    }

    public boolean readPairedDevices() {
        Set<BluetoothDevice> bondedDevices = mLocalAdapter.getBondedDevices();
        if (bondedDevices == null) {
            return false;
        }

        boolean deviceAdded = false;
        for (BluetoothDevice device : bondedDevices) {
            FwCachedBluetoothDevice cachedDevice = mDeviceManager.findDevice(device);
            if (cachedDevice == null) {
                cachedDevice = mDeviceManager.addDevice(mLocalAdapter, mProfileManager, device);
                dispatchDeviceAdded(cachedDevice);
                deviceAdded = true;
            }
        }

        return deviceAdded;
    }


}
