package com.sunplus.bluetoothhelper.manager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.sunplus.bluetoothhelper.common.ClsUtils;
import com.sunplus.bluetoothhelper.common.Utils;
import com.sunplus.bluetoothhelper.profile.FwA2dpProfile;
import com.sunplus.bluetoothhelper.profile.FwBluetoothProfileManager;
import com.sunplus.bluetoothhelper.profile.FwHeadsetProfile;
import com.sunplus.bluetoothhelper.profile.IFwBluetoothProfile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Created by w.feng on 2018/9/20
 * Email: w.feng@sunmedia.com.cn
 */
public class FwCachedBluetoothDevice implements Comparable<FwCachedBluetoothDevice> {
    private static final String TAG = "CachedBluetoothDevice";
    private static final boolean DEBUG = Utils.V;

    private final Context mContext;
    private final FwBluetoothAdapter mLocalAdapter;
    private final FwBluetoothProfileManager mProfileManager;
    private final BluetoothDevice mDevice;
    private String mName;
    private short mRssi;
    private BluetoothClass mBtClass;
    private HashMap<IFwBluetoothProfile, Integer> mProfileConnectionState;

    private final List<IFwBluetoothProfile> mProfiles =
            new ArrayList<>();
    private final List<IFwBluetoothProfile> mRemovedProfiles =
            new ArrayList<>();

    private boolean mLocalNapRoleConnected;

    private boolean mVisible;

    private int mMessageRejectionCount;

    private final Collection<Callback> mCallbacks = new ArrayList<Callback>();


    public final static int ACCESS_UNKNOWN = 0;

    public final static int ACCESS_ALLOWED = 1;

    public final static int ACCESS_REJECTED = 2;


    private final static int MESSAGE_REJECTION_COUNT_LIMIT_TO_PERSIST = 2;

    private final static String MESSAGE_REJECTION_COUNT_PREFS_NAME = "bluetooth_message_reject";


    private boolean mIsConnectingErrorPossible;

    private long mConnectAttempted;


    private String describe(IFwBluetoothProfile profile) {
        StringBuilder sb = new StringBuilder();
        sb.append("Address:").append(mDevice);
        if (profile != null) {
            sb.append(" Profile:").append(profile);
        }

        return sb.toString();
    }

    public void onProfileStateChanged(IFwBluetoothProfile profile, int newProfileState) {
        if (Utils.D) {
            Log.d(TAG, "onProfileStateChanged: profile " + profile +
                    " newProfileState " + newProfileState);
        }
        if (mLocalAdapter.getBluetoothState() == BluetoothAdapter.STATE_TURNING_OFF) {
            if (Utils.D) {
                Log.d(TAG, " BT Turninig Off...Profile conn state change ignored...");
            }
            return;
        }
        mProfileConnectionState.put(profile, newProfileState);
        if (newProfileState == BluetoothProfile.STATE_CONNECTED) {
            if (!mProfiles.contains(profile)) {
                mRemovedProfiles.remove(profile);
                mProfiles.add(profile);
            }
        }
    }

    FwCachedBluetoothDevice(Context context,
                            FwBluetoothAdapter adapter,
                            FwBluetoothProfileManager profileManager,
                            BluetoothDevice device) {
        mContext = context;
        mLocalAdapter = adapter;
        mProfileManager = profileManager;
        mDevice = device;
        mProfileConnectionState = new HashMap<IFwBluetoothProfile, Integer>();
        fillData();
    }

    public void disconnect() {
        for (IFwBluetoothProfile profile : mProfiles) {
            disconnect(profile);
        }

    }

    public void disconnect(IFwBluetoothProfile profile) {
        if (profile.disconnect(mDevice)) {
            if (Utils.D) {
                Log.d(TAG, "Command sent successfully:DISCONNECT " + describe(profile));
            }
        }
    }

