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

import android.app.SearchManager;
import android.content.Intent;

import java.util.Map;

/**
 * Holds data for each suggest item including the display data and how to launch the result.
 * Used for passing from the provider to the suggest cursor.
 *
 */
public class SuggestionData {

    private final Source mSource;
    private String mFormat;
    private String mText1;
    private String mText2;
    private String mIcon1;
    private String mIcon2;
    private Intent mIntent;
    private String mDisplayQuery;
    private String mShortcutId;
    private boolean mIsSpinnerWhileRefreshing;
    private Map<Integer,String> mActionMsgs;
    private String mIntentAction;
    private String mIntentData;
    private String mIntentExtraData;
    private String mSuggestionQuery;

    public SuggestionData(Source source) {
        mSource = source;
    }

    public Source getSuggestionSource() {
        return mSource;
    }

    public String getSuggestionFormat() {
        return mFormat;
    }

    public String getSuggestionText1() {
        return mText1;
    }

    public String getSuggestionText2() {
        return mText2;
    }

    public String getSuggestionIcon1() {
        return mIcon1;
    }

    public String getSuggestionIcon2() {
        return mIcon2;
    }

    public boolean isSpinnerWhileRefreshing() {
        return mIsSpinnerWhileRefreshing;
    }

    public String getIntentExtraData() {
        return mIntent == null ? null : mIntent.getStringExtra(SearchManager.EXTRA_DATA_KEY);
    }

    public String getSuggestionDisplayQuery() {
        return mDisplayQuery;
    }

    public String getShortcutId() {
        return mShortcutId;
    }

    public String getActionMsg(int keyCode) {
        return mActionMsgs == null ? null : mActionMsgs.get(keyCode);
    }

    public Map<Integer, String> getActionMsgs() {
        return mActionMsgs;
    }

    public String getSuggestionIntentAction() {
        return mIntentAction;
    }

    public String getSuggestionIntentDataString() {
        return mIntentData;
    }

    public String getSuggestionIntentExtraData() {
        return mIntentExtraData;
    }

    public String getSuggestionQuery() {
        return mSuggestionQuery;
    }

    public void setActionMsgs(Map<Integer, String> actionMsgs) {
        mActionMsgs = actionMsgs;
    }

    public void setFormat(String format) {
        mFormat = format;
    }

    public void setText1(String text1) {
        mText1 = text1;
    }

    public void setText2(String text2) {
        mText2 = text2;
    }

    public void setIcon1(String icon1) {
        mIcon1 = icon1;
    }

    public void setIcon2(String icon2) {
        mIcon2 = icon2;
    }

    public void setIntent(Intent intent) {
        mIntent = intent == null ? null : new Intent(intent);
    }

    public void setDisplayQuery(String displayQuery) {
        mDisplayQuery = displayQuery;
    }

    public void setShortcutId(String shortcutId) {
        mShortcutId = shortcutId;
    }

    private String makeKeyComponent(String str) {
        return str == null ? "" : str;
    }

    public String getSuggestionKey() {
        String action = "";
        String data = "";
        String query = "";
        if (mIntent != null) {
            action = makeKeyComponent(mIntent.getAction());
            data = makeKeyComponent(mIntent.getDataString());
            query = makeKeyComponent(mIntent.getStringExtra(SearchManager.QUERY));
        }
        // calculating accurate size of string builder avoids an allocation vs starting with
        // the default size and having to expand.
        int size = action.length() + 2 + data.length() + query.length();
        return new StringBuilder(size)
                .append(action)
                .append('#')
                .append(data)
                .append('#')
                .append(query)
                .toString();
    }

    private String getIntentString() {
        if (mIntent == null) {
            return null;
        }
        return mIntent.toUri(Intent.URI_INTENT_SCHEME);
    }

    public String getSuggestionLogType() {
        return getSuggestionSource().getLogName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SuggestionData that = (SuggestionData) o;

        if (notEqual(mSource, that.mSource)) return false;
        if (notEqual(mFormat, that.mFormat)) return false;
        if (notEqual(mText1, that.mText1)) return false;
        if (notEqual(mText2, that.mText2)) return false;
        if (notEqual(mIcon1, that.mIcon1)) return false;
        if (notEqual(mIcon2, that.mIcon2)) return false;
        if (notEqual(getIntentString(), that.getIntentString())) return false;
        if (notEqual(mShortcutId, that.mShortcutId)) return false;
        if (notEqual(mActionMsgs, that.mActionMsgs)) return false;
        return true;
    }

    private static boolean notEqual(Object x, Object y) {
        if (x == null) {
            return y != null;
        }
        if (x == y) {
            return false;
        }
        return !x.equals(y);
    }

    @Override
    public int hashCode() {
        int result = mSource.hashCode();
        result = addHashCode(result, mFormat);
        result = addHashCode(result, mText1);
        result = addHashCode(result, mText2);
        result = addHashCode(result, mIcon1);
        result = addHashCode(result, mIcon2);
        result = addHashCode(result, getIntentString());
        result = addHashCode(result, mShortcutId);
        result = addHashCode(result, mActionMsgs);
        return result;
    }

    private static int addHashCode(int old, Object obj) {
        return 31 * old + (obj != null ? obj.hashCode() : 0);
    }

    /**
     * Returns a string representation of the contents of this SuggestionData,
     * for debugging purposes.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("SuggestionData(");
        builder.append("source=").append(mSource.getFlattenedComponentName())
                .append(", title=").append(mText1);
        if (mIntent != null) {
            builder.append(", intent=").append(getIntentString());
        }
        if (mShortcutId != null) {
            builder.append(", shortcutid=").append(mShortcutId);
        }

        builder.append(")");
        return builder.toString();
    }

}
