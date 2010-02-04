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

import android.content.ComponentName;
import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Decides whether a given source should be queried for a given query, taking
 * into account the source's query threshold and query after zero results flag.
 *
 * This class is thread safe.
 */
class ShouldQueryStrategy {
    private static final boolean DBG = true;
    private static final String TAG = "QSB.ShouldQueryStrategy";

    // The last query we've seen
    private String mLastQuery = "";

    // The current implementation keeps a record of those sources that have
    // returned zero results for some prefix of the current query. mEmptySources
    // maps from source component name to the length of the query which returned
    // zero results.  When a query is shortened (e.g., by deleting characters)
    // or changed entirely, mEmptySources is pruned (in updateQuery)
    private final HashMap<ComponentName, Integer> mEmptySources
            = new HashMap<ComponentName, Integer>();

    /**
     * Returns whether we should query the given source for the given query.
     */
    public synchronized boolean shouldQuerySource(Source source, String query) {
        updateQuery(query);
        if (query.length() >= source.getQueryThreshold()) {
            ComponentName sourceName = source.getComponentName();
            if (!source.queryAfterZeroResults() && mEmptySources.containsKey(sourceName)) {
                if (DBG) Log.i(TAG, "Not querying " + sourceName + ", returned 0 after "
                        + mEmptySources.get(sourceName));
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Called to notify ShouldQueryStrategy when a source reports no results for a query.
     */
    public synchronized void onZeroResults(ComponentName source, String query) {
        if (DBG) Log.i(TAG, source + " returned 0 results for " + query);
        // Make sure this result is actually for a prefix of the current query.
        if (mLastQuery.startsWith(query)) {
            // TODO: Don't bother if queryAfterZeroResults is true
            mEmptySources.put(source, query.length());
        }
    }

    private void updateQuery(String query) {
        if (query.startsWith(mLastQuery)) {
            // This is a refinement of the last query
            mLastQuery = query;
        } else if (mLastQuery.startsWith(query)) {
            // This is a widening of the last query: clear out any sources
            // that reported zero results after this query.
            Iterator<Map.Entry<ComponentName, Integer>> iter = mEmptySources.entrySet().iterator();
            while (iter.hasNext()) {
                if (iter.next().getValue() > query.length()) {
                    iter.remove();
                }
            }
        } else {
            // This is a completely different query, clear everything.
            mEmptySources.clear();
        }
    }
}
