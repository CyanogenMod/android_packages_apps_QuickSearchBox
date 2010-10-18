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

package com.android.quicksearchbox.ui;

import com.android.quicksearchbox.Corpus;
import com.android.quicksearchbox.CorpusResult;
import com.android.quicksearchbox.Promoter;
import com.android.quicksearchbox.R;
import com.android.quicksearchbox.ShortcutCursor;
import com.android.quicksearchbox.Suggestion;
import com.android.quicksearchbox.SuggestionCursor;
import com.android.quicksearchbox.Suggestions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;

/**
 * Two-pane variant for the search activity view.
 */
public class SearchActivityViewTwoPane extends SearchActivityView {

    private ImageView mSettingsButton;

    // View that shows the results other than the query completions
    private SuggestionsView mResultsView;
    private SuggestionsAdapter mResultsAdapter;

    // Corpus selection view
    private AbsListView mCorpusListView;
    private CorporaAdapter mCorporaAdapter;

    public SearchActivityViewTwoPane(Context context) {
        super(context);
    }

    public SearchActivityViewTwoPane(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SearchActivityViewTwoPane(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mSettingsButton = (ImageView) findViewById(R.id.settings_icon);

        mResultsView = (SuggestionsView) findViewById(R.id.shortcuts);
        mResultsAdapter = createSuggestionsAdapter();
        mResultsAdapter.setSuggestionAdapterChangeListener(this);
        mResultsView.setOnKeyListener(new SuggestionsViewKeyListener());

        mCorpusListView = (AbsListView) findViewById(R.id.corpus_list);
        mCorpusListView.setOnItemClickListener(new CorpusClickListener());
        mCorporaAdapter = new CorporaAdapter(getContext(), getCorpora(),
                R.layout.corpus_grid_item);
        mCorpusListView.setAdapter(mCorporaAdapter);
    }

    @Override
    public void onSuggestionAdapterChanged() {
        mResultsView.setAdapter(mResultsAdapter);
        super.onSuggestionAdapterChanged();
    }

    @Override
    public void onResume() {
    }

    @Override
    public void onStop() {
    }

    @Override
    public void start() {
        super.start();
        mResultsView.setAdapter(mResultsAdapter);
    }

    @Override
    public void destroy() {
        mResultsView.setAdapter(null);

        mCorpusListView.setAdapter(null);
        mCorporaAdapter.close();

        super.destroy();
    }

    @Override
    public void considerHidingInputMethod() {
        // Don't hide keyboard when interacting with suggestions list
    }

    @Override
    public void hideSuggestions() {
        // Never hiding suggestions view in two-pane UI
    }

    @Override
    public void showSuggestions() {
        // Never hiding suggestions view in two-pane UI
    }

    @Override
    public void showCorpusSelectionDialog() {
        // not used
    }

    @Override
    public void clearSuggestions() {
        super.clearSuggestions();
        mResultsAdapter.setSuggestions(null);
    }

    @Override
    public void setMaxPromoted(int maxPromoted) {
        super.setMaxPromoted(maxPromoted);
        mResultsView.setLimitSuggestionsToViewHeight(false);
        mResultsAdapter.setMaxPromoted(maxPromoted);
    }

    @Override
    public void limitSuggestionsToViewHeight() {
        super.limitSuggestionsToViewHeight();
        mResultsView.setLimitSuggestionsToViewHeight(true);
    }


    @Override
    public void setSettingsButtonClickListener(OnClickListener listener) {
        mSettingsButton.setOnClickListener(listener);
    }

    @Override
    public void setSuggestionClickListener(SuggestionClickListener listener) {
        super.setSuggestionClickListener(listener);
        mResultsAdapter.setSuggestionClickListener(listener);
    }

    @Override
    public void setSuggestions(Suggestions suggestions) {
        super.setSuggestions(suggestions);
        suggestions.acquire();
        mResultsAdapter.setSuggestions(suggestions);
    }

    @Override
    protected void setCorpus(Corpus corpus) {
        super.setCorpus(corpus);

        mCorporaAdapter.setCurrentCorpus(getCorpus());
        mResultsAdapter.setPromoter(createResultsPromoter());
    }

    @Override
    protected Promoter createSuggestionsPromoter() {
        return getQsbApplication().createWebPromoter();
    }

    protected Promoter createResultsPromoter() {
        Corpus corpus = getCorpus();
        if (corpus == null) {
            return getQsbApplication().createResultsPromoter();
        } else {
            return getQsbApplication().createSingleCorpusResultsPromoter(corpus);
        }
    }

    @Override
    protected void onSuggestionsChanged() {
        super.onSuggestionsChanged();
        mCorporaAdapter.setCorpusResultCounts(getCorpusResultCounts(getSuggestions()));
    }

    private Multiset<String> getCorpusResultCounts(Suggestions suggestions) {
        if (suggestions == null) return null;
        Multiset<String> counts = HashMultiset.create();
        ShortcutCursor shortcuts = suggestions.getShortcuts();
        if (shortcuts != null) {
            for (int i = 0; i < shortcuts.getCount(); i++) {
                shortcuts.moveTo(i);
                if (!shortcuts.isWebSearchSuggestion()) {
                    counts.add(getSuggestionCorpusName(shortcuts));
                }
            }
        }
        for (CorpusResult corpusResult : suggestions.getCorpusResults()) {
            int count = corpusResult.getCount();
            if (corpusResult.getCorpus().isWebCorpus()) {
                count = countNonWebSuggestions(corpusResult);
            }
            counts.add(corpusResult.getCorpus().getName(), count);
        }
        return counts;
    }

    private int countNonWebSuggestions(SuggestionCursor c) {
        int count = 0;
        for (int i = 0; i < c.getCount(); i++) {
            c.moveTo(i);
            if (!c.isWebSearchSuggestion()) count++;
        }
        return count;
    }

    private String getSuggestionCorpusName(Suggestion s) {
        Corpus corpus = getSuggestionCorpus(s);
        return corpus == null ? null : corpus.getName();
    }

    private Corpus getSuggestionCorpus(Suggestion s) {
        return getCorpora().getCorpusForSource(s.getSuggestionSource());
    }

    @Override
    public Corpus getSearchCorpus() {
        return getWebCorpus();
    }

    private class CorpusClickListener implements AdapterView.OnItemClickListener {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Corpus corpus = (Corpus) parent.getItemAtPosition(position);
            if (DBG) Log.d(TAG, "Corpus selected: " + corpus);
            String corpusName = corpus == null ? null : corpus.getName();
            SearchActivityViewTwoPane.this.onCorpusSelected(corpusName);
        }
    }

}
