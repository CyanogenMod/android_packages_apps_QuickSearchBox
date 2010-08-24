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

package com.android.quicksearchbox.util;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for a one-place cache that holds a value that is produced
 * asynchronously.
 *
 * @param <A> The type of the data held in the cache.
 */
public abstract class AsyncCache<A> {

    private static final String TAG = "QSB.AsyncCache";
    private static final boolean DBG = true;

    private final Object mLock = new Object();

    private A mValue;

    private boolean mValid;

    private List<Consumer<A>> mWaitingConsumers;

    /**
     * Creates the object to store in the cache. This method must call
     * {@link #store} when it's done.
     * This method must not block.
     */
    protected abstract void create();

    /**
     * Saves a new value to the cache.
     */
    protected void store(A value) {
        List<Consumer<A>> waitingConsumers;
        synchronized (mLock) {
            mValue = value;
            mValid = true;
            waitingConsumers = mWaitingConsumers;
            mWaitingConsumers = null;
        }
        if (waitingConsumers != null) {
            for (Consumer<A> consumer : waitingConsumers) {
                if (DBG) Log.d(TAG, "Calling consumer: " + consumer);
                consumer.consume(value);
            }
        }
    }

    /**
     * Gets the value.
     *
     * @param consumer A consumer that will be given the cached value.
     *        The consumer may be called synchronously, or asynchronously on
     *        an unspecified thread.
     */
    public void get(Consumer<A> consumer) {
        boolean valid;
        A value;
        synchronized (mLock) {
            valid = mValid;
            value = mValue;
            if (!valid) {
                if (mWaitingConsumers == null) mWaitingConsumers = new ArrayList<Consumer<A>>();
                mWaitingConsumers.add(consumer);
            }
        }
        if (valid) {
            consumer.consume(value);
        } else {
            create();
        }
    }

    /**
     * Clears the cache.
     */
    public void clear() {
        synchronized (mLock) {
            mValue = null;
            mValid = false;
        }
    }

}
