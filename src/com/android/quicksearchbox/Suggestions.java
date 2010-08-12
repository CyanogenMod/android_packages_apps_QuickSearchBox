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

import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.util.Log;

public abstract class Suggestions {
    private static final boolean DBG = false;
    private static final String TAG = "QSB.Suggestions";

    /** True if {@link Suggestions#close} has been called. */
    private boolean mClosed = false;
    protected final String mQuery;

    /**
     * The observers that want notifications of changes to the published suggestions.
     * This object may be accessed on any thread.
     */
    private final DataSetObservable mDataSetObservable = new DataSetObservable();

    protected Suggestions(String query) {
        mQuery = query;
    }

    /**
     * Registers an observer that will be notified when the reported results or
     * the done status changes.
     */
    public void registerDataSetObserver(DataSetObserver observer) {
        if (mClosed) {
            throw new IllegalStateException("registerDataSetObserver() when closed");
        }
        mDataSetObservable.registerObserver(observer);
    }


    /**
     * Unregisters an observer.
     */
    public void unregisterDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.unregisterObserver(observer);
    }

    /**
     * Calls {@link DataSetObserver#onChanged()} on all observers.
     */
    protected void notifyDataSetChanged() {
        if (DBG) Log.d(TAG, "notifyDataSetChanged()");
        mDataSetObservable.notifyChanged();
    }

    public void close() {
        if (mClosed) {
            throw new IllegalStateException("Double close()");
        }
        mClosed = true;
        mDataSetObservable.unregisterAll();
    }

    public boolean isClosed() {
        return mClosed;
    }

    @Override
    protected void finalize() {
        if (!mClosed) {
            Log.e(TAG, "LEAK! Finalized without being closed: Suggestions[" + getQuery() + "]");
        }
        
    }

    public String getQuery() {
        return mQuery;
    }

    public abstract SuggestionCursor getPromoted();

    public abstract boolean isDone();
}
