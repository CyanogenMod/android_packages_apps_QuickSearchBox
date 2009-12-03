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
import android.graphics.drawable.Drawable;
import android.net.Uri;

/**
 * Base class for SuggestionCursor implementations that can get a {@link Source}
 * object for each suggestion (possibly the same for all suggestions).
 *
 */
public abstract class AbstractSourceSuggestionCursor extends AbstractSuggestionCursor {

    public AbstractSourceSuggestionCursor(String userQuery) {
        super(userQuery);
    }

    /**
     * Gets the source for the current suggestion.
     */
    protected abstract Source getSource();

    public ComponentName getSourceComponentName() {
        return getSource().getSearchActivity();
    }

    public CharSequence getSourceLabel() {
        return getSource().getLabel();
    }

    public Drawable getSourceIcon() {
        return getSource().getSourceIcon();
    }

    public Uri getSourceIconUri() {
        return getSource().getSourceIconUri();
    }

    public Drawable getIcon(String drawableId) {
        return getSource().getIcon(drawableId);
    }

    public Uri getIconUri(String iconId) {
        return getSource().getIconUri(iconId);
    }

}
