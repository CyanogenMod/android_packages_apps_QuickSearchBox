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

import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TabHost;

/**
 * A wrapper around {@link TabHost}.
 */
public class TabView extends TabHost {

    private static final boolean DBG = true;
    private static final String TAG = "QSB.TabView";

    private DataSetObserver mDataSetObserver;

    private TabContentFactory mTabContentFactory;

    private TabAdapter mAdapter;

    private GestureDetector mGestureDetector;

    public TabView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mGestureDetector = new GestureDetector(getContext(), new GestureListener());
    }

    public TabAdapter getAdapter() {
        return mAdapter;
    }

    public void setAdapter(TabAdapter adapter) {
        if (mDataSetObserver == null) {
            mDataSetObserver = createDataSetObserver();
        }
        if (adapter == mAdapter) {
            return;
        }
        if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(mDataSetObserver);
            mAdapter.close();
        }
        mAdapter = adapter;
        if (mAdapter != null) {
            mAdapter.registerDataSetObserver(mDataSetObserver);
        }
        onDataSetChanged();
    }

    protected DataSetObserver createDataSetObserver() {
        return new TabAdapterObserver();
    }

    protected TabContentFactory createTabContentFactory() {
        return new TabViewContentFactory();
    }

    @Override
    public void onFinishInflate() {
        setup();
        // TODO: The height of the TabView is the height of the tallest
        // tab that has been displayed, because TabHost uses a FrameLayout which contains
        // all the viewed tab contents, and makes the hidden ones INVISIBLE, not GONE.
        // This is a workaround that sets the visibility to GONE for all the invisible
        // tab content views.
        // This should be fixed in TabHost instead.
        setOnTabChangedListener(new OnTabChangeListener() {
            public void onTabChanged(String tag) {
                FrameLayout contentFrame = getTabContentView();
                View currentView = getCurrentView();
                int count = contentFrame.getChildCount();
                for (int i = 0; i < count; i++) {
                    View child = contentFrame.getChildAt(i);
                    if (child != currentView) {
                        child.setVisibility(View.GONE);
                    }
                }
            }
        });
    }

    public int getTabHeight() {
        View tabWidget = getTabWidget();
        int height = tabWidget.getHeight();
        if (height == 0) {
            // Not measured yet, hack around it
            tabWidget.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            height = tabWidget.getMeasuredHeight();
        }
        return height;
    }

    protected class TabAdapterObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            onDataSetChanged();
        }
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        //if (DBG) Log.d(TAG, "onInterceptTouchEvent(" + event + ")");
        // TODO: this should be in onTouchEvent(), but that doesn't get called.
        // TabHost weirdness?
        if (mGestureDetector.onTouchEvent(event)) {
            return true;
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (DBG) Log.d(TAG, "onTouchEvent(" + event + ")");
        return super.onTouchEvent(event);
    }

    /**
     * Called when the tab set changes.
     */
    protected void onDataSetChanged() {
        if (DBG) Log.d(TAG, "onDataSetChanged() " + mAdapter);

        clearAllTabs();

        if (mAdapter == null) {
            return;
        }

        if (mTabContentFactory == null) {
            mTabContentFactory = createTabContentFactory();
        }
        ViewGroup tabWidget = getTabWidget();
        int count = mAdapter.getTabCount();
        for (int i = 0; i < count; i++) {
            View indicator = mAdapter.getTabHandleView(i, tabWidget);
            String tag = mAdapter.getTag(i);
            addTab(newTabSpec(tag).setIndicator(indicator).setContent(mTabContentFactory));
        }
    }

    private int getTabCount() {
        return mAdapter == null ? 0 : mAdapter.getTabCount();
    }

    protected View createTabContentView(int position) {
        return mAdapter.getTabContentView(position, getTabContentView());
    }

    private class TabViewContentFactory implements TabContentFactory {
        public View createTabContent(String tag) {
            if (DBG) Log.d(TAG, "createTabContent(" + tag + ")");
            int position = mAdapter.getTabPosition(tag);
            return createTabContentView(position);
        }
    }

    private boolean toPreviousTab() {
        int newTab = getCurrentTab() - 1;
        if (newTab >= 0) {
            setCurrentTab(newTab);
            return true;
        } else {
            return false;
        }
    }

    private boolean toNextTab() {
        int newTab = getCurrentTab() + 1;
        if (newTab < getTabCount()) {
            setCurrentTab(newTab);
            return true;
        } else {
            return false;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (DBG) Log.d(TAG, "onFling(" + velocityX + "," + velocityY + ")");
            float absX = Math.abs(velocityX);
            float absY = Math.abs(velocityY);
            // TODO(bryanmawhinney): Tune these thresholds
            if (absX > 700 && absY < 300) {
                if (velocityX > 0) {
                    toPreviousTab();
                } else {
                    toNextTab();
                }
            }
            return true;
        }
    }
}
