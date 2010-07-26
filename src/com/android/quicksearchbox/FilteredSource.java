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

import android.util.Log;

import java.util.HashMap;

/**
 *
 */
public class FilteredSource<S extends Source> extends SourceProxy<S> {
    private static final String TAG = "QSB.FilteredSource";
    private static final boolean DBG = false;

    private final HashMap<String, SuggestionsRequest> mActiveQueries;

    public FilteredSource(S parent) {
        super(parent);
        mActiveQueries = new HashMap<String, SuggestionsRequest>();
    }

    public Source getFiltered(Filter filter) {
        return new FilteringSource<S>(this, filter);
    }

    @Override
    public SourceResult getSuggestions(String query, int queryLimit, boolean onlySource) {
        return new SuggestionsRequest(query, queryLimit, onlySource).run();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[parent=" + mParent + "]";
    }

    public interface Filter {
        boolean accept(Suggestion suggestion);
    }

    private static class FilteringSource<S extends Source> extends SourceProxy<FilteredSource<S>> {
        private final Filter mFilter;
        FilteringSource(FilteredSource<S> parent, Filter filter) {
            super(parent);
            mFilter = filter;
        }

        @Override
        public SourceResult getSuggestions(String query, int queryLimit, boolean onlySource) {
            SourceResult unfiltered = super.getSuggestions(query, queryLimit, onlySource);
            ListSourceResult filtered = new ListSourceResult(query, unfiltered.getCount(), this);
            if (DBG) Log.d(TAG, this + ".getSuggestions('" + query + "') unfiltered=" + unfiltered);
            synchronized (unfiltered) {
                for (int i = 0; i < unfiltered.getCount(); ++i) {
                    unfiltered.moveTo(i);
                    if (mFilter.accept(unfiltered)) {
                        filtered.add(new SuggestionPosition(unfiltered, i));
                    }
                }
            }
            if (DBG) Log.d(TAG, this + ".getSuggestions('" + query + "') filtered=" + filtered);
            return filtered;
        }

        @Override
        public String toString() {
            return "FilteringSource[filter=" + mFilter + ";parent=" + mParent +"]";
        }
    }

    private static class ListSourceResult extends ListSuggestionCursor implements SourceResult {
        private final Source mSource;
        public ListSourceResult(String query, int capacity, Source source) {
            super(query, capacity);
            mSource = source;
        }
        @Override
        public Source getSource() {
            return mSource;
        }
    }

    private class SuggestionsRequest {
        public final String mQuery;
        public final int mQueryLimit;
        public final boolean mOnlySource;
        private SourceResult mResult;
        public SuggestionsRequest(String query, int queryLimit, boolean onlySource) {
            mQuery = query;
            mQueryLimit = queryLimit;
            mOnlySource = onlySource;
        }
        public SourceResult run() {
            SuggestionsRequest existing = null;
            synchronized (mActiveQueries) {
                existing = mActiveQueries.get(mQuery);
                if (existing == null) {
                    mActiveQueries.put(mQuery, this);
                }
            }
            if (existing == null) {
                SourceResult result =mParent.getSuggestions(mQuery, mQueryLimit, mOnlySource);
                synchronized (this) {
                    mResult = result; 
                    notifyAll();
                }
            } else {
                synchronized(existing) {
                    try {
                        existing.wait();
                        mResult = existing.mResult;
                    } catch (InterruptedException e) {
                    }
                }
            }
            if (existing == null) {
                synchronized (mActiveQueries) {
                    mActiveQueries.remove(mQuery);
                }
            }
            return mResult;
        }
    }

}
