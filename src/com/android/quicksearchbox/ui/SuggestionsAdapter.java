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
import com.android.quicksearchbox.Promoter;
import com.android.quicksearchbox.Source;
import com.android.quicksearchbox.SuggestionCursor;
import com.android.quicksearchbox.SuggestionPosition;
import com.android.quicksearchbox.Suggestions;

import android.database.DataSetObserver;
import android.util.Log;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.HashMap;

/**
 * Uses a {@link Suggestions} object to back a {@link SuggestionsView}.
 */
public class SuggestionsAdapter extends BaseAdapter {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.SuggestionsAdapter";

    private final Corpora mCorpora;

    private DataSetObserver mDataSetObserver;

    private Promoter mPromoter;

    private int mMaxPromoted;

    private SuggestionCursor mCursor;
    private final HashMap<String, Integer> mViewTypeMap;
    private final SuggestionViewFactory mFallback;

    private Corpus mCorpus = null;

    private Suggestions mSuggestions;

    private SuggestionClickListener mSuggestionClickListener;
    private OnFocusChangeListener mOnFocusChangeListener;

    private SuggestionsAdapterChangeListener mListener;

    private final CorporaObserver mCorporaObserver = new CorporaObserver();

    private boolean mClosed = false;

    public SuggestionsAdapter(SuggestionViewFactory fallbackFactory, Corpora corpora) {
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
        mMaxPromoted = maxPromoted;
        onSuggestionsChanged();
    }

    public boolean isClosed() {
        return mClosed;
    }

    public void close() {
        setSuggestions(null);
        mCorpora.unregisterDataSetObserver(mCorporaObserver);
        mCorpus = null;
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

    /**
     * Gets the source whose results are displayed.
     */
    public Corpus getCorpus() {
        return mCorpus;
    }

    /**
     * Sets the source whose results are displayed.
     */
    public void setCorpus(Corpus corpus) {
        if (mSuggestions != null) {
            // TODO: if (mCorpus == null && corpus != null)
            // we've just switched from the 'All' corpus to a specific corpus
            // we can filter the existing results immediately.
            if (corpus != null) {
                // Note, when switching from a specific corpus to 'All' we do not change the
                // suggestions, since they're still relevant for 'All'. Hence 'corpus != null'
                if (DBG) Log.d(TAG, "setCorpus(" + corpus.getName() + ") Clear suggestions");
                mSuggestions.unregisterDataSetObserver(mDataSetObserver);
                mSuggestions.release();
                mSuggestions = null;
            }
        }
        mCorpus = corpus;
        onSuggestionsChanged();
    }

    public int getCount() {
        return mCursor == null ? 0 : mCursor.getCount();
    }

    public SuggestionPosition getItem(int position) {
        if (mCursor == null) return null;
        return new SuggestionPosition(mCursor, position);
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return mViewTypeMap.size();
    }

    private String currentSuggestionViewType() {
        String viewType = mCursor.getSuggestionSource().getSuggestionViewFactory()
                .getViewType(mCursor);
        if (!mViewTypeMap.containsKey(viewType)) {
            throw new IllegalStateException("Unknown viewType " + viewType);
        }
        return viewType;
    }

    @Override
    public int getItemViewType(int position) {
        if (DBG) Log.d(TAG, "getItemViewType(" + position + ") mCursor=" + mCursor);
        if (mCursor == null) {
            return 0;
        }
        mCursor.moveTo(position);
        return mViewTypeMap.get(currentSuggestionViewType());
    }

    // Implements Adapter#getView()
    public View getView(int position, View convertView, ViewGroup parent) {
        if (mCursor == null) {
            throw new IllegalStateException("getView() called with null cursor");
        }
        mCursor.moveTo(position);
        String viewType = currentSuggestionViewType();
        SuggestionViewFactory factory = mCursor.getSuggestionSource().getSuggestionViewFactory();
        View v = factory.getView(mCursor, mCursor.getUserQuery(), convertView, parent);
        if (v == null) {
            v = mFallback.getView(mCursor, mCursor.getUserQuery(), convertView, parent);
        }
        if (v instanceof SuggestionView) {
            ((SuggestionView) v).bindAdapter(this, position);
        } else {
            SuggestionViewClickListener l = new SuggestionViewClickListener(position);
            v.setOnClickListener(l);
            v.setOnLongClickListener(l);
        }

        if (mOnFocusChangeListener != null) {
            v.setOnFocusChangeListener(mOnFocusChangeListener);
        }
        return v;
    }

    protected void onSuggestionsChanged() {
        if (DBG) Log.d(TAG, "onSuggestionsChanged(" + mSuggestions + ")");
        SuggestionCursor cursor = getPromoted(mSuggestions, mCorpus);
        changeCursor(cursor);
    }

    /**
     * Gets the cursor containing the currently shown suggestions. The caller should not hold
     * on to or modify the returned cursor.
     */
    public SuggestionCursor getCurrentSuggestions() {
        return mCursor;
    }

    /**
     * Gets the cursor for the given source.
     */
    protected SuggestionCursor getPromoted(Suggestions suggestions, Corpus corpus) {
        if (suggestions == null) return null;
        return suggestions.getPromoted(mPromoter, mMaxPromoted);
    }

    /**
     * Replace the cursor.
     *
     * This does not close the old cursor. Instead, all the cursors are closed in
     * {@link #setSuggestions(Suggestions)}.
     */
    private void changeCursor(SuggestionCursor newCursor) {
        if (DBG) Log.d(TAG, "changeCursor(" + newCursor + ")");
        if (newCursor == mCursor) {
            if (newCursor != null) {
                // Shortcuts may have changed without the cursor changing.
                notifyDataSetChanged();
            }
            return;
        }
        mCursor = newCursor;
        if (mCursor != null) {
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

    public boolean onSuggestionLongClicked(int position) {
        if (mSuggestionClickListener != null) {
            return mSuggestionClickListener.onSuggestionLongClicked(this, position);
        }
        return false;
    }

    public void onSuggestionQueryRefineClicked(int position) {
        if (mSuggestionClickListener != null) {
            mSuggestionClickListener.onSuggestionQueryRefineClicked(this, position);
        }
    }

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

    private class SuggestionViewClickListener
            implements View.OnClickListener, View.OnLongClickListener {
        private final int mPosition;
        public SuggestionViewClickListener(int position) {
            mPosition = position;
        }
        public void onClick(View v) {
            onSuggestionClicked(mPosition);
        }
        public boolean onLongClick(View v) {
            return onSuggestionLongClicked(mPosition);
        }
    }

    /**
     * Callback interface used to notify the view when the adapter has changed (i.e. the number and
     * type of views returned has changed).
     */
    public interface SuggestionsAdapterChangeListener {
        void onSuggestionAdapterChanged();
    }

}
