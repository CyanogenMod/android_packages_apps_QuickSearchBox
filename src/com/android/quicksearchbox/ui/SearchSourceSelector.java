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
public class SearchSourceSelector {

    private static final String TAG = "SearchSourceSelector";

    private static final String SCHEME_COMPONENT = "android.component";

    public static final int ICON_VIEW_ID = R.id.search_source_selector_icon;

    private final View mView;

    private final ImageButton mIconView;

    public SearchSourceSelector(View view) {
        mView = view;
        mIconView = (ImageButton) view.findViewById(ICON_VIEW_ID);
    }

    /**
     * Sets the icon displayed in the search source selector.
     */
    public void setSourceIcon(Drawable icon) {
        mIconView.setImageDrawable(icon);
    }

    public void setVisibility(int visibility) {
        mView.setVisibility(visibility);
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

    public void setOnKeyListener(View.OnKeyListener listener) {
        mIconView.setOnKeyListener(listener);
    }

    public void setOnClickListener(View.OnClickListener listener) {
        mIconView.setOnClickListener(listener);
    }

}
