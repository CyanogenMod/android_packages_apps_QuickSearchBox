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

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TabHost;

/**
 * Shows all suggestions in a tabbed interface.
 *
 */
public class SuggestionsView extends TabHost {

    private static final boolean DBG = true;
    private static final String TAG = "QSB.SuggestionsView";

    private final TabContentFactory mTabContentFactory = new SuggestionTabContentFactory();

    private BatchedDataSetObserver mDataSetObserver;
    private long mSourceResultPublishDelayMillis;
    private long mInitialSourceResultWaitMillis;

    private LayoutInflater mInflater;

    private GestureDetector mGestureDetector;

    private ClickListener mClickListener;
    private Suggestions mSuggestions;

    public interface ClickListener {
        void onIconClicked(SuggestionPosition result, Rect rect);
        void onItemClicked(SuggestionPosition result);
        void onInteraction();
    }

    public SuggestionsView setSourceResultPublishDelayMillis(long millis) {
        mSourceResultPublishDelayMillis = millis;
        return this;
    }

    public SuggestionsView setInitialSourceResultWaitMillis(long millis) {
        mInitialSourceResultWaitMillis = millis;
        return this;
    }

    public SuggestionsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onFinishInflate() {
        mInflater =
            (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        setup();
        // TODO: The height of the SuggestionsView is the height of the tallest
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
        mGestureDetector = new GestureDetector(getContext(), new GestureListener());
    }

    public void setClickListener(ClickListener listener) {
        mClickListener = listener;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    public int getTabHeight() {
        View tabView = getTabWidget();
        int height = tabView.getHeight();
        if (height == 0) {
            // Not measured yet, hack around it
            tabView.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            height = tabView.getMeasuredHeight();
        }
        return height;
    }

    /**
     * Sets the suggestions that this view is showing.
     */
    public void setSuggestions(Suggestions suggestions) {
        if (mDataSetObserver == null) {
            mDataSetObserver = new BatchedDataSetObserver(
                    new Handler(mContext.getMainLooper()),
                    new SuggestionsObserver(),
                    mSourceResultPublishDelayMillis);
        }
        mDataSetObserver.cancelPendingChanges();

        if (mSuggestions != null && mSuggestions != suggestions) {
            mSuggestions.close();
        }
        mSuggestions = suggestions;
        if (mSuggestions != null) {
            mSuggestions.registerDataSetObserver(mDataSetObserver);
        }
        if (suggestions == null || mSuggestions.getShortcuts().getCount() > 0) {
            // We have some suggestions, update view immediately.
            onDataSetChanged();
        } else {
            // Allow some time for suggestions to arrive before updating view.
            mDataSetObserver.delayOnChanged(mInitialSourceResultWaitMillis);
        }
    }

    public void close() {
        if (mSuggestions != null) {
            mSuggestions.close();
            mSuggestions = null;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (DBG) Log.d(TAG, "onInterceptTouchEvent(" + event + ")");
        if (event.getAction() == MotionEvent.ACTION_DOWN && mClickListener != null) {
            mClickListener.onInteraction();
        }
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
     * Called when the suggestions change.
     */
    protected void onDataSetChanged() {
        if (DBG) Log.d(TAG, "onDataSetChanged() " + mSuggestions);

        if (mSuggestions == null) {
            clearAllTabs();
            return;
        }

        // TODO: TabHost.addTab() throws an NPE at
        // TabWidget.setCurrentTab():265 if we don't do this here.
        setCurrentTab(0);

        // TODO: It's inefficient to clear all the tabs add add them back every time
        // something new is added
        clearAllTabs();

        addTab("0", getResources().getDrawable(android.R.drawable.btn_star_big_on));
        int sourceCount = mSuggestions.getSourceCount();
        for (int i = 0; i < sourceCount; i++) {
            SuggestionCursor c = mSuggestions.getSourceResult(i);
            addTab(String.valueOf(i+1), c.getSourceIcon());
        }
    }

    public void addTab(String tag, Drawable icon) {
        // Need to pass in TabWidget as parent, to create the correct LayoutParams subclass.
        View tab = mInflater.inflate(R.layout.tab_indicator, getTabWidget(), false);
        ImageView iconView = (ImageView) tab.findViewById(R.id.tab_icon);
        iconView.setImageDrawable(icon);
        addTab(newTabSpec(tag).setIndicator(tab).setContent(mTabContentFactory));
    }

    private int getTabCount() {
        return mSuggestions == null ? 0 : mSuggestions.getSourceCount() + 1;
    }

    private SuggestionCursor getSuggestionsForTab(String tag) {
        if (mSuggestions == null) {
            return null;
        }
        try {
            int pageNum = Integer.parseInt(tag);
            if (pageNum == 0) {
                return mSuggestions.getPromoted();
            } else if (pageNum > 0 && pageNum <= mSuggestions.getSourceCount()) {
                return mSuggestions.getSourceResult(pageNum - 1);
            } else {
                Log.e(TAG, "Invalid page number " + pageNum);
                return null;
            }
        } catch (NumberFormatException ex) {
            Log.e(TAG, "Invalid page number " + tag);
            return null;
        }
    }

    private class SuggestionTabContentFactory implements TabContentFactory {
        public View createTabContent(String tag) {
            if (DBG) Log.d(TAG, "createTabContent(" + tag + ")");
            LinearLayout suggestList = new LinearLayout(getContext());
            suggestList.setOrientation(LinearLayout.VERTICAL);
            suggestList.setFocusable(true);

            SuggestionCursor r = getSuggestionsForTab(tag);
            int count = r == null ? 0 : r.getCount();
            for (int i = 0; i < count; i++) {
                r.moveTo(i);
                suggestList.addView(createSuggestionView(r, suggestList));
            }

            return suggestList;
        }
    }

    private View createSuggestionView(SuggestionCursor r, ViewGroup parentViewType)  {
        // TODO: Recycle SuggestionViews.
        SuggestionView v =
                (SuggestionView) mInflater.inflate(R.layout.suggestion, parentViewType, false);
        v.bindAsSuggestion(r);
        View.OnClickListener listener = new SuggestionClickListener(r);
        v.setIconClickListener(listener);
        v.setOnClickListener(listener);
        return v;
    }

    private class SuggestionClickListener implements View.OnClickListener {
        private final SuggestionCursor mResult;
        private final int mPos;

        public SuggestionClickListener(SuggestionCursor result) {
            mResult = result;
            mPos = result.getPosition();
        }

        public void onClick(View view) {
            if (DBG) Log.d(TAG, "Got click on view " + view);
            if (mClickListener == null) {
                return;
            }
            SuggestionPosition suggestion = new SuggestionPosition(mResult, mPos);

            if (view instanceof SuggestionView) {
                mClickListener.onItemClicked(suggestion);
                return;
            }

            mClickListener.onIconClicked(suggestion, getOnScreenRect(view));
        }
    }

    private static Rect getOnScreenRect(View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        Rect rect = new Rect();
        rect.left = location[0];
        rect.top = location[1];
        rect.right = rect.left + view.getWidth();
        rect.bottom = rect.top + view.getHeight();
        return rect;
    }

    private class SuggestionsObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            onDataSetChanged();
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
            // TODO: Tune these thresholds
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
