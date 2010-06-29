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
package com.android.quicksearchbox.google;

import com.android.quicksearchbox.SourceResult;
import com.android.quicksearchbox.SuggestionCursor;

import android.content.ComponentName;

/**
 * Interface for Google suggestion clients.
 */
public interface GoogleClient {

    public ComponentName getIntentComponent();

    public boolean isLocationAware();

    public SourceResult query(String query);

    public SuggestionCursor refreshShortcut(String shortcutId, String oldExtraData);

}
