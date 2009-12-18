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
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;

/**
 * Source indicator widget. Touching the widget brings up the source selection activity.
 */
public class SourceSelector extends FrameLayout {

    private static final boolean DBG = true;
    private static final String TAG = "QSB.SourceSelector";

    // TODO: Move to android.content.Intent?
    private static final String ACTION_SELECT_SEARCH_SOURCE
            = "android.intent.action.SELECT_SEARCH_SOURCE";

    private static final String GLOBAL_SEARCH_COMPONENT
            = "global_search_component";

    private ImageButton mIconView;

    private SearchInfo mSearchInfo;

    public SourceSelector(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SourceSelector(Context context) {
        super(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mIconView = (ImageButton) findViewById(R.id.source_selector_icon);
        mIconView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startSelectorActivity();
            }
        });
    }

    public void update() {
        if (mSearchInfo == null) {
            mIconView.setImageDrawable(null);
        } else {
            mIconView.setImageDrawable(mSearchInfo.getSourceIcon());
        }
    }

    public void setSearchInfo(SearchInfo searchInfo) {
        mSearchInfo = searchInfo;
        update();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            startSelectorActivity();
            // TODO: This breaks clicks.
            return true;
        }
        return super.onTouchEvent(event);
    }

    protected void startSelectorActivity() {
        String query = null;
        Bundle appSearchData = null;
        ComponentName source = null;

        if (mSearchInfo != null) {
            source = mSearchInfo.getSourceName();
            query = mSearchInfo.getQuery();
            appSearchData = mSearchInfo.getAppSearchData();
        }

        Rect rect = Util.getOnScreenRect(this);
        Intent intent = new Intent(ACTION_SELECT_SEARCH_SOURCE);
        intent.setSourceBounds(rect);
        if (source != null) {
            setGlobalSearchComponent(intent, source);
        }
        if (query != null) {
            intent.putExtra(SearchManager.QUERY, query);
        }
        if (appSearchData != null) {
            intent.putExtra(SearchManager.APP_DATA, appSearchData);
        }
        try {
            if (mSearchInfo != null) {
                mSearchInfo.startActivity(intent);
            } else {
                getContext().startActivity(intent);
            }
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "Could open source selector: " + ex);
        }
    }

    public static ComponentName getGlobalSearchComponent(Intent intent) {
        String name = intent.getStringExtra(GLOBAL_SEARCH_COMPONENT);
        return name == null ? null : ComponentName.unflattenFromString(name);
    }

    public static void setGlobalSearchComponent(Intent intent, ComponentName name) {
        if (name != null) {
            intent.putExtra(GLOBAL_SEARCH_COMPONENT, name.flattenToShortString());
        }
    }

    public interface SearchInfo {
        ComponentName getSourceName();
        Drawable getSourceIcon();
        String getQuery();
        Bundle getAppSearchData();
        void startActivity(Intent intent) throws ActivityNotFoundException;
    }
}
