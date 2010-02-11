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

import com.android.quicksearchbox.Corpora;
import com.android.quicksearchbox.Corpus;
import com.android.quicksearchbox.CorpusRanker;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;

/**
 * Adapter for showing a list of sources in the source selection activity.
 */
public class CorporaAdapter extends BaseAdapter {

    private final SuggestionViewFactory mViewFactory;

    private ArrayList<Corpus> mRankedEnabledCorpora;

    public CorporaAdapter(SuggestionViewFactory viewFactory, Corpora corpora,
            CorpusRanker ranker) {
        mViewFactory = viewFactory;
        mRankedEnabledCorpora = ranker.rankCorpora(corpora.getEnabledCorpora());
    }

    public int getCount() {
        return 1 + mRankedEnabledCorpora.size();
    }

    public Corpus getItem(int position) {
        if (position == 0) {
            return null;
        } else {
            return mRankedEnabledCorpora.get(position - 1);
        }
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        CorpusView view = (CorpusView) convertView;
        if (view == null) {
            view = mViewFactory.createSourceView(parent);
        }
        Corpus corpus = getItem(position);
        if (corpus == null) {
            view.setIcon(mViewFactory.getGlobalSearchIcon());
            view.setLabel(mViewFactory.getGlobalSearchLabel());
        } else {
            view.setIcon(corpus.getCorpusIcon());
            view.setLabel(corpus.getLabel());
        }
        return view;
    }
}
