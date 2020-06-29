package com.sunplus.toolbox.utils

import android.util.Log
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.LinkedBlockingQueue

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
abstract class MessageTask : Runnable {
  class TaskBreak : RuntimeException()
  class Request {
    var request: Int
    var arg1 = 0
    var arg2 = 0
    var obj: Object? = null
    var requestForResult: Int
    var result: Object? = null
      set(value) = synchronized(this) {
        field = value
        requestForResult = REQUEST_TASK_NON
        request = requestForResult
        (this as Object).notifyAll()
      }

    constructor() {
      requestForResult = REQUEST_TASK_NON
      request = requestForResult
    }

    /**
     * @param request minus value is reserved internal use
     */
    constructor(request: Int, arg1: Int, arg2: Int, obj: Object?) {
      this.request = request
      this.arg1 = arg1
      this.arg2 = arg2
      this.obj = obj
      requestForResult = REQUEST_TASK_NON
    }

    override fun equals(o: Any?): Boolean {
      return if (o is Request) request == (o as Request?)!!.request
          && requestForResult == (o as Request?)!!.requestForResult
          && arg1 == (o as Request?)!!.arg1
          && arg2 == (o as Request?)!!.arg2
          && obj === (o as Request?)!!.obj else super.equals(
        o
      )
    }
  }

  private val mSync: Object = Object()

  /** 池/队列大小，如果为-1则无限制  */
  private val mMaxRequest: Int
  private val mRequestPool // FIXME これはArrayListにした方が速いかも
      : LinkedBlockingQueue<Request?>?
  private val mRequestQueue: LinkedBlockingDeque<Request?>?

  @Volatile
  private var mIsRunning = false

  @Volatile
  private var mFinished = false
  private var mWorkerThread: Thread? = null

  /**
   * 构造函数
   * 无限大小的池和队列
   * 池创建为空
   */
  protected constructor() {
    mMaxRequest = -1
    mRequestPool =
      LinkedBlockingQueue()
    mRequestQueue =
      LinkedBlockingDeque()
  }

  /**
   * 构造函数
   * 无限大小的池和队列
   *
   * @param initNum 　指定要合并的初始请求数
   */
  constructor(initNum: Int) {
    mMaxRequest = -1
    mRequestPool =
      LinkedBlockingQueue()
    mRequestQueue =
      LinkedBlockingDeque()
    for (i in 0 until initNum) {
      if (!mRequestPool.offer(Request())) {
        break
      }
    }
  }

  /**
   * 构造函数
   * 通过指定可以合并和排队的最大大小进行初始化
   *
   * @param maxRequest 指定最大队列大小
   * @param initNum 指定要池化的初始请求数，如果大于max_request则截断
   */
  constructor(maxRequest: Int, initNum: Int) {
    mMaxRequest = maxRequest
    mRequestPool =
      LinkedBlockingQueue(
        maxRequest
      )
    mRequestQueue =
      LinkedBlockingDeque(
        maxRequest
      )
    for (i in 0 until initNum) {
      if (!mRequestPool.offer(Request())) {
        break
      }
    }
  }

  /**
   * 初始化请求，继承类构造函数调用
   * 参数传递给onInit
   */
  protected fun init(arg1: Int, arg2: Int, obj: Object?) {
    mFinished = false
    mRequestQueue!!.offer(obtain(REQUEST_TASK_START, arg1, arg2, obj))
    //		offer(REQUEST_TASK_START, arg1, arg2, obj);
  }

  /** 初期化处理  */
  protected abstract fun onInit(arg1: Int, arg2: Int, obj: Any?)

  /** 在请求处理循环开始之前调用  */
  protected abstract fun onStart()

  /** 在onStop之前立即调用，在中断时不调用  */
  protected open fun onBeforeStop() {}

  /** 停止处理，中断时不调用  */
  protected abstract fun onStop()

  /** 在onStop之后调用，即使onStop上发生异常，也会调用它  */
  protected abstract fun onRelease()

  /**
   * 在消息处理循环中发生错误时进行处理
   * 默认值返回true并结束消息处理循环
   *
   * @return 返回true会终止消息处理循环
   */
  protected open fun onError(e: Exception?): Boolean {
    return true
  }

  /**
   * 处理请求消息（内部消息未到来）
   * 抛出TaskBreak时，请求消息处理循环终止
   */
  @Throws(TaskBreak::class) protected abstract fun processRequest(
    request: Int, arg1: Int, arg2: Int,
    obj: Object?
  ): Object?

  /** 处理以检索请求消息（如果没有请求消息则被阻止）  */
  @Throws(InterruptedException::class)
  protected open fun takeRequest(): Request? {
    return mRequestQueue!!.take()
  }

