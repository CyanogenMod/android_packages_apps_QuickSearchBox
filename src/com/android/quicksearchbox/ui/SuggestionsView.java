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
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

/**
 * Holds a list of suggestions.
 */
public class SuggestionsView extends ListView {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.SuggestionsView";

    private SuggestionClickListener mSuggestionClickListener;

    private SuggestionSelectionListener mSuggestionSelectionListener;

    private InteractionListener mInteractionListener;

    public SuggestionsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        setOnItemClickListener(new ItemClickListener());
        setOnItemLongClickListener(new ItemLongClickListener());
        setOnItemSelectedListener(new ItemSelectedListener());
    }

    public void setSuggestionClickListener(SuggestionClickListener listener) {
        mSuggestionClickListener = listener;
    }

    public void setSuggestionSelectionListener(SuggestionSelectionListener listener) {
        mSuggestionSelectionListener = listener;
    }

    public void setInteractionListener(InteractionListener listener) {
        mInteractionListener = listener;
    }

    /**
     * Gets the position of the selected suggestion.
     *
     * @return A 0-based index, or {@code -1} if no suggestion is selected.
     */
    public int getSelectedPosition() {
        return getSelectedItemPosition();
    }

    /**
     * Gets the selected suggestion.
     *
     * @return {@code null} if no suggestion is selected.
     */
    public SuggestionPosition getSelectedSuggestion() {
        return (SuggestionPosition) getSelectedItem();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && mInteractionListener != null) {
            mInteractionListener.onInteraction();
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (DBG) {
            Log.d(TAG, "Suggestions focus change, gainFocus: " + gainFocus
                    + ", selected=" + getSelectedItemPosition());
        }
        // In non-touch mode, ListView does not clear the list selection when
        // the ListView loses focus. And when it regains focus, onItemSelected() never gets
        // called if the new selected position is the same as the old. We work around that
        // by firing extra selection events on focus changes in non-touch mode.
        // This implementation can result in duplicate selection events when the old selected
        // item is not the same as the new.
        if (!isInTouchMode()) {
            if (gainFocus) {
                int position = getSelectedPosition();
                if (position != INVALID_POSITION) {
                    fireSuggestionSelected(position);
                }
            } else {
                fireNothingSelected();
            }
        }
    }

    private void fireSuggestionSelected(int position) {
        if (DBG) Log.d(TAG, "fireSuggestionSelected(" + position + ")");
        if (mSuggestionSelectionListener != null) {
            mSuggestionSelectionListener.onSuggestionSelected(position);
        }
    }

    private void fireNothingSelected() {
        if (DBG) Log.d(TAG, "fireNothingSelected()");
        if (mSuggestionSelectionListener != null) {
            mSuggestionSelectionListener.onNothingSelected();
        }
    }

    public interface InteractionListener {
        /**
         * Called when the user interacts with this view.
         */
        void onInteraction();
    }

    private class ItemClickListener implements AdapterView.OnItemClickListener {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (DBG) Log.d(TAG, "onItemClick(" + position + ")");
            SuggestionView suggestionView = (SuggestionView) view;
            if (mSuggestionClickListener != null) {
                mSuggestionClickListener.onSuggestionClicked(position);
            }
        }
    }

    private class ItemLongClickListener implements AdapterView.OnItemLongClickListener {
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            if (DBG) Log.d(TAG, "onItemLongClick(" + position + ")");
            SuggestionView suggestionView = (SuggestionView) view;
            if (mSuggestionClickListener != null) {
                return mSuggestionClickListener.onSuggestionLongClicked(position);
            }
            return false;
        }
    }

    private class ItemSelectedListener implements AdapterView.OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            // Only fire suggestion selection events when the list has focus.
            // This suppresses selection events caused by data set changes (as opposed
            // to user action).
            if (hasFocus()) {
                fireSuggestionSelected(position);
            } else {
                if (DBG) Log.d(TAG, "Suppressed selection event for position " + position);
            }
        }

        public void onNothingSelected(AdapterView<?> parent) {
            fireNothingSelected();
        }
    }
}