    public void connect(boolean connectAllProfiles) {
        if (!ensurePaired()) {
            return;
        }

        mConnectAttempted = SystemClock.elapsedRealtime();
        connectWithoutResettingTimer(connectAllProfiles);
    }

    void onBondingDockConnect() {
        connect(false);
    }

    private void connectWithoutResettingTimer(boolean connectAllProfiles) {

        if (mProfiles.isEmpty()) {

            Log.d(TAG, "No profiles. Maybe we will connect later");
            return;
        }

        mIsConnectingErrorPossible = true;

        int preferredProfiles = 0;
        for (IFwBluetoothProfile profile : mProfiles) {
            if (connectAllProfiles ? profile.isConnectable() : profile.isAutoConnectable()) {
                if (profile.isPreferred(mDevice)) {
                    ++preferredProfiles;
                    connectInt(profile);
                }
            }
        }
        if (DEBUG) {
            Log.d(TAG, "Preferred profiles = " + preferredProfiles);
        }

        if (preferredProfiles == 0) {
            connectAutoConnectableProfiles();
        }
    }

    private void connectAutoConnectableProfiles() {
        if (!ensurePaired()) {
            return;
        }

        mIsConnectingErrorPossible = true;

        for (IFwBluetoothProfile profile : mProfiles) {
            if (profile.isAutoConnectable()) {
                profile.setPreferred(mDevice, true);
                connectInt(profile);
            }
        }
    }


    public void connectProfile(IFwBluetoothProfile profile) {
        mConnectAttempted = SystemClock.elapsedRealtime();

        mIsConnectingErrorPossible = true;
        connectInt(profile);
        refresh();
    }

    synchronized void connectInt(IFwBluetoothProfile profile) {
        if (!ensurePaired()) {
            return;
        }
        if (profile.connect(mDevice)) {
            if (Utils.D) {
                Log.d(TAG, "Command sent successfully:CONNECT " + describe(profile));
            }
            return;
        }
        Log.i(TAG, "Failed to connect " + profile.toString() + " to " + mName);
    }

    private boolean ensurePaired() {
        if (getBondState() == BluetoothDevice.BOND_NONE) {
            startPairing();
            return false;
        } else {
            return true;
        }
    }

    public boolean startPairing() {
        if (mLocalAdapter.isDiscovering()) {
            mLocalAdapter.cancelDiscovery();
        }

        return mDevice.createBond();
    }


