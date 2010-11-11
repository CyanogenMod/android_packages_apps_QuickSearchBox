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

import com.android.quicksearchbox.Corpora;
import com.android.quicksearchbox.Corpus;
import com.android.quicksearchbox.Promoter;
import com.android.quicksearchbox.Source;
import com.android.quicksearchbox.Suggestion;
import com.android.quicksearchbox.SuggestionCursor;
import com.android.quicksearchbox.SuggestionPosition;
import com.android.quicksearchbox.Suggestions;

import android.database.DataSetObserver;
import android.util.Log;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;

import java.util.HashMap;

/**
 * Base class for suggestions adapters. The templated class A is the list adapter class.
 */
public abstract class SuggestionsAdapterBase<A> implements SuggestionsAdapter<A> {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.SuggestionsAdapter";

    private final Corpora mCorpora;

    private DataSetObserver mDataSetObserver;

    private Promoter mPromoter;

    private int mMaxPromoted;

    private SuggestionCursor mPromotedSuggestions;
    private final HashMap<String, Integer> mViewTypeMap;
    private final SuggestionViewFactory mFallback;

    private Suggestions mSuggestions;

    private SuggestionClickListener mSuggestionClickListener;
    private OnFocusChangeListener mOnFocusChangeListener;

    private SuggestionsAdapterChangeListener mListener;

    private final CorporaObserver mCorporaObserver = new CorporaObserver();

    private boolean mClosed = false;

    private boolean mIcon1Enabled = true;

    protected SuggestionsAdapterBase(SuggestionViewFactory fallbackFactory, Corpora corpora) {
        mCorpora = corpora;
        mFallback = fallbackFactory;
        mViewTypeMap = new HashMap<String, Integer>();
        mCorpora.registerDataSetObserver(mCorporaObserver);
        buildViewTypeMap();
    }

    public void setSuggestionAdapterChangeListener(SuggestionsAdapterChangeListener l) {
        mListener = l;
    }

    private boolean addViewTypes(SuggestionViewFactory f) {
        boolean changed = false;
        for (String viewType : f.getSuggestionViewTypes()) {
            if (!mViewTypeMap.containsKey(viewType)) {
                mViewTypeMap.put(viewType, mViewTypeMap.size());
                changed = true;
            }
        }
        return changed;
    }

    private boolean buildViewTypeMap() {
        boolean changed = addViewTypes(mFallback);
        for (Corpus c : mCorpora.getEnabledCorpora()) {
            for (Source s : c.getSources()) {
                SuggestionViewFactory f = s.getSuggestionViewFactory();
                changed |= addViewTypes(f);
            }
        }
        return changed;
    }

    public void setMaxPromoted(int maxPromoted) {
        if (DBG) Log.d(TAG, "setMaxPromoted " + maxPromoted);
        mMaxPromoted = maxPromoted;
        onSuggestionsChanged();
    }

    public void setIcon1Enabled(boolean enabled) {
        mIcon1Enabled = enabled;
    }

    public boolean isClosed() {
        return mClosed;
    }

    public void close() {
        setSuggestions(null);
        mCorpora.unregisterDataSetObserver(mCorporaObserver);
        mClosed = true;
    }

    public void setPromoter(Promoter promoter) {
        mPromoter = promoter;
        onSuggestionsChanged();
    }

    public void setSuggestionClickListener(SuggestionClickListener listener) {
        mSuggestionClickListener = listener;
    }

    public void setOnFocusChangeListener(OnFocusChangeListener l) {
        mOnFocusChangeListener = l;
    }

    public void setSuggestions(Suggestions suggestions) {
        if (mSuggestions == suggestions) {
            return;
        }
        if (mClosed) {
            if (suggestions != null) {
                suggestions.release();
            }
            return;
        }
        if (mDataSetObserver == null) {
            mDataSetObserver = new MySuggestionsObserver();
        }
        // TODO: delay the change if there are no suggestions for the currently visible tab.
        if (mSuggestions != null) {
            mSuggestions.unregisterDataSetObserver(mDataSetObserver);
            mSuggestions.release();
        }
        mSuggestions = suggestions;
        if (mSuggestions != null) {
            mSuggestions.registerDataSetObserver(mDataSetObserver);
        }
        onSuggestionsChanged();
    }

    public Suggestions getSuggestions() {
        return mSuggestions;
    }

    protected int getPromotedCount() {
        return mPromotedSuggestions == null ? 0 : mPromotedSuggestions.getCount();
    }

    protected SuggestionPosition getPromotedSuggestion(int position) {
        if (mPromotedSuggestions == null) return null;
        return new SuggestionPosition(mPromotedSuggestions, position);
    }

