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

import com.android.common.Search;
import com.android.quicksearchbox.ui.SearchSourceSelector;
import com.android.quicksearchbox.ui.SuggestionClickListener;
import com.android.quicksearchbox.ui.SuggestionSelectionListener;
import com.android.quicksearchbox.ui.SuggestionViewFactory;
import com.android.quicksearchbox.ui.SuggestionsAdapter;
import com.android.quicksearchbox.ui.SuggestionsView;

import android.app.Activity;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
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

import java.util.ArrayList;

/**
 * The main activity for Quick Search Box. Shows the search UI.
 *
 */
public class SearchActivity extends Activity {

    private static final boolean DBG = true;
    private static final String TAG = "QSB.SearchActivity";

    public static final String INTENT_ACTION_QSB_AND_SELECT_SEARCH_SOURCE
            = "com.android.quicksearchbox.action.QSB_AND_SELECT_SEARCH_SOURCE";

    // Keys for the saved instance state.
    private static final String INSTANCE_KEY_SOURCE = "source";
    private static final String INSTANCE_KEY_USER_QUERY = "query";

    // Dialog IDs
    private static final int DIALOG_SOURCE_SELECTOR = 0;

    // Timestamp for last onCreate()/onNewIntent() call, as returned by SystemClock.uptimeMillis().
    private long mStartTime;
    // Whether QSB is starting. True between the calls to onCreate()/onNewIntent() and onResume().
    private boolean mStarting;
    // True if the user has taken some action, e.g. launching a search, voice search,
    // or suggestions, since QSB was last started.
    private boolean mTookAction;

    protected SuggestionsAdapter mSuggestionsAdapter;

    protected EditText mQueryTextView;

    protected SuggestionsView mSuggestionsView;

    protected ImageButton mSearchGoButton;
    protected ImageButton mVoiceSearchButton;
    protected SearchSourceSelector mSourceSelector;

    private Launcher mLauncher;