    public void unpair() {
        int state = getBondState();

        if (state == BluetoothDevice.BOND_BONDING) {
            try {
                ClsUtils.cancelBondProcess(BluetoothDevice.class, mDevice);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (state != BluetoothDevice.BOND_NONE) {
            final BluetoothDevice dev = mDevice;
            if (dev != null) {
                boolean successful = false;
                try {
                    successful = ClsUtils.removeBond(BluetoothDevice.class, dev);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (successful) {
                    if (Utils.D) {
                        Log.d(TAG, "Command sent successfully:REMOVE_BOND " + describe(null));
                    }
                } else if (Utils.V) {
                    Log.v(TAG, "Framework rejected command immediately:REMOVE_BOND " +
                            describe(null));
                }
            }
        }
    }

    public int getProfileConnectionState(IFwBluetoothProfile profile) {
        if (mProfileConnectionState == null ||
                mProfileConnectionState.get(profile) == null) {

            int state = profile.getConnectState(mDevice);
            mProfileConnectionState.put(profile, state);
        }
        return mProfileConnectionState.get(profile);
    }

    public void clearProfileConnectionState() {
        if (Utils.D) {
            Log.d(TAG, " Clearing all connection state for dev:" + mDevice.getName());
        }
        for (IFwBluetoothProfile profile : getProfiles()) {
            mProfileConnectionState.put(profile, BluetoothProfile.STATE_DISCONNECTED);
        }
    }


    private void fillData() {
        fetchBtClass();
        updateProfiles();
        fetchMessageRejectionCount();

        mVisible = false;
        dispatchAttributesChanged();
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }

    public String getName() {
        return mName;
    }


    void setNewName(String name) {
        if (mName == null) {
            mName = name;
            if (mName == null || TextUtils.isEmpty(mName)) {
                mName = mDevice.getAddress();
            }
            dispatchAttributesChanged();
        }
    }


    public void refresh() {
        dispatchAttributesChanged();
    }

    public boolean isVisible() {
        return mVisible;
    }

    public void setVisible(boolean visible) {
        if (mVisible != visible) {
            mVisible = visible;
            dispatchAttributesChanged();
        }
    }

    public int getBondState() {
        return mDevice.getBondState();
    }

    void setRssi(short rssi) {
        if (mRssi != rssi) {
            mRssi = rssi;
            dispatchAttributesChanged();
        }
    }

    public boolean isConnected() {
        for (IFwBluetoothProfile profile : mProfiles) {
            int status = getProfileConnectionState(profile);
            if (status == BluetoothProfile.STATE_CONNECTED) {
                return true;
            }
        }

        return false;
    }

    public boolean isConnectedProfile(IFwBluetoothProfile profile) {
        int status = getProfileConnectionState(profile);
        return status == BluetoothProfile.STATE_CONNECTED;

    }

    public boolean isBusy() {
        for (IFwBluetoothProfile profile : mProfiles) {
            int status = getProfileConnectionState(profile);
            if (status == BluetoothProfile.STATE_CONNECTING
                    || status == BluetoothProfile.STATE_DISCONNECTING) {
                return true;
            }
        }
        return getBondState() == BluetoothDevice.BOND_BONDING;
    }


    private void fetchBtClass() {
        mBtClass = mDevice.getBluetoothClass();
    }

    private boolean updateProfiles() {

        mProfileManager.updateProfiles(mProfiles, mRemovedProfiles, mDevice);

        if (DEBUG) {
            Log.e(TAG, "updating profiles for " + mDevice.getName());
            BluetoothClass bluetoothClass = mDevice.getBluetoothClass();

            if (bluetoothClass != null) {
                Log.v(TAG, "Class: " + bluetoothClass.toString());
            }
        }
        return true;
    }


    void refreshBtClass() {
        fetchBtClass();
        dispatchAttributesChanged();
    }


    void onBondingStateChanged(int bondState) {
        if (bondState == BluetoothDevice.BOND_NONE) {
            mProfiles.clear();
            mMessageRejectionCount = 0;
            saveMessageRejectionCount();
        }

        refresh();

        if (bondState == BluetoothDevice.BOND_BONDED) {

            connect(false);
        }
    }

    void setBtClass(BluetoothClass btClass) {
        if (btClass != null && mBtClass != btClass) {
            mBtClass = btClass;
            dispatchAttributesChanged();
        }
    }

    public BluetoothClass getBtClass() {
        return mBtClass;
    }

    public List<IFwBluetoothProfile> getProfiles() {
        return Collections.unmodifiableList(mProfiles);
    }

    public List<IFwBluetoothProfile> getConnectableProfiles() {
        List<IFwBluetoothProfile> connectableProfiles =
                new ArrayList<IFwBluetoothProfile>();
        for (IFwBluetoothProfile profile : mProfiles) {
            if (profile.isConnectable()) {
                connectableProfiles.add(profile);
            }
        }
        return connectableProfiles;
    }

    public List<IFwBluetoothProfile> getRemovedProfiles() {
        return mRemovedProfiles;
    }

    public void registerCallback(Callback callback) {
        synchronized (mCallbacks) {
            mCallbacks.add(callback);
        }
    }

    public void unregisterCallback(Callback callback) {
        synchronized (mCallbacks) {
            mCallbacks.remove(callback);
        }
    }

    private void dispatchAttributesChanged() {
        synchronized (mCallbacks) {
            for (Callback callback : mCallbacks) {
                callback.onDeviceAttributesChanged();
            }
        }
    }

    @Override
    public String toString() {
        return mDevice.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FwCachedBluetoothDevice)) {
            return false;
        }
        return mDevice.equals(((FwCachedBluetoothDevice) o).mDevice);
    }

    @Override
    public int hashCode() {
        return mDevice.getAddress().hashCode();
    }


    @Override
    public int compareTo(@NonNull FwCachedBluetoothDevice another) {
        int comparison = (another.isConnected() ? 1 : 0) - (isConnected() ? 1 : 0);
        if (comparison != 0) {
            return comparison;
        }

        comparison = (another.getBondState() == BluetoothDevice.BOND_BONDED ? 1 : 0) -
                (getBondState() == BluetoothDevice.BOND_BONDED ? 1 : 0);
        if (comparison != 0) {
            return comparison;
        }


        comparison = (another.mVisible ? 1 : 0) - (mVisible ? 1 : 0);
        if (comparison != 0) {
            return comparison;
        }


        comparison = another.mRssi - mRssi;
        if (comparison != 0) {
            return comparison;
        }

        return mName.compareTo(another.mName);
    }

    public interface Callback {
        void onDeviceAttributesChanged();
    }


    public boolean checkAndIncreaseMessageRejectionCount() {
        if (mMessageRejectionCount < MESSAGE_REJECTION_COUNT_LIMIT_TO_PERSIST) {
            mMessageRejectionCount++;
            saveMessageRejectionCount();
        }
        return mMessageRejectionCount >= MESSAGE_REJECTION_COUNT_LIMIT_TO_PERSIST;
    }

    private void fetchMessageRejectionCount() {
        SharedPreferences preference = mContext.getSharedPreferences(
                MESSAGE_REJECTION_COUNT_PREFS_NAME, Context.MODE_PRIVATE);
        mMessageRejectionCount = preference.getInt(mDevice.getAddress(), 0);
    }

    private void saveMessageRejectionCount() {
        SharedPreferences.Editor editor = mContext.getSharedPreferences(
                MESSAGE_REJECTION_COUNT_PREFS_NAME, Context.MODE_PRIVATE).edit();
        if (mMessageRejectionCount == 0) {
            editor.remove(mDevice.getAddress());
        } else {
            editor.putInt(mDevice.getAddress(), mMessageRejectionCount);
        }
        editor.apply();
    }


    public int getMaxConnectionState() {
        int maxState = BluetoothProfile.STATE_DISCONNECTED;
        for (IFwBluetoothProfile profile : getProfiles()) {
            int connectionStatus = getProfileConnectionState(profile);
            if (connectionStatus > maxState) {
                maxState = connectionStatus;
            }
        }
        return maxState;
    }


    public int getConnectionSummary() {
        boolean profileConnected = false;
        boolean a2dpNotConnected = false;
        boolean hfpNotConnected = false;

        for (IFwBluetoothProfile profile : getProfiles()) {
            int connectionStatus = getProfileConnectionState(profile);

            switch (connectionStatus) {
                case BluetoothProfile.STATE_CONNECTING:
                case BluetoothProfile.STATE_DISCONNECTING:
                    return Utils.getConnectionStateSummary(connectionStatus);

                case BluetoothProfile.STATE_CONNECTED:
                    profileConnected = true;
                    break;

                case BluetoothProfile.STATE_DISCONNECTED:
                    if (profile.isProfileReady()) {
                        if (profile instanceof FwA2dpProfile) {
                            a2dpNotConnected = true;
                        } else if ((profile instanceof FwHeadsetProfile)) {
                            hfpNotConnected = true;
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        if (profileConnected) {
            if (a2dpNotConnected && hfpNotConnected) {
                return 0;
            } else if (a2dpNotConnected) {
                return 0;
            } else if (hfpNotConnected) {
                return 0;
            } else {
                return 0;
            }
        }

        return getBondState() == BluetoothDevice.BOND_BONDING ? 1 : 0;
    }

}
