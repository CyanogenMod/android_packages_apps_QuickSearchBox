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

import com.android.quicksearchbox.Promoter;
import com.android.quicksearchbox.SuggestionCursor;
import com.android.quicksearchbox.Suggestions;

import android.database.DataSetObserver;
import android.util.Log;
import android.view.View.OnFocusChangeListener;

/**
 * A {@link SuggestionsListAdapter} that doesn't expose the new suggestions
 * until there are some results to show.
 */
public class DelayingSuggestionsAdapter<A> implements SuggestionsAdapter<A> {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.DelayingSuggestionsAdapter";

    private DataSetObserver mPendingDataSetObserver;

    private Suggestions mPendingSuggestions;

    private final SuggestionsAdapterBase<A> mDelayedAdapter;

    public DelayingSuggestionsAdapter(SuggestionsAdapterBase<A> delayed) {
        mDelayedAdapter = delayed;
    }

    public void close() {
        setPendingSuggestions(null);
        mDelayedAdapter.close();
    }

    @Override
    public void setSuggestions(Suggestions suggestions) {
        if (suggestions == null) {
            mDelayedAdapter.setSuggestions(null);
            setPendingSuggestions(null);
            return;
        }
        if (shouldPublish(suggestions)) {
            if (DBG) Log.d(TAG, "Publishing suggestions immediately: " + suggestions);
            mDelayedAdapter.setSuggestions(suggestions);
            // Clear any old pending suggestions.
            setPendingSuggestions(null);
        } else {
            if (DBG) Log.d(TAG, "Delaying suggestions publishing: " + suggestions);
            setPendingSuggestions(suggestions);
        }
    }

    /**
     * Gets whether the given suggestions are non-empty for the selected source.
     */
    private boolean shouldPublish(Suggestions suggestions) {
        if (suggestions.isDone()) return true;
        SuggestionCursor cursor = mDelayedAdapter.getPromoted(suggestions);
        return cursor != null && cursor.getCount() > 0;
    }

    private void setPendingSuggestions(Suggestions suggestions) {
        if (mPendingSuggestions == suggestions) {
            return;
        }
        if (mDelayedAdapter.isClosed()) {
            if (suggestions != null) {
                suggestions.release();
            }
            return;
        }
        if (mPendingDataSetObserver == null) {
            mPendingDataSetObserver = new PendingSuggestionsObserver();
        }
        if (mPendingSuggestions != null) {
            mPendingSuggestions.unregisterDataSetObserver(mPendingDataSetObserver);
            // Close old suggestions, but only if they are not also the current
            // suggestions.
            if (mPendingSuggestions != getSuggestions()) {
                mPendingSuggestions.release();
            }
        }
        mPendingSuggestions = suggestions;
        if (mPendingSuggestions != null) {
            mPendingSuggestions.registerDataSetObserver(mPendingDataSetObserver);
        }
    }

    protected void onPendingSuggestionsChanged() {
        if (DBG) {
            Log.d(TAG, "onPendingSuggestionsChanged(), mPendingSuggestions="
                    + mPendingSuggestions);
        }
        if (shouldPublish(mPendingSuggestions)) {
            if (DBG) Log.d(TAG, "Suggestions now available, publishing: " + mPendingSuggestions);
            mDelayedAdapter.setSuggestions(mPendingSuggestions);
            // The suggestions are no longer pending.
            setPendingSuggestions(null);
        }
    }

    private class PendingSuggestionsObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            onPendingSuggestionsChanged();
        }
    }

    public A getListAdapter() {
        return mDelayedAdapter.getListAdapter();
    }

    public SuggestionCursor getCurrentPromotedSuggestions() {
        return mDelayedAdapter.getCurrentPromotedSuggestions();
    }

    public Suggestions getSuggestions() {
        return mDelayedAdapter.getSuggestions();
    }

    public void onSuggestionClicked(int position) {
        mDelayedAdapter.onSuggestionClicked(position);
    }

    public void onSuggestionQueryRefineClicked(int position) {
        mDelayedAdapter.onSuggestionQueryRefineClicked(position);
    }

    public void onSuggestionQuickContactClicked(int position) {
        mDelayedAdapter.onSuggestionQuickContactClicked(position);
    }

    public void onSuggestionRemoveFromHistoryClicked(int position) {
        mDelayedAdapter.onSuggestionRemoveFromHistoryClicked(position);
    }

    public void setMaxPromoted(int maxPromoted) {
        mDelayedAdapter.setMaxPromoted(maxPromoted);
    }

    public void setOnFocusChangeListener(OnFocusChangeListener l) {
        mDelayedAdapter.setOnFocusChangeListener(l);
    }

    @Override
    public void setPromoter(Promoter promoter) {
        mDelayedAdapter.setPromoter(promoter);
    }

    public void setSuggestionAdapterChangeListener(SuggestionsAdapterChangeListener l) {
        mDelayedAdapter.setSuggestionAdapterChangeListener(l);
    }

    public void setSuggestionClickListener(SuggestionClickListener listener) {
        mDelayedAdapter.setSuggestionClickListener(listener);
    }

    public void setIcon1Enabled(boolean enabled) {
        mDelayedAdapter.setIcon1Enabled(enabled);
    }

}