  fun waitReady(): Boolean {
    synchronized(mSync) {
      while (!mIsRunning && !mFinished) {
        try {
          mSync.wait(500)
        } catch (e: InterruptedException) {
          break
        }
      }
      return mIsRunning
    }
  }

  fun isRunning(): Boolean {
    return mIsRunning
  }

  fun isFinished(): Boolean {
    return mFinished
  }

  override fun run() {
    var request: Request? = null
    mIsRunning = true
    try {
      request = mRequestQueue!!.take()
    } catch (e: InterruptedException) {
      mIsRunning = false
      mFinished = true
    }
    synchronized(mSync) {
      if (mIsRunning) {
        mWorkerThread = Thread.currentThread()
        try {
          if (request != null) {
            onInit(request!!.arg1, request!!.arg2, request!!.obj)
          }
        } catch (e: Exception) {
          Log.w(TAG, e)
          mIsRunning = false
          mFinished = true
        }
      }
      mSync.notifyAll()
    }
    if (mIsRunning) {
      try {
        onStart()
      } catch (e: Exception) {
        if (callOnError(e)) {
          mIsRunning = false
          mFinished = true
        }
      }
    }
    LOOP@ while (mIsRunning) {
      try {
        request = takeRequest()
        when (request!!.request) {
          REQUEST_TASK_NON -> {
          }
          REQUEST_TASK_QUIT -> break@LOOP
          REQUEST_TASK_RUN -> if (request.obj is Runnable) {
            try {
              (request.obj as Runnable?)!!.run()
            } catch (e: Exception) {
              if (callOnError(e)) {
                break@LOOP
              }
            }
          }
          REQUEST_TASK_RUN_AND_WAIT -> try {
            request.result = processRequest(
              request.requestForResult, request.arg1, request.arg2,
              request.obj
            )
          } catch (e: TaskBreak) {
            request.result = null
            break@LOOP
          } catch (e: Exception) {
            request.result = null
            if (callOnError(e)) {
              break@LOOP
            }
          }
          else -> try {
            processRequest(request.request, request.arg1, request.arg2, request.obj)
          } catch (e: TaskBreak) {
            break@LOOP
          } catch (e: Exception) {
            if (callOnError(e)) {
              break@LOOP
            }
          }
        }
        request.requestForResult = REQUEST_TASK_NON
        request.request = request.requestForResult
        // 把它归还给池
        mRequestPool!!.offer(request)
      } catch (e: InterruptedException) {
        break
      }
    }
    val interrupted = Thread.interrupted()
    synchronized(mSync) {
      mWorkerThread = null
      mIsRunning = false
      mFinished = true
    }
    if (!interrupted) {
      try {
        onBeforeStop()
        onStop()
      } catch (e: Exception) {
        callOnError(e)
      }
    }
    try {
      onRelease()
    } catch (e: Exception) {
      // callOnError(e);
    }
    synchronized(mSync) { mSync.notifyAll() }
  }

  /**
   * 错误处理，调用onError。
   * 返回true会终止请求消息处理循环
   */
  protected fun callOnError(e: Exception?): Boolean {
    try {
      return onError(e)
    } catch (e2: Exception) {
      //			if (DEBUG) Log.e(TAG, "exception occurred in callOnError", e);
    }
    return true
  }

  /**
   * 从请求池中获取请求
   * 如果池为空，请创建一个新池
   *
   * @param request minus values and zero are reserved
   * @return Request
   */
  private fun obtain(
    request: Int,
    arg1: Int,
    arg2: Int,
    obj: Object?
  ): Request? {
    var req = mRequestPool!!.poll()
    if (req != null) {
      req.request = request
      req.arg1 = arg1
      req.arg2 = arg2
      req.obj = obj
    } else {
      req = Request(request, arg1, arg2, obj)
    }
    return req
  }

  /**
   * offer request to run on worker thread
   *
   * @param request minus values and zero are reserved
   * @return true if success offer
   */
  fun offer(request: Int, arg1: Int, arg2: Int, obj: Object?): Boolean {
    return !mFinished && mRequestQueue!!.offer(obtain(request, arg1, arg2, obj))
  }

  /**
   * offer request to run on worker thread
   *
   * @param request minus values and zero are reserved
   * @return true if success offer
   */
  fun offer(request: Int, arg1: Int, obj: Object?): Boolean {
    return !mFinished && mRequestQueue!!.offer(obtain(request, arg1, 0, obj))
  }

  /**
   * offer request to run on worker thread
   *
   * @param request minus values and zero are reserved
   * @return true if success offer
   */
  fun offer(request: Int, arg1: Int, arg2: Int): Boolean {
    return !mFinished && mIsRunning && mRequestQueue!!.offer(obtain(request, arg1, arg2, null))
  }

