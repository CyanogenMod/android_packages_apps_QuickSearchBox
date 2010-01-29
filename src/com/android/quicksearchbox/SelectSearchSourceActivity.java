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

import android.app.Activity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.GridView;


/**
 * Search source selection activity.
 */
public class SelectSearchSourceActivity extends Activity {

    private static final boolean DBG = true;
    private static final String TAG = "QSB.SelectSearchSourceActivity";

    private GridView mSourceList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.select_search_source);
        mSourceList = (GridView) findViewById(R.id.source_list);
        mSourceList.setOnItemClickListener(new SourceClickListener());
        // TODO: for some reason, putting this in the XML layout instead makes
        // the list items unclickable.
        mSourceList.setFocusable(true);

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (DBG) Log.d(TAG, "handleIntent(" + getIntent().toUri(0) + ")");
        updateSources();
        positionWindow(intent.getSourceBounds());
    }

    private void positionWindow(Rect target) {
        if (DBG) Log.d(TAG, "target: " + target);
        Window window = getWindow();

        WindowManager.LayoutParams lp = window.getAttributes();
        // TODO: Get these offsets from the position of the bubble arrow
        // TODO: These need to be scaled to the device density
        int offsetX = 0;
        int offsetY = 10;
        lp.x = 0;
        lp.y = target.bottom - offsetY;
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

    private void updateSources() {
        mSourceList.setAdapter(new SourcesAdapter(getViewFactory(), getGlobalSuggestionsProvider()));
    }

    private QsbApplication getQsbApplication() {
        return (QsbApplication) getApplication();
    }

    private SuggestionsProvider getGlobalSuggestionsProvider() {
        return getQsbApplication().getGlobalSuggestionsProvider();
    }

    private SuggestionViewFactory getViewFactory() {
        return getQsbApplication().getSuggestionViewFactory();
    }

    protected void selectSource(Source source) {
        // If a new source was selected, start QSB with that source.
        // Also, always start QSB if no source (= global search) is selected.
        // If the old source was selected, just finish.
        if (source == null || !isPreviousSource(source)) {
            switchSource(source);
        }
        finish();
    }

    private boolean isPreviousSource(Source source) {
        Intent intent = getIntent();
        ComponentName previousSource = SearchSourceSelector.getSource(intent);
        if (source == null) return previousSource == null;
        return source.getComponentName().equals(previousSource);
    }

    private void switchSource(Source source) {
        if (DBG) Log.d(TAG, "switchSource(" + source + ")");
        Intent selectIntent = getIntent();
        String query = selectIntent.getStringExtra(SearchManager.QUERY);
        Bundle appSearchData = selectIntent.getBundleExtra(SearchManager.APP_DATA);

        Intent searchIntent = new Intent(SearchManager.INTENT_ACTION_GLOBAL_SEARCH);
        searchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        String sourceName = source == null ? null : source.getFlattenedComponentName();
        searchIntent.putExtra(SearchActivity.EXTRA_KEY_SEARCH_SOURCE, sourceName);
        searchIntent.putExtra(SearchManager.QUERY, query);
        searchIntent.putExtra(SearchManager.APP_DATA, appSearchData);

        try {
            if (DBG) Log.d(TAG, "startActivity(" + searchIntent.toUri(0) + ")");
            startActivity(searchIntent);
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "Couldn't start QSB: " + ex);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Dismiss source selector on touch outside.
        if (event.getAction() == MotionEvent.ACTION_DOWN && isOutOfBounds(event)) {
            finish();
            return true;
        }
        // TODO: select source on ACTION_UP, to allow press and drag on source selector.
        return super.onTouchEvent(event);
    }

    private boolean isOutOfBounds(MotionEvent event) {
        final int x = (int) event.getX();
        final int y = (int) event.getY();
        final int slop = ViewConfiguration.get(this).getScaledWindowTouchSlop();
        final View decorView = getWindow().getDecorView();
        return (x < -slop) || (y < -slop)
                || (x > (decorView.getWidth() + slop))
                || (y > (decorView.getHeight() + slop));
    }

    private class SourceClickListener implements AdapterView.OnItemClickListener {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Source source = (Source) parent.getItemAtPosition(position);
            selectSource(source);
        }
    }
}
