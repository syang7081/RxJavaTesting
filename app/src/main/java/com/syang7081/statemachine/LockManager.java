package com.syang7081.statemachine;

import android.util.Log;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by syang7081 on 2/11/2018.
 */

public class LockManager {
    private static final String TAG = LockManager.class.getSimpleName();
    private static final long MAX_LOCK_INTERVAL = 30000L; // 30s
    private Semaphore semaphore = new Semaphore(1, true);
    private GlobalLock globalLock;
    private long lastLockAcquiredTime = 0;

    private static LockManager INSTANCE = new LockManager();

    private LockManager() {};

    public static LockManager getInstance() {
        return INSTANCE;
    }

    public GlobalLock acquireLock(long timeToWait /* ms */) {
        try {
            if (!semaphore.tryAcquire(timeToWait, TimeUnit.MILLISECONDS)) {
                if (System.currentTimeMillis() - lastLockAcquiredTime > MAX_LOCK_INTERVAL) {
                    // break the lock
                    Log.d(TAG, "Number of blocked threads: " + semaphore.getQueueLength());
                    semaphore.release();
                    globalLock = null;
                    if (!semaphore.tryAcquire(1, TimeUnit.SECONDS)) {
                        // Lock is acquired by another thread right after released above
                        return null;
                    }
                }
                else {
                    return null;
                }
            }
            globalLock = new GlobalLock();
            lastLockAcquiredTime = System.currentTimeMillis();
            Log.d(TAG, "Lock is granted: " + globalLock);
            return  globalLock;
        }
        catch (InterruptedException e) {
            Log.d(TAG, "Failed to acquire lock: " + Log.getStackTraceString(e));
        }
        return null;
    }

    public GlobalLock acquireLock() {
        return acquireLock(MAX_LOCK_INTERVAL);
    }

    private void releaseLock(GlobalLock globalLock) {
        if (this.globalLock == globalLock) {
            semaphore.release();
            lastLockAcquiredTime = 0;
            this.globalLock = null;
        }
    }

    public static class GlobalLock {
        public void release() {
            getInstance().releaseLock(this);
        }
    }
}
