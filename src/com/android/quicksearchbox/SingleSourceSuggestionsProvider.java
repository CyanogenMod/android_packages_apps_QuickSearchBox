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
 * A suggestions provider that gets suggestions from a single source.
 */
public class SingleSourceSuggestionsProvider extends AbstractSuggestionsProvider {

    private final Source mSource;

    private final ArrayList<Source> mSources;

    private final ShortcutRepository mShortcutRepo;

    public SingleSourceSuggestionsProvider(Config config, Source source,
            SourceTaskExecutor queryExecutor,
            Handler publishThread,
            Promoter promoter,
            ShortcutRepository shortcutRepo) {
        super(config, queryExecutor, publishThread, promoter);
        mSource = source;
        mSources = new ArrayList<Source>(1);
        mSources.add(source);
        mShortcutRepo = shortcutRepo;
    }

    public ArrayList<Source> getOrderedSources() {
        return mSources;
    }

    @Override
    protected SuggestionCursor getShortcutsForQuery(String query) {
        if (mShortcutRepo == null) return null;
        // TODO: Allow restricting shortcuts by source
        //return mShortcutRepo.getShortcutsForQuery(mSource.getComponentName(), query);
        return null;
    }

}
