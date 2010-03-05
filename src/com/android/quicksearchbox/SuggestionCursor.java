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

import android.database.DataSetObserver;


/**
 * A sequence of suggestions, with a current position.
 */
public interface SuggestionCursor {

    /**
     * Gets the number of suggestions in this result.
     *
     * @return The number of suggestions, or {@code 0} if this result represents a failed query.
     */
    int getCount();

    /**
     * Moves to a given suggestion.
     *
     * @param pos The position to move to.
     * @throws IndexOutOfBoundsException if {@code pos < 0} or {@code pos >= getCount()}.
     */
    void moveTo(int pos);

    /**
     * Moves to the next suggestion, if there is one.
     *
     * @return {@code false} if there is no next suggestion.
     */
    boolean moveToNext();

    /**
     * Gets the current position within the cursor.
     */
    int getPosition();

    /**
     * Frees any resources used by this cursor.
     */
    void close();

    /**
     * Register an observer that is called when changes happen to this data set.
     *
     * @param observer gets notified when the data set changes.
     */
    void registerDataSetObserver(DataSetObserver observer);

    /**
     * Unregister an observer that has previously been registered with 
     * {@link #registerDataSetObserver(DataSetObserver)}
     *
     * @param observer the observer to unregister.
     */
    void unregisterDataSetObserver(DataSetObserver observer);

    /**
     * Gets the source that produced the current suggestion.
     */
    Source getSuggestionSource();

    /**
     * Gets the query that the user typed to get this suggestion.
     */
    String getUserQuery();

    /**
     * Gets the shortcut ID of the current suggestion.
     */
    String getShortcutId();

    /**
     * Whether to show a spinner while refreshing this shortcut.
     */
    boolean isSpinnerWhileRefreshing();

    /**
     * Gets the format of the text returned by {@link #getSuggestionText1()}
     * and {@link #getSuggestionText2()}.
     *
     * @return {@code null} or "html"
     */
    String getSuggestionFormat();

    /**
     * Gets the first text line for the current suggestion.
     */
    String getSuggestionText1();

    /**
     * Gets the second text line for the current suggestion.
     */
    String getSuggestionText2();

    /**
     * Gets the second text line URL for the current suggestion.
     */
    String getSuggestionText2Url();

    /**
     * Gets the left-hand-side icon for the current suggestion.
     *
     * @return A string that can be passed to {@link Source#getIcon(String)}.
     */
    String getSuggestionIcon1();

    /**
     * Gets the right-hand-side icon for the current suggestion.
     *
     * @return A string that can be passed to {@link Source#getIcon(String)}.
     */
    String getSuggestionIcon2();

    /**
     * Gets the intent action for the current suggestion.
     */
    String getSuggestionIntentAction();

    /**
     * Gets the extra data associated with this suggestion's intent.
     */
    String getSuggestionIntentExtraData();

    /**
     * Gets the data associated with this suggestion's intent.
     */
    String getSuggestionIntentDataString();

    /**
     * Gets the data associated with this suggestion's intent.
     */
    String getSuggestionQuery();

    String getSuggestionDisplayQuery();

    /**
     * Gets a unique key that identifies this suggestion. This is used to avoid
     * duplicate suggestions in the promoted list. This key should be based on
     * the intent of the suggestion.
     */
    String getSuggestionKey();

    /**
     * Gets the suggestion log type for the current suggestion. This is logged together
     * with the value returned from {@link Source#getName()}.
     * The value is source-specific. Most sources return {@code null}.
     */
    String getSuggestionLogType();

    /**
     * Checks if this suggestion is a shortcut.
     */
    boolean isSuggestionShortcut();
}
