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
import com.android.quicksearchbox.R;
import com.android.quicksearchbox.Suggestions;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Two-pane variant for the search activity view.
 */
public class SearchActivityViewTwoPane extends SearchActivityView {

    private ImageView mSettingsButton;

    // View that shows the results other than the query completions
    private SuggestionsView mResultsView;
    private SuggestionsAdapter mResultsAdapter;

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
        super.destroy();
    }

    @Override
    protected void considerHidingInputMethod() {
        // Don't hide keyboard when interacting with suggestions list
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
        mResultsAdapter.setMaxPromoted(maxPromoted);
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
    public void setCorpus(Corpus corpus) {
        getSuggestionsAdapter().setPromoter(getQsbApplication().createWebPromoter());

        mResultsAdapter.setCorpus(corpus);
        if (corpus == null) {
            mResultsAdapter.setPromoter(getQsbApplication().createResultsPromoter());
        } else {
            mResultsAdapter.setPromoter(getQsbApplication().createSingleCorpusPromoter());
        }

        updateUi(getQuery().length() == 0);
    }

    @Override
    public Corpus getSearchCorpus() {
        return getWebCorpus();
    }

    @Override
    public Corpus getCorpus() {
        return null;
    }

}
