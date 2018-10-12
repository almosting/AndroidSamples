package com.sunplus.toolbox.utils;

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
class StorageInfo {
  private long totalBytes;

  private long freeBytes;

  StorageInfo(final long total, final long free) {
    totalBytes = total;
    freeBytes = free;
  }
}
