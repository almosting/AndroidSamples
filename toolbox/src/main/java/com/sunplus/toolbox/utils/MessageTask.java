package com.sunplus.toolbox.utils;

import android.util.Log;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
public abstract class MessageTask implements Runnable {
  private static final String TAG = MessageTask.class.getSimpleName();

  public static class TaskBreak extends RuntimeException {
  }

  protected static final class Request {
    int request;
    int arg1;
    int arg2;
    Object obj;
    int request_for_result;
    Object result;

    private Request() {
      request = request_for_result = REQUEST_TASK_NON;
    }

    /**
     * @param _request minus value is reserved internal use
     */
    public Request(final int _request, final int _arg1, final int _arg2, final Object _obj) {
      request = _request;
      arg1 = _arg1;
      arg2 = _arg2;
      obj = _obj;
      request_for_result = REQUEST_TASK_NON;
    }

    public void setResult(final Object result) {
      synchronized (this) {
        this.result = result;
        request = request_for_result = REQUEST_TASK_NON;
        notifyAll();
      }
    }

    @Override
    public boolean equals(final Object o) {
      return (o instanceof Request)
          ? (request == ((Request) o).request)
          && (request_for_result == ((Request) o).request_for_result)
          && (arg1 == ((Request) o).arg1)
          && (arg2 == ((Request) o).arg2)
          && (obj == ((Request) o).obj)
          : super.equals(o);
    }
  }

  // minus values and zero are reserved for internal use
  protected static final int REQUEST_TASK_NON = 0;
  protected static final int REQUEST_TASK_RUN = -1;
  protected static final int REQUEST_TASK_RUN_AND_WAIT = -2;
  protected static final int REQUEST_TASK_START = -8;
  protected static final int REQUEST_TASK_QUIT = -9;

  private final Object mSync = new Object();
  /** 池/队列大小，如果为-1则无限制 */
  private final int mMaxRequest;
  private final LinkedBlockingQueue<Request> mRequestPool;  // FIXME これはArrayListにした方が速いかも
  private final LinkedBlockingDeque<Request> mRequestQueue;
  private volatile boolean mIsRunning, mFinished;
  private Thread mWorkerThread;

  /**
   * 构造函数
   * 无限大小的池和队列
   * 池创建为空
   */
  public MessageTask() {
    mMaxRequest = -1;
    mRequestPool = new LinkedBlockingQueue<Request>();
    mRequestQueue = new LinkedBlockingDeque<Request>();
  }

  /**
   * 构造函数
   * 无限大小的池和队列
   *
   * @param init_num 　指定要合并的初始请求数
   */
  public MessageTask(final int init_num) {
    mMaxRequest = -1;
    mRequestPool = new LinkedBlockingQueue<Request>();
    mRequestQueue = new LinkedBlockingDeque<Request>();
    for (int i = 0; i < init_num; i++) {
      if (!mRequestPool.offer(new Request())) break;
    }
  }

  /**
   * 构造函数
   * 通过指定可以合并和排队的最大大小进行初始化
   *
   * @param max_request 指定最大队列大小
   * @param init_num 指定要池化的初始请求数，如果大于max_request则截断
   */
  public MessageTask(final int max_request, final int init_num) {
    mMaxRequest = max_request;
    mRequestPool = new LinkedBlockingQueue<Request>(max_request);
    mRequestQueue = new LinkedBlockingDeque<Request>(max_request);
    for (int i = 0; i < init_num; i++) {
      if (!mRequestPool.offer(new Request())) {
        break;
      }
    }
  }

  /**
   * 初始化请求，继承类构造函数调用
   * 参数传递给onInit
   */
  protected void init(final int arg1, final int arg2, final Object obj) {
    mFinished = false;
    mRequestQueue.offer(obtain(REQUEST_TASK_START, arg1, arg2, obj));
    //		offer(REQUEST_TASK_START, arg1, arg2, obj);
  }

  /** 初期化处理 */
  protected abstract void onInit(final int arg1, final int arg2, final Object obj);

  /** 在请求处理循环开始之前调用 */
  protected abstract void onStart();

  /** 在onStop之前立即调用，在中断时不调用 */
  protected void onBeforeStop() {
  }

  /** 停止处理，中断时不调用 */
  protected abstract void onStop();

  /** 在onStop之后调用，即使onStop上发生异常，也会调用它 */
  protected abstract void onRelease();

  /**
   * 在消息处理循环中发生错误时进行处理
   * 默认值返回true并结束消息处理循环
   *
   * @return 返回true会终止消息处理循环
   */
  protected boolean onError(final Exception e) {
    //		if (DEBUG) Log.w(TAG, e);
    return true;
  }

