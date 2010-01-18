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

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;

import java.net.URISyntaxException;

public abstract class CursorBackedSuggestionCursor extends AbstractSourceSuggestionCursor {

    public static final String SUGGEST_COLUMN_SECONDARY_INTENT = "suggestion_secondary_intent";
    public static final String TARGET_RECT_KEY = "target_rect";

    private static final boolean DBG = false;
    protected static final String TAG = "QSB.CursorBackedSuggestionCursor";

    /** The suggestions, or {@code null} if the suggestions query failed. */
    protected final Cursor mCursor;

    /** Column index of {@link SearchManager.SUGGEST_COLUMN_FORMAT} in @{link mCursor}. */
    private final int mFormatCol;

    /** Column index of {@link SearchManager.SUGGEST_COLUMN_TEXT_1} in @{link mCursor}. */
    private final int mText1Col;

    /** Column index of {@link SearchManager.SUGGEST_COLUMN_TEXT_2} in @{link mCursor}. */
    private final int mText2Col;

    /** Column index of {@link SearchManager.SUGGEST_COLUMN_ICON_1} in @{link mCursor}. */
    private final int mIcon1Col;

    /** Column index of {@link SearchManager.SUGGEST_COLUMN_ICON_1} in @{link mCursor}. */
    private final int mIcon2Col;

    /** Column index of {@link SearchManager.SUGGEST_COLUMN_SPINNER_WHILE_REFRESHING}
     * in @{link mCursor}.
     **/
    private final int mRefreshSpinnerCol;

    /** True if this result has been closed. */
    private boolean mClosed = false;

