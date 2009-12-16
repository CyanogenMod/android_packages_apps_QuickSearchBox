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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

public interface SuggestionCursor {

    /**
     * The user query that returned these suggestions.
     */
    String getUserQuery();

    /**
     * Gets the component name of the source that produced this result.
     */
    ComponentName getSourceComponentName();

    /**
     * Gets the localized, human-readable label for the source that produced this result.
     */
    CharSequence getSourceLabel();

    /**
     * Gets the icon for the source that produced this result.
     */
    Drawable getSourceIcon();

    /**
     * Gets the icon URI for the source that produced this result.
     */
    Uri getSourceIconUri();

    /**
     * Gets an icon by ID. Used for getting the icons returned by {@link #getSuggestionIcon1()}
     * and {@link #getSuggestionIcon2()}.
     */
    Drawable getIcon(String iconId);

    /**
     * Gets the URI for an icon.
     */
    Uri getIconUri(String iconId);

    /**
     * Checks whether this result represents a failed suggestion query.
     */
    boolean isFailed();

    /**
     * Closes this suggestion result. 
     */
    void close();

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
     * Gets the current position within the cursor.
     */
    int getPosition();

    /**
     * Gets the text to put in the search box when the current suggestion is selected.
     */
    String getSuggestionDisplayQuery();

    /**
     * Gets the shortcut ID of the current suggestion.
     */
    String getShortcutId();

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
     * Gets the left-hand-side icon for the current suggestion.
     *
     * @return A string that can be passed to {@link #getIcon()}.
     */
    String getSuggestionIcon1();

    /**
     * Gets the right-hand-side icon for the current suggestion.
     *
     * @return A string that can be passed to {@link #getIcon()}.
     */
    String getSuggestionIcon2();

    /**
     * Gets the intent that this suggestion launches.
     *
     * @param context Used for resolving the intent target.
     * @param actionKey
     * @param actionMsg
     * @return
     */
    Intent getSuggestionIntent(Context context, Bundle appSearchData,
            int actionKey, String actionMsg);

    /**
     * Gets the secondary intent associated with this suggestion.
     *
     * @param context Used for resolving the intent target.
     * @param appSearchData application specific data passed in the search request
     * @param target Rect describing the position of this result on the screen
     * @return the secondary intent, or {@code null} if there is none
     */
    Intent getSecondarySuggestionIntent(Context context, Bundle appSearchData,
            Rect target);

    /**
     * Checks whether the current suggestion has a secondary intent.
     */
    boolean hasSecondaryIntent();

    /**
     * Gets a unique key that identifies this suggestion. This is used to avoid
     * duplicate suggestions in the promoted list. This key should be based on
     * the intent of the suggestion.
     */
    String getSuggestionKey();

    /**
     * Gets the first suggestion text line as styled text.
     */
    CharSequence getSuggestionFormattedText1();

    /**
     * Gets the second suggestion text line as styled text.
     */
    CharSequence getSuggestionFormattedText2();

    /**
     * Gets the first suggestion icon.
     */
    Drawable getSuggestionDrawableIcon1();

    /**
     * Gets the second suggestion icon.
     */
    Drawable getSuggestionDrawableIcon2();

    /**
     * Gets the action message for a key code for the current suggestion.
     *
     * @param keyCode Key code, see {@link android.view.KeyEvent}.
     * @return The action message for the key, or {@code null} if there is none.
     */
    String getActionKeyMsg(int keyCode);

}
