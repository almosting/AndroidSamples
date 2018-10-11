package com.sunplus.bluetoothhelper.profile;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.sunplus.bluetoothhelper.common.Utils;
import com.sunplus.bluetoothhelper.manager.FwBluetoothAdapter;
import com.sunplus.bluetoothhelper.manager.FwBluetoothEventManager;
import com.sunplus.bluetoothhelper.manager.FwCachedBluetoothDevice;
import com.sunplus.bluetoothhelper.manager.FwCachedBluetoothDeviceManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by w.feng on 2018/9/20
 * Email: w.feng@sunmedia.com.cn
 */
public class FwBluetoothProfileManager {
    private static final String TAG = "BluetoothProfileManager";
    private static final boolean DEBUG = Utils.D;

    private static FwBluetoothProfileManager sInstance;

    public interface ServiceListener {
        void onServiceConnected();

        void onServiceDisconnected();

    }

    private final Context mContext;
    private final FwBluetoothAdapter mAdapter;
    private final FwCachedBluetoothDeviceManager mDeviceManager;
    private final FwBluetoothEventManager mEventManager;

    private FwA2dpProfile mA2dpProfile;
    private FwHeadsetProfile mHeadsetProfile;


    private final Map<String, IFwBluetoothProfile> mProfileMap = new HashMap<>();

    public FwBluetoothProfileManager(Context context, FwBluetoothAdapter adapter,
                                     FwCachedBluetoothDeviceManager deviceManager, FwBluetoothEventManager eventManager) {
        mContext = context;
        mAdapter = adapter;
        mDeviceManager = deviceManager;
        mEventManager = eventManager;

        mAdapter.setProfileManager(this);
        mEventManager.setProfileManager(this);
        updateLocalProfiles();

        if (DEBUG) {
            Log.d(TAG, "LocalBluetoothProfileManager construction complete");
        }

    }

    private void updateLocalProfiles() {
        mA2dpProfile = new FwA2dpProfile(mContext, mAdapter, mDeviceManager, this);
        addProfile(mA2dpProfile, FwA2dpProfile.NAME,
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);

        mHeadsetProfile = new FwHeadsetProfile(mContext, mAdapter, mDeviceManager, this);
        addProfile(mHeadsetProfile, FwHeadsetProfile.NAME, BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);

    }

    private final Collection<ServiceListener> mServiceListeners = new ArrayList<>();

    private void addProfile(IFwBluetoothProfile profile,
                            String profileName, String stateChangedAction) {
        mEventManager.addProfileHandler(stateChangedAction, new StateChangedHandler(profile));
        mProfileMap.put(profileName, profile);
    }

    public IFwBluetoothProfile getProfileByName(String name) {
        return mProfileMap.get(name);
    }


    public void setBluetoothStateOn() {
        updateLocalProfiles();
        mEventManager.readPairedDevices();
    }

    private class StateChangedHandler implements FwBluetoothEventManager.Handler {
        final IFwBluetoothProfile mProfile;

        StateChangedHandler(IFwBluetoothProfile profile) {
            mProfile = profile;
        }

        @Override
        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            FwCachedBluetoothDevice cachedDevice = mDeviceManager.findDevice(device);
            if (cachedDevice == null) {
                Log.w(TAG, "StateChangedHandler found new device: " + device);
                cachedDevice = mDeviceManager.addDevice(mAdapter,
                        FwBluetoothProfileManager.this, device);
            }
            int newState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, 0);
            int oldState = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, 0);
            if (newState == BluetoothProfile.STATE_DISCONNECTED &&
                    oldState == BluetoothProfile.STATE_CONNECTING) {
                Log.i(TAG, "Failed to connect " + mProfile + " device");
            }

            cachedDevice.onProfileStateChanged(mProfile, newState);
            cachedDevice.refresh();
        }
    }

    public void addServiceListener(ServiceListener l) {
        mServiceListeners.add(l);
    }

    public void removeServiceListener(ServiceListener l) {
        mServiceListeners.remove(l);
    }

    public void callServiceConnectedListeners() {
        for (ServiceListener l : mServiceListeners) {
            l.onServiceConnected();
        }
    }

    public void callServiceDisconnectedListeners() {
        for (ServiceListener listener : mServiceListeners) {
            listener.onServiceDisconnected();
        }
    }

    public synchronized boolean isManagerReady() {
        IFwBluetoothProfile profile = mHeadsetProfile;
        if (profile != null) {
            return profile.isProfileReady();
        }

        profile = mA2dpProfile;
        return profile != null && profile.isProfileReady();

    }

    public FwA2dpProfile getA2dpProfile() {
        return mA2dpProfile;
    }

    public FwHeadsetProfile getHeadsetProfile() {
        return mHeadsetProfile;
    }

    public synchronized void updateProfiles(Collection<IFwBluetoothProfile> profiles, Collection<IFwBluetoothProfile>
            removedProfiles, BluetoothDevice device) {
        removedProfiles.clear();
        removedProfiles.addAll(profiles);
        if (DEBUG) {
            Log.d(TAG, "Current Profiles" + profiles.toString());
        }
        profiles.clear();

        if (mHeadsetProfile != null) {
            profiles.add(mHeadsetProfile);
            removedProfiles.remove(mHeadsetProfile);
        }

        if (mA2dpProfile != null) {
            profiles.add(mA2dpProfile);
            removedProfiles.remove(mA2dpProfile);
        }

        if (DEBUG) {
            Log.d(TAG, "New Profiles" + profiles.toString());
        }

    }
}
