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
import com.android.quicksearchbox.SuggestionPosition;
import com.android.quicksearchbox.Suggestions;

import android.content.ComponentName;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * Uses a {@link Suggestions} object to back a {@link SuggestionsView}.
 */
public class SuggestionsAdapter extends BaseAdapter {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.SuggestionsAdapter";

    private DataSetObserver mDataSetObserver;

    private final SuggestionViewFactory mViewFactory;

    private SuggestionCursor mCursor;

    private ComponentName mSource = null;

    private Suggestions mSuggestions;

    private boolean mClosed = false;

    public SuggestionsAdapter(SuggestionViewFactory viewFactory) {
        mViewFactory = viewFactory;
    }

    public boolean isClosed() {
        return mClosed;
    }

    public void close() {
        setSuggestions(null);
        mSource = null;
        mClosed = true;
    }

    public void setSuggestions(Suggestions suggestions) {
        if (mSuggestions == suggestions) {
            return;
        }
        if (mClosed) {
            if (suggestions != null) {
                suggestions.close();
            }
            return;
        }
        if (mDataSetObserver == null) {
            mDataSetObserver = new MySuggestionsObserver();
        }
        // TODO: delay the change if there are no suggestions for the currently visible tab.
        if (mSuggestions != null) {
            mSuggestions.unregisterDataSetObserver(mDataSetObserver);
            mSuggestions.close();
        }
        mSuggestions = suggestions;
        if (mSuggestions != null) {
            mSuggestions.registerDataSetObserver(mDataSetObserver);
        }
        onSuggestionsChanged();
    }

    protected Suggestions getSuggestions() {
        return mSuggestions;
    }

    /**
     * Gets the source whose results are displayed.
     */
    public ComponentName getSource() {
        return mSource;
    }

    /**
     * Sets the source whose results are displayed.
     *
     * @param source The name of a source, or {@code null} to show
     *        the promoted results.
     */
    public void setSource(ComponentName source) {
        mSource = source;
        onSuggestionsChanged();
    }

    public int getCount() {
        return mCursor == null ? 0 : mCursor.getCount();
    }

    public SuggestionPosition getItem(int position) {
        if (mCursor == null) return null;
        return new SuggestionPosition(mCursor, position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        if (mCursor == null) {
            throw new IllegalStateException("getView() called with null cursor");
        }
        SuggestionView view;
        if (convertView == null) {
            view = mViewFactory.createSuggestionView(parent);
        } else {
            view = (SuggestionView) convertView;
        }
        mCursor.moveTo(position);
        view.bindAsSuggestion(mCursor);
        return view;
    }

    protected void onSuggestionsChanged() {
        if (DBG) Log.d(TAG, "onSuggestionsChanged(), mSuggestions=" + mSuggestions);
        SuggestionCursor cursor = getSourceCursor(mSuggestions, mSource);
        changeCursor(cursor);
    }

    /**
     * Gets the cursor for the given source.
     */
    protected SuggestionCursor getSourceCursor(Suggestions suggestions, ComponentName source) {
        if (suggestions == null) return null;
        if (source == null) return suggestions.getPromoted();
        return suggestions.getSourceResult(source);
    }

    /**
     * Replace the cursor.
     *
     * This does not close the old cursor. Instead, all the cursors are closed in
     * {@link #setSuggestions(Suggestions)}.
     */
    private void changeCursor(SuggestionCursor newCursor) {
        if (DBG) Log.d(TAG, "changeCursor(" + newCursor + ")");
        if (newCursor == mCursor) {
            return;
        }
        mCursor = newCursor;
        if (mCursor != null) {
            // TODO: Register observers here to watch for
            // changes in the cursor, e.g. shortcut refreshes?
            notifyDataSetChanged();
        } else {
            notifyDataSetInvalidated();
        }
    }

    private class MySuggestionsObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            onSuggestionsChanged();
        }
    }

}
