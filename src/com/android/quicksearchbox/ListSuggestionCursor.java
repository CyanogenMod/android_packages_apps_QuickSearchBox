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

package com.android.quicksearchbox;

import android.database.DataSetObservable;
import android.database.DataSetObserver;

import java.util.ArrayList;

/**
 * A SuggestionCursor that is backed by a list of SuggestionPosition objects.
 * This cursor does not own the SuggestionCursors that the SuggestionPosition
 * objects refer to.
 *
 */
public class ListSuggestionCursor extends AbstractSuggestionCursorWrapper {

    private final DataSetObservable mDataSetObservable = new DataSetObservable();

    private final ArrayList<SuggestionPosition> mSuggestions;

    private int mPos;

    public ListSuggestionCursor(String userQuery) {
        super(userQuery);
        mSuggestions = new ArrayList<SuggestionPosition>();
        mPos = 0;
    }

    /**
     * Adds a suggestion from another suggestion cursor.
     *
     * @param suggestionPos
     * @return {@code true} if the suggestion was added.
     */
    public boolean add(SuggestionPosition suggestionPos) {
        mSuggestions.add(suggestionPos);
        return true;
    }

    public void close() {
        mSuggestions.clear();
    }

    public int getPosition() {
        return mPos;
    }

    public void moveTo(int pos) {
        mPos = pos;
    }

    public boolean moveToNext() {
        int size = mSuggestions.size();
        if (mPos >= size) {
            // Already past the end
            return false;
        }
        mPos++;
        return mPos < size;
    }

    public void removeRow() {
        mSuggestions.remove(mPos);
    }

    public void replaceRow(SuggestionPosition suggestionPos) {
        mSuggestions.set(mPos, suggestionPos);
    }

    public int getCount() {
        return mSuggestions.size();
    }

    @Override
    protected SuggestionCursor current() {
        return mSuggestions.get(mPos).current();
    }

    @Override
    public String toString() {
        return "[" + getUserQuery() + "] " + mSuggestions;
    }

    /**
     * Register an observer that is called when changes happen to this data set.
     *
     * @param observer gets notified when the data set changes.
     */
    public void registerDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.registerObserver(observer);
    }

    /**
     * Unregister an observer that has previously been registered with 
     * {@link #registerDataSetObserver(DataSetObserver)}
     *
     * @param observer the observer to unregister.
     */
    public void unregisterDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.unregisterObserver(observer);
    }

    protected void notifyDataSetChanged() {
        mDataSetObservable.notifyChanged();
    }
}
