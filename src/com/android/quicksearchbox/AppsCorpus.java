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

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

/**
 * The apps search source.
 */
public class AppsCorpus extends SingleSourceCorpus {

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
    public Intent createSearchIntent(String query, Bundle appData) {
        // TODO: Start a Market search if Market is installed
        return null;
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

}
