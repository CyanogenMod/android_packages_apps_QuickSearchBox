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
package com.android.quicksearchbox.google;

import com.android.quicksearchbox.AbstractSource;
import com.android.quicksearchbox.CursorBackedSourceResult;
import com.android.quicksearchbox.QsbApplication;
import com.android.quicksearchbox.R;
import com.android.quicksearchbox.SourceResult;
import com.android.quicksearchbox.SuggestionCursor;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

/**
 * Special source implementation for Google suggestions.
 */
public class GoogleSource extends AbstractSource {

    private static final String GOOGLE_SOURCE_NAME = "google";

    private final GoogleClient mClient;

    public GoogleSource(Context context) {
        super(context);
        mClient = createGoogleClient();
    }

    protected GoogleClient createGoogleClient() {
        return new GoogleSuggestClient(getContext(), this);
    }

    public boolean canRead() {
        return true;
    }

    public Intent createVoiceSearchIntent(Bundle appData) {
        return createVoiceWebSearchIntent(appData);
    }

    public String getDefaultIntentAction() {
        return Intent.ACTION_WEB_SEARCH;
    }

    public String getDefaultIntentData() {
        return null;
    }

    public CharSequence getHint() {
        return getContext().getString(R.string.google_search_hint);
    }

    @Override
    protected String getIconPackage() {
        return getContext().getPackageName();
    }

    public ComponentName getIntentComponent() {
        return mClient.getIntentComponent();
    }

    public CharSequence getLabel() {
        return getContext().getString(R.string.google_search_label);
    }

    public String getName() {
        return GOOGLE_SOURCE_NAME;
    }

    public int getQueryThreshold() {
        return 0;
    }

    public CharSequence getSettingsDescription() {
        return getContext().getString(R.string.google_search_description);
    }

    public Drawable getSourceIcon() {
        return getContext().getResources().getDrawable(getSourceIconResource());
    }

    public Uri getSourceIconUri() {
        return Uri.parse("android.resource://" + getContext().getPackageName()
                + "/" +  getSourceIconResource());
    }

    private int getSourceIconResource() {
        return R.drawable.google_icon;
    }

    public SourceResult getSuggestions(String query, int queryLimit, boolean onlySource) {
        SourceResult result = mClient.query(query);
        if (result == null) {
            // getSuggestions() should never return null
            return new CursorBackedSourceResult(this, query);
        } else {
            return result;
        }
    }

    public int getVersionCode() {
        return QsbApplication.get(getContext()).getVersionCode();
    }

    public boolean queryAfterZeroResults() {
        return true;
    }

    public SuggestionCursor refreshShortcut(String shortcutId, String extraData) {
        return mClient.refreshShortcut(shortcutId, extraData);
    }

    public boolean voiceSearchEnabled() {
        return true;
    }

    public boolean isWebSuggestionSource() {
        return true;
    }

    public boolean isLocationAware() {
        return mClient.isLocationAware();
    }

}
