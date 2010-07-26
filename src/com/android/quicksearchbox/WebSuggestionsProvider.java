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

import android.os.Handler;

/**
 * SuggestionProvider that provides web suggestions only.
 */
public class WebSuggestionsProvider implements SuggestionsProvider {

    private final Source mSource;
    private final Handler mPublishThread;

    public WebSuggestionsProvider(GoogleSource webSource, Handler publishThread) {
        mSource = webSource.getWebSuggestOnlySource();
        mPublishThread = publishThread;
    }

    @Override
    public void close() {
    }

    @Override
    public Suggestions getSuggestions(String query, Corpus singleCorpus, int maxSuggestions) {
        SingleSourceSuggestions suggestions = new SingleSourceSuggestions(query);
        Runnable task = new QueryTask(suggestions, query, maxSuggestions);
        new Thread(task).start();
        return suggestions;
    }

    private class QueryTask implements Runnable {
        private final SingleSourceSuggestions mSuggestions;
        private final String mQuery;
        private final int mMaxResults;
        public QueryTask(SingleSourceSuggestions suggestions, String query, int maxResults) {
            mSuggestions = suggestions;
            mQuery = query;
            mMaxResults = maxResults;
        }

        public void run() {
            final SourceResult result = mSource.getSuggestions(mQuery, mMaxResults, true);
            mPublishThread.post(new Runnable(){
                public void run() {
                    mSuggestions.setResult(result);
                }});
        }
    }

}