  /**
   * 处理请求消息（内部消息未到来）
   * 抛出TaskBreak时，请求消息处理循环终止
   */
  protected abstract Object processRequest(final int request, final int arg1, final int arg2,
                                           final Object obj) throws TaskBreak;

  /** 处理以检索请求消息（如果没有请求消息则被阻止） */
  protected Request takeRequest() throws InterruptedException {
    return mRequestQueue.take();
  }

  public boolean waitReady() {
    synchronized (mSync) {
      for (; !mIsRunning && !mFinished; ) {
        try {
          mSync.wait(500);
        } catch (final InterruptedException e) {
          break;
        }
      }
      return mIsRunning;
    }
  }

  public boolean isRunning() {
    return mIsRunning;
  }

  public boolean isFinished() {
    return mFinished;
  }

  @Override
  public void run() {
    Request request = null;
    mIsRunning = true;
    try {
      request = mRequestQueue.take();
    } catch (final InterruptedException e) {
      mIsRunning = false;
      mFinished = true;
    }
    synchronized (mSync) {
      if (mIsRunning) {
        mWorkerThread = Thread.currentThread();
        try {
          onInit(request.arg1, request.arg2, request.obj);
        } catch (final Exception e) {
          Log.w(TAG, e);
          mIsRunning = false;
          mFinished = true;
        }
      }
      mSync.notifyAll();
    }
    if (mIsRunning) {
      try {
        onStart();
      } catch (final Exception e) {
        if (callOnError(e)) {
          mIsRunning = false;
          mFinished = true;
        }
      }
    }
    LOOP:
    for (; mIsRunning; ) {
      try {
        request = takeRequest();
        switch (request.request) {
          case REQUEST_TASK_NON:
            break;
          case REQUEST_TASK_QUIT:
            break LOOP;
          case REQUEST_TASK_RUN:
            if (request.obj instanceof Runnable) {
              try {
                ((Runnable) request.obj).run();
              } catch (final Exception e) {
                if (callOnError(e)) {
                  break LOOP;
                }
              }
            }
            break;
          case REQUEST_TASK_RUN_AND_WAIT:
            try {
              request.setResult(
                  processRequest(request.request_for_result, request.arg1, request.arg2,
                      request.obj));
            } catch (final TaskBreak e) {
              request.setResult(null);
              break LOOP;
            } catch (final Exception e) {
              request.setResult(null);
              if (callOnError(e)) {
                break LOOP;
              }
            }
            break;
          default:
            try {
              processRequest(request.request, request.arg1, request.arg2, request.obj);
            } catch (final TaskBreak e) {
              break LOOP;
            } catch (final Exception e) {
              if (callOnError(e)) {
                break LOOP;
              }
            }
            break;
        }
        request.request = request.request_for_result = REQUEST_TASK_NON;
        // 把它归还给池
        mRequestPool.offer(request);
      } catch (final InterruptedException e) {
        break;
      }
    }
    final boolean interrupted = Thread.interrupted();
    synchronized (mSync) {
      mWorkerThread = null;
      mIsRunning = false;
      mFinished = true;
    }
    if (!interrupted) {
      try {
        onBeforeStop();
        onStop();
      } catch (final Exception e) {
        callOnError(e);
      }
    }
    try {
      onRelease();
    } catch (final Exception e) {
      // callOnError(e);
    }
    synchronized (mSync) {
      mSync.notifyAll();
    }
  }

  /**
   * 错误处理，调用onError。
   * 返回true会终止请求消息处理循环
   */
  protected boolean callOnError(final Exception e) {
    try {
      return onError(e);
    } catch (final Exception e2) {
      //			if (DEBUG) Log.e(TAG, "exception occurred in callOnError", e);
    }
    return true;
  }

  /**
   * 从请求池中获取请求
   * 如果池为空，请创建一个新池
   *
   * @param request minus values and zero are reserved
   * @return Request
   */
  protected Request obtain(final int request, final int arg1, final int arg2, final Object obj) {
    Request req = mRequestPool.poll();
    if (req != null) {
      req.request = request;
      req.arg1 = arg1;
      req.arg2 = arg2;
      req.obj = obj;
    } else {
      req = new Request(request, arg1, arg2, obj);
    }
    return req;
  }

  /**
   * offer request to run on worker thread
   *
   * @param request minus values and zero are reserved
   * @return true if success offer
   */
  public boolean offer(final int request, final int arg1, final int arg2, final Object obj) {
    return !mFinished && mRequestQueue.offer(obtain(request, arg1, arg2, obj));
  }

  /**
   * offer request to run on worker thread
   *
   * @param request minus values and zero are reserved
   * @return true if success offer
   */
  public boolean offer(final int request, final int arg1, final Object obj) {
    return !mFinished && mRequestQueue.offer(obtain(request, arg1, 0, obj));
  }

