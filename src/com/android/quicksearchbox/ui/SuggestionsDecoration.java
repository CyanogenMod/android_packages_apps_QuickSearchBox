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

package com.android.quicksearchbox.ui;

import com.android.quicksearchbox.SuggestionCursor;
import com.android.quicksearchbox.Suggestions;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.ViewGroup;

/**
 * Extra view that can be shown at the top or bottom of the suggestion list
 */
public class SuggestionsDecoration {

    private final DataSetObserver mDataSetObserver = new AdapterObserver();

    private final Context mContext;

    private SuggestionsAdapter mAdapter;

    public SuggestionsDecoration(Context context) {
        mContext = context;
    }

    public void addToContainer(ViewGroup parent) {
        // Do nothing - default is no decoration.
    }

    protected void onSuggestionsChanged() {
        // Do nothing
    }

    protected Context getContext() {
        return mContext;
    }

    public void setAdapter(SuggestionsAdapter adapter) {
        if (mAdapter == adapter) {
            return;
        }
        if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(mDataSetObserver);
        }
        mAdapter = adapter;
        if (mAdapter != null) {
            mAdapter.registerDataSetObserver(mDataSetObserver);
        }
        onSuggestionsChanged();
    }

    protected Suggestions getSuggestions() {
        return mAdapter == null ? null : mAdapter.getSuggestions();
    }

    protected SuggestionCursor getCurrentSuggestions() {
        return mAdapter == null ? null : mAdapter.getCurrentSuggestions();
    }

    private class AdapterObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            onSuggestionsChanged();
        }

        @Override
        public void onInvalidated() {
            onSuggestionsChanged();
        }
    }

}
