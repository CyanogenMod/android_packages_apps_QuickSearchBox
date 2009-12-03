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
 * A suggestions provider that gets suggestions from all enabled sources that
 * want to be included in global search. The suggestion queries are done
 * asynchronously, using a thread pool.
 *
 * The provider will only handle a single query at a time. If a new query comes
 * in, the old one is canceled.
 *
 */
public class GlobalSuggestionsProvider implements SuggestionsProvider {

    private static final boolean DBG = true;
    private static final String TAG = "GlobalSuggestionsProvider";

    private final Config mConfig;

    private final SourceLookup mSources;

    private final SourceTaskExecutor mQueryExecutor;

    private final Handler mPublishThread;

    private final Promoter mPromoter;

    private final ShortcutRepository mShortcutRepo;

    public GlobalSuggestionsProvider(Config config, SourceLookup sources,
            SourceTaskExecutor queryExecutor,
            Handler publishThread,
            Promoter promoter,
            ShortcutRepository shortcutRepo) {
        mSources = sources;
        mConfig = config;
        mQueryExecutor = queryExecutor;
        mPublishThread = publishThread;
        mPromoter = promoter;
        mShortcutRepo = shortcutRepo;
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

    /**
     * Gets suggestions for the given query.
     */
    public Suggestions getSuggestions(String query) {
        if (DBG) Log.d(TAG, "getSuggestions(" + query + ")");
        cancelPendingTasks();
        ArrayList<Source> sourcesToQuery = mSources.getSourcesToQuery(query);
        Suggestions suggestions = new Suggestions(mPublishThread,
                mPromoter,
                mConfig.getMaxPromotedSuggestions(),
                mConfig.getSourceResultPublishDelayMillis(),
                query,
                sourcesToQuery.size());
        if (mShortcutRepo != null) {
            suggestions.setShortcuts(mShortcutRepo.getShortcutsForQuery(query));
        }
        for (Source source : sourcesToQuery) {
            QueryTask task = new QueryTask(query,
                    source,
                    mConfig.getMaxResultsPerSource(),
                    suggestions);
            mQueryExecutor.execute(task);
        }

        return suggestions;
    }

    /**
     * Gets suggestions from a given source.
     */
    private class QueryTask implements SourceTask {
        private final String mQuery;
        private final Source mSource;
        private final int mQueryLimit;
        private final Suggestions mSuggestions;

        public QueryTask(String query, Source source, int queryLimit,
                Suggestions suggestions) {
            mQuery = query;
            mSource = source;
            mQueryLimit = queryLimit;
            mSuggestions = suggestions;
        }

        public Source getSource() {
            return mSource;
        }

        public void run() {
            final SuggestionCursor cursor = mSource.getSuggestions(mQuery, mQueryLimit);
            mPublishThread.post(new Runnable() {
                public void run() {
                    mSuggestions.addSourceResult(cursor);
                }
            });
        }

        @Override
        public String toString() {
            return mSource + "[" + mQuery + "]";
        }
    }

}
