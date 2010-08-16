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

import android.database.DataSetObserver;

/**
 * A SourceResult that may have multiple users, and hence required reference counting to handle
 * closing properly.
 */
public class ReferenceCountedSourceResult implements SourceResult {

    private SourceResult mParent;
    private int mRefCount = 0;

    public ReferenceCountedSourceResult(SourceResult aParent) {
        mParent = aParent;
        mRefCount = 1;
    }

    protected ReferenceCountedSourceResult() {
        mRefCount = 1;
    }

    protected void setResult(SourceResult result) {
        mParent = result;
    }

    public synchronized SourceResult getRef() {
        if (mRefCount == 0) throw new IllegalStateException("Already closed");
        mRefCount++;
        return this;
    }

    public Source getSource() {
        return mParent.getSource();
    }

    public synchronized void close() {
        if (mRefCount == 0) throw new IllegalStateException("Already closed");
        --mRefCount;
        if (mRefCount == 0) {
            mParent.close();
        }
    }

    public int getCount() {
        return mParent.getCount();
    }

    public int getPosition() {
        return mParent.getPosition();
    }

    public String getUserQuery() {
        return mParent.getUserQuery();
    }

    public void moveTo(int pos) {
        mParent.moveTo(pos);
    }

    public boolean moveToNext() {
        return mParent.moveToNext();
    }

    public void registerDataSetObserver(DataSetObserver observer) {
        mParent.registerDataSetObserver(observer);
    }

    public void unregisterDataSetObserver(DataSetObserver observer) {
        mParent.unregisterDataSetObserver(observer);
    }

    public String getShortcutId() {
        return mParent.getShortcutId();
    }

    public String getSuggestionFormat() {
        return mParent.getSuggestionFormat();
    }

    public String getSuggestionIcon1() {
        return mParent.getSuggestionIcon1();
    }

    public String getSuggestionIcon2() {
        return mParent.getSuggestionIcon2();
    }

    public String getSuggestionIntentAction() {
        return mParent.getSuggestionIntentAction();
    }

    public String getSuggestionIntentDataString() {
        return mParent.getSuggestionIntentDataString();
    }

    public String getSuggestionIntentExtraData() {
        return mParent.getSuggestionIntentExtraData();
    }

    public String getSuggestionLogType() {
        return mParent.getSuggestionLogType();
    }

    public String getSuggestionQuery() {
        return mParent.getSuggestionQuery();
    }

    public Source getSuggestionSource() {
        return mParent.getSuggestionSource();
    }

    public String getSuggestionText1() {
        return mParent.getSuggestionText1();
    }

    public String getSuggestionText2() {
        return mParent.getSuggestionText2();
    }

    public String getSuggestionText2Url() {
        return mParent.getSuggestionText2Url();
    }

    public boolean isSpinnerWhileRefreshing() {
        return mParent.isSpinnerWhileRefreshing();
    }

    public boolean isSuggestionShortcut() {
        return mParent.isSuggestionShortcut();
    }

    public boolean isWebSearchSuggestion() {
        return mParent.isWebSearchSuggestion();
    }

}
