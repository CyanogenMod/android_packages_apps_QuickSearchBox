/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.util.Log;

import java.util.HashSet;

/**
 * A SuggestionCursor that allows shortcuts to be updated by overlaying
 * with results from another cursor.
 */
class ShortcutCursor extends ListSuggestionCursor {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.ShortcutCursor";

    // mShortcuts is used to close the underlying cursor when we're closed.
    private final CursorBackedSuggestionCursor mShortcuts;
    // mRefreshed contains all the cursors that have been refreshed, so that
    // they can be closed when ShortcutCursor is closed.
    private final HashSet<SuggestionCursor> mRefreshed;

    private boolean mClosed;

    public ShortcutCursor(int maxShortcuts, CursorBackedSuggestionCursor shortcuts) {
        super(shortcuts.getUserQuery());
        mShortcuts = shortcuts;
        mRefreshed = new HashSet<SuggestionCursor>();
        int count = shortcuts.getCount();
        for (int i = 0; i < count; i++) {
            if (getCount() >= maxShortcuts) break;
            shortcuts.moveTo(i);
            if (shortcuts.getSuggestionSource() != null) {
                add(new SuggestionPosition(shortcuts));
            } else {
                if (DBG) Log.d(TAG, "Skipping shortcut " + i);
            }
        }
    }

    /**
     * Updates this SuggestionCursor with a refreshed result from another.
     * Since this modifies the cursor, it should be called on the UI thread.
     * This class assumes responsibility for closing refreshed.
     */
    public void refresh(Source source, String shortcutId, SuggestionCursor refreshed) {
        if (DBG) Log.d(TAG, "refresh " + shortcutId);
        if (mClosed) {
            if (refreshed != null) {
                refreshed.close();
            }
            return;
        }
        if (refreshed != null) {
            mRefreshed.add(refreshed);
        }
        int count = getCount();
        for (int i = 0; i < count; i++) {
            moveTo(i);
            if (shortcutId.equals(getShortcutId()) && source.equals(getSuggestionSource())) {
              if (refreshed != null && refreshed.getCount() > 0) {
                  replaceRow(new SuggestionPosition(refreshed));
              } else {
                  removeRow();
              }
              notifyDataSetChanged();
              break;
            }
        }
    }

    @Override
    public void close() {
        if (DBG) Log.d(TAG, "close()");
        if (mClosed) {
            throw new IllegalStateException("Double close()");
        }
        mClosed = true;
        mShortcuts.close();
        for (SuggestionCursor cursor : mRefreshed) {
             cursor.close();
        }
        super.close();
    }
}