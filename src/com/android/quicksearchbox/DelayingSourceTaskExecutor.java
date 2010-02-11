/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.quicksearchbox;

import android.util.Log;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A source task executor that tries to avoid starting all tasks at the same time.
 *
 */
public class DelayingSourceTaskExecutor implements SourceTaskExecutor {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.DelayingSourceTaskExecutor";

    private final TaskDelayer mDelayer;

    /**
     * Creates a new query executor.
     */
    public DelayingSourceTaskExecutor(Config config, ThreadFactory threadFactory) {
        mDelayer = new TaskDelayer(config, threadFactory);
        mDelayer.start();
    }

    public void close() {
        if (DBG) Log.d(TAG, "close()");
        mDelayer.close();
    }

    /**
     * Remove all tasks that have not yet been started.
     */
    public void cancelPendingTasks() {
        if (DBG) Log.d(TAG, "cancelPendingTasks()");
        mDelayer.cancelPendingTasks();
    }

    /**
     * Runs a task asynchronously. Does not throw {@link RejectedExecutionException}.
     *
     * @param runnable The task to run.
     */
    public void execute(SourceTask runnable) {
        mDelayer.execute(runnable);
    }

    /**
     * Thread that moves tasks from {@link #mDelayedTasks} to {@link #mExecutor}
     * then the executor is running no tasks, or after a fixed delay.
     */
    private static class TaskDelayer extends Thread implements SourceTaskExecutor {

        private final NestedExecutor mExecutor;

        private final long mDelayNanos;

        private final LinkedBlockingQueue<SourceTask> mDelayedTasks;

        private volatile boolean mClosed = false;

        public TaskDelayer(Config config, ThreadFactory threadFactory) {
            mExecutor = new NestedExecutor(config, threadFactory);
            mDelayNanos = TimeUnit.MILLISECONDS.toNanos(config.getThreadStartDelayMillis());
            mDelayedTasks = new LinkedBlockingQueue<SourceTask>();
        }

        public void execute(SourceTask sourceTask) {
            mDelayedTasks.add(sourceTask);
        }

        /**
         * Cancels all tasks that have not yet been sent to the executor.
         */
        public void cancelPendingTasks() {
            mDelayedTasks.clear();
            mExecutor.cancelPendingTasks();
            interrupt();
        }

        /**
         * Shuts down.
         */
        public void close() {
            mClosed = true;
            cancelPendingTasks();
            mExecutor.close();
        }

        @Override
        public void run() {
            while (!mClosed) {
                try {
                    // Wait until there are no running tasks, or the timeout expires.
                    mExecutor.waitUntilNoRunningTasks(mDelayNanos);
                    // Grab the next task.
                    Runnable r = mDelayedTasks.take();  // TODO: I've seen this throw IllegalMonitorStateException
                    // Run/queue it in the thread pool
                    mExecutor.execute(r);
                    // Wait for some task to start, so that we don't add multiple
                    // tasks for each one that finishes.
                    mExecutor.waitUntilSomeRunning();
                } catch (InterruptedException ex) {
                    // if close() was called, the loop will exit
                    // if cancelPendingTasks() was called, we should keep going
                }
            }
        }
    }

    /**
     * A thread pool executor that has an unbounded work queue, and uses a fixed
     * maximum number of worker thread.
     */
    private static class NestedExecutor extends ThreadPoolSourceTaskExecutor {

        private int mRunningTaskCount = 0;
        private final ReentrantLock runningCountLock = new ReentrantLock();
        private final Condition noRunning = runningCountLock.newCondition();
        private final Condition someRunning = runningCountLock.newCondition();

        public NestedExecutor(Config config, ThreadFactory threadFactory) {
            super(config, threadFactory);
        }


        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            if (DBG) Log.d(TAG, "afterExecute(" + r + ")");

            /* From ThreadPoolExecutor:
            *
            * To properly nest multiple overridings, subclasses
            * should generally invoke {@code super.afterExecute} at the
            * beginning of this method.
            */
            super.afterExecute(r, t);

            if (t != null) {
                Log.e(TAG, r + " failed", t);
            }

            taskFinished();
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            if (DBG) Log.d(TAG, "beforeExecute(" + r + ")");

            taskStarted();

            /* From ThreadPoolExecutor:
             *
             * To properly nest multiple overridings, subclasses
             * should generally invoke {@code super.beforeExecute} at the end of
             * this method.
             */
            super.beforeExecute(t, r);
        }

        /**
         * Increments the count of running tasks, and signals one of the threads waiting
         * on {@link #someRunning}.
         */
        private void taskStarted() {
            runningCountLock.lock();
            try {
                ++mRunningTaskCount;
                someRunning.signal();
            } finally {
                runningCountLock.unlock();
            }
        }

        /**
         * Decrements the count of running tasks and signals one of the threads waiting on
         * on {@link #noRunning} if the count reaches zero.
         */
        private void taskFinished() {
            runningCountLock.lock();
            try {
                if (--mRunningTaskCount <= 0) {
                    noRunning.signal();
                }
            } finally {
                runningCountLock.unlock();
            }
        }

        /**
         * Blocks until {@code mRunningTaskCount <= 0}, waiting for at most
         * {@code timeoutNanos}.
         *
         * @param timeoutNanos The maximum time to wait, in nanoseconds.
         * @return {@code false} if the timeout has expired upon return, else {@code true}.
         *         That is, when {@code true} is returned, it is likely that no tasks are running.
         *         This is not guaranteed however, since a thread may start running after this
         *         method reads the count of running tasks.
         */
        public boolean waitUntilNoRunningTasks(long timeoutNanos) throws InterruptedException {
            runningCountLock.lock();
            try {
                while (mRunningTaskCount > 0 && timeoutNanos > 0) {
                    timeoutNanos = noRunning.awaitNanos(timeoutNanos);
                }
                return timeoutNanos > 0;
            } finally {
                runningCountLock.unlock();
            }
        }

        /**
         * Blocks until there is some task running.
         */
        public void waitUntilSomeRunning() throws InterruptedException {
            runningCountLock.lock();
            try {
                while (mRunningTaskCount <= 0) {
                    someRunning.await();
                }
            } finally {
                runningCountLock.unlock();
            }
        }

        /**
         * Checks whether there are any running tasks.
         */
        public boolean hasRunningTasks() {
            runningCountLock.lock();
            try {
                return mRunningTaskCount > 0;
            } finally {
                runningCountLock.unlock();
            }
        }
    }

}
