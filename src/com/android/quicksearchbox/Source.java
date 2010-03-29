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
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

/**
 * Interface for suggestion sources.
 *
 */
public interface Source extends SuggestionCursorProvider<SourceResult> {

    /**
     * Gets the name of the activity that this source is for. When a suggestion is
     * clicked, the resulting intent will be sent to this activity.
     */
    ComponentName getComponentName();

    /**
     * Gets the version code of the source. This is expected to change when the app that
     * this source is for is upgraded.
     */
    int getVersionCode();

    /**
     * Gets the localized, human-readable label for this source.
     */
    CharSequence getLabel();

    /**
     * Gets the icon for this suggestion source.
     */
    Drawable getSourceIcon();

    /**
     * Gets the icon URI for this suggestion source.
     */
    Uri getSourceIconUri();

    /**
     * Gets an icon from this suggestion source.
     *
     * @param drawableId Resource ID or URI.
     */
    Drawable getIcon(String drawableId);

    /**
     * Gets the URI for an icon form this suggestion source.
     *
     * @param drawableId Resource ID or URI.
     */
    Uri getIconUri(String drawableId);

    /**
     * Gets the search hint text for this suggestion source.
     */
    CharSequence getHint();

    /**
     * Gets the description to use for this source in system search settings.
     */
    CharSequence getSettingsDescription();

    /**
     *
     *  Note: this does not guarantee that this source will be queried for queries of
     *  this length or longer, only that it will not be queried for anything shorter.
     *
     * @return The minimum number of characters needed to trigger this source.
     */
    int getQueryThreshold();

    /**
     * Indicates whether a source should be invoked for supersets of queries it has returned zero
     * results for in the past.  For example, if a source returned zero results for "bo", it would
     * be ignored for "bob".
     *
     * If set to <code>false</code>, this source will only be ignored for a single session; the next
     * time the search dialog is brought up, all sources will be queried.
     *
     * @return <code>true</code> if this source should be queried after returning no results.
     */
    boolean queryAfterZeroResults();

    boolean voiceSearchEnabled();

    Intent createSearchIntent(String query, Bundle appData);

    Intent createVoiceSearchIntent(Bundle appData);

    /**
     * Gets suggestions from this source.
     *
     * @param query The user query.
     * @param queryLimit An advisory maximum number of results that the source should return.
     * @return The suggestion results.
     */
    SourceResult getSuggestions(String query, int queryLimit);

    /**
     * Updates a shorcut.
     *
     * @param shortcutId The id of the shortcut to update.
     * @param extraData associated with this shortcut.
     * @return A SuggestionCursor positioned at the updated shortcut.  If the
     *         cursor is empty or <code>null</code>, the shortcut will be removed.
     */
    SuggestionCursor refreshShortcut(String shortcutId, String extraData);

    /**
     * Checks whether this is a web suggestion source.
     */
    boolean isWebSuggestionSource();

    /**
     * Checks whether the text in the query field should come from the suggestion intent data.
     */
    boolean shouldRewriteQueryFromData();

    /**
     * Checks whether the text in the query field should come from the suggestion title.
     */
    boolean shouldRewriteQueryFromText();

    /**
     * Gets the default intent action for suggestions from this source.
     *
     * @return The default intent action, or {@code null}.
     */
    String getDefaultIntentAction();

    /**
     * Gets the default intent data for suggestions from this source.
     *
     * @return The default intent data, or {@code null}.
     */
    String getDefaultIntentData();

}
