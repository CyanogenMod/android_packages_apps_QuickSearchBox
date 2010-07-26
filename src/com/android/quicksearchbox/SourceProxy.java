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

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

/**
 * @author mathewi@google.com (Your Name Here)
 *
 */
public class SourceProxy<S extends Source> implements Source {

    protected final S mParent;

    protected SourceProxy(S parent) {
        mParent = parent;
    }

    public boolean canRead() {
        return mParent.canRead();
    }

    public Intent createSearchIntent(String query, Bundle appData) {
        return mParent.createSearchIntent(query, appData);
    }

    public Intent createVoiceSearchIntent(Bundle appData) {
        return mParent.createVoiceSearchIntent(appData);
    }

    public String getDefaultIntentAction() {
        return mParent.getDefaultIntentAction();
    }

    public String getDefaultIntentData() {
        return mParent.getDefaultIntentData();
    }

    public CharSequence getHint() {
        return mParent.getHint();
    }

    public Drawable getIcon(String drawableId) {
        return mParent.getIcon(drawableId);
    }

    public Uri getIconUri(String drawableId) {
        return mParent.getIconUri(drawableId);
    }

    public ComponentName getIntentComponent() {
        return mParent.getIntentComponent();
    }

    public CharSequence getLabel() {
        return mParent.getLabel();
    }

    public int getQueryThreshold() {
        return mParent.getQueryThreshold();
    }

    public CharSequence getSettingsDescription() {
        return mParent.getSettingsDescription();
    }

    public Drawable getSourceIcon() {
        return mParent.getSourceIcon();
    }

    public Uri getSourceIconUri() {
        return mParent.getSourceIconUri();
    }

    public SourceResult getSuggestions(String query, int queryLimit, boolean onlySource) {
        return mParent.getSuggestions(query, queryLimit, onlySource);
    }

    public int getVersionCode() {
        return mParent.getVersionCode();
    }

    public boolean isLocationAware() {
        return mParent.isLocationAware();
    }

    public boolean isVersionCodeCompatible(int version) {
        return mParent.isVersionCodeCompatible(version);
    }

    public boolean isWebSuggestionSource() {
        return mParent.isWebSuggestionSource();
    }

    public boolean queryAfterZeroResults() {
        return mParent.queryAfterZeroResults();
    }

    public SuggestionCursor refreshShortcut(String shortcutId, String extraData) {
        return mParent.refreshShortcut(shortcutId, extraData);
    }

    public boolean voiceSearchEnabled() {
        return mParent.voiceSearchEnabled();
    }

    public String getName() {
        return mParent.getName();
    }

    public Source getRoot() {
        return mParent.getRoot();
    }

    @Override
    public boolean equals(Object o) {
        return getRoot().equals(o);
    }

    @Override
    public int hashCode() {
        return getRoot().hashCode();
    }

}
