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

package com.android.quicksearchbox;

import com.android.quicksearchbox.util.Consumer;

import android.util.Log;

import java.util.ArrayList;

/**
 * Suggestions provided by a single Source.
 */
public class SingleSourceSuggestions extends Suggestions implements Consumer<SourceResult> {
    private static final String TAG = "QSB.SingleSourceSuggestions";
    private static final boolean DBG = false;

    SourceResult mResult;
    private final Promoter<SourceResult> mPromoter;

    public SingleSourceSuggestions(String query, Promoter<SourceResult> promoter, int maxPromoted) {
        super(query, maxPromoted);
        mPromoter = promoter;
    }

    @Override
    public void close() {
        super.close();
        if (mResult != null) {
            mResult.close();
            mResult = null;
        }
    }

    @Override
    protected SuggestionCursor buildPromoted() {
        ListSuggestionCursor promoted =  new ListSuggestionCursorNoDuplicates(mQuery);
        if (mPromoter == null) return promoted;
        ArrayList<SourceResult> result = new ArrayList<SourceResult>(1);
        if (mResult != null) {
            result.add(mResult);
        }
        if (DBG) Log.d(TAG, "buildPromoted result=" + mResult + " shortcuts=" + getShortcuts());
        mPromoter.pickPromoted(getShortcuts(), result, getMaxPromoted(), promoted);
        return promoted;
    }

    @Override
    public boolean isDone() {
        return mResult != null;
    }

    public boolean consume(SourceResult result) {
        if (mResult != null) {
            Log.e(TAG, "q=" + getQuery() + ": already have a result", new IllegalStateException());
            return false;
        }
        if (isClosed()) {
            return false;
        }
        if (DBG) Log.d(TAG, "q=" + getQuery() + ": got result " + result);
        mResult = result;
        notifyDataSetChanged();
        return true;
    }

}
