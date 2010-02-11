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

import java.util.ArrayList;

/**
 * A suggestions provider that gets suggestions from all enabled sources that
 * want to be included in global search.
 */
public class GlobalSuggestionsProvider extends AbstractSuggestionsProvider {

    private final Corpora mCorpora;

    private final CorpusRanker mCorpusRanker;

    private final ShortcutRepository mShortcutRepo;

    public GlobalSuggestionsProvider(Config config, Corpora corpora,
            SourceTaskExecutor queryExecutor,
            Handler publishThread,
            Promoter promoter,
            CorpusRanker corpusRanker,
            ShortcutRepository shortcutRepo) {
        super(config, queryExecutor, publishThread, promoter);
        mCorpora = corpora;
        mCorpusRanker = corpusRanker;
        mShortcutRepo = shortcutRepo;
    }

    // TODO: Cache this list?
    @Override
    public ArrayList<Corpus> getOrderedCorpora() {
        return mCorpusRanker.rankCorpora(mCorpora.getEnabledCorpora());
    }

    @Override
    protected SuggestionCursor getShortcutsForQuery(String query) {
        if (mShortcutRepo == null) return null;
        return mShortcutRepo.getShortcutsForQuery(query);
    }

}
