package com.sunplus.bluetoothhelper.controller;

/**
 * Created by w.feng on 2018/9/21
 * Email: w.feng@sunmedia.com.cn
 */
public interface CallbackController<T> {
    void addCallback(T listener);

    void removeCallback(T listener);
}
