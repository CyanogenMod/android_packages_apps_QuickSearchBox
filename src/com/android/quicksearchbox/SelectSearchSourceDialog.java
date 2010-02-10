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

import com.android.quicksearchbox.ui.SearchSourceSelector;
import com.android.quicksearchbox.ui.SourcesAdapter;
import com.android.quicksearchbox.ui.SuggestionViewFactory;

import android.app.Dialog;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.GridView;


/**
 * Search source selection dialog.
 */
public class SelectSearchSourceDialog extends Dialog {

    private static final boolean DBG = true;
    private static final String TAG = "QSB.SelectSearchSourceDialog";

    private GridView mSourceList;

    private ComponentName mSource;

    private String mQuery;

    private Bundle mAppData;

    public SelectSearchSourceDialog(Context context) {
        super(context, R.style.Theme_SelectSearchSource);
        setContentView(R.layout.select_search_source);
        mSourceList = (GridView) findViewById(R.id.source_list);
        mSourceList.setOnItemClickListener(new SourceClickListener());
        // TODO: for some reason, putting this in the XML layout instead makes
        // the list items unclickable.
        mSourceList.setFocusable(true);
        setCanceledOnTouchOutside(true);
        positionWindow();
    }

    public void setSource(ComponentName source) {
        mSource = source;
    }

    public void setQuery(String query) {
        mQuery = query;
    }

    public void setAppData(Bundle appData) {
        mAppData = appData;
    }

    private void positionWindow() {
        Resources resources = getContext().getResources();
        int x = resources.getDimensionPixelSize(R.dimen.select_source_x);
        int y = resources.getDimensionPixelSize(R.dimen.select_source_y);
        positionArrowAt(x, y);
    }

    private void positionArrowAt(int x, int y) {
        Window window = getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.x = x;
        lp.y = y;
        lp.gravity = Gravity.TOP | Gravity.LEFT;
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        // Use screen coordinates
        lp.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        lp.flags |=  WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        // Put window on top of input method
        lp.flags |= WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
        window.setAttributes(lp);
        if (DBG) Log.d(TAG, "Window params: " + lp);
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateSources();
    }

    private void updateSources() {
        mSourceList.setAdapter(new SourcesAdapter(getViewFactory(), getGlobalSuggestionsProvider()));
    }

    private QsbApplication getQsbApplication() {
        return (QsbApplication) getContext().getApplicationContext();
    }

    private SuggestionsProvider getGlobalSuggestionsProvider() {
        return getQsbApplication().getGlobalSuggestionsProvider();
    }

    private SuggestionViewFactory getViewFactory() {
        return getQsbApplication().getSuggestionViewFactory();
    }

    protected void selectSource(Source source) {
        dismiss();
        // If a new source was selected, start QSB with that source.
        // If the old source was selected, just finish.
        if (!isCurrentSource(source)) {
            switchSource(source);
        }
    }

    private boolean isCurrentSource(Source source) {
        if (source == null) return mSource == null;
        return source.getComponentName().equals(mSource);
    }

    private void switchSource(Source source) {
        if (DBG) Log.d(TAG, "switchSource(" + source + ")");

        Intent searchIntent = new Intent(SearchManager.INTENT_ACTION_GLOBAL_SEARCH);
        searchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ComponentName sourceName = source == null ? null : source.getComponentName();
        SearchSourceSelector.setSource(searchIntent, sourceName);
        searchIntent.putExtra(SearchManager.QUERY, mQuery);
        searchIntent.putExtra(SearchManager.APP_DATA, mAppData);

        try {
            if (DBG) Log.d(TAG, "startActivity(" + searchIntent.toUri(0) + ")");
            getOwnerActivity().startActivity(searchIntent);
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "Couldn't start QSB: " + ex);
        }
    }

    private class SourceClickListener implements AdapterView.OnItemClickListener {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Source source = (Source) parent.getItemAtPosition(position);
            selectSource(source);
        }
    }
}
