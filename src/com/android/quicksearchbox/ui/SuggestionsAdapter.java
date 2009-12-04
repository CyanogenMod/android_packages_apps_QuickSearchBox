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

import com.android.quicksearchbox.Source;
import com.android.quicksearchbox.SuggestionCursor;
import com.android.quicksearchbox.Suggestions;

import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

/**
 * Uses a {@link Suggestions} object to back a {@link TabView}.
 */
public class SuggestionsAdapter implements TabAdapter {

    private static final boolean DBG = true;
    private static final String TAG = "QSB.SuggestionsAdapter";

    private long mSourceResultPublishDelayMillis;
    private long mInitialSourceResultWaitMillis;

    private final DataSetObservable mDataSetObservable = new DataSetObservable();

    private final SuggestionViewFactory mViewFactory;

    private DataSetObserver mDataSetObserver;

    /**
     * The tabs, in their display order.
     */
    private final ArrayList<Tab> mTabs = new ArrayList<Tab>();

    private Suggestions mSuggestions;

    private boolean mClosed = false;

    public SuggestionsAdapter(SuggestionViewFactory viewFactory) {
        mViewFactory = viewFactory;
    }

    public void setSourceResultPublishDelayMillis(long millis) {
        mSourceResultPublishDelayMillis = millis;
    }

    public void setInitialSourceResultWaitMillis(long millis) {
        mInitialSourceResultWaitMillis = millis;
    }

    public int getTabCount() {
        return mTabs.size();
    }

    public void setSources(ArrayList<Source> sources) {
        setSuggestions(null);
        mTabs.clear();
        SuggestionCursorAdapter promoted = new SuggestionCursorAdapter(mViewFactory);
        mTabs.add(new PromotedTab(promoted));
        int count = sources.size();
        for (int i = 0; i < count; i++) {
            Source source = sources.get(i);
            // TODO: Each source should specify its own view factory
            SuggestionCursorAdapter adapter = new SuggestionCursorAdapter(mViewFactory);
            mTabs.add(new SourceTab(source, adapter));
        }
        notifyDataSetChanged();
    }

    public void close() {
        setSuggestions(null);
        mTabs.clear();
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

    public View getTabContentView(int position, ViewGroup parent) {
        if (DBG) Log.d(TAG, "getTabContent(" + position + ")");
        return getTab(position).getListView(parent);
    }

    public View getTabHandleView(int position, ViewGroup parent) {
        return getTab(position).getTabHandleView(parent);
    }

    private Tab getTab(int position) {
        if (mClosed) {
            throw new IllegalStateException("SuggestionsAdapter is closed.");
        }
        return mTabs.get(position);
    }

    public String getTag(int position) {
        return String.valueOf(position);
    }

    public int getTabPosition(String tag) {
        return Integer.parseInt(tag);
    }

    public void registerDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.registerObserver(observer);
    }

    public void unregisterDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.unregisterObserver(observer);
    }

    protected void notifyDataSetChanged() {
        mDataSetObservable.notifyChanged();
    }

    protected void notifyDataSetInvalidated() {
        mDataSetObservable.notifyInvalidated();
    }

    protected void onSuggestionsChanged() {
        if (DBG) Log.d(TAG, "onSuggestionsChanged(), mSuggestions=" + mSuggestions);
        // TODO: It's inefficient to change all cursors every time a
        // new one is added to Suggestions, we should get a set of
        // changed ones in the call.
        for (Tab tab : mTabs) {
            tab.update();
        }
    }

    private class MySuggestionsObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            onSuggestionsChanged();
        }
    }

    private abstract class Tab {
        private final SuggestionCursorAdapter mAdapter;

        public Tab(SuggestionCursorAdapter adapter) {
            mAdapter = adapter;
        }

        public SuggestionCursorAdapter getAdapter() {
            return mAdapter;
        }

        public void update() {
            if (mSuggestions == null) {
                mAdapter.changeCursor(null);
            } else {
                mAdapter.changeCursor(getCursor(mSuggestions));
            }
        }

        protected abstract SuggestionCursor getCursor(Suggestions suggestions);

        protected abstract Drawable getIcon();

        public View getTabHandleView(ViewGroup parent) {
            TabHandleView view = mViewFactory.createSuggestionTabView(parent);
            view.setIcon(getIcon());
            return view;
        }

        public View getListView(ViewGroup parent) {
            if (DBG) Log.d(TAG, "getListView()");
            SuggestionListView view = mViewFactory.createSuggestionListView(parent);
            view.setAdapter(mAdapter);
            return view;
        }
    }

    private class PromotedTab extends Tab {

        public PromotedTab(SuggestionCursorAdapter adapter) {
            super(adapter);
        }

        @Override
        protected SuggestionCursor getCursor(Suggestions suggestions) {
            return suggestions.getPromoted();
        }

        @Override
        protected Drawable getIcon() {
            return mViewFactory.getPromotedIcon();
        }
    }

    private class SourceTab extends Tab {
        private final Source mSource;

        public SourceTab(Source source, SuggestionCursorAdapter adapter) {
            super(adapter);
            mSource = source;
        }

        @Override
        protected SuggestionCursor getCursor(Suggestions suggestions) {
            return suggestions.getSourceResult(mSource.getComponentName());
        }

        @Override
        protected Drawable getIcon() {
            return mSource.getSourceIcon();
        }
    }

}
