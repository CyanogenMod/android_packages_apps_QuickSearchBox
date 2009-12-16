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

import com.android.quicksearchbox.SuggestionPosition;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;

import java.util.ArrayList;

/**
 * View for a list of suggestions.
 */
public class SuggestionListView extends LinearLayout {

    private static final boolean DBG = true;
    private static final String TAG = "QSB.SuggestionListView";

    private static final int RECYCLING_BIN_CAPACITY = 20;

    private DataSetObserver mDataSetObserver;

    private SuggestionCursorAdapter mAdapter;

    private SuggestionClickListener mSuggestionClickListener;

    private final ArrayList<SuggestionView> mRecyclingBin
            = new ArrayList<SuggestionView>(RECYCLING_BIN_CAPACITY);

    public SuggestionListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SuggestionListView(Context context) {
        super(context);
    }

    public void setSuggestionClickListener(SuggestionClickListener clickListener) {
        mSuggestionClickListener = clickListener;
    }

    public SuggestionCursorAdapter getAdapter() {
        return mAdapter;
    }

    public int getCount() {
        return mAdapter == null ? 0 : mAdapter.getCount();
    }

    public int getSelectedPosition() {
        SuggestionView view = (SuggestionView) getFocusedChild();
        if (view == null) return -1;
        return indexOfChild(view);  // TODO: this is a linear search, not great
    }

    public SuggestionPosition getSelectedSuggestion() {
        SuggestionView view = (SuggestionView) getFocusedChild();
        if (view == null) return null;
        return view.getSuggestionPosition();
    }

    public void setAdapter(SuggestionCursorAdapter adapter) {
        if (mAdapter == adapter) {
            return;
        }
        if (mDataSetObserver == null) {
            mDataSetObserver = new AdapterObserver();
        }
        if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(mDataSetObserver);
        }
        mAdapter = adapter;
        if (mAdapter != null) {
            mAdapter.registerDataSetObserver(mDataSetObserver);
        }
        onDataSetChanged();
    }

    @Override
    public void removeAllViews() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            addToRecyclingBin((SuggestionView) getChildAt(i));
        }
        super.removeAllViews();
    }

    private void addToRecyclingBin(SuggestionView view) {
        if (mRecyclingBin.size() < RECYCLING_BIN_CAPACITY) {
            if (DBG) Log.d(TAG, "Storing SuggestionView for recycling. parent=" + view.getParent());
            mRecyclingBin.add(view);
            // TODO: clear old drawables etc to free memory?
        }
    }

    private SuggestionView getFromRecyclingBin() {
        int size = mRecyclingBin.size();
        if (size > 0) {
            if (DBG) Log.d(TAG, "Recycling SuggestionView.");
            return mRecyclingBin.remove(size - 1);
        } else {
            return null;
        }
    }

    protected void onDataSetChanged() {
        removeAllViews();
        int count = getCount();
        for (int i = 0; i < count; i++) {
            SuggestionView recycled = getFromRecyclingBin();
            SuggestionView view = mAdapter.getView(i, recycled, this);
            view.setSuggestionClickListener(mSuggestionClickListener);
            addView(view);
        }
    }

    private class AdapterObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            onDataSetChanged();
        }
    }

}
