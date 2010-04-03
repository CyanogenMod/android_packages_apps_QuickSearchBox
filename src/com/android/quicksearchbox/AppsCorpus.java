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


import com.android.quicksearchbox.util.Util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

/**
 * The apps search source.
 */
public class AppsCorpus extends SingleSourceCorpus {

    private static final String TAG = "QSB.AppsCorpus";

    private static final String APPS_CORPUS_NAME = "apps";

    public AppsCorpus(Context context, Source appsSource) {
        super(context, appsSource);
    }

    @Override
    public CharSequence getLabel() {
        return getContext().getText(R.string.corpus_label_apps);
    }

    @Override
    public CharSequence getHint() {
        return getContext().getText(R.string.corpus_hint_apps);
    }

    @Override
    public Drawable getCorpusIcon() {
        // TODO: Should we have a different icon for the apps corpus?
        return getContext().getResources().getDrawable(android.R.drawable.sym_def_app_icon);
    }

    @Override
    public Uri getCorpusIconUri() {
        return Util.getResourceUri(getContext(), android.R.drawable.sym_def_app_icon);
    }

    @Override
    public String getName() {
        return APPS_CORPUS_NAME;
    }

    @Override
    public CharSequence getSettingsDescription() {
        return getContext().getText(R.string.corpus_description_apps);
    }

    @Override
    public Intent createSearchIntent(String query, Bundle appData) {
        Intent appSearchIntent = createAppSearchIntent(query, appData);
        if (appSearchIntent != null) {
            return appSearchIntent;
        } else {
            // Fall back to sending the intent to ApplicationsProvider
            return super.createSearchIntent(query, appData);
        }
    }

    /**
     * Creates an intent that starts the search activity specified in
     * R.string.apps_search_activity.
     *
     * @return An intent, or {@code null} if the search activity is not set or can't be found.
     */
    private Intent createAppSearchIntent(String query, Bundle appData) {
        ComponentName name = getComponentName(getContext(), R.string.apps_search_activity);
        if (name == null) return null;
        Intent intent = SearchableSource.createSourceSearchIntent(name, query, appData);
        if (intent == null) return null;
        ActivityInfo ai = intent.resolveActivityInfo(getContext().getPackageManager(), 0);
        if (ai != null) {
            return intent;
        } else {
            Log.w(TAG, "Can't find app search acitivity " + name);
            return null;
        }
    }

    private static ComponentName getComponentName(Context context, int res) {
        String nameStr = context.getString(res);
        if (TextUtils.isEmpty(nameStr)) {
            return null;
        } else {
            return ComponentName.unflattenFromString(nameStr);
        }
    }
}
