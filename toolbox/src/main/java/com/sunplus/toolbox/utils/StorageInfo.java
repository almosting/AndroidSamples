package com.sunplus.toolbox.utils;

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
public class StorageInfo {
  public long totalBytes;
  public long freeBytes;

  public StorageInfo(final long total, final long free) {
    totalBytes = total;
    freeBytes = free;
  }
}
