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

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * List adapter backed by a {@link SuggestionCursor}.
 */
public class SuggestionCursorAdapter extends BaseAdapter {

    private static final boolean DBG = true;
    private static final String TAG = "QSB.SuggestionCursorAdapter";

    private final SuggestionViewFactory mViewFactory;

    private SuggestionCursor mCursor;

    public SuggestionCursorAdapter(SuggestionViewFactory viewFactory) {
        mViewFactory = viewFactory;
    }

    /**
     * Replace the cursor.
     *
     * This does not close the old cursor. We rely on {@link SuggestionsAdapter}
     * for that.
     */
    public void changeCursor(SuggestionCursor newCursor) {
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

    public int getCount() {
        return mCursor == null ? 0 : mCursor.getCount();
    }

    public Object getItem(int position) {
        // Unused
        return null;
    }

    public long getItemId(int position) {
        return position;  // We don't have any IDs for suggestions.
    }

    public SuggestionView getView(int position, View convertView, ViewGroup parent) {
        if (DBG) Log.d(TAG, "getView(" + position + ")");
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

}
