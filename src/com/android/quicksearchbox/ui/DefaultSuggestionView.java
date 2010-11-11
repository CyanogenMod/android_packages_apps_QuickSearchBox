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

import com.android.quicksearchbox.QsbApplication;
import com.android.quicksearchbox.R;
import com.android.quicksearchbox.Source;
import com.android.quicksearchbox.Suggestion;
import com.android.quicksearchbox.SuggestionFormatter;
import com.android.quicksearchbox.util.Consumer;
import com.android.quicksearchbox.util.NowOrLater;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * View for the items in the suggestions list. This includes promoted suggestions,
 * sources, and suggestions under each source.
 */
public class DefaultSuggestionView extends RelativeLayout implements SuggestionView {

    private static final boolean DBG = false;

    public static final String VIEW_ID = "default";

    private static int sId = 0;
    // give the TAG an unique ID to make debugging easier (there are lots of these!)
    private final String TAG = "QSB.SuggestionView:" + (sId++);

    private TextView mText1;
    private TextView mText2;
    private AsyncIcon mIcon1;
    private AsyncIcon mIcon2;
    private final SuggestionFormatter mSuggestionFormatter;
    private boolean mIsFromHistory;
    private boolean mRefineable;
    private int mPosition;
    private SuggestionsAdapter<?> mAdapter;
    private KeyListener mKeyListener;
    private boolean mIcon1Enabled = true;

