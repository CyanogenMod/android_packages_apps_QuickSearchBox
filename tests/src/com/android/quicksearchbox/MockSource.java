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
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;

/**
 * Mock implementation of {@link Source}.
 *
 */
public class MockSource implements Source {

    public static final Source SOURCE_1 = new MockSource("SOURCE_1");

    public static final Source SOURCE_2 = new MockSource("SOURCE_2");

    private final String mName;

    public MockSource(String name) {
        mName = name;
    }

    public ComponentName getComponentName() {
        // Not an activity, but no code should treat it as one.
        return new ComponentName("com.android.quicksearchbox",
                "com.android.quicksearchbox.MockSource." + mName);
    }

    public String getDefaultIntentAction() {
        return null;
    }

    public String getDefaultIntentData() {
        return null;
    }

    public Drawable getIcon(String drawableId) {
        return null;
    }

    public Uri getIconUri(String drawableId) {
        return null;
    }

    public String getLabel() {
        return "MockSource " + mName;
    }

    public int getQueryThreshold() {
        return 0;
    }

    public ComponentName getSearchActivity() {
        // Not an activity, so this will case intent resolution to fail
        return new ComponentName("com.android.quicksearchbox",
                "com.android.quicksearchbox.MockSource." + mName + ".Target");
    }

    public String getSettingsDescription() {
        return "Suggestions from MockSource " + mName;
    }

    public Drawable getSourceIcon() {
        return null;
    }

    public Uri getSourceIconUri() {
        return null;
    }

    public SuggestionCursor getSuggestions(String query, int queryLimit) {
        if (query.length() == 0) {
            return null;
        }
        DataSuggestionCursor cursor = new DataSuggestionCursor(query);
        Intent i1 = new Intent(Intent.ACTION_VIEW);
        i1.setData(Uri.parse("content://com.android.quicksearchbox.MockSource/1"));
        SuggestionData s1 = new SuggestionData.Builder(this)
                .text1(query + "_1")
                .displayQuery(query + "_1")
                .intent(i1)
                .build();
        Intent i2 = new Intent(Intent.ACTION_VIEW);
        i1.setData(Uri.parse("content://com.android.quicksearchbox.MockSource/2"));
        SuggestionData s2 = new SuggestionData.Builder(this)
                .text1(query + "_2")
                .displayQuery(query + "_2")
                .intent(i2)
                .build();
        cursor.add(s1);
        cursor.add(s2);
        return cursor;
    }

    public boolean isWebSuggestionSource() {
        return false;
    }

    public boolean queryAfterZeroResults() {
        return false;
    }

    public boolean shouldRewriteQueryFromData() {
        return false;
    }

    public boolean shouldRewriteQueryFromText() {
        return false;
    }

}