  /**
   * offer request to run on worker thread
   *
   * @param request minus values and zero are reserved
   * @return true if success offer
   */
  public boolean offer(final int request, final int arg1, final int arg2) {
    return !mFinished && mIsRunning && mRequestQueue.offer(obtain(request, arg1, arg2, null));
  }

  /**
   * offer request to run on worker thread
   *
   * @param request minus values and zero are reserved
   * @return true if success offer
   */
  public boolean offer(final int request, final int arg1) {
    return !mFinished && mIsRunning && mRequestQueue.offer(obtain(request, arg1, 0, null));
  }

  /**
   * offer request to run on worker thread
   *
   * @param request minus values and zero are reserved
   * @return true if success offer
   */
  public boolean offer(final int request) {
    return !mFinished && mIsRunning && mRequestQueue.offer(obtain(request, 0, 0, null));
  }

  /**
   * offer request to run on worker thread
   *
   * @param request minus values and zero are reserved
   * @return true if success offer
   */
  public boolean offer(final int request, final Object obj) {
    return !mFinished && mIsRunning && mRequestQueue.offer(obtain(request, 0, 0, obj));
  }

  /**
   * offer request to run on worker thread on top of the request queue
   *
   * @param request minus values and zero are reserved
   */
  public boolean offerFirst(final int request, final int arg1, final int arg2, final Object obj) {
    return !mFinished && mIsRunning && mRequestQueue.offerFirst(obtain(request, arg1, arg2, obj));
  }

  /**
   * offer request to run on worker thread and wait for result
   * caller thread is blocked until the request finished running on worker thread
   * FIXME在正在运行的线程上调用MessageTask时，此方法会发生死锁
   */
  public Object offerAndWait(final int request, final int arg1, final int arg2, final Object obj) {
    if (!mFinished && (request > REQUEST_TASK_NON)) {
      final Request req = obtain(REQUEST_TASK_RUN_AND_WAIT, arg1, arg2, obj);
      synchronized (req) {
        req.request_for_result = request;
        req.result = null;
        mRequestQueue.offer(req);
        for (; mIsRunning && (req.request_for_result != REQUEST_TASK_NON); ) {
          try {
            req.wait(100);
          } catch (final InterruptedException e) {
            break;
          }
        }
      }
      return req.result;
    } else {
      return null;
    }
  }

  /**
   * request to run on worker thread
   *
   * @return true if success queue
   */
  public boolean queueEvent(final Runnable task) {
    return !mFinished && (task != null) && offer(REQUEST_TASK_RUN, task);
  }

  public void removeRequest(final Request request) {
    for (final Request req : mRequestQueue) {
      if (!mIsRunning || mFinished) break;
      if (req.equals(request)) {
        mRequestQueue.remove(req);
        mRequestPool.offer(req);
      }
    }
  }

  public void removeRequest(final int request) {
    for (final Request req : mRequestQueue) {
      if (!mIsRunning || mFinished) break;
      if (req.request == request) {
        mRequestQueue.remove(req);
        mRequestPool.offer(req);
      }
    }
  }

  /**
   * request terminate worker thread and release all related resources
   */
  public void release() {
    release(false);
  }

  /**
   * request terminate worker thread and release all related resources
   *
   * @param interrupt 如果为true，则中断执行任务
   */
  public void release(final boolean interrupt) {
    final boolean b = mIsRunning;
    mIsRunning = false;
    if (!mFinished) {
      mRequestQueue.clear();
      mRequestQueue.offerFirst(obtain(REQUEST_TASK_QUIT, 0, 0, null));
      synchronized (mSync) {
        if (b) {
          final long current = Thread.currentThread().getId();
          final long id = mWorkerThread != null ? mWorkerThread.getId() : current;
          if (id != current) {
            if (interrupt && (mWorkerThread != null)) {
              mWorkerThread.interrupt();
            }
            for (; !mFinished; ) {
              try {
                mSync.wait(300);
              } catch (final InterruptedException e) {
                // ignore
              }
            }
          }
        }
      }
    }
  }

  /**
   * 结束后释放活动任务
   */
  public void releaseSelf() {
    mIsRunning = false;
    if (!mFinished) {
      mRequestQueue.clear();
      mRequestQueue.offerFirst(obtain(REQUEST_TASK_QUIT, 0, 0, null));
    }
  }

  /**
   * Helper方法，用于极大地终止processRequest中的消息循环
   * 只需简单地抛出TaskBreak
   *
   * @throws TaskBreak
   */
  public void userBreak() throws TaskBreak {
    throw new TaskBreak();
  }
}
