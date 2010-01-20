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

package com.android.quicksearchbox.ui;

import com.android.quicksearchbox.SuggestionPosition;

/**
 * Listener interface for suggestion selection.
 */
public interface SuggestionSelectionListener {
    /**
     * Called when the suggestion selection changes.
     *
     * @param suggestion The new selected suggestion, or {@code null} if
     *        no suggestion is now selected.
     */
    void onSelectionChanged(SuggestionPosition suggestion);
}
