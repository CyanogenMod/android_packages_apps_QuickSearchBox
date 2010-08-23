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


/**
 * A simple promoter that concatenates the shortcuts and the source results.
 */
public class ConcatPromoter implements Promoter {

    private final int mMaxShortcuts;

    public ConcatPromoter(int maxShortcuts) {
        mMaxShortcuts = maxShortcuts;
    }

    public void pickPromoted(Suggestions suggestions, int maxPromoted,
            ListSuggestionCursor promoted) {
        // Add shortcuts
        SuggestionCursor shortcuts = suggestions.getShortcuts();
        int shortcutCount = shortcuts == null ? 0 : shortcuts.getCount();
        for (int i = 0; i < shortcutCount && promoted.getCount() < mMaxShortcuts; i++) {
            promoted.add(new SuggestionPosition(shortcuts, i));
        }

        // Add suggestions
        for (SuggestionCursor c : suggestions.getCorpusResults()) {
            for (int i = 0; i < c.getCount(); i++) {
                if (promoted.getCount() >= maxPromoted) {
                    return;
                }
                promoted.add(new SuggestionPosition(c, i));
            }
        }
    }

}
