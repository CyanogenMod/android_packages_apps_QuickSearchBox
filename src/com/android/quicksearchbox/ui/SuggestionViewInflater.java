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

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

/**
 * Inflates suggestion views.
 */
public class SuggestionViewInflater implements SuggestionViewFactory {

    private static final boolean DBG = true;
    private static final String TAG = "QSB.SuggestionViewInflater";

    private final Context mContext;

    private SuggestionClickListener mSuggestionClickListener;

    public SuggestionViewInflater(Context context) {
        mContext = context;
    }

    private LayoutInflater getInflater() {
        return (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setSuggestionClickListener(SuggestionClickListener listener) {
        mSuggestionClickListener = listener;
    }

    public SuggestionView createSuggestionView(ViewGroup parentViewType) {
        if (DBG) Log.d(TAG, "createSuggestionView()");
        SuggestionView view = (SuggestionView)
                getInflater().inflate(R.layout.suggestion, parentViewType, false);
        if (mSuggestionClickListener != null) {
            view.setSuggestionClickListener(mSuggestionClickListener);
        }
        return view;
    }

    public SourceView createSourceView(ViewGroup parentViewType) {
        if (DBG) Log.d(TAG, "createSourceView()");
        SourceView view = (SourceView)
                getInflater().inflate(R.layout.source_list_item, parentViewType, false);
        return view;
    }

    public String getGlobalSearchLabel() {
        return mContext.getString(R.string.global_search_label);
    }

    public Drawable getGlobalSearchIcon() {
        return mContext.getResources().getDrawable(R.drawable.global_search_source);
    }

    public Uri getGlobalSearchIconUri() {
        return new Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(mContext.getPackageName())
                .appendEncodedPath(String.valueOf(R.drawable.global_search_source))
                .build();
    }
}
