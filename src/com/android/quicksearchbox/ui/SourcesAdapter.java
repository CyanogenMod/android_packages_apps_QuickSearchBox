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
import com.android.quicksearchbox.SuggestionsProvider;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;

/**
 * Adapter for showing a list of sources in the source selection activity.
 */
public class SourcesAdapter extends BaseAdapter {

    private final SuggestionViewFactory mViewFactory;

    private final SuggestionsProvider mProvider;

    private ArrayList<Source> mEnabledSources;

    public SourcesAdapter(SuggestionViewFactory viewFactory, SuggestionsProvider provider) {
        mViewFactory = viewFactory;
        mProvider = provider;
        updateSources();
    }

    private void updateSources() {
        mEnabledSources = mProvider.getOrderedSources();
    }

    public int getCount() {
        return 1 + mEnabledSources.size();
    }

    public Source getItem(int position) {
        if (position == 0) {
            return null;
        } else {
            return mEnabledSources.get(position - 1);
        }
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        SourceView view = (SourceView) convertView;
        if (view == null) {
            view = mViewFactory.createSourceView(parent);
        }
        Source source = getItem(position);
        if (source == null) {
            view.setIcon(mViewFactory.getGlobalSearchIcon());
            view.setLabel(mViewFactory.getGlobalSearchLabel());
        } else {
            view.setIcon(source.getSourceIcon());
            view.setLabel(source.getLabel());
        }
        return view;
    }
}
