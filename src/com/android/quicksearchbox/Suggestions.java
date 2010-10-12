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

import com.google.common.annotations.VisibleForTesting;

import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Collects all corpus results for a single query.
 */
public class Suggestions {
    private static final boolean DBG = false;
    private static final String TAG = "QSB.Suggestions";

    /** True if {@link Suggestions#close} has been called. */
    private boolean mClosed = false;
    protected final String mQuery;

    private ShortcutCursor mShortcuts;

    private final MyShortcutsObserver mShortcutsObserver = new MyShortcutsObserver();

    /**
     * The observers that want notifications of changes to the published suggestions.
     * This object may be accessed on any thread.
     */
    private final DataSetObservable mDataSetObservable = new DataSetObservable();

    /** The sources that are expected to report. */
    private final List<Corpus> mExpectedCorpora;

    /**
     * All {@link SuggestionCursor} objects that have been published so far,
     * in the order that they were published.
     * This object may only be accessed on the UI thread.
     * */
    private final ArrayList<CorpusResult> mCorpusResults;

    private CorpusResult mWebResult;

    private int mRefCount = 0;

    public Suggestions(String query, List<Corpus> expectedCorpora) {
        mQuery = query;
        mExpectedCorpora = expectedCorpora;
        mCorpusResults = new ArrayList<CorpusResult>(mExpectedCorpora.size());
        if (DBG) {
            Log.d(TAG, "new Suggestions [" + hashCode() + "] query \"" + query
                    + "\" expected corpora: " + mExpectedCorpora);
        }
    }

    public void acquire() {
        mRefCount++;
    }

    public void release() {
        mRefCount--;
        if (mRefCount <= 0) {
            close();
        }
    }

    public List<Corpus> getExpectedCorpora() {
        return mExpectedCorpora;
    }

    /**
     * Gets the number of corpora that are expected to report.
     */
    @VisibleForTesting
    public int getExpectedResultCount() {
        return mExpectedCorpora.size();
    }

    /**
     * Gets the set of corpora that have reported results to this suggestions set.
     *
     * @return A collection of corpora.
     */
    public Set<Corpus> getIncludedCorpora() {
        HashSet<Corpus> corpora = new HashSet<Corpus>();
        for (CorpusResult result : mCorpusResults) {
            corpora.add(result.getCorpus());
        }
        return corpora;
    }

    /**
     * Sets the shortcut suggestions.
     * Must be called on the UI thread, or before this object is seen by the UI thread.
     *
     * @param shortcuts The shortcuts.
     */
    public void setShortcuts(ShortcutCursor shortcuts) {
        if (DBG) Log.d(TAG, "setShortcuts(" + shortcuts + ")");
        if (mShortcuts != null) {
            throw new IllegalStateException("Got duplicate shortcuts: old: " + mShortcuts
                    + ", new: " + shortcuts);
        }
        if (shortcuts == null) return;
        if (isClosed()) {
            shortcuts.close();
            return;
        }
        if (!mQuery.equals(shortcuts.getUserQuery())) {
            throw new IllegalArgumentException("Got shortcuts for wrong query: "
                    + mQuery + " != " + shortcuts.getUserQuery());
        }
        mShortcuts = shortcuts;
        if (shortcuts != null) {
            mShortcuts.registerDataSetObserver(mShortcutsObserver);
        }
        notifyDataSetChanged();
    }

    /**
     * Checks whether all sources have reported.
     * Must be called on the UI thread, or before this object is seen by the UI thread.
     */
    public boolean isDone() {
        // TODO: Handle early completion because we have all the results we want.
        return mCorpusResults.size() >= mExpectedCorpora.size();
    }

    /**
     * Adds a list of corpus results. Must be called on the UI thread, or before this
     * object is seen by the UI thread.
     */
    public void addCorpusResults(List<CorpusResult> corpusResults) {
        if (isClosed()) {
            for (CorpusResult corpusResult : corpusResults) {
                corpusResult.close();
            }
            return;
        }

        for (CorpusResult corpusResult : corpusResults) {
            if (DBG) {
                Log.d(TAG, "addCorpusResult["+ hashCode() + "] corpus:" +
                        corpusResult.getCorpus().getName() + " results:" + corpusResult.getCount());
            }
            if (!mQuery.equals(corpusResult.getUserQuery())) {
              throw new IllegalArgumentException("Got result for wrong query: "
                    + mQuery + " != " + corpusResult.getUserQuery());
            }
            mCorpusResults.add(corpusResult);
            if (corpusResult.getCorpus().isWebCorpus()) {
                mWebResult = corpusResult;
            }
        }
        notifyDataSetChanged();
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

    /**
     * Closes all the source results and unregisters all observers.
     */
    private void close() {
        if (DBG) Log.d(TAG, "close() [" + hashCode() + "]");
        if (mClosed) {
            throw new IllegalStateException("Double close()");
        }
        mClosed = true;
        mDataSetObservable.unregisterAll();
        if (mShortcuts != null) {
            mShortcuts.close();
            mShortcuts = null;
        }

        for (CorpusResult result : mCorpusResults) {
            result.close();
        }
        mCorpusResults.clear();
    }

    public boolean isClosed() {
        return mClosed;
    }

    public ShortcutCursor getShortcuts() {
        return mShortcuts;
    }

    private void refreshShortcuts(SuggestionCursor promoted) {
        if (DBG) Log.d(TAG, "refreshShortcuts(" + promoted + ")");
        for (int i = 0; i < promoted.getCount(); ++i) {
            promoted.moveTo(i);
            if (promoted.isSuggestionShortcut()) {
                getShortcuts().refresh(promoted);
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

    public SuggestionCursor getPromoted(Promoter promoter, int maxPromoted) {
        SuggestionCursor promoted = buildPromoted(promoter, maxPromoted);
        refreshShortcuts(promoted);
        return promoted;
    }

    protected SuggestionCursor buildPromoted(Promoter promoter, int maxPromoted) {
        ListSuggestionCursor promoted = new ListSuggestionCursorNoDuplicates(mQuery);
        if (promoter == null) {
            return promoted;
        }
        promoter.pickPromoted(this, maxPromoted, promoted);
        if (DBG) {
            Log.d(TAG, "pickPromoted(" + getShortcuts() + "," + mCorpusResults + ","
                    + maxPromoted + ") = " + promoted);
        }
        return promoted;
    }

    /**
     * Gets the list of corpus results reported so far. Do not modify or hang on to
     * the returned iterator.
     */
    public Iterable<CorpusResult> getCorpusResults() {
        return mCorpusResults;
    }

    public CorpusResult getCorpusResult(Corpus corpus) {
        for (CorpusResult result : mCorpusResults) {
            if (result.getCorpus().equals(corpus)) {
                return result;
            }
        }
        return null;
    }

    public CorpusResult getWebResult() {
        return mWebResult;
    }

    /**
     * Gets the number of source results.
     * Must be called on the UI thread, or before this object is seen by the UI thread.
     */
    public int getResultCount() {
        if (isClosed()) {
            throw new IllegalStateException("Called getSourceCount() when closed.");
        }
        return mCorpusResults == null ? 0 : mCorpusResults.size();
    }

    @Override
    public String toString() {
        return "Suggestions@" + hashCode() + "{expectedCorpora=" + mExpectedCorpora
                + ",mCorpusResults.size()=" + mCorpusResults.size() + "}";
    }

    private class MyShortcutsObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            notifyDataSetChanged();
        }
    }

}
