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

import android.app.SearchableInfo;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;

/**
 * The web search source.
 */
public class WebSource extends SearchableSource {

    private static final boolean DBG = true;
    private static final String TAG = "QSB.WebSource";

    private static final String WEB_SOURCE_LOG_NAME = "web";

    public WebSource(Context context, SearchableInfo searchable) throws NameNotFoundException {
        super(context, searchable);
    }

    @Override
    public String getLogName() {
        return WEB_SOURCE_LOG_NAME;
    }

    @Override
    public boolean isWebSuggestionSource() {
        return true;
    }

}
