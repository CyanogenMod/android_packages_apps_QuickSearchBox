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

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Shows all suggestions in a tabbed interface.
 *
 */
public class SuggestionsView extends TabView {

    private static final boolean DBG = true;
    private static final String TAG = "QSB.SuggestionsView";

    private SuggestionClickListener mSuggestionClickListener;

    private InteractionListener mInteractionListener;

    public SuggestionsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setSuggestionClickListener(SuggestionClickListener listener) {
        mSuggestionClickListener = listener;
    }

    public void setInteractionListener(InteractionListener listener) {
        mInteractionListener = listener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && mInteractionListener != null) {
            mInteractionListener.onInteraction();
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override
    protected SuggestionListView createTabContentView(int position) {
        SuggestionListView view = (SuggestionListView) super.createTabContentView(position);
        view.setSuggestionClickListener(mSuggestionClickListener);
        return view;
    }

    public interface InteractionListener {
        /**
         * Called when the user interacts with this view.
         */
        void onInteraction();
    }

}