    protected int getViewTypeCount() {
        return mViewTypeMap.size();
    }

    private String suggestionViewType(Suggestion suggestion) {
        String viewType = suggestion.getSuggestionSource().getSuggestionViewFactory()
                .getViewType(suggestion);
        if (!mViewTypeMap.containsKey(viewType)) {
            throw new IllegalStateException("Unknown viewType " + viewType);
        }
        return viewType;
    }

    protected int getSuggestionViewType(SuggestionCursor cursor, int position) {
        if (cursor == null) {
            return 0;
        }
        cursor.moveTo(position);
        return mViewTypeMap.get(suggestionViewType(cursor));
    }

    protected int getSuggestionViewTypeCount() {
        return mViewTypeMap.size();
    }

    protected View getView(SuggestionCursor suggestions, int position,
            View convertView, ViewGroup parent) {
        suggestions.moveTo(position);
        SuggestionViewFactory factory = suggestions.getSuggestionSource().getSuggestionViewFactory();
        View v = factory.getView(suggestions, suggestions.getUserQuery(), convertView, parent);
        if (v == null) {
            v = mFallback.getView(suggestions, suggestions.getUserQuery(), convertView, parent);
        }
        if (v instanceof SuggestionView) {
            ((SuggestionView) v).setIcon1Enabled(mIcon1Enabled);
            ((SuggestionView) v).bindAdapter(this, position);
        } else {
            SuggestionViewClickListener l = new SuggestionViewClickListener(position);
            v.setOnClickListener(l);
        }

        if (mOnFocusChangeListener != null) {
            v.setOnFocusChangeListener(mOnFocusChangeListener);
        }
        return v;
    }

    protected void onSuggestionsChanged() {
        if (DBG) Log.d(TAG, "onSuggestionsChanged(" + mSuggestions + ")");
        SuggestionCursor cursor = getPromoted(mSuggestions);
        changePromoted(cursor);
    }

    /**
     * Gets the cursor containing the currently shown suggestions. The caller should not hold
     * on to or modify the returned cursor.
     */
    public SuggestionCursor getCurrentPromotedSuggestions() {
        return mPromotedSuggestions;
    }

    /**
     * Gets the cursor for the given source.
     */
    protected SuggestionCursor getPromoted(Suggestions suggestions) {
        if (suggestions == null) return null;
        return suggestions.getPromoted(mPromoter, mMaxPromoted);
    }

    /**
     * Replace the cursor.
     *
     * This does not close the old cursor. Instead, all the cursors are closed in
     * {@link #setSuggestions(Suggestions)}.
     */
    private void changePromoted(SuggestionCursor newCursor) {
        if (DBG) {
            Log.d(TAG, "changeCursor(" + newCursor + ") count=" +
                    (newCursor == null ? 0 : newCursor.getCount()));
        }
        if (newCursor == mPromotedSuggestions) {
            if (newCursor != null) {
                // Shortcuts may have changed without the cursor changing.
                notifyDataSetChanged();
            }
            return;
        }
        mPromotedSuggestions = newCursor;
        if (mPromotedSuggestions != null) {
            notifyDataSetChanged();
        } else {
            notifyDataSetInvalidated();
        }
    }

    public void onSuggestionClicked(int position) {
        if (mSuggestionClickListener != null) {
            mSuggestionClickListener.onSuggestionClicked(this, position);
        }
    }

    public void onSuggestionQuickContactClicked(int position) {
        if (mSuggestionClickListener != null) {
            mSuggestionClickListener.onSuggestionQuickContactClicked(this, position);
        }
    }

    public void onSuggestionRemoveFromHistoryClicked(int position) {
        if (mSuggestionClickListener != null) {
            mSuggestionClickListener.onSuggestionRemoveFromHistoryClicked(this, position);
        }
    }

    public void onSuggestionQueryRefineClicked(int position) {
        if (mSuggestionClickListener != null) {
            mSuggestionClickListener.onSuggestionQueryRefineClicked(this, position);
        }
    }

    public abstract A getListAdapter();

    protected abstract void notifyDataSetInvalidated();

    protected abstract void notifyDataSetChanged();

    private class MySuggestionsObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            onSuggestionsChanged();
        }
    }

    private class CorporaObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            if (buildViewTypeMap()) {
                if (mListener != null) {
                    mListener.onSuggestionAdapterChanged();
                }
            }
        }
    }

    private class SuggestionViewClickListener implements View.OnClickListener {
        private final int mPosition;
        public SuggestionViewClickListener(int position) {
            mPosition = position;
        }
        public void onClick(View v) {
            onSuggestionClicked(mPosition);
        }
    }

}
