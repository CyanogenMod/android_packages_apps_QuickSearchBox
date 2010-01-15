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

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

public class SearchableSourceFactory implements SourceFactory {

    private static final String TAG = "QSB.SearchableSourceFactory";

    private final Context mContext;

    private final SearchManager mSearchManager;

    public SearchableSourceFactory(Context context) {
        mContext = context;
        mSearchManager = (SearchManager) context.getSystemService(Context.SEARCH_SERVICE);
    }

    protected Context getContext() {
        return mContext;
    }

    protected ComponentName getWebSearchComponent() {
        // Looks for an activity in the current package that handles ACTION_WEB_SEARCH.
        // This indirect method is used to allow easy replacement of the web
        // search activity when extending this package.
        Intent webSearchIntent = new Intent(Intent.ACTION_WEB_SEARCH);
        webSearchIntent.setPackage(mContext.getPackageName());
        PackageManager pm = mContext.getPackageManager();
        return webSearchIntent.resolveActivity(pm);
    }

    public Source createSource(SearchableInfo searchable) {
        if (searchable == null) return null;
        try {
            return new SearchableSource(mContext, searchable);
        } catch (NameNotFoundException ex) {
            Log.e(TAG, "Source not found: " + ex);
            return null;
        }
    }

    // TODO: Create a special Source subclass that bypasses the ContentProvider interface
    public Source createWebSearchSource() {
        ComponentName sourceName = getWebSearchComponent();
        SearchableInfo searchable = mSearchManager.getSearchableInfo(sourceName);
        if (searchable == null) {
            Log.e(TAG, "Web search source " + sourceName + " is not searchable.");
            return null;
        }
        try {
            return new SearchableSource(mContext, searchable, true);
        } catch (NameNotFoundException ex) {
            Log.e(TAG, "Web search source not found: " + sourceName);
            return null;
        }
    }

}
