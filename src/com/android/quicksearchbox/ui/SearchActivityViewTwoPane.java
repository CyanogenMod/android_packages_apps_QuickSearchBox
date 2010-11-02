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
import com.android.quicksearchbox.Promoter;
import com.android.quicksearchbox.R;
import com.android.quicksearchbox.Suggestions;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

/**
 * Two-pane variant for the search activity view.
 */
public class SearchActivityViewTwoPane extends SearchActivityView {

    private static final int TINT_ANIMATION_DURATION = 400; // in millis

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
        setupWallpaperTint();
    }

    private void setupWallpaperTint() {
        // Alpha fade-in the background tint when the activity resumes.
        final Drawable drawable = getBackground();
        ValueAnimator animator = ObjectAnimator.ofInt(drawable, "alpha", 0, 255);
        // TODO: Remove this listener when the alpha animation update issue is fixed.
        animator.addUpdateListener(new AnimatorUpdateListener() {

            public void onAnimationUpdate(ValueAnimator animator) {
                drawable.invalidateSelf();
            }
        });
        animator.setDuration(TINT_ANIMATION_DURATION);
        animator.start();
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
    protected boolean shouldShowVoiceSearch(boolean queryEmpty) {
        return true;
    }

    @Override
    public void clearSuggestions() {
        super.clearSuggestions();
        mResultsAdapter.setSuggestions(null);
    }

    @Override
    public void setMaxPromotedResults(int maxPromoted) {
        mResultsView.setLimitSuggestionsToViewHeight(false);
        mResultsAdapter.setMaxPromoted(maxPromoted);
    }

    @Override
    public void limitResultsToViewHeight() {
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
    public void setEmptySpaceClickListener(final View.OnClickListener listener) {
        findViewById(R.id.panes).setOnClickListener(listener);
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
    }

    @Override
    public Corpus getSearchCorpus() {
        return getWebCorpus();
    }

}