  /**
   * offer request to run on worker thread
   *
   * @param request minus values and zero are reserved
   * @return true if success offer
   */
  fun offer(request: Int, arg1: Int): Boolean {
    return !mFinished && mIsRunning && mRequestQueue!!.offer(obtain(request, arg1, 0, null))
  }

  /**
   * offer request to run on worker thread
   *
   * @param request minus values and zero are reserved
   * @return true if success offer
   */
  fun offer(request: Int): Boolean {
    return !mFinished && mIsRunning && mRequestQueue!!.offer(obtain(request, 0, 0, null))
  }

  /**
   * offer request to run on worker thread
   *
   * @param request minus values and zero are reserved
   * @return true if success offer
   */
  private fun offer(request: Int, obj: Object?): Boolean {
    return !mFinished && mIsRunning && mRequestQueue!!.offer(obtain(request, 0, 0, obj))
  }

  /**
   * offer request to run on worker thread on top of the request queue
   *
   * @param request minus values and zero are reserved
   */
  fun offerFirst(request: Int, arg1: Int, arg2: Int, obj: Object?): Boolean {
    return !mFinished && mIsRunning && mRequestQueue!!.offerFirst(obtain(request, arg1, arg2, obj))
  }

  /**
   * offer request to run on worker thread and wait for result
   * caller thread is blocked until the request finished running on worker thread
   * FIXME在正在运行的线程上调用MessageTask时，此方法会发生死锁
   */
  fun offerAndWait(request: Int, arg1: Int, arg2: Int, obj: Object?): Object? {
    return if (!mFinished && request > REQUEST_TASK_NON) {
      val req =
        obtain(REQUEST_TASK_RUN_AND_WAIT, arg1, arg2, obj)
      synchronized(req as Object) {
        req!!.requestForResult = request
        req.result = null
        mRequestQueue!!.offer(req)
        while (mIsRunning && req.requestForResult != REQUEST_TASK_NON) {
          try {
            req.wait(100)
          } catch (e: InterruptedException) {
            break
          }
        }
      }
      req.result
    } else {
      null
    }
  }

  /**
   * request to run on worker thread
   *
   * @return true if success queue
   */
  fun queueEvent(task: Runnable?): Boolean {
    return !mFinished && task != null && offer(REQUEST_TASK_RUN, task as Object)
  }

  fun removeRequest(request: Request?) {
    if (mRequestQueue != null) {
      for (req in mRequestQueue) {
        if (!mIsRunning || mFinished) {
          break
        }
        if (req == request) {
          mRequestQueue!!.remove(req)
          mRequestPool!!.offer(req)
        }
      }
    }
  }

  fun removeRequest(request: Int) {
    if (mRequestQueue != null) {
      for (req in mRequestQueue) {
        if (!mIsRunning || mFinished) {
          break
        }
        if (req!!.request == request) {
          mRequestQueue!!.remove(req)
          mRequestPool!!.offer(req)
        }
      }
    }
  }
  /**
   * request terminate worker thread and release all related resources
   *
   * @param interrupt 如果为true，则中断执行任务
   */
  /**
   * request terminate worker thread and release all related resources
   */
  @JvmOverloads fun release(interrupt: Boolean = false) {
    val b = mIsRunning
    mIsRunning = false
    if (!mFinished) {
      mRequestQueue!!.clear()
      mRequestQueue.offerFirst(obtain(REQUEST_TASK_QUIT, 0, 0, null))
      synchronized(mSync) {
        if (b) {
          val current = Thread.currentThread().id
          val id = mWorkerThread?.getId() ?: current
          if (id != current) {
            if (interrupt && mWorkerThread != null) {
              mWorkerThread!!.interrupt()
            }
            while (!mFinished) {
              try {
                mSync.wait(300)
              } catch (e: InterruptedException) {
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
  fun releaseSelf() {
    mIsRunning = false
    if (!mFinished) {
      mRequestQueue!!.clear()
      mRequestQueue.offerFirst(obtain(REQUEST_TASK_QUIT, 0, 0, null))
    }
  }

  /**
   * Helper方法，用于极大地终止processRequest中的消息循环
   * 只需简单地抛出TaskBreak
   *
   * @throws TaskBreak
   */
  @Throws(TaskBreak::class) fun userBreak() {
    throw TaskBreak()
  }

  companion object {
    private val TAG = MessageTask::class.java.simpleName

    // minus values and zero are reserved for internal use
    private const val REQUEST_TASK_NON = 0
    private const val REQUEST_TASK_RUN = -1
    private const val REQUEST_TASK_RUN_AND_WAIT = -2
    private const val REQUEST_TASK_START = -8
    private const val REQUEST_TASK_QUIT = -9
  }
}