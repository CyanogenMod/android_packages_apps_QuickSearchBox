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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import java.util.ArrayList;

/**
 * A SuggestionCursor that is backed by a list of SuggestionPosition objects.
 * This cursor does not own the SuggestionCursors that the SuggestionPosition
 * objects refer to.
 *
 */
public class ListSuggestionCursor extends AbstractSuggestionCursor {

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
        super.close();
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

    public void removeRow() {
        mSuggestions.remove(mPos);
    }

    public void replaceRow(SuggestionPosition suggestionPos) {
        mSuggestions.set(mPos, suggestionPos);
    }

    public int getCount() {
        return mSuggestions.size();
    }

    private SuggestionCursor current() {
        return mSuggestions.get(mPos).getSuggestion();
    }

    public Drawable getIcon(String iconId) {
        return current().getIcon(iconId);
    }

    public Uri getIconUri(String iconId) {
        return current().getIconUri(iconId);
    }

    public Intent getSecondarySuggestionIntent(Context context, Bundle appSearchData, Rect target) {
        return current().getSecondarySuggestionIntent(context, appSearchData, target);
    }

    public String getShortcutId() {
        return current().getShortcutId();
    }

    public ComponentName getSourceComponentName() {
        return current().getSourceComponentName();
    }

    public Drawable getSourceIcon() {
        return current().getSourceIcon();
    }

    public Uri getSourceIconUri() {
        return current().getSourceIconUri();
    }

    public CharSequence getSourceLabel() {
        return current().getSourceLabel();
    }

    public String getSuggestionDisplayQuery() {
        return current().getSuggestionDisplayQuery();
    }

    public String getSuggestionFormat() {
        return current().getSuggestionFormat();
    }

    public String getSuggestionIcon1() {
        return current().getSuggestionIcon1();
    }

    public String getSuggestionIcon2() {
        return current().getSuggestionIcon2();
    }

    public boolean isSpinnerWhileRefreshing() {
        return current().isSpinnerWhileRefreshing();
    }

    public Intent getSuggestionIntent(Context context, Bundle appSearchData, int actionKey,
            String actionMsg) {
        return current().getSuggestionIntent(context, appSearchData, actionKey, actionMsg);
    }

    public String getSuggestionIntentExtraData() {
        return current().getSuggestionIntentExtraData();
    }

    public String getSuggestionText1() {
        return current().getSuggestionText1();
    }

    public String getSuggestionText2() {
        return current().getSuggestionText2();
    }

    public boolean hasSecondaryIntent() {
        return current().hasSecondaryIntent();
    }

    public String getSuggestionKey() {
        return current().getSuggestionKey();
    }

    public String getActionKeyMsg(int keyCode) {
        return current().getActionKeyMsg(keyCode);
    }

}