    public CursorBackedSuggestionCursor(String userQuery, Cursor cursor) {
        super(userQuery);
        mCursor = cursor;
        mFormatCol = getColumnIndex(SearchManager.SUGGEST_COLUMN_FORMAT);
        mText1Col = getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1);
        mText2Col = getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_2);
        mIcon1Col = getColumnIndex(SearchManager.SUGGEST_COLUMN_ICON_1);
        mIcon2Col = getColumnIndex(SearchManager.SUGGEST_COLUMN_ICON_2);
        mRefreshSpinnerCol = getColumnIndex(SearchManager.SUGGEST_COLUMN_SPINNER_WHILE_REFRESHING);
    }

    protected String getDefaultIntentAction() {
        return getSource().getDefaultIntentAction();
    }

    protected String getDefaultIntentData() {
        return getSource().getDefaultIntentData();
    }

    protected boolean shouldRewriteQueryFromData() {
        return getSource().shouldRewriteQueryFromData();
    }

    protected boolean shouldRewriteQueryFromText() {
        return getSource().shouldRewriteQueryFromText();
    }

    public boolean isFailed() {
        return mCursor == null;
    }

    public void close() {
        if (DBG) Log.d(TAG, "close()");
        if (mClosed) {
            throw new IllegalStateException("Double close()");
        }
        mClosed = true;
        if (mCursor != null) {
            // TODO: all operations on cross-process cursors can throw random exceptions
            mCursor.close();
        }
    }

    @Override
    protected void finalize() {
        if (!mClosed) {
            Log.e(TAG, "LEAK! Finalized without being closed: " + toString());
            close();
        }
    }

    public int getCount() {
        if (mClosed) {
            throw new IllegalStateException("getCount() after close()");
        }
        if (mCursor == null) return 0;
        // TODO: all operations on cross-process cursors can throw random exceptions
        return mCursor.getCount();
    }

    public void moveTo(int pos) {
        if (mClosed) {
            throw new IllegalStateException("moveTo(" + pos + ") after close()");
        }
        // TODO: all operations on cross-process cursors can throw random exceptions
        if (mCursor == null || pos < 0 || pos >= mCursor.getCount()) {
            throw new IndexOutOfBoundsException(pos + ", count=" + getCount());
        }
        // TODO: all operations on cross-process cursors can throw random exceptions
        mCursor.moveToPosition(pos);
    }

    public int getPosition() {
        if (mClosed) {
            throw new IllegalStateException("getPosition after close()");
        }
        return mCursor.getPosition();
    }

    public String getSuggestionDisplayQuery() {
        String query = getSuggestionQuery();
        if (query != null) {
            return query;
        }
        if (shouldRewriteQueryFromData()) {
            String data = getSuggestionIntentDataString();
            if (data != null) {
                return data;
            }
        }
        if (shouldRewriteQueryFromText()) {
            String text1 = getSuggestionText1();
            if (text1 != null) {
                return text1;
            }
        }
        return null;
    }

    public String getShortcutId() {
        return getStringOrNull(SearchManager.SUGGEST_COLUMN_SHORTCUT_ID);
    }

    public String getSuggestionFormat() {
        return getStringOrNull(mFormatCol);
    }

    public String getSuggestionText1() {
        return getStringOrNull(mText1Col);
    }

    public String getSuggestionText2() {
        return getStringOrNull(mText2Col);
    }

    public String getSuggestionIcon1() {
        return getStringOrNull(mIcon1Col);
    }

    public String getSuggestionIcon2() {
        return getStringOrNull(mIcon2Col);
    }

    public boolean isSpinnerWhileRefreshing() {
        return "true".equals(getStringOrNull(mRefreshSpinnerCol));
    }

    public Intent getSuggestionIntent(Context context, Bundle appSearchData,
            int actionKey, String actionMsg) {
        String action = getSuggestionIntentAction();
        Uri data = getSuggestionIntentData();
        String query = getSuggestionQuery();
        String userQuery = getUserQuery();
        String extraData = getSuggestionIntentExtraData();

        // Now build the Intent
        Intent intent = new Intent(action);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // We need CLEAR_TOP to avoid reusing an old task that has other activities
        // on top of the one we want.
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (data != null) {
            intent.setData(data);
        }
        intent.putExtra(SearchManager.USER_QUERY, userQuery);
        if (query != null) {
            intent.putExtra(SearchManager.QUERY, query);
        }
        if (extraData != null) {
            intent.putExtra(SearchManager.EXTRA_DATA_KEY, extraData);
        }
        if (appSearchData != null) {
            intent.putExtra(SearchManager.APP_DATA, appSearchData);
        }
        if (actionKey != KeyEvent.KEYCODE_UNKNOWN) {
            intent.putExtra(SearchManager.ACTION_KEY, actionKey);
            intent.putExtra(SearchManager.ACTION_MSG, actionMsg);
        }
        // TODO: Use this to tell sources this comes form global search
        // The constants are currently hidden.
        //        intent.putExtra(SearchManager.SEARCH_MODE,
        //                SearchManager.MODE_GLOBAL_SEARCH_SUGGESTION);
        intent.setComponent(getSuggestionIntentComponent(context, intent));
        return intent;
    }

    public Intent getSecondarySuggestionIntent(Context context, Bundle appSearchData, Rect target) {
        String intentString = getStringOrNull(SUGGEST_COLUMN_SECONDARY_INTENT);
        if (intentString != null) {
            try {
                Intent intent = Intent.parseUri(intentString, Intent.URI_INTENT_SCHEME);
                if (appSearchData != null) {
                    intent.putExtra(SearchManager.APP_DATA, appSearchData);
                }
                // TODO: Do we need to pass action keys?
                // TODO: Should we try to use defaults such as getDefaultIntentData?
                intent.putExtra(TARGET_RECT_KEY, target);
                intent.setComponent(getSuggestionIntentComponent(context, intent));
                return intent;
            }  catch (URISyntaxException e) {
                Log.w(TAG, "Unable to parse secondary intent " + intentString);
            }
        }
        return null;
    }

    /**
     * Updates the intent with the component to which intents created
     * from the current suggestion should be sent.
     */
    protected ComponentName getSuggestionIntentComponent(Context context, Intent intent) {
        ComponentName component = getSourceComponentName();
        // Limit intent resolution to the source package.
        intent.setPackage(component.getPackageName());
        ComponentName resolvedComponent = intent.resolveActivity(context.getPackageManager());
        if (resolvedComponent != null) {
            // It's ok if the intent resolves to an activity in the same
            // package as component.  We set the component explicitly to
            // avoid having to re-resolve, and to prevent race conditions.
            return resolvedComponent;
        } else {
            return component;
        }
    }

    public boolean hasSecondaryIntent() {
           return getStringOrNull(SUGGEST_COLUMN_SECONDARY_INTENT) != null;
       }

    public String getActionKeyMsg(int keyCode) {
        String result = null;
        String column = getSource().getSuggestActionMsgColumn(keyCode);
        if (column != null) {
            result = getStringOrNull(column);
        }
        // If the cursor didn't give us a message, see if there's a single message defined
        // for the actionkey (for all suggestions)
        if (result == null) {
            result = getSource().getSuggestActionMsg(keyCode);
        }
        return result;
    }

    /**
     * Gets the intent action for the current suggestion.
     */
    protected String getSuggestionIntentAction() {
        // use specific action if supplied, or default action if supplied, or fixed default
        String action = getStringOrNull(SearchManager.SUGGEST_COLUMN_INTENT_ACTION);
        if (action == null) {
            action = getDefaultIntentAction();
            if (action == null) {
                action = Intent.ACTION_SEARCH;
            }
        }
        return action;
    }

    /**
     * Gets the query for the current suggestion.
     */
    protected String getSuggestionQuery() {
        return getStringOrNull(SearchManager.SUGGEST_COLUMN_QUERY);
    }

    private String getSuggestionIntentDataString() {
         // use specific data if supplied, or default data if supplied
         String data = getStringOrNull(SearchManager.SUGGEST_COLUMN_INTENT_DATA);
         if (data == null) {
             data = getDefaultIntentData();
         }
         // then, if an ID was provided, append it.
         if (data != null) {
             String id = getStringOrNull(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
             if (id != null) {
                 data = data + "/" + Uri.encode(id);
             }
         }
         return data;
     }

    /**
     * Gets the intent data for the current suggestion.
     */
    protected Uri getSuggestionIntentData() {
        String data = getSuggestionIntentDataString();
        return (data == null) ? null : Uri.parse(data);
    }

    /**
     * Gets the intent extra data for the current suggestion.
     */
    public String getSuggestionIntentExtraData() {
        return getStringOrNull(SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA);
    }

    /**
     * Gets the index of a column in {@link mCursor} by name.
     *
     * @return The index, or {@code -1} if the column was not found.
     */
    protected int getColumnIndex(String colName) {
        if (mCursor == null) return -1;
        // TODO: all operations on cross-process cursors can throw random exceptions
        return mCursor.getColumnIndex(colName);
    }

    /**
     * Gets the string value of a column in {@link mCursor} by column index.
     *
     * @param col Column index.
     * @return The string value, or {@code null}.
     */
    protected String getStringOrNull(int col) {
        if (mCursor == null) return null;
        if (col == -1) {
            return null;
        }
        try {
            // TODO: all operations on cross-process cursors can throw random exceptions
            return mCursor.getString(col);
        } catch (Exception e) {
            Log.e(TAG,
                    "unexpected error retrieving valid column from cursor, "
                            + "did the remote process die?", e);
            return null;
        }
    }

    /**
     * Gets the string value of a column in {@link mCursor} by column name.
     *
     * @param colName Column name.
     * @return The string value, or {@code null}.
     */
    protected String getStringOrNull(String colName) {
        int col = getColumnIndex(colName);
        return getStringOrNull(col);
    }

    private String makeKeyComponent(String str) {
        return str == null ? "" : str;
    }

    public String getSuggestionKey() {
        String action = makeKeyComponent(getSuggestionIntentAction());
        String data = makeKeyComponent(getSuggestionIntentDataString());
        String query = makeKeyComponent(getSuggestionQuery());
        // calculating accurate size of string builder avoids an allocation vs starting with
        // the default size and having to expand.
        int size = action.length() + 2 + data.length() + query.length();
        return new StringBuilder(size)
                .append(action)
                .append('#')
                .append(data)
                .append('#')
                .append(query)
                .toString();
    }
}
