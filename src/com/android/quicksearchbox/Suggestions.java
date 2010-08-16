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

    private final int mMaxPromoted;
    private SuggestionCursor mPromoted;

    private ShortcutCursor mShortcuts;

    private final MyShortcutsObserver mShortcutsObserver = new MyShortcutsObserver();

    /**
     * The observers that want notifications of changes to the published suggestions.
     * This object may be accessed on any thread.
     */
    private final DataSetObservable mDataSetObservable = new DataSetObservable();

    protected Suggestions(String query, int maxPromoted) {
        mQuery = query;
        mMaxPromoted = maxPromoted;
    }

    private void clearPromoted() {
        if (mPromoted != null) {
            mPromoted.close();
        }
        mPromoted = null;
    }

    /**
     * Sets the shortcut suggestions.
     * Must be called on the UI thread, or before this object is seen by the UI thread.
     *
     * @param shortcuts The shortcuts.
     */
    public void setShortcuts(ShortcutCursor shortcuts) {
        if (DBG) Log.d(TAG, "setShortcuts(" + shortcuts + ")");
        mShortcuts = shortcuts;
        clearPromoted();
        if (shortcuts != null) {
            mShortcuts.registerDataSetObserver(mShortcutsObserver);
        }
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
        clearPromoted();
        mDataSetObservable.notifyChanged();
    }

    public void close() {
        if (mClosed) {
            throw new IllegalStateException("Double close()");
        }
        mClosed = true;
        mDataSetObservable.unregisterAll();
        if (mPromoted != null) {
            mPromoted.close();
        }
        if (mShortcuts != null) {
            mShortcuts.close();
            mShortcuts = null;
        }
    }

    public boolean isClosed() {
        return mClosed;
    }

    protected ShortcutCursor getShortcuts() {
        return mShortcuts;
    }

    protected int getMaxPromoted() {
        return mMaxPromoted;
    }

    private void refreshShortcuts() {
        if (DBG) Log.d(TAG, "refreshShortcuts(" + mPromoted + ")");
        for (int i = 0; i < mPromoted.getCount(); ++i) {
            mPromoted.moveTo(i);
            if (mPromoted.isSuggestionShortcut()) {
                getShortcuts().refresh(mPromoted);
            }
        }
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

    public SuggestionCursor getPromoted() {
        if (mPromoted == null) {
            mPromoted = buildPromoted();
            refreshShortcuts();
        }
        return mPromoted;
    }

    protected abstract SuggestionCursor buildPromoted();

    public abstract boolean isDone();

    private class MyShortcutsObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            notifyDataSetChanged();
        }
    }

}
