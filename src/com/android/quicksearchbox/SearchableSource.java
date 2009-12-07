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
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;

import java.util.Arrays;

/**
 * Represents a single suggestion source, e.g. Contacts.
 *
 */
public class SearchableSource implements Source {

    private static final boolean DBG = true;
    private static final String TAG = "SearchableSource";

    private final Context mContext;

    private final SearchableInfo mSearchable;

    private final ActivityInfo mActivityInfo;

    // Cached label for the activity
    private CharSequence mLabel = null;

    // Cached icon for the activity
    private Drawable.ConstantState mSourceIcon = null;

    private final boolean mIsWebSuggestionSource;

    private final IconLoader mIconLoader;

    public SearchableSource(Context context, SearchableInfo searchable)
            throws NameNotFoundException {
        this(context, searchable, false);
    }

    public SearchableSource(Context context, SearchableInfo searchable,
            boolean isWebSuggestionSource) throws NameNotFoundException {
        ComponentName componentName = searchable.getSearchActivity();
        mContext = context;
        mSearchable = searchable;
        mActivityInfo = context.getPackageManager().getActivityInfo(componentName, 0);
        mIsWebSuggestionSource = isWebSuggestionSource;

        Context activityContext = searchable.getActivityContext(context);
        Context providerContext = searchable.getProviderContext(context, activityContext);
        mIconLoader = new CachingIconLoader(new PackageIconLoader(providerContext));
    }

    public ComponentName getComponentName() {
        return mSearchable.getSearchActivity();
    }

    public Drawable getIcon(String drawableId) {
        return mIconLoader.getIcon(drawableId);
    }

    public Uri getIconUri(String drawableId) {
        return mIconLoader.getIconUri(drawableId);
    }

    public CharSequence getLabel() {
        if (mLabel == null) {
            // Load label lazily
            mLabel = mActivityInfo.loadLabel(mContext.getPackageManager());
        }
        return mLabel;
    }

    public int getQueryThreshold() {
        return mSearchable.getSuggestThreshold();
    }

    public String getSettingsDescription() {
        return mSearchable.getSettingsDescription();
    }

    public Drawable getSourceIcon() {
        if (mSourceIcon == null) {
            // Load icon lazily
            int iconRes = getSourceIconResource();
            PackageManager pm = mContext.getPackageManager();
            Drawable icon = pm.getDrawable(mActivityInfo.packageName, iconRes,
                    mActivityInfo.applicationInfo);
            // Can't share Drawable instances, save constant state instead.
            mSourceIcon = (icon != null) ? icon.getConstantState() : null;
            // Optimization, return the Drawable the first time
            return icon;
        }
        return (mSourceIcon != null) ? mSourceIcon.newDrawable() : null;
    }

    public Uri getSourceIconUri() {
        return mIconLoader.getIconUri(String.valueOf(getSourceIconResource()));
    }

    private int getSourceIconResource() {
        int icon = mActivityInfo.getIconResource();
        return (icon != 0) ? icon : android.R.drawable.sym_def_app_icon;
    }

    public SuggestionCursor getSuggestions(String query, int queryLimit) {
        try {
            Cursor cursor = getSuggestions(mContext, mSearchable, query, queryLimit);
            if (DBG) Log.d(TAG, toString() + "[" + query + "] returned.");
            return new SourceResult(this, query, cursor);
        } catch (RuntimeException ex) {
            Log.e(TAG, toString() + "[" + query + "] failed", ex);
            return new SourceResult(this, query);
        }
    }

    /**
     * This is a copy of {@link SearchManager#getSuggestions(SearchableInfo, String)}.
     */
    private static Cursor getSuggestions(Context context, SearchableInfo searchable, String query,
            int queryLimit) {
        if (searchable == null) {
            return null;
        }

        String authority = searchable.getSuggestAuthority();
        if (authority == null) {
            return null;
        }

        Uri.Builder uriBuilder = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(authority);

        // if content path provided, insert it now
        final String contentPath = searchable.getSuggestPath();
        if (contentPath != null) {
            uriBuilder.appendEncodedPath(contentPath);
        }

        // append standard suggestion query path
        uriBuilder.appendPath(SearchManager.SUGGEST_URI_PATH_QUERY);

        // get the query selection, may be null
        String selection = searchable.getSuggestSelection();
        // inject query, either as selection args or inline
        String[] selArgs = null;
        if (selection != null) {    // use selection if provided
            selArgs = new String[] { query };
        } else {                    // no selection, use REST pattern
            uriBuilder.appendPath(query);
        }

        uriBuilder.appendQueryParameter("limit", String.valueOf(queryLimit));

        Uri uri = uriBuilder.build();

        // finally, make the query
        if (DBG) {
            Log.d(TAG, "query(" + uri + ",null," + selection + ","
                    + Arrays.toString(selArgs) + ",null)");
        }
        return context.getContentResolver().query(uri, null, selection, selArgs, null);
    }

    public boolean isWebSuggestionSource() {
        return mIsWebSuggestionSource;
    }

    public boolean queryAfterZeroResults() {
        return mSearchable.queryAfterZeroResults();
    }

    public boolean shouldRewriteQueryFromData() {
        return mSearchable.shouldRewriteQueryFromData();
    }

    public boolean shouldRewriteQueryFromText() {
        return mSearchable.shouldRewriteQueryFromText();
    }

    @Override
    public String toString() {
        return "SearchableSource{component=" + getComponentName().flattenToShortString() + "}";
    }

    public String getDefaultIntentAction() {
        return mSearchable.getSuggestIntentAction();
    }

    public String getDefaultIntentData() {
        return mSearchable.getSuggestIntentData();
    }

    public ComponentName getSearchActivity() {
        return mSearchable.getSearchActivity();
    }

}
