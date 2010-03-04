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

import com.android.quicksearchbox.util.BatchingNamedTaskExecutor;
import com.android.quicksearchbox.util.Consumer;
import com.android.quicksearchbox.util.NamedTaskExecutor;
import com.android.quicksearchbox.util.Util;

import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

    private final NamedTaskExecutor mQueryExecutor;

    private final Handler mPublishThread;

    private final Promoter mPromoter;

    private final ShouldQueryStrategy mShouldQueryStrategy = new ShouldQueryStrategy();

    private BatchingNamedTaskExecutor mBatchingExecutor;

    public AbstractSuggestionsProvider(Config config,
            NamedTaskExecutor queryExecutor,
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
        if (mBatchingExecutor != null) {
            mBatchingExecutor.cancelPendingTasks();
            mBatchingExecutor = null;
        }
    }

    public abstract List<Corpus> getOrderedCorpora();

    protected abstract SuggestionCursor getShortcutsForQuery(String query);

    /**
     * Gets the sources that should be queried for the given query.
     *
     */
    private ArrayList<Corpus> getCorporaToQuery(String query) {
        if (query.length() == 0) {
            return new ArrayList<Corpus>(0);
        }
        List<Corpus> orderedCorpora = getOrderedCorpora();
        ArrayList<Corpus> corporaToQuery = new ArrayList<Corpus>(orderedCorpora.size());
        for (Corpus corpus : orderedCorpora) {
            if (shouldQueryCorpus(corpus, query)) {
                corporaToQuery.add(corpus);
            }
        }
        return corporaToQuery;
    }

    protected boolean shouldQueryCorpus(Corpus corpus, String query) {
        return mShouldQueryStrategy.shouldQueryCorpus(corpus, query);
    }

    private void updateShouldQueryStrategy(CorpusResult cursor) {
        if (cursor.getCount() == 0) {
            mShouldQueryStrategy.onZeroResults(cursor.getCorpus(),
                    cursor.getUserQuery());
        }
    }

    public Suggestions getSuggestions(String query) {
        if (DBG) Log.d(TAG, "getSuggestions(" + query + ")");
        cancelPendingTasks();
        ArrayList<Corpus> corporaToQuery = getCorporaToQuery(query);
        int numPromotedSources = mConfig.getNumPromotedSources();
        Set<Corpus> promotedCorpora = Util.setOfFirstN(corporaToQuery, numPromotedSources);
        final Suggestions suggestions = new Suggestions(mPromoter,
                mConfig.getMaxPromotedSuggestions(),
                query,
                corporaToQuery.size(),
                promotedCorpora);
        SuggestionCursor shortcuts = getShortcutsForQuery(query);
        if (shortcuts != null) {
            suggestions.setShortcuts(shortcuts);
        }

        // Fast path for the zero sources case
        if (corporaToQuery.size() == 0) {
            return suggestions;
        }

        mBatchingExecutor = new BatchingNamedTaskExecutor(mQueryExecutor, numPromotedSources);

        SuggestionCursorReceiver receiver = new SuggestionCursorReceiver(
                mBatchingExecutor, suggestions);

        int maxResultsPerSource = mConfig.getMaxResultsPerSource();
        QueryTask.startQueries(query, maxResultsPerSource, corporaToQuery, mBatchingExecutor,
                mPublishThread, receiver);

        return suggestions;
    }

    private class SuggestionCursorReceiver implements Consumer<CorpusResult> {
        private final BatchingNamedTaskExecutor mExecutor;
        private final Suggestions mSuggestions;

        public SuggestionCursorReceiver(BatchingNamedTaskExecutor executor,
                Suggestions suggestions) {
            mExecutor = executor;
            mSuggestions = suggestions;
        }

        public boolean consume(CorpusResult cursor) {
            updateShouldQueryStrategy(cursor);
            mSuggestions.addCorpusResult(cursor);
            if (!mSuggestions.isClosed()) {
                executeNextBatchIfNeeded();
            }
            return true;
        }

        private void executeNextBatchIfNeeded() {
            if (mSuggestions.getSourceCount() % mConfig.getNumPromotedSources() == 0) {
                // We've just finished one batch
                if (mSuggestions.getPromoted().getCount() < mConfig.getMaxPromotedSuggestions()) {
                    // But we still don't have enough results, ask for more
                    mExecutor.executeNextBatch();
                }
            }
        }
    }

}
