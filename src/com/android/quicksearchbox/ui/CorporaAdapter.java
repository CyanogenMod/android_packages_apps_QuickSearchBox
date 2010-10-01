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

import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Adapter for showing a list of sources in the source selection activity.
 */
public class CorporaAdapter extends BaseAdapter {

    private static final String TAG = "CorporaAdapter";
    private static final boolean DBG = false;

    private final CorpusViewFactory mViewFactory;

    private final Corpora mCorpora;

    private final boolean mGridView;

    private final DataSetObserver mCorporaObserver = new CorporaObserver();

    private List<Corpus> mSortedCorpora;

    public CorporaAdapter(CorpusViewFactory viewFactory,
            Corpora corpora, boolean gridView) {
        mViewFactory = viewFactory;
        mCorpora = corpora;
        mGridView = gridView;
        mCorpora.registerDataSetObserver(mCorporaObserver);
        updateCorpora();
    }

    private void updateCorpora() {
        List<Corpus> enabledCorpora = mCorpora.getEnabledCorpora();
        ArrayList<Corpus> sorted = new ArrayList<Corpus>(enabledCorpora.size());
        for (Corpus corpus : enabledCorpora) {
            if (!corpus.isCorpusHidden()) {
                sorted.add(corpus);
            }
        }
        Collections.sort(sorted, new CorpusComparator());
        mSortedCorpora = sorted;
        notifyDataSetChanged();
    }

    private static class CorpusComparator implements Comparator<Corpus> {
        public int compare(Corpus corpus1, Corpus corpus2) {
            // Comparing a corpus against itself
            if (corpus1 == corpus2) return 0;
            // Web always comes first
            if (corpus1.isWebCorpus()) return -1;
            if (corpus2.isWebCorpus()) return 1;
            // Alphabetically by name
            return corpus1.getLabel().toString().compareTo(corpus2.getLabel().toString());
        }
    }

    public void close() {
        mCorpora.unregisterDataSetObserver(mCorporaObserver);
    }

    public int getCount() {
        return 1 + (mSortedCorpora == null ? 0 : mSortedCorpora.size());
    }

    public Corpus getItem(int position) {
        if (position == 0) {
            return null;
        } else {
            return mSortedCorpora.get(position - 1);
        }
    }

    public long getItemId(int position) {
        return position;
    }

    /**
     * Gets the position of the given corpus.
     */
    public int getCorpusPosition(Corpus corpus) {
        if (corpus == null) {
            return 0;
        }
        int count = getCount();
        for (int i = 0; i < count; i++) {
            if (corpus.equals(getItem(i))) {
                return i;
            }
        }
        Log.w(TAG, "Corpus not in adapter: " + corpus);
        return 0;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        CorpusView view = (CorpusView) convertView;
        if (view == null) {
            view = createView(parent);
        }
        Corpus corpus = getItem(position);
        Drawable icon;
        CharSequence label;
        if (corpus == null) {
            icon = mViewFactory.getGlobalSearchIcon();
            label = mViewFactory.getGlobalSearchLabel();
        } else {
            icon = corpus.getCorpusIcon();
            label = corpus.getLabel();
        }
        if (DBG) Log.d(TAG, "Binding " + position + ", label=" + label);
        view.setIcon(icon);
        view.setLabel(label);
        return view;
    }

    protected CorpusView createView(ViewGroup parent) {
        if (mGridView) {
            return mViewFactory.createGridCorpusView(parent);
        } else {
            return mViewFactory.createListCorpusView(parent);
        }
    }

    private class CorporaObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            updateCorpora();
        }

        @Override
        public void onInvalidated() {
            updateCorpora();
        }
    }

}
