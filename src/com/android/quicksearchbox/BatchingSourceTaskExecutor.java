/*
 * Copyright (C) 2010 The Android Open Source Project
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

import java.util.ArrayList;
import java.util.List;

/**
 * Executes SourceTasks in batches of a predefined size.  Tasks in excess of
 * the batch size are queued until the caller indicates that more results
 * are required.
 */
class BatchingSourceTaskExecutor implements SourceTaskExecutor {

    private static final boolean DBG = true;
    private static final String TAG = "QSB.BatchingSourceTaskExecutor";

    private final SourceTaskExecutor mExecutor;

    private final int mBatchSize;

    /** Queue of tasks waiting to be dispatched to mExecutor **/
    private final ArrayList<SourceTask> mQueuedTasks = new ArrayList<SourceTask>();

    /** Count of tasks already dispatched to mExecutor in this batch **/
    private int mDispatchedCount;

    /**
     * Creates a new BatchingSourceTaskExecutor.
     *
     * @param executor A SourceTaskExecutor for actually executing the tasks.
     * @param batchSize The number of tasks to submit in each batch.
     */
    public BatchingSourceTaskExecutor(SourceTaskExecutor executor, int batchSize) {
        mExecutor = executor;
        mBatchSize = batchSize;
    }

    public void execute(SourceTask task) {
        synchronized (mQueuedTasks) {
            if (mDispatchedCount == mBatchSize) {
                if (DBG) Log.d(TAG, "Queueing " + task);
                mQueuedTasks.add(task);
                return;
            }
            mDispatchedCount++;
        }
        // Avoid holding the lock when dispatching the task.
        dispatch(task);
    }

    private void dispatch(SourceTask task) {
        if (DBG) Log.d(TAG, "Dispatching " + task);
        mExecutor.execute(task);
    }

    /**
     * Instructs the executor to submit the next batch of results.
     */
    public void executeNextBatch() {
        SourceTask[] batch = new SourceTask[0];
        synchronized (mQueuedTasks) {
            int count = Math.min(mQueuedTasks.size(), mBatchSize);
            List<SourceTask> nextTasks = mQueuedTasks.subList(0, count);
            batch = nextTasks.toArray(batch);
            nextTasks.clear();
            mDispatchedCount = count;
            if (DBG) Log.d(TAG, "Dispatching batch of " + count);
        }

        for (SourceTask task : batch) {
            dispatch(task);
        }
    }

    /**
     * Cancel any unstarted tasks running in this executor.  This instance 
     * should not be re-used after calling this method.
     */
    public void cancelPendingTasks() {
        synchronized (mQueuedTasks) {
            mQueuedTasks.clear();
            mDispatchedCount = 0;
        }
        mExecutor.cancelPendingTasks();
    }

    public void close() {
        cancelPendingTasks();
        mExecutor.close();
    }
}