    private Source mSource;
    private Bundle mAppSearchData;
    private boolean mUpdateSuggestions;
    private String mUserQuery;
    private boolean mSelectAll;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        recordStartTime();
        if (DBG) Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.search_bar);

        mSuggestionsAdapter = getQsbApplication().createSuggestionsAdapter();

        mQueryTextView = (EditText) findViewById(R.id.search_src_text);
        mSuggestionsView = (SuggestionsView) findViewById(R.id.suggestions);
        mSuggestionsView.setSuggestionClickListener(new ClickHandler());
        mSuggestionsView.setSuggestionSelectionListener(new SelectionHandler());
        mSuggestionsView.setInteractionListener(new InputMethodCloser());
        mSuggestionsView.setOnKeyListener(new SuggestionsViewKeyListener());
        mSuggestionsView.setOnFocusChangeListener(new SuggestListFocusListener());

        mSearchGoButton = (ImageButton) findViewById(R.id.search_go_btn);
        mVoiceSearchButton = (ImageButton) findViewById(R.id.search_voice_btn);
        mSourceSelector = new SearchSourceSelector(findViewById(R.id.search_source_selector));

        mLauncher = new Launcher(this);
        // TODO: should this check for voice search in the current source?
        mVoiceSearchButton.setVisibility(
                mLauncher.isVoiceSearchAvailable() ? View.VISIBLE : View.GONE);

        mQueryTextView.addTextChangedListener(new SearchTextWatcher());
        mQueryTextView.setOnKeyListener(new QueryTextViewKeyListener());
        mQueryTextView.setOnFocusChangeListener(new QueryTextViewFocusListener());

        mSourceSelector.setOnClickListener(new SourceSelectorClickListener());

        mSearchGoButton.setOnClickListener(new SearchGoButtonClickListener());

        mVoiceSearchButton.setOnClickListener(new VoiceSearchButtonClickListener());

        ButtonsKeyListener buttonsKeyListener = new ButtonsKeyListener();
        mSearchGoButton.setOnKeyListener(buttonsKeyListener);
        mVoiceSearchButton.setOnKeyListener(buttonsKeyListener);
        mSourceSelector.setOnKeyListener(buttonsKeyListener);

        mUpdateSuggestions = true;

        // First get setup from intent
        Intent intent = getIntent();
        setupFromIntent(intent);
        // Then restore any saved instance state
        restoreInstanceState(savedInstanceState);

        // Do this at the end, to avoid updating the list view when setSource()
        // is called.
        mSuggestionsView.setAdapter(mSuggestionsAdapter);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        recordStartTime();
        setIntent(intent);
        setupFromIntent(intent);
    }

    private void recordStartTime() {
        mStartTime = SystemClock.uptimeMillis();
        mStarting = true;
        mTookAction = false;
    }

    protected void restoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState == null) return;
        ComponentName sourceName = savedInstanceState.getParcelable(INSTANCE_KEY_SOURCE);
        String query = savedInstanceState.getString(INSTANCE_KEY_USER_QUERY);
        setSource(getSourceByComponentName(sourceName));
        setUserQuery(query);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // We don't save appSearchData, since we always get the value
        // from the intent and the user can't change it.
        outState.putParcelable(INSTANCE_KEY_SOURCE, getSourceName());
        outState.putString(INSTANCE_KEY_USER_QUERY, mUserQuery);
    }

    private void setupFromIntent(Intent intent) {
        if (DBG) Log.d(TAG, "setupFromIntent(" + intent.toUri(0) + ")");
        ComponentName sourceName = SearchSourceSelector.getSource(intent);
        String query = intent.getStringExtra(SearchManager.QUERY);
        Bundle appSearchData = intent.getBundleExtra(SearchManager.APP_DATA);

        Source source = getSourceByComponentName(sourceName);
        setSource(source);
        setUserQuery(query);
        mSelectAll = intent.getBooleanExtra(SearchManager.EXTRA_SELECT_QUERY, false);
        setAppSearchData(appSearchData);

        if (INTENT_ACTION_QSB_AND_SELECT_SEARCH_SOURCE.equals(intent.getAction())) {
            showSourceSelectorDialog();
        }
    }

    private Source getSourceByComponentName(ComponentName sourceName) {
        if (sourceName == null) return null;
        Source source = getSources().getSourceByComponentName(sourceName);
        if (source == null) {
            Log.w(TAG, "Unknown source " + sourceName);
            return null;
        }
        return source;
    }

    private void setSource(Source source) {
        if (DBG) Log.d(TAG, "setSource(" + source + ")");
        mSource = source;
        Drawable sourceIcon;
        if (source == null) {
            sourceIcon = getSuggestionViewFactory().getGlobalSearchIcon();
        } else {
            sourceIcon = source.getSourceIcon();
        }
        ComponentName sourceName = getSourceName();
        mSuggestionsAdapter.setSource(sourceName);
        mSourceSelector.setSourceIcon(sourceIcon);
    }

    private ComponentName getSourceName() {
        return mSource == null ? null : mSource.getComponentName();
    }

    private QsbApplication getQsbApplication() {
        return (QsbApplication) getApplication();
    }

    private SourceLookup getSources() {
        return getQsbApplication().getSources();
    }

    private ShortcutRepository getShortcutRepository() {
        return getQsbApplication().getShortcutRepository();
    }

    private SuggestionsProvider getSuggestionsProvider() {
        return getQsbApplication().getSuggestionsProvider(mSource);
    }

    private SuggestionViewFactory getSuggestionViewFactory() {
        return getQsbApplication().getSuggestionViewFactory();
    }

    private Logger getLogger() {
        return getQsbApplication().getLogger();
    }

    @Override
    protected void onDestroy() {
        if (DBG) Log.d(TAG, "onDestroy()");
        super.onDestroy();
        mSuggestionsView.setAdapter(null);  // closes mSuggestionsAdapter
    }

    @Override
    protected void onStop() {
        if (DBG) Log.d(TAG, "onStop()");
        // Close all open suggestion cursors. The query will be redone in onResume()
        // if we come back to this activity.
        mSuggestionsAdapter.setSuggestions(null);
        getQsbApplication().getShortcutRefresher().reset();
        super.onStop();
    }

    @Override
    protected void onResume() {
        if (DBG) Log.d(TAG, "onResume()");
        super.onResume();
        setQuery(mUserQuery, mSelectAll);
        // Only select everything the first time after creating the activity.
        mSelectAll = false;
        updateSuggestions(mUserQuery);
        mQueryTextView.requestFocus();
        if (mStarting) {
            mStarting = false;
            // Start up latency should not exceed 2^31 ms (~ 25 days). Note that
            // SystemClock.uptimeMillis() does not advance during deep sleep.
            int latency = (int) (SystemClock.uptimeMillis() - mStartTime);
            String source = getIntent().getStringExtra(Search.SOURCE);
            getLogger().logStart(latency, source, mSource,
                    getSuggestionsProvider().getOrderedSources());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        Intent settings = new Intent(SearchManager.INTENT_ACTION_SEARCH_SETTINGS);
        settings.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        // Don't show activity chooser if there are multiple search settings activities,
        // e.g. from different QSB implementations.
        settings.setPackage(this.getPackageName());
        menu.add(Menu.NONE, Menu.NONE, 0, R.string.menu_settings)
                .setIcon(android.R.drawable.ic_menu_preferences).setAlphabeticShortcut('P')
                .setIntent(settings);

        return true;
    }

    /**
     * Sets the query as typed by the user. Does not update the suggestions
     * or the text in the query box.
     */
    protected void setUserQuery(String userQuery) {
        if (userQuery == null) userQuery = "";
        mUserQuery = userQuery;
    }

    protected void setAppSearchData(Bundle appSearchData) {
        mAppSearchData = appSearchData;
        mLauncher.setAppSearchData(appSearchData);
    }

    protected String getQuery() {
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

    protected void showSourceSelectorDialog() {
        showDialog(DIALOG_SOURCE_SELECTOR);
    }


    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case DIALOG_SOURCE_SELECTOR:
                return createSourceSelectorDialog();
            default:
                throw new IllegalArgumentException("Unknown dialog: " + id);
        }
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        switch (id) {
            case DIALOG_SOURCE_SELECTOR:
                prepareSourceSelectorDialog((SelectSearchSourceDialog) dialog);
                break;
            default:
                throw new IllegalArgumentException("Unknown dialog: " + id);
        }
    }

    protected SelectSearchSourceDialog createSourceSelectorDialog() {
        SelectSearchSourceDialog dialog = new SelectSearchSourceDialog(this);
        dialog.setOwnerActivity(this);
        return dialog;
    }

    protected void prepareSourceSelectorDialog(SelectSearchSourceDialog dialog) {
        dialog.setSource(getSourceName());
        dialog.setQuery(getQuery());
        dialog.setAppData(mAppSearchData);
    }

    protected void onSearchClicked(int method) {
        String query = getQuery();
        if (DBG) Log.d(TAG, "Search clicked, query=" + query);
        mTookAction = true;
        getLogger().logSearch(mSource, method, query.length());
        mLauncher.startSearch(mSource, query);
    }

    protected void onVoiceSearchClicked() {
        if (DBG) Log.d(TAG, "Voice Search clicked");
        mTookAction = true;
        getLogger().logVoiceSearch(mSource);

        // TODO: should this start voice search in the current source?
        mLauncher.startVoiceSearch();
    }

    protected boolean launchSuggestion(SuggestionPosition suggestion) {
        return launchSuggestion(suggestion, KeyEvent.KEYCODE_UNKNOWN, null);
    }

    protected boolean launchSuggestion(SuggestionPosition suggestion,
            int actionKey, String actionMsg) {
        if (DBG) Log.d(TAG, "Launching suggestion " + suggestion);
        mTookAction = true;
        SuggestionCursor suggestions = mSuggestionsAdapter.getCurrentSuggestions();
        // TODO: This should be just the queried sources, but currently
        // all sources are queried
        ArrayList<Source> sources = getSuggestionsProvider().getOrderedSources();
        getLogger().logSuggestionClick(suggestion.getPosition(), suggestions, sources);

        mLauncher.launchSuggestion(suggestion, actionKey, actionMsg);
        getShortcutRepository().reportClick(suggestion);
        return true;
    }

    protected boolean onSuggestionLongClicked(SuggestionPosition suggestion) {
        SuggestionCursor sourceResult = suggestion.getSuggestion();
        if (DBG) Log.d(TAG, "Long clicked on suggestion " + sourceResult.getSuggestionText1());
        return false;
    }

    protected void onSuggestionSelected(SuggestionPosition suggestion) {
        if (suggestion == null) {
            // This happens when a suggestion has been selected with the
            // dpad / trackball and then a different UI element is touched.
            // Do nothing, since we want to keep the query of the selection
            // in the search box.
            return;
        }
        SuggestionCursor sourceResult = suggestion.getSuggestion();
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

    protected boolean onSuggestionKeyDown(SuggestionPosition suggestion,
            int keyCode, KeyEvent event) {
        // Treat enter or search as a click
        if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_SEARCH) {
            return launchSuggestion(suggestion);
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_UP
                && mSuggestionsView.getSelectedItemPosition() == 0) {
            // Moved up from the top suggestion, restore the user query and focus query box
            if (DBG) Log.d(TAG, "Up and out");
            restoreUserQuery();
            return false;  // let the framework handle the move
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            // Moved left / right from a suggestion, keep current query, move
            // focus to query box, and move cursor to far left / right
            if (DBG) Log.d(TAG, "Left/right on a suggestion");
            int cursorPos = (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) ? 0 : mQueryTextView.length();
            mQueryTextView.setSelection(cursorPos);
            mQueryTextView.requestFocus();
            // TODO: should we modify the list selection?
            return true;
        }

        // Handle source-specified action keys
        String actionMsg = suggestion.getSuggestion().getActionKeyMsg(keyCode);
        if (actionMsg != null) {
            return launchSuggestion(suggestion, keyCode, actionMsg);
        }

        return false;
    }

    protected void onSourceSelected() {
        if (DBG) Log.d(TAG, "No suggestion selected");
        restoreUserQuery();
    }

    protected int getSelectedPosition() {
        return mSuggestionsView.getSelectedPosition();
    }

    protected SuggestionPosition getSelectedSuggestion() {
        return mSuggestionsView.getSelectedSuggestion();
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

    protected void showInputMethodForQuery() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(mQueryTextView, 0);
        }
    }

    /**
     * Hides the input method when the suggestions get focus.
     */
    private class SuggestListFocusListener implements OnFocusChangeListener {
        public void onFocusChange(View v, boolean focused) {
            if (DBG) Log.d(TAG, "Suggestions focus change, now: " + focused);
            if (focused) {
                // The suggestions list got focus, hide the input method
                hideInputMethod();
            }
        }
    }

    private class QueryTextViewFocusListener implements OnFocusChangeListener {
        public void onFocusChange(View v, boolean focused) {
            if (DBG) Log.d(TAG, "Query focus change, now: " + focused);
            if (focused) {
                // The query box got focus, show the input method if the
                // query box got focus?
                showInputMethodForQuery();
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

    private void updateSuggestions(String query) {
        query = ltrim(query);
        LatencyTracker latency = new LatencyTracker(TAG);
        Suggestions suggestions = getSuggestionsProvider().getSuggestions(query);
        latency.addEvent("getSuggestions_done");
        if (!suggestions.isDone()) {
            suggestions.registerDataSetObserver(new ProgressUpdater(suggestions));
            startSearchProgress();
        } else {
            stopSearchProgress();
        }
        mSuggestionsAdapter.setSuggestions(suggestions);
        latency.addEvent("shortcuts_shown");
        long userVisibleLatency = latency.getUserVisibleLatency();
        if (DBG) {
            Log.d(TAG, "User visible latency (shortcuts): " + userVisibleLatency + " ms.");
        }
    }

    private boolean forwardKeyToQueryTextView(int keyCode, KeyEvent event) {
        if (!event.isSystem() && !isDpadKey(keyCode)) {
            if (DBG) Log.d(TAG, "Forwarding key to query box: " + event);
            if (mQueryTextView.requestFocus()) {
                return mQueryTextView.dispatchKeyEvent(event);
            }
        }
        return false;
    }

    private boolean isDpadKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                return true;
            default:
                return false;
        }
    }

    /**
     * Filters the suggestions list when the search text changes.
     */
    private class SearchTextWatcher implements TextWatcher {
        public void afterTextChanged(Editable s) {
            if (mUpdateSuggestions) {
                String query = s == null ? "" : s.toString();
                setUserQuery(query);
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
            // Handle IME search action key
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                onSearchClicked(Logger.SEARCH_METHOD_KEYBOARD);
            }
            return false;
        }
    }

    /**
     * Handles key events on the search and voice search buttons,
     * by refocusing to EditText.
     */
    private class ButtonsKeyListener implements View.OnKeyListener {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            return forwardKeyToQueryTextView(keyCode, event);
        }
    }

    /**
     * Handles key events on the suggestions list view.
     */
    private class SuggestionsViewKeyListener implements View.OnKeyListener {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                SuggestionPosition suggestion = getSelectedSuggestion();
                if (suggestion != null) {
                    if (onSuggestionKeyDown(suggestion, keyCode, event)) {
                        return true;
                    }
                }
            }
            return forwardKeyToQueryTextView(keyCode, event);
        }
    }

    private class InputMethodCloser implements SuggestionsView.InteractionListener {
        public void onInteraction() {
            hideInputMethod();
        }
    }

    private class ClickHandler implements SuggestionClickListener {
       public void onSuggestionClicked(SuggestionPosition suggestion) {
           launchSuggestion(suggestion);
       }

       public boolean onSuggestionLongClicked(SuggestionPosition suggestion) {
           return SearchActivity.this.onSuggestionLongClicked(suggestion);
       }
    }

    private class SelectionHandler implements SuggestionSelectionListener {
        public void onSelectionChanged(SuggestionPosition suggestion) {
            onSuggestionSelected(suggestion);
        }
    }

    /**
     * Listens for clicks on the source selector.
     */
    private class SearchGoButtonClickListener implements View.OnClickListener {
        public void onClick(View view) {
            onSearchClicked(Logger.SEARCH_METHOD_BUTTON);
        }
    }

    /**
     * Listens for clicks on the search button.
     */
    private class SourceSelectorClickListener implements View.OnClickListener {
        public void onClick(View view) {
            showSourceSelectorDialog();
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

    private static String ltrim(String text) {
        int start = 0;
        int length = text.length();
        while (start < length && Character.isWhitespace(text.charAt(start))) {
            start++;
        }
        return start > 0 ? text.substring(start, length) : text;
    }

}
