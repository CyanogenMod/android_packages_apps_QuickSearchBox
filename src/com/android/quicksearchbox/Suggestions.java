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

import java.util.ArrayList;

import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.os.Handler;
import android.util.Log;

/**
 * Contains all non-empty {@link SuggestionCursor} objects that have been reported so far.
 *
 */
public class Suggestions {

    private static final boolean DBG = true;
    private static final String TAG = "QSB.Suggestions";

    private final int mMaxPromoted;

    private final String mQuery;

    /** The number of sources that are expected to report. */
    private final int mExpectedSourceCount;

    /**
     * The observers that want notifications of changes to the published suggestions.
     * This object may be accessed on any thread.
     */
    private final DataSetObservable mDataSetObservable = new DataSetObservable();

    /**
     * All non-empty {@link SuggestionCursor} objects that have been published so far.
     * This object may only be accessed on the UI thread.
     * */
    private final ArrayList<SuggestionCursor> mSourceResults;

    /**
     * The number of sources that have reported so far. This may be greater
     * that the size of {@link #mSourceResults}, since this count also includes
     * sources that failed or reported zero results.
     */
    private int mPublishedSourceCount = 0;

    private SuggestionCursor mShortcuts;

    /** True if {@link Suggestions#close} has been called. */
    private boolean mClosed = false;

    private final Promoter mPromoter;

    private ListSuggestionCursor mPromoted;

    /**
     * Creates a new empty Suggestions.
     *
     * @param expectedSourceCount The number of sources that are expected to report.
     */
    public Suggestions(Promoter promoter, int maxPromoted,
            String query, int expectedSourceCount) {
        mPromoter = promoter;
        mMaxPromoted = maxPromoted;
        mQuery = query;
        mExpectedSourceCount = expectedSourceCount;
        mSourceResults = new ArrayList<SuggestionCursor>(mExpectedSourceCount);
        mPromoted = null;  // will be set by updatePromoted()
    }

    public String getQuery() {
        return mQuery;
    }

    /**
     * Gets the number of sources that are expected to report.
     */
    public int getExpectedSourceCount() {
        return mExpectedSourceCount;
    }

    /**
     * Gets the number of sources whose results have been published. This may be higher than
     * {@link #getSourceCount()}, since empty results are not included in
     * {@link #getSourceCount()}.
     */
    public int getPublishedSourceCount() {
        return mPublishedSourceCount;
    }

    /**
     * Gets the current progress of the suggestions, in the inclusive range 0-100.
     */
    public int getProgress() {
        if (mExpectedSourceCount == 0) return 100;
        return 100 * mPublishedSourceCount / mExpectedSourceCount;
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

    public SuggestionCursor getPromoted() {
        if (mPromoted == null) {
            updatePromoted();
        }
        return mPromoted;
    }

    /**
     * Calls {@link DataSetObserver#onChanged()} on all observers.
     */
    private void notifyDataSetChanged() {
        if (DBG) Log.d(TAG, "notifyDataSetChanged()");
        mDataSetObservable.notifyChanged();
    }

    /**
     * Closes all the source results and unregisters all observers.
     */
    public void close() {
        if (DBG) Log.d(TAG, "close()");
        if (mClosed) {
            Log.w(TAG, "Double close()");
            return;
        }
        mDataSetObservable.unregisterAll();
        mClosed = true;
        if (mShortcuts != null) {
            mShortcuts.close();
        }
        for (SuggestionCursor result : mSourceResults) {
            result.close();
        }
        mSourceResults.clear();
    }

    @Override
    protected void finalize() {
        if (!mClosed) {
            Log.e(TAG, "Leak! Finalized without being closed.");
            close();
        }
    }

    /**
     * Checks whether all sources have reported.
     * Must be called on the UI thread, or before this object is seen by the UI thread.
     */
    public boolean isDone() {
        return mPublishedSourceCount >= mExpectedSourceCount;
    }

    public SuggestionCursor getShortcuts() {
        return mShortcuts;
    }

    public void setShortcuts(SuggestionCursor shortcuts) {
        if (DBG)  Log.d(TAG, "setShortcuts(" + shortcuts + ")");
        mShortcuts = shortcuts;
    }

    /**
     * Adds a source result. Must be called on the UI thread, or before this
     * object is seen by the UI thread.
     *
     * @param sourceResult The source result.
     */
    public void addSourceResult(SuggestionCursor sourceResult) {
        if (mClosed) {
            sourceResult.close();
            return;
        }
        if (!mQuery.equals(sourceResult.getUserQuery())) {
          throw new IllegalArgumentException("Got result for wrong query: "
                + mQuery + " != " + sourceResult.getUserQuery());
        }
        boolean changed = publishSourceResult(sourceResult);
        if (changed) {
            mPromoted = null;
            notifyDataSetChanged();
        }
    }

    private void updatePromoted() {
        mPromoted = new ListSuggestionCursorNoDuplicates(mQuery);
        if (mPromoter == null) {
            return;
        }
        mPromoter.pickPromoted(mShortcuts, mSourceResults, mMaxPromoted, mPromoted);
    }

    /**
     * Publishes the result from a source. Does not notify the observers.
     * Must be called on the UI thread, or before this object is seen by the UI thread.
     * If called after the UI has seen this object, {@link #notifyDataSetChanged()}
     * must be called if this method returns {@code true}.
     *
     * @param sourceResult
     * @return {@code true} if any suggestions were added or the state changed to done.
     */
    private boolean publishSourceResult(SuggestionCursor sourceResult) {
        if (mClosed) {
            throw new IllegalStateException("publishSourceResult(" + sourceResult
                + ") after close()");
        }
        if (DBG) Log.d(TAG, "publishSourceResult(" + sourceResult + ")");
        mPublishedSourceCount++;
        final int count = sourceResult.getCount();
        boolean added = false;
        if (count > 0) {
            added = true;
            mSourceResults.add(sourceResult);
        } else {
            sourceResult.close();
        }
        return (added || isDone());
    }

    /**
     * Gets a given source result.
     * Must be called on the UI thread, or before this object is seen by the UI thread.
     *
     * @param sourcePos 
     * @return The source result at the given position.
     * @throws IndexOutOfBoundsException If {@code sourcePos < 0} or
     *         {@code sourcePos >= getSourceCount()}.
     */
    public SuggestionCursor getSourceResult(int sourcePos) {
        if (mClosed) {
            throw new IllegalStateException("Called getSourceResult(" + sourcePos
                + ") when closed.");
        }
        return mSourceResults.get(sourcePos);
    }

    /**
     * Gets the number of source results.
     * Must be called on the UI thread, or before this object is seen by the UI thread.
     */
    public int getSourceCount() {
        if (mClosed) {
            throw new IllegalStateException("Called getSourceCount() when closed.");
        }
        return mSourceResults == null ? 0 : mSourceResults.size();
    }

}
