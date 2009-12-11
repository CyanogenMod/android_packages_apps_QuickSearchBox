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

import android.database.DataSetObserver;
import android.os.Handler;

/**
 * Wraps a DataSetObserver, and batches up onChanged notifications, sending them
 * at most once per configurable time period. onInvalidated notifications are
 * sent without any delay.
 */
class BatchedDataSetObserver extends DataSetObserver {

    private final Handler mHandler;
    private final DataSetObserver mDelegateObserver;
    private final long mDelayMillis;

    private final Runnable mOnChangeRunnable = new Runnable() {
        public void run() {
            cancelPendingChanges();
            mDelegateObserver.onChanged();
        }
    };

    /**
     * Create a new BatchedDataSetObserver.
     *
     * Instances are thread safe, and ensure that methods of the wrapped
     * DataSetObserver are only called on the thread associated with the Handler.
     *
     * @param handler to use for queuing notifications.
     * @param delegateObserver to which to send the actual notifications.
     * @param delayMillis interval used for batching.
     */
    public BatchedDataSetObserver(Handler handler, DataSetObserver delegateObserver,
            long delayMillis) {
        mHandler = handler;
        mDelegateObserver = delegateObserver;
        mDelayMillis = delayMillis;
    }

    @Override
    public void onChanged() {
        delayOnChanged(mDelayMillis);
    }

    @Override
    public void onInvalidated() {
        mHandler.post(new Runnable() {
            public void run() {
                mDelegateObserver.onInvalidated();
            }
        });
    }

    /**
     * Submits an onChange notification after the given delay.
     * @param delayMillis delay in milliseconds before onChange is called.
     */
    public void delayOnChanged(long delayMillis) {
        mHandler.postDelayed(mOnChangeRunnable, delayMillis);
    }

    /**
     * Remove any pending change notifications.
     */
    public void cancelPendingChanges() {
        mHandler.removeCallbacks(mOnChangeRunnable);
    }
}
