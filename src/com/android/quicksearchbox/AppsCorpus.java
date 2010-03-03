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


import com.android.quicksearchbox.util.NamedTaskExecutor;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import java.util.ArrayList;

/**
 * The web search source.
 */
public class AppsCorpus extends MultiSourceCorpus {

    private static final String APPS_CORPUS_NAME = "apps";

    public AppsCorpus(Context context, NamedTaskExecutor executor,
            Source appsSource) {
        super(context, executor, appsSource);
    }

    public CharSequence getLabel() {
        return getContext().getText(R.string.corpus_label_apps);
    }

    public CharSequence getHint() {
        return getContext().getText(R.string.corpus_hint_apps);
    }

    public Intent createSearchIntent(String query, Bundle appData) {
        // TODO: Start a Market search if Market is installed
        return null;
    }

    public SuggestionData createSearchShortcut(String query) {
        // No search shortcuts for apps at the moment
        return null;
    }

    public Intent createVoiceSearchIntent(Bundle appData) {
        // No voice search for apps at the moment
        return null;
    }

    public Drawable getCorpusIcon() {
        // TODO: Should we have a different icon for the apps corpus?
        return getContext().getResources().getDrawable(android.R.drawable.sym_def_app_icon);
    }

    public Uri getCorpusIconUri() {
        int resourceId = android.R.drawable.sym_def_app_icon;
        return new Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(getContext().getPackageName())
                .appendEncodedPath(String.valueOf(resourceId))
                .build();
    }

    public String getName() {
        return APPS_CORPUS_NAME;
    }

    public int getQueryThreshold() {
        return 0;
    }

    public boolean queryAfterZeroResults() {
        return false;
    }

    public boolean voiceSearchEnabled() {
        return false;
    }

    public boolean isWebCorpus() {
        return false;
    }

    public CharSequence getSettingsDescription() {
        return getContext().getText(R.string.corpus_description_apps);
    }

    @Override
    protected Result createResult(String query, ArrayList<SourceResult> results) {
        return new Result(query, results);
    }

}
