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

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;

import java.util.ArrayList;

/**
 * A SuggestionCursor that is backed by a list of SuggestionData objects.
 *
 */
public class DataSuggestionCursor extends AbstractSourceSuggestionCursor {

    private final ArrayList<SuggestionData> mSuggestions;

    private int mPos;

    public DataSuggestionCursor(String userQuery) {
        super(userQuery);
        mSuggestions = new ArrayList<SuggestionData>();
        mPos = 0;
    }

    /**
     * Adds a suggestion.
     *
     * @param suggestion
     * @return {@code true}
     */
    public boolean add(SuggestionData suggestion) {
        mSuggestions.add(suggestion);
        return true;
    }

    public void close() {
        mSuggestions.clear();
    }

    public boolean isFailed() {
        return false;
    }

    public int getPosition() {
        return mPos;
    }

    public void moveTo(int pos) {
        mPos = pos;
    }

    public int getCount() {
        return mSuggestions.size();
    }

    private SuggestionData current() {
        return mSuggestions.get(mPos);
    }

    protected Source getSource() {
        return current().getSource();
    }

    public Intent getSecondarySuggestionIntent(Context context, Bundle appSearchData, Rect target) {
        return current().getSecondaryIntent();
    }

    public String getShortcutId() {
        return current().getShortcutId();
    }

    public String getSuggestionDisplayQuery() {
        return current().getDisplayQuery();
    }

    public String getSuggestionFormat() {
        return current().getFormat();
    }

    public String getSuggestionIcon1() {
        return current().getIcon1();
    }

    public String getSuggestionIcon2() {
        return current().getIcon2();
    }

    public Intent getSuggestionIntent(Context context, Bundle appSearchData, int actionKey,
            String actionMsg) {
        return current().getIntent();
    }

    public String getSuggestionText1() {
        return current().getText1();
    }

    public String getSuggestionText2() {
        return current().getText2();
    }

    public boolean hasSecondaryIntent() {
        return current().hasSecondaryIntent();
    }

    public String getSuggestionKey() {
        return current().getSuggestionKey();
    }
}
