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

import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;

/**
 * Common suggestions provider base class.
 *
 * The provider will only handle a single query at a time. If a new query comes
 * in, the old one is canceled.
 */
public abstract class AbstractSuggestionsProvider implements SuggestionsProvider {

    private static final boolean DBG = true;
    private static final String TAG = "QSB.AbstractSuggestionsProvider";

    private final Config mConfig;

    private final SourceTaskExecutor mQueryExecutor;

    private final Handler mPublishThread;

    private final Promoter mPromoter;

    private final ShouldQueryStrategy mShouldQueryStrategy = new ShouldQueryStrategy();

    public AbstractSuggestionsProvider(Config config,
            SourceTaskExecutor queryExecutor,
            Handler publishThread,
            Promoter promoter) {
        mConfig = config;
        mQueryExecutor = queryExecutor;
        mPublishThread = publishThread;
        mPromoter = promoter;
    }

    public void close() {
        cancelPendingTasks();
    }

    /**
     * Cancels all pending query tasks.
     */
    private void cancelPendingTasks() {
        mQueryExecutor.cancelPendingTasks();
    }

    public abstract ArrayList<Source> getOrderedSources();

    protected abstract SuggestionCursor getShortcutsForQuery(String query);

    /**
     * Gets the sources that should be queried for the given query.
     *
     */
    private ArrayList<Source> getSourcesToQuery(String query) {
        if (query.length() == 0) {
            return new ArrayList<Source>();
        }
        ArrayList<Source> orderedSources = getOrderedSources();
        ArrayList<Source> sourcesToQuery = new ArrayList<Source>(orderedSources.size());
        for (Source source : orderedSources) {
            if (shouldQuerySource(source, query)) {
                sourcesToQuery.add(source);
            }
        }
        return sourcesToQuery;
    }

    protected boolean shouldQuerySource(Source source, String query) {
        return mShouldQueryStrategy.shouldQuerySource(source, query);
    }

    public Suggestions getSuggestions(String query) {
        if (DBG) Log.d(TAG, "getSuggestions(" + query + ")");
        cancelPendingTasks();
        ArrayList<Source> sourcesToQuery = getSourcesToQuery(query);
        final Suggestions suggestions = new Suggestions(mPromoter,
                mConfig.getMaxPromotedSuggestions(),
                query,
                sourcesToQuery.size());
        SuggestionCursor shortcuts = getShortcutsForQuery(query);
        if (shortcuts != null) {
            suggestions.setShortcuts(shortcuts);
        }

        // Fast path for the zero sources case
        if (sourcesToQuery.size() == 0) {
            return suggestions;
        }

        SuggestionCursorReceiver receiver = new SuggestionCursorReceiver() {
            public void receiveSuggestionCursor(final SuggestionCursor cursor) {
                mPublishThread.post(new Runnable() {
                    public void run() {
                        if (cursor.getCount() == 0) {
                            mShouldQueryStrategy.onZeroResults(
                                    cursor.getSourceComponentName(), cursor.getUserQuery());
                        }
                        suggestions.addSourceResult(cursor);
                    }
                });
            }
        };

        int maxResultsPerSource = mConfig.getMaxResultsPerSource();
        for (Source source : sourcesToQuery) {
            QueryTask task = new QueryTask(query, source, maxResultsPerSource, receiver);
            mQueryExecutor.execute(task);
        }

        return suggestions;
    }

    private interface SuggestionCursorReceiver {
        void receiveSuggestionCursor(SuggestionCursor cursor);
    }

    /**
     * Gets suggestions from a given source.
     */
    private static class QueryTask implements SourceTask {
        private final String mQuery;
        private final Source mSource;
        private final int mQueryLimit;
        private final SuggestionCursorReceiver mReceiver;

        public QueryTask(String query, Source source, int queryLimit,
                SuggestionCursorReceiver receiver) {
            mQuery = query;
            mSource = source;
            mQueryLimit = queryLimit;
            mReceiver = receiver;
        }

        public Source getSource() {
            return mSource;
        }

        public void run() {
            SuggestionCursor cursor = mSource.getSuggestions(mQuery, mQueryLimit);
            mReceiver.receiveSuggestionCursor(cursor);
        }

        @Override
        public String toString() {
            return mSource + "[" + mQuery + "]";
        }
    }
}
