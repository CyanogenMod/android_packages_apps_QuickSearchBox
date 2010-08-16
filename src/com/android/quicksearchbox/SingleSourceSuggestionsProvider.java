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

import com.android.quicksearchbox.google.GoogleSource;
import com.android.quicksearchbox.util.NamedTaskExecutor;

import android.os.Handler;
import android.util.Log;

/**
 * SuggestionsProvider implementation that provides suggestions from a single source.
 */
public class SingleSourceSuggestionsProvider implements SuggestionsProvider {
    private static final String TAG = "QSB.SingleSourceSuggestionsProvider";
    private static final boolean DBG = false;

    private final Source mSource;
    private final NamedTaskExecutor mQueryExecutor;
    private final Handler mPublishThread;
    private Promoter<SourceResult> mPromoter;

    public SingleSourceSuggestionsProvider(GoogleSource webSource,
            NamedTaskExecutor queryExecutor, Handler publishThread) {

        mSource = webSource.getWebSuggestOnlySource();
        mQueryExecutor = queryExecutor;
        mPublishThread = publishThread;
    }

    public void setPromoter(Promoter<SourceResult> promoter) {
        mPromoter = promoter;
    }

    public void close() {
    }

    public Suggestions getSuggestions(
            String query, Corpus singleCorpus, int maxSuggestions) {

        if (DBG) Log.d(TAG, "getSuggestions('" + query + "')");
        SingleSourceSuggestions suggestions = new SingleSourceSuggestions(
                query, mPromoter, maxSuggestions);

        QueryTask.startQuery(query, maxSuggestions,
                mSource, mQueryExecutor, mPublishThread, suggestions, true);

        return suggestions;
    }

}
