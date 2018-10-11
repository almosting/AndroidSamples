package com.sunplus.bluetoothhelper.manager;

import android.content.Context;
import com.sunplus.bluetoothhelper.profile.FwBluetoothProfileManager;

/**
 * Created by w.feng on 2018/9/20
 * Email: w.feng@sunmedia.com.cn
 */
public class FwBluetoothManager {
    private static final String TAG = "FwBluetoothManager";

    private static FwBluetoothManager sInstance;

    private final Context mContext;

    private final FwBluetoothAdapter mAdapter;

    private final FwCachedBluetoothDeviceManager mCachedDeviceManager;

    private final FwBluetoothProfileManager mProfileManager;
    private final FwBluetoothEventManager mEventManager;

    public static synchronized FwBluetoothManager getInstance(Context context, BluetoothManagerCallback onInitCallback) {
        if (sInstance == null) {
            FwBluetoothAdapter adapter = FwBluetoothAdapter.getInstance();
            if (adapter == null) {
                return null;
            }

            Context appContext = context.getApplicationContext();
            sInstance = new FwBluetoothManager(adapter, appContext);
            if (onInitCallback != null) {
                onInitCallback.onBluetoothManagerInitialized(appContext, sInstance);
            }
        }
        return sInstance;
    }

    private FwBluetoothManager(FwBluetoothAdapter adapter, Context context) {
        mContext = context;
        mAdapter = adapter;
        mCachedDeviceManager = new FwCachedBluetoothDeviceManager(context, this);
        mEventManager = new FwBluetoothEventManager(mAdapter, mCachedDeviceManager, context);
        mProfileManager = new FwBluetoothProfileManager(context, mAdapter,mCachedDeviceManager,mEventManager);
    }

    public FwBluetoothAdapter getBluetoothAdapter() {
        return mAdapter;
    }

    public Context getContext() {
        return mContext;
    }

    public FwCachedBluetoothDeviceManager getCachedDeviceManager() {
        return mCachedDeviceManager;
    }

    public FwBluetoothEventManager getEventManager() {
        return mEventManager;
    }

    public FwBluetoothProfileManager getProfileManager() {
        return mProfileManager;
    }

    public interface BluetoothManagerCallback {
        void onBluetoothManagerInitialized(Context appContext,
                                           FwBluetoothManager bluetoothManager);
    }
}
