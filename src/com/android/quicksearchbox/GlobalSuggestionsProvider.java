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

import android.content.ComponentName;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedHashSet;

/**
 * A suggestions provider that gets suggestions from all enabled sources that
 * want to be included in global search.
 */
public class GlobalSuggestionsProvider extends AbstractSuggestionsProvider {

    private static final boolean DBG = true;
    private static final String TAG = "QSB.GlobalSuggestionsProvider";

    private final SourceLookup mSources;

    private final ShortcutRepository mShortcutRepo;

    public GlobalSuggestionsProvider(Config config, SourceLookup sources,
            SourceTaskExecutor queryExecutor,
            Handler publishThread,
            Promoter promoter,
            ShortcutRepository shortcutRepo) {
        super(config, queryExecutor, publishThread, promoter);
        mSources = sources;
        mShortcutRepo = shortcutRepo;
    }

    // TODO: Cache this list?
    public ArrayList<Source> getOrderedSources() {
        // Using a LinkedHashSet to get the sources in the order added while
        // avoiding duplicates.
        LinkedHashSet<Source> orderedSources = new LinkedHashSet<Source>();
        // Add web search source first, so that it's always queried first,
        // to do network traffic while the rest are using the CPU.
        Source webSource = mSources.getSelectedWebSearchSource();
        if (webSource != null) {
            orderedSources.add(webSource);
        }
        // Then add all ranked sources
        ArrayList<ComponentName> rankedSources = mShortcutRepo.getSourceRanking();
        if (DBG) Log.d(TAG, "Ranked sources: " + rankedSources);
        for (ComponentName sourceName : rankedSources) {
            Source source = mSources.getSourceByComponentName(sourceName);
            if (source != null && mSources.isEnabledSource(source)) {
                orderedSources.add(source);
            }
        }
        // Last, add all unranked enabled sources.
        orderedSources.addAll(mSources.getEnabledSources());
        if (DBG) Log.d(TAG, "All sources ordered " + orderedSources);
        return new ArrayList<Source>(orderedSources);
    }

    @Override
    protected SuggestionCursor getShortcutsForQuery(String query) {
        if (mShortcutRepo == null) return null;
        return mShortcutRepo.getShortcutsForQuery(query);
    }

}
