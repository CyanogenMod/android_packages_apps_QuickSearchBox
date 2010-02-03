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

package com.android.quicksearchbox.ui;

import com.android.quicksearchbox.R;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import java.util.List;

/**
 * Utilities for setting up the search source selector.
 *
 * They should keep the same look and feel as much as possible,
 * but only the intent details must absolutely stay in sync.
 *
 * @hide
 */
public class SearchSourceSelector implements View.OnClickListener {

    private static final String TAG = "SearchSourceSelector";

    public static final String INTENT_ACTION_SELECT_SEARCH_SOURCE
            = "com.android.quicksearchbox.action.SELECT_SEARCH_SOURCE";

    private static final String SCHEME_COMPONENT = "android.component";

    public static final int ICON_VIEW_ID = R.id.search_source_selector_icon;

    private final View mView;

    private final ImageButton mIconView;

    private ComponentName mSource;

    private Bundle mAppSearchData;

    private String mQuery;

    public SearchSourceSelector(View view) {
        mView = view;
        mIconView = (ImageButton) view.findViewById(ICON_VIEW_ID);
        mIconView.setOnClickListener(this);
    }

    /**
     * Sets the icon displayed in the search source selector.
     */
    public void setSourceIcon(Drawable icon) {
        mIconView.setImageDrawable(icon);
    }

    /**
     * Sets the current search source.
     */
    public void setSource(ComponentName source) {
        mSource = source;
    }

    /**
     * Sets the app-specific data that will be passed to the search activity if
     * the user opens the source selector and chooses a source.
     */
    public void setAppSearchData(Bundle appSearchData) {
        mAppSearchData = appSearchData;
    }

     /**
      * Sets the initial query that will be passed to the search activity if
      * the user opens the source selector and chooses a source.
      */
    public void setQuery(String query) {
        mQuery = query;
    }

    public void setVisibility(int visibility) {
        mView.setVisibility(visibility);
    }

    /**
     * Creates an intent for opening the search source selector activity.
     *
     * @param source The current search source.
     * @param query The initial query that will be passed to the search activity if
     *        the user opens the source selector and chooses a source.
     * @param appSearchData The app-specific data that will be passed to the search
     *        activity if the user opens the source selector and chooses a source.
     */
    public static Intent createIntent(ComponentName source, String query, Bundle appSearchData) {
        Intent intent = new Intent(INTENT_ACTION_SELECT_SEARCH_SOURCE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        setSource(intent, source);
        if (query != null) {
            intent.putExtra(SearchManager.QUERY, query);
        }
        if (query != null) {
            intent.putExtra(SearchManager.APP_DATA, appSearchData);
        }
        return intent;
    }

    public static ComponentName getSource(Intent intent) {
        return uriToComponentName(intent.getData());
    }

    public static void setSource(Intent intent, ComponentName source) {
        if (source != null) {
            intent.setData(componentNameToUri(source));
        }
    }

    private static Uri componentNameToUri(ComponentName name) {
        if (name == null) return null;
        return new Uri.Builder()
                .scheme(SCHEME_COMPONENT)
                .authority(name.getPackageName())
                .path(name.getClassName())
                .build();
    }

    private static ComponentName uriToComponentName(Uri uri) {
        if (uri == null) return null;
        if (!SCHEME_COMPONENT.equals(uri.getScheme())) return null;
        String pkg = uri.getAuthority();
        List<String> path = uri.getPathSegments();
        if (path == null || path.isEmpty()) return null;
        String cls = path.get(0);
        if (TextUtils.isEmpty(pkg) || TextUtils.isEmpty(cls)) return null;
        return new ComponentName(pkg, cls);
    }

    public void onClick(View v) {
        trigger();
    }

    private void trigger() {
        try {
            Intent intent = createIntent(mSource, mQuery, mAppSearchData);
            intent.setSourceBounds(getOnScreenRect(mIconView));
            mIconView.getContext().startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "No source selector activity found", ex);
        }
    }

    // Note: this is different from android.app.SearchSourceSelector,
    // since the implementation there uses a hidden method.
    private static Rect getOnScreenRect(View v) {
        return Util.getOnScreenRect(v);
    }

    public void setOnKeyListener(View.OnKeyListener listener) {
        mIconView.setOnKeyListener(listener);
    }

}
