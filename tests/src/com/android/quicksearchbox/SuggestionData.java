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
 * Use {@link Builder} to create new instances.
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
    private Intent mSecondaryIntent;
    private String mDisplayQuery;
    private String mShortcutId;
    private Map<Integer,String> mActionMsgs;

    public SuggestionData(Source source) {
        mSource = source;
    }

    public SuggestionData(
            Source source,
            String format,
            String text1,
            String text2,
            String icon1,
            String icon2,
            Intent intent,
            Intent secondaryIntent,
            String displayQuery,
            String shortcutId,
            Map<Integer,String> actionMsgs) {
        mSource = source;
        mFormat = format;
        mText1 = text1;
        mText2 = text2;
        mIcon1 = icon1;
        mIcon2 = icon2;
        mIntent = intent == null ? null : new Intent(intent);
        mSecondaryIntent = secondaryIntent == null ? null : new Intent(intent);
        mDisplayQuery = displayQuery;
        mShortcutId = shortcutId;
        mActionMsgs = actionMsgs;
    }

    /**
     * Gets the suggestion source that created this suggestion.
     */
    public Source getSource() {
        return mSource;
    }

    /**
     * Gets the format of the text in the title and description.
     */
    public String getFormat() {
        return mFormat;
    }

    /**
     * Gets the display title (typically shown as the first line).
     */
    public String getText1() {
        return mText1;
    }

    /**
     * Gets the display description (typically shown as the second line).
     */
    public String getText2() {
        return mText2;
    }

    /**
     * Resource ID or URI for the first icon (typically shown on the left).
     */
    public String getIcon1() {
        return mIcon1;
    }

    /**
     * Resource ID or URI for the second icon (typically shown on the right).
     */
    public String getIcon2() {
        return mIcon2;
    }

    /**
     * The intent to launch.
     */
    public Intent getIntent() {
        return mIntent == null ? null : new Intent(mIntent);
    }

    public boolean hasSecondaryIntent() {
        return mSecondaryIntent != null;
    }

    /**
     * The secondary intent for the suggestion.
     */
    public Intent getSecondaryIntent() {
        return mSecondaryIntent == null ? null : new Intent(mSecondaryIntent);
    }

    /**
     * The query to display when this suggestion is selected.
     */
    public String getDisplayQuery() {
        return mDisplayQuery;
    }

    /**
     * The shortcut id.
     */
    public String getShortcutId() {
        return mShortcutId;
    }

    public String getActionMsg(int keyCode) {
        return mActionMsgs == null ? null : mActionMsgs.get(keyCode);
    }

    public Map<Integer, String> getActionMsgs() {
        return mActionMsgs;
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

    public void setSecondaryIntent(Intent intent) {
        mSecondaryIntent = intent == null ? null : new Intent(intent);
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

    private String getSecondaryIntentString() {
        if (mSecondaryIntent == null) {
            return null;
        }
        return mSecondaryIntent.toUri(Intent.URI_INTENT_SCHEME);
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
        if (notEqual(getSecondaryIntentString(), that.getSecondaryIntentString())) return false;
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
        result = addHashCode(result, getSecondaryIntentString());
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
        builder.append("source=").append(mSource.getComponentName().toShortString())
                .append(", title=").append(mText1);
        if (mIntent != null) {
            builder.append(", intent=").append(getIntentString());
        }
        if (mSecondaryIntent != null) {
            builder.append(", secondaryIntent=").append(getSecondaryIntentString());
        }
        if (mShortcutId != null) {
            builder.append(", shortcutid=").append(mShortcutId);
        }

        builder.append(")");
        return builder.toString();
    }

}
