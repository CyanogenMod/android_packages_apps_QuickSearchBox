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

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.ScrollView;

// TODO: close / deactivate cursors in onPause() or onStop()
// TODO: use permission to get extended Genie suggestions
// TODO: show spinner until done
// TODO: don't show new results until there is at least one, or it's done
// TODO: add timeout for source queries
// TODO: group refreshes that happen close to each other.
// TODO: handle long clicks
// TODO: search / voice search when button pressed
// TODO: use queryAfterZeroResults()
// TODO: support action keys
// TODO: support IME search action
// TODO: allow typing everywhere in the UI
// TODO: support intent extras for source (e.g. launcher widget)
// TODO: support intent extras for initial state, e.g. query, selection
// TODO: make Config server-side configurable
// TODO: add resourced for hi-res version, and fix layouts
// TODO: log impressions
// TODO: use source ranking
// TODO: Show tabs at bottom too

/**
 * The main activity for Quick Search Box. Shows the search UI.
 *
 */
public class SearchActivity extends Activity {

    private static final boolean DBG = true;
    private static final String TAG = "SearchActivity";

    // TODO: This is hidden in SearchManager
    public final static String INTENT_ACTION_SEARCH_SETTINGS 
            = "android.search.action.SEARCH_SETTINGS";

    protected EditText mQueryTextView;

    protected ScrollView mSuggestionsScrollView;
    protected SuggestionsView mSuggestionsView;

    protected ImageButton mSearchGoButton;
    protected ImageButton mVoiceSearchButton;
    protected ProgressBar mProgressBar;

    private Launcher mLauncher;

