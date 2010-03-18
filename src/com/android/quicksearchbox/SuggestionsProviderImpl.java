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
 * Suggestions provider implementation.
 *
 * The provider will only handle a single query at a time. If a new query comes
 * in, the old one is canceled.
 */
public class SuggestionsProviderImpl implements SuggestionsProvider {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.SuggestionsProviderImpl";

    private final Config mConfig;

    private final NamedTaskExecutor mQueryExecutor;

    private final Handler mPublishThread;

    private final Promoter mPromoter;

    private final ShortcutRepository mShortcutRepo;

    private final Logger mLogger;

    private final ShouldQueryStrategy mShouldQueryStrategy = new ShouldQueryStrategy();

    private final Corpora mCorpora;

    private BatchingNamedTaskExecutor mBatchingExecutor;

    public SuggestionsProviderImpl(Config config,
            NamedTaskExecutor queryExecutor,
            Handler publishThread,
            Promoter promoter,
            ShortcutRepository shortcutRepo,
            Corpora corpora,
            Logger logger) {
        mConfig = config;
        mQueryExecutor = queryExecutor;
        mPublishThread = publishThread;
        mPromoter = promoter;
        mShortcutRepo = shortcutRepo;
        mLogger = logger;
        mCorpora = corpora;
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

    protected SuggestionCursor getShortcutsForQuery(String query, List<Corpus> corpora,
            int maxShortcuts) {
        if (mShortcutRepo == null) return null;
        return mShortcutRepo.getShortcutsForQuery(query, corpora, maxShortcuts);
    }

    /**
     * Gets the sources that should be queried for the given query.
     */
    private List<Corpus> getCorporaToQuery(String query, List<Corpus> orderedCorpora) {
        ArrayList<Corpus> corporaToQuery = new ArrayList<Corpus>(orderedCorpora.size());
        for (Corpus corpus : orderedCorpora) {
            if (shouldQueryCorpus(corpus, query)) {
                corporaToQuery.add(corpus);
            }
        }
        return corporaToQuery;
    }

    protected boolean shouldQueryCorpus(Corpus corpus, String query) {
        if (query.length() == 0 && !corpus.isWebCorpus()) {
            // Only the web corpus sees zero length queries.
            return false;
        }
        return mShouldQueryStrategy.shouldQueryCorpus(corpus, query);
    }

    private void updateShouldQueryStrategy(CorpusResult cursor) {
        if (cursor.getCount() == 0) {
            mShouldQueryStrategy.onZeroResults(cursor.getCorpus(),
                    cursor.getUserQuery());
        }
    }

    public Suggestions getSuggestions(String query, List<Corpus> corpora, int maxSuggestions) {
        if (DBG) Log.d(TAG, "getSuggestions(" + query + ")");
        cancelPendingTasks();
        List<Corpus> corporaToQuery = getCorporaToQuery(query, corpora);
        final Suggestions suggestions = new Suggestions(mPromoter,
                maxSuggestions,
                query,
                corporaToQuery.size());
        int maxShortcuts = mConfig.getMaxShortcutsReturned();
        SuggestionCursor shortcuts = getShortcutsForQuery(query, corpora, maxShortcuts);
        if (shortcuts != null) {
            suggestions.setShortcuts(shortcuts);
        }

        // Fast path for the zero sources case
        if (corporaToQuery.size() == 0) {
            return suggestions;
        }

        int initialBatchSize = countDefaultCorpora(corporaToQuery);
        initialBatchSize = Math.min(initialBatchSize, mConfig.getNumPromotedSources());
        if (initialBatchSize == 0) {
            initialBatchSize = mConfig.getNumPromotedSources();
        }

        mBatchingExecutor = new BatchingNamedTaskExecutor(mQueryExecutor);

        SuggestionCursorReceiver receiver = new SuggestionCursorReceiver(
                mBatchingExecutor, suggestions, initialBatchSize);

        int maxResultsPerSource = mConfig.getMaxResultsPerSource();
        QueryTask.startQueries(query, maxResultsPerSource, corporaToQuery, mBatchingExecutor,
                mPublishThread, receiver);
        mBatchingExecutor.executeNextBatch(initialBatchSize);

        return suggestions;
    }

    private int countDefaultCorpora(List<Corpus> corpora) {
        int count = 0;
        for (Corpus corpus : corpora) {
            if (mCorpora.isCorpusDefaultEnabled(corpus)) {
                count++;
            }
        }
        return count;
    }

    private class SuggestionCursorReceiver implements Consumer<CorpusResult> {
        private final BatchingNamedTaskExecutor mExecutor;
        private final Suggestions mSuggestions;

        private int mCountAtWhichToExecuteNextBatch;

        public SuggestionCursorReceiver(BatchingNamedTaskExecutor executor,
                Suggestions suggestions, int initialBatchSize) {
            mExecutor = executor;
            mSuggestions = suggestions;
            mCountAtWhichToExecuteNextBatch = initialBatchSize;
        }

        public boolean consume(CorpusResult cursor) {
            updateShouldQueryStrategy(cursor);
            mSuggestions.addCorpusResult(cursor);
            if (!mSuggestions.isClosed()) {
                executeNextBatchIfNeeded();
            }
            if (cursor != null && mLogger != null) {
                mLogger.logLatency(cursor);
            }
            return true;
        }

        private void executeNextBatchIfNeeded() {
            if (mSuggestions.getSourceCount() == mCountAtWhichToExecuteNextBatch) {
                // We've just finished one batch
                if (mSuggestions.getPromoted().getCount() < mConfig.getMaxPromotedSuggestions()) {
                    // But we still don't have enough results, ask for more
                    int nextBatchSize = mConfig.getNumPromotedSources();
                    mCountAtWhichToExecuteNextBatch += nextBatchSize;
                    mExecutor.executeNextBatch(nextBatchSize);
                }
            }
        }
    }

}
