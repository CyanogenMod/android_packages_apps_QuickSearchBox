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

import com.android.quicksearchbox.util.NamedTaskExecutor;

import android.os.Handler;

import java.util.ArrayList;

/**
 * A suggestions provider that gets suggestions from a single corpus.
 */
public class SingleCorpusSuggestionsProvider extends AbstractSuggestionsProvider {

    private final ArrayList<Corpus> mCorpora;

    private final ShortcutRepository mShortcutRepo;

    public SingleCorpusSuggestionsProvider(Config config, Corpus corpus,
            NamedTaskExecutor queryExecutor,
            Handler publishThread,
            Promoter promoter,
            ShortcutRepository shortcutRepo) {
        super(config, queryExecutor, publishThread, promoter);
        mCorpora = new ArrayList<Corpus>(1);
        mCorpora.add(corpus);
        mShortcutRepo = shortcutRepo;
    }

    @Override
    public ArrayList<Corpus> getOrderedCorpora() {
        return mCorpora;
    }

    @Override
    protected SuggestionCursor getShortcutsForQuery(String query) {
        if (mShortcutRepo == null) return null;
        // TODO: Allow restricting shortcuts by source
        //return mShortcutRepo.getShortcutsForQuery(mSource.getComponentName(), query);
        return null;
    }

}