    private boolean mUpdateSuggestions = true;
    private String mUserQuery = "";
    private boolean mSelectAll = false;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (DBG) Log.d(TAG, "onCreate()");
        // TODO: Use savedInstanceState to restore state
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_bar);

        Config config = getConfig();
        mQueryTextView = (EditText) findViewById(R.id.search_src_text);
        mSuggestionsScrollView = (ScrollView) findViewById(R.id.suggestions_scroll);
        mSuggestionsView = (SuggestionsView) findViewById(R.id.suggestions);
        mSuggestionsView
                .setInitialSourceResultWaitMillis(config.getInitialSourceResultWaitMillis())
                .setSourceResultPublishDelayMillis(config.getSourceResultPublishDelayMillis());

        mSearchGoButton = (ImageButton) findViewById(R.id.search_go_btn);
        mVoiceSearchButton = (ImageButton) findViewById(R.id.search_voice_btn);

        mQueryTextView.addTextChangedListener(new SearchTextWatcher());
        mQueryTextView.setOnKeyListener(new QueryTextViewKeyListener());
        mQueryTextView.setOnFocusChangeListener(new SuggestListFocusListener());

        mSearchGoButton.setOnClickListener(new SearchGoButtonClickListener());
        mVoiceSearchButton.setOnClickListener(new VoiceSearchButtonClickListener());

        Bundle appSearchData = null;

        Intent intent = getIntent();
        // getIntent() currently always returns non-null, but the API does not guarantee
        // that it always will.
        if (intent != null) {
            String initialQuery = intent.getStringExtra(SearchManager.QUERY);
            if (!TextUtils.isEmpty(initialQuery)) {
                mUserQuery = initialQuery;
            }
            // TODO: Declare an intent extra for selectAll
            appSearchData = intent.getBundleExtra(SearchManager.APP_DATA);
        }
        mLauncher = new Launcher(this, appSearchData);
        mVoiceSearchButton.setVisibility(
                mLauncher.isVoiceSearchAvailable() ? View.VISIBLE : View.GONE);

        mSuggestionsView.setClickListener(new SuggestionsClickListener());
    }

    private QsbApplication getQsbApplication() {
        return (QsbApplication) getApplication();
    }

    private Config getConfig() {
        return getQsbApplication().getConfig();
    }

    private ShortcutRepository getShortcutRepository() {
        return getQsbApplication().getShortcutRepository();
    }

    private SuggestionsProvider getSuggestionsProvider() {
        return getQsbApplication().getSuggestionsProvider();
    }

    @Override
    protected void onDestroy() {
        if (DBG) Log.d(TAG, "onDestroy()");
        super.onDestroy();
        if (mSuggestionsView != null) {
            mSuggestionsView.close();
            mSuggestionsView = null;
        }
    }

    @Override
    protected void onStart() {
        if (DBG) Log.d(TAG, "onStart()");
        super.onStart();
        setQuery(mUserQuery, mSelectAll);
        // Only select everything the first time after creating the activity.
        mSelectAll = false;
        updateSuggestions(mUserQuery);
    }

    @Override
    protected void onStop() {
        if (DBG) Log.d(TAG, "onResume()");
        // Close all open suggestion cursors. The query will be redone in onStart()
        // if we come back to this activity.
        mSuggestionsView.setSuggestions(null);
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mQueryTextView.requestFocus();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        Intent settings = new Intent(INTENT_ACTION_SEARCH_SETTINGS);
        settings.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        // Don't show activity chooser if there are multiple search settings activities,
        // e.g. from different QSB implementations.
        settings.setPackage(this.getPackageName());
        menu.add(Menu.NONE, Menu.NONE, 0, R.string.menu_settings)
                .setIcon(android.R.drawable.ic_menu_preferences).setAlphabeticShortcut('P')
                .setIntent(settings);

        return true;
    }

    private String getQuery() {
        CharSequence q = mQueryTextView.getText();
        return q == null ? "" : q.toString();
    }

    /** 
     * Restores the query entered by the user.
     */
    private void restoreUserQuery() {
        if (DBG) Log.d(TAG, "Restoring query to '" + mUserQuery + "'");
        setQuery(mUserQuery, false);
    }

    /**
     * Sets the text in the query box. Does not update the suggestions,
     * and does not change the saved user-entered query.
     * {@link #restoreUserQuery()} will restore the query to the last
     * user-entered query.
     */
    private void setQuery(String query, boolean selectAll) {
        mUpdateSuggestions = false;
        mQueryTextView.setText(query);
        setTextSelection(selectAll);
        mUpdateSuggestions = true;
    }

    /**
     * Sets the text selection in the query text view.
     *
     * @param selectAll If {@code true}, selects the entire query.
     *        If {@false}, no characters are selected, and the cursor is placed
     *        at the end of the query.
     */
    private void setTextSelection(boolean selectAll) {
        if (selectAll) {
            mQueryTextView.setSelection(0, mQueryTextView.length());
        } else {
            mQueryTextView.setSelection(mQueryTextView.length());
        }
    }

    protected void onSearchClicked() {
        String query = getQuery();
        if (DBG) Log.d(TAG, "Search clicked, query=" + query);
        mLauncher.startWebSearch(query);
    }

    protected void onVoiceSearchClicked() {
        if (DBG) Log.d(TAG, "Voice Search clicked");
        mLauncher.startVoiceSearch();
    }

    protected boolean onSuggestionClicked(SuggestionPosition suggestion) {
        if (DBG) Log.d(TAG, "Clicked on suggestion " + suggestion);
        // TODO: handle action keys
        mLauncher.launchSuggestion(suggestion, KeyEvent.KEYCODE_UNKNOWN, null);
        getShortcutRepository().reportClick(suggestion);
        // Update search widgets, since the top shortcuts can have changed.
        SearchWidgetProvider.updateSearchWidgets(this);
        return true;
    }

    protected boolean onIconClicked(SuggestionPosition suggestion, Rect target) {
      if (DBG) Log.d(TAG, "Clicked on suggestion icon " + suggestion);
      mLauncher.launchSuggestionSecondary(suggestion, target);
      getShortcutRepository().reportClick(suggestion);
      return true;
    }

    protected boolean onSuggestionLongClicked(SuggestionCursor sourceResult) {
        if (DBG) Log.d(TAG, "Long clicked on suggestion " + sourceResult.getSuggestionText1());
        return false;
    }

    protected void onSuggestionSelected(SuggestionCursor sourceResult) {
        String displayQuery = sourceResult.getSuggestionDisplayQuery();
        if (DBG) {
            Log.d(TAG, "Selected suggestion " + sourceResult.getSuggestionText1()
                    + ",displayQuery="+ displayQuery);
        }
        if (TextUtils.isEmpty(displayQuery)) {
            restoreUserQuery();
        } else {
            setQuery(displayQuery, false);
        }
    }

    protected void onSourceSelected() {
        if (DBG) Log.d(TAG, "No suggestion selected");
        restoreUserQuery();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (DBG) Log.d(TAG, "onKeyUp(" + event + ")");
        return super.onKeyUp(keyCode, event);
    }

    /**
     * Hides the input method.
     */
    protected void hideInputMethod() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(mQueryTextView.getWindowToken(), 0);
        }
    }

    /**
     * Hides the input method when the suggestions get focus.
     */
    private class SuggestListFocusListener implements OnFocusChangeListener {
        public void onFocusChange(View v, boolean focused) {
            if (v == mQueryTextView) {
                if (!focused) {
                    hideInputMethod();
                } else {
                    // TODO: clear list selection?
                }
            }
        }
    }

    private void startSearchProgress() {
        // TODO: Cache animation between calls?
        mSearchGoButton.setImageResource(R.drawable.searching);
        Animatable animation = (Animatable) mSearchGoButton.getDrawable();
        animation.start();
    }

    private void stopSearchProgress() {
        Drawable animation = mSearchGoButton.getDrawable();
        if (animation instanceof Animatable) {
            // TODO: Is this needed, or is it done automatically when the
            // animation is removed?
            ((Animatable) animation).stop();
        }
        mSearchGoButton.setImageResource(R.drawable.ic_btn_search);
    }

    private void scrollPastTabs() {
        // TODO: Right after starting, the scroll view hasn't been measured,
        // so it doesn't know whether its contents are tall enough to scroll.
        int yOffset = mSuggestionsView.getTabHeight();
        mSuggestionsScrollView.scrollTo(0, yOffset);
        if (DBG) {
            Log.d(TAG, "After scrollTo(0," + yOffset + "), scrollY="
                    + mSuggestionsScrollView.getScrollY());
        }
    }

    private void updateSuggestions(String query) {
        LatencyTracker latency = new LatencyTracker(TAG);
        Suggestions suggestions = getSuggestionsProvider().getSuggestions(query);
        latency.addEvent("getSuggestions_done");
        if (!suggestions.isDone()) {
            suggestions.registerDataSetObserver(new ProgressUpdater(suggestions));
            startSearchProgress();
        } else {
            stopSearchProgress();
        }
        mSuggestionsView.setSuggestions(suggestions);
        scrollPastTabs();
        latency.addEvent("shortcuts_shown");
        long userVisibleLatency = latency.getUserVisibleLatency();
        if (DBG) {
            Log.d(TAG, "User visible latency (shortcuts): " + userVisibleLatency + " ms.");
        }
    }

    /**
     * Filters the suggestions list when the search text changes.
     */
    private class SearchTextWatcher implements TextWatcher {
        public void afterTextChanged(Editable s) {
            if (mUpdateSuggestions) {
                String query = s == null ? "" : s.toString();
                mUserQuery = query;
                updateSuggestions(query);
            }
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }

    /**
     * Handles non-text keys in the query text view.
     */
    private class QueryTextViewKeyListener implements View.OnKeyListener {
        public boolean onKey(View view, int keyCode, KeyEvent event) {
            if (DBG) Log.d(TAG, "QueryTextViewKeyListener.onKey(" + event + ")");
            // Handle IME search action key
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                onSearchClicked();
            }
            return false;
        }
    }

    private class SuggestionsClickListener implements SuggestionsView.ClickListener {
       public void onIconClicked(SuggestionPosition suggestion, Rect rect) {
           SearchActivity.this.onIconClicked(suggestion, rect);
       }

       public void onItemClicked(SuggestionPosition suggestion) {
           SearchActivity.this.onSuggestionClicked(suggestion);
       }

       public void onInteraction() {
           hideInputMethod();
       }
    }

    /**
     * Listens for clicks on the search button.
     */
    private class SearchGoButtonClickListener implements View.OnClickListener {
        public void onClick(View view) {
            onSearchClicked();
        }
    }

    /**
     * Listens for clicks on the voice search button.
     */
    private class VoiceSearchButtonClickListener implements View.OnClickListener {
        public void onClick(View view) {
            onVoiceSearchClicked();
        }
    }

    /**
     * Updates the progress bar when the suggestions adapter changes its progress.
     */
    private class ProgressUpdater extends DataSetObserver {
        private final Suggestions mSuggestions;

        public ProgressUpdater(Suggestions suggestions) {
            mSuggestions = suggestions;
        }

        @Override
        public void onChanged() {
            if (mSuggestions.isDone()) {
                stopSearchProgress();
            }
        }
    }
}
