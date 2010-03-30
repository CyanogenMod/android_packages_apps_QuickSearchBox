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
import android.os.Bundle;

/**
 * Mock implementation of {@link Source}.
 *
 */
public class MockSource implements Source {

    public static final Source SOURCE_1 = new MockSource("SOURCE_1");

    public static final Source SOURCE_2 = new MockSource("SOURCE_2");

    public static final Source SOURCE_3 = new MockSource("SOURCE_3");

    private final String mName;

    private final int mVersionCode;

    public MockSource(String name) {
        this(name, 0);
    }

    public MockSource(String name, int versionCode) {
        mName = name;
        mVersionCode = versionCode;
    }

    public ComponentName getComponentName() {
        // Not an activity, but no code should treat it as one.
        return new ComponentName("com.android.quicksearchbox",
                getClass().getName() + "." + mName);
    }

    public int getVersionCode() {
        return mVersionCode;
    }

    public String getName() {
        return getComponentName().flattenToShortString();
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

    public CharSequence getHint() {
        return null;
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

    public SourceResult getSuggestions(String query, int queryLimit) {
        if (query.length() == 0) {
            return null;
        }
        DataSuggestionCursor cursor = new DataSuggestionCursor(query);
        Intent i1 = new Intent(Intent.ACTION_VIEW);
        SuggestionData s1 = new SuggestionData(this)
                .setText1(query + "_1")
                .setIntentAction(Intent.ACTION_VIEW)
                .setIntentData("content://" + getClass().getName() + "/1");
        SuggestionData s2 = new SuggestionData(this)
                .setText1(query + "_2")
                .setIntentAction(Intent.ACTION_VIEW)
                .setIntentData("content://" + getClass().getName() + "/2");
        cursor.add(s1);
        cursor.add(s2);
        return new Result(query, cursor);
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o.getClass().equals(this.getClass())) {
            MockSource s = (MockSource) o;
            return s.mName.equals(mName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mName.hashCode();
    }

    @Override
    public String toString() {
        return getName() + ":" + getVersionCode();
    }

    private class Result extends SuggestionCursorWrapper implements SourceResult {

        public Result(String userQuery, SuggestionCursor cursor) {
            super(userQuery, cursor);
        }

        public Source getSource() {
            return MockSource.this;
        }

    }

    public SuggestionCursor refreshShortcut(String shortcutId, String extraData) {
        return null;
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

    public Intent createSearchIntent(String query, Bundle appData) {
        return null;
    }

    public SuggestionData createSearchShortcut(String query) {
        return null;
    }

    public Intent createVoiceSearchIntent(Bundle appData) {
        return null;
    }

    public boolean voiceSearchEnabled() {
        return false;
    }

}
