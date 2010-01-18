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
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.TextUtils;

/**
 * Base class for SuggestionCursor implementations.
 *
 */
public abstract class AbstractSuggestionCursor implements SuggestionCursor {

    /** The user query that returned these suggestions. */
    private final String mUserQuery;

    private final DataSetObservable mDataSetObservable = new DataSetObservable();

    public AbstractSuggestionCursor(String userQuery) {
        mUserQuery = userQuery;
    }

    public String getUserQuery() {
        return mUserQuery;
    }

    public void registerDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.registerObserver(observer);
    }

    public void unregisterDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.unregisterObserver(observer);
    }

    protected void notifyDataSetChanged() {
        mDataSetObservable.notifyChanged();
    }

    public void close() {
        mDataSetObservable.unregisterAll();
    }

    public Drawable getSuggestionDrawableIcon1() {
        String icon1Id = getSuggestionIcon1();
        Drawable icon1 = getIcon(icon1Id);
        return icon1 == null ? getSourceIcon() : icon1;
    }

    public Drawable getSuggestionDrawableIcon2() {
        return getIcon(getSuggestionIcon2());
    }

    public CharSequence getSuggestionFormattedText1() {
        return formatText(getSuggestionText1());
    }

    public CharSequence getSuggestionFormattedText2() {
        return formatText(getSuggestionText2());
    }

    private CharSequence formatText(String str) {
        boolean isHtml = "html".equals(getSuggestionFormat());
        if (isHtml && looksLikeHtml(str)) {
            return Html.fromHtml(str);
        } else {
            return str;
        }
    }

    private boolean looksLikeHtml(String str) {
        if (TextUtils.isEmpty(str)) return false;
        for (int i = str.length() - 1; i >= 0; i--) {
            char c = str.charAt(i);
            if (c == '>' || c == '&') return true;
        }
        return false;
    }
}