    public DefaultSuggestionView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mSuggestionFormatter = QsbApplication.get(context).getSuggestionFormatter();
    }

    public DefaultSuggestionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mSuggestionFormatter = QsbApplication.get(context).getSuggestionFormatter();
    }

    public DefaultSuggestionView(Context context) {
        super(context);
        mSuggestionFormatter = QsbApplication.get(context).getSuggestionFormatter();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mText1 = (TextView) findViewById(R.id.text1);
        mText2 = (TextView) findViewById(R.id.text2);
        mIcon1 = new AsyncIcon((ImageView) findViewById(R.id.icon1)) {
            // override default icon (when no other available) with default source icon
            @Override
            protected String getFallbackIconId(Source source) {
                return source.getSourceIconUri().toString();
            }
            @Override
            protected Drawable getFallbackIcon(Source source) {
                return source.getSourceIcon();
            }
        };
        mIcon2 = new AsyncIcon((ImageView) findViewById(R.id.icon2));
        // for some reason, creating mKeyListener inside the constructor causes it not to work.
        mKeyListener = new KeyListener();

        setOnKeyListener(mKeyListener);
    }

    public void bindAsSuggestion(Suggestion suggestion, String userQuery) {
        setOnClickListener(new ClickListener());

        CharSequence text1 = formatText(suggestion.getSuggestionText1(), suggestion, userQuery);
        CharSequence text2 = suggestion.getSuggestionText2Url();
        if (text2 != null) {
            text2 = formatUrl(text2);
        } else {
            text2 = formatText(suggestion.getSuggestionText2(), suggestion, null);
        }
        // If there is no text for the second line, allow the first line to be up to two lines
        if (TextUtils.isEmpty(text2)) {
            mText1.setSingleLine(false);
            mText1.setMaxLines(2);
            mText1.setEllipsize(TextUtils.TruncateAt.START);
        } else {
            mText1.setSingleLine(true);
            mText1.setMaxLines(1);
            mText1.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        }
        setText1(text1);
        setText2(text2);
        if (mIcon1Enabled) {
            mIcon1.set(suggestion.getSuggestionSource(), suggestion.getSuggestionIcon1());
        }
        mIcon2.set(suggestion.getSuggestionSource(), suggestion.getSuggestionIcon2());
        updateIsFromHistory(suggestion);
        updateRefinable(suggestion);

        setLongClickable(needsContextMenu());

        if (DBG) {
            Log.d(TAG, "bindAsSuggestion(), text1=" + text1 + ",text2=" + text2 + ",q='" +
                    userQuery + "',refinable=" + mRefineable + ",fromHistory=" + mIsFromHistory);
        }
    }

    public void bindAdapter(SuggestionsAdapter<?> adapter, int position) {
        mAdapter = adapter;
        mPosition = position;
    }

    public void setIcon1Enabled(boolean enabled) {
        mIcon1Enabled = enabled;
        if (mIcon1 != null && mIcon1.mView != null) {
            mIcon1.mView.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
        }
    }

    protected boolean needsContextMenu() {
        return isFromHistory();
    }

    protected boolean isFromHistory() {
        return mIsFromHistory;
    }

    protected void updateIsFromHistory(Suggestion suggestion) {
        mIsFromHistory = suggestion.isSuggestionShortcut() || suggestion.isHistorySuggestion();
    }

    protected void updateRefinable(Suggestion suggestion) {
        mRefineable =
                suggestion.isWebSearchSuggestion()
                && mIcon2.getView().getDrawable() == null
                && !TextUtils.isEmpty(suggestion.getSuggestionQuery());
        if (DBG) Log.d(TAG, "updateRefinable: " + mRefineable);
        setRefinable(suggestion, mRefineable);
    }

    protected void setRefinable(Suggestion suggestion, boolean refinable) {
        if (refinable) {
            mIcon2.getView().setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    onSuggestionQueryRefineClicked();
                }
            });
            mIcon2.getView().setFocusable(true);
            mIcon2.getView().setOnKeyListener(mKeyListener);
            Drawable icon2 = getContext().getResources().getDrawable(R.drawable.ic_commit);
            Drawable background =
                    getContext().getResources().getDrawable(R.drawable.edit_query_background);
            mIcon2.setDrawable(icon2, background, String.valueOf(R.drawable.ic_commit));
        } else {
            mIcon2.getView().setOnClickListener(null);
            mIcon2.getView().setFocusable(false);
            mIcon2.getView().setOnKeyListener(null);
        }
    }

    private CharSequence formatUrl(CharSequence url) {
        SpannableString text = new SpannableString(url);
        ColorStateList colors = getResources().getColorStateList(R.color.url_text);
        text.setSpan(new TextAppearanceSpan(null, 0, 0, colors, null),
                0, url.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return text;
    }

    private CharSequence formatText(String str, Suggestion suggestion,
                String userQuery) {
        boolean isHtml = "html".equals(suggestion.getSuggestionFormat());
        if (isHtml && looksLikeHtml(str)) {
            return Html.fromHtml(str);
        } else if (suggestion.isWebSearchSuggestion() && !TextUtils.isEmpty(userQuery)) {
            return mSuggestionFormatter.formatSuggestion(userQuery, str);
        } else {
            return str;
        }
    }

    private boolean looksLikeHtml(String str) {
        if (TextUtils.isEmpty(str)) return false;
        for (int i = str.length() - 1; i >= 0; i--) {
            char c = str.charAt(i);
            if (c == '>' || c == '&') return true;
        }
        return false;
    }

    /**
     * Sets the first text line.
     */
    private void setText1(CharSequence text) {
        mText1.setText(text);
    }

    /**
     * Sets the second text line.
     */
    private void setText2(CharSequence text) {
        mText2.setText(text);
        if (TextUtils.isEmpty(text)) {
            mText2.setVisibility(GONE);
        } else {
            mText2.setVisibility(VISIBLE);
        }
    }

    /**
     * Sets the drawable in an image view, makes sure the view is only visible if there
     * is a drawable.
     */
    private static void setViewDrawable(ImageView v, Drawable drawable) {
        // Set the icon even if the drawable is null, since we need to clear any
        // previous icon.
        v.setImageDrawable(drawable);

        if (drawable == null) {
            v.setVisibility(View.GONE);
        } else {
            v.setVisibility(View.VISIBLE);

            // This is a hack to get any animated drawables (like a 'working' spinner)
            // to animate. You have to setVisible true on an AnimationDrawable to get
            // it to start animating, but it must first have been false or else the
            // call to setVisible will be ineffective. We need to clear up the story
            // about animated drawables in the future, see http://b/1878430.
            drawable.setVisible(false, false);
            drawable.setVisible(true, false);
        }
    }

    @Override
    protected void onCreateContextMenu(ContextMenu menu) {
        super.onCreateContextMenu(menu);
        if (isFromHistory()) {
            MenuInflater inflater = new MenuInflater(getContext());
            inflater.inflate(R.menu.remove_from_history, menu);
            MenuItem removeFromHistory = menu.findItem(R.id.remove_from_history);
            removeFromHistory.setOnMenuItemClickListener(new RemoveFromHistoryListener());
        }
    }

    protected void onSuggestionClicked() {
        if (mAdapter != null) {
            mAdapter.onSuggestionClicked(mPosition);
        }
    }

    protected void onSuggestionQuickContactClicked() {
        if (mAdapter != null) {
            mAdapter.onSuggestionQuickContactClicked(mPosition);
        }
    }

    protected void onRemoveFromHistoryClicked() {
        if (mAdapter != null) {
            mAdapter.onSuggestionRemoveFromHistoryClicked(mPosition);
        }
    }

    protected void onSuggestionQueryRefineClicked() {
        if (mAdapter != null) {
            mAdapter.onSuggestionQueryRefineClicked(mPosition);
        }
    }

    private class ClickListener implements OnClickListener {
        public void onClick(View v) {
            onSuggestionClicked();
        }
    }

    private class RemoveFromHistoryListener implements MenuItem.OnMenuItemClickListener {
        public boolean onMenuItemClick(MenuItem item) {
            onRemoveFromHistoryClicked();
            return false;
        }
    }

    private class KeyListener implements View.OnKeyListener {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            boolean consumed = false;
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && v != mIcon2.getView()) {
                    consumed = mIcon2.getView().requestFocus();
                    if (DBG) Log.d(TAG, "onKey Icon2 accepted focus: " + consumed);
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && v == mIcon2.getView()) {
                    consumed = requestFocus();
                    if (DBG) Log.d(TAG, "onKey SuggestionView accepted focus: " + consumed);
                }
            }
            return consumed;
        }
    }

    private class AsyncIcon {
        private final ImageView mView;
        private String mCurrentId;
        private String mWantedId;

        public AsyncIcon(ImageView view) {
            mView = view;
        }

        public void set(final Source source, final String iconId) {
            if (iconId != null) {
                mWantedId = iconId;
                if (!TextUtils.equals(mWantedId, mCurrentId)) {
                    if (DBG) Log.d(TAG, "getting icon Id=" + iconId);
                    NowOrLater<Drawable> icon = source.getIcon(iconId);
                    if (icon.haveNow()) {
                        if (DBG) Log.d(TAG, "getIcon ready now");
                        handleNewDrawable(icon.getNow(), iconId, source);
                    } else {
                        // make sure old icon is not visible while new one is loaded
                        if (DBG) Log.d(TAG , "getIcon getting later");
                        clearDrawable();
                        icon.getLater(new Consumer<Drawable>(){
                            public boolean consume(Drawable icon) {
                                if (DBG) {
                                    Log.d(TAG, "IconConsumer.consume got id " + iconId +
                                            " want id " + mWantedId);
                                }
                                // ensure we have not been re-bound since the request was made.
                                if (TextUtils.equals(iconId, mWantedId)) {
                                    handleNewDrawable(icon, iconId, source);
                                    return true;
                                }
                                return false;
                            }});
                    }
                }
            } else {
                mWantedId = null;
                handleNewDrawable(null, null, source);
            }
        }

        public ImageView getView() {
            return mView;
        }

        private void handleNewDrawable(Drawable icon, String id, Source source) {
            if (icon == null) {
                mWantedId = getFallbackIconId(source);
                if (TextUtils.equals(mWantedId, mCurrentId)) {
                    return;
                }
                icon = getFallbackIcon(source);
            }
            setDrawable(icon, id);
        }

        public void setDrawable(Drawable icon, Drawable background, String id) {
            mCurrentId = id;
            mWantedId = id;
            setViewDrawable(mView, icon);
            mView.setBackgroundDrawable(background);
        }

        private void setDrawable(Drawable icon, String id) {
            mCurrentId = id;
            setViewDrawable(mView, icon);
        }

        private void clearDrawable() {
            mCurrentId = null;
            mView.setImageDrawable(null);
        }

        protected String getFallbackIconId(Source source) {
            return null;
        }

        protected Drawable getFallbackIcon(Source source) {
            return null;
        }

    }

}
