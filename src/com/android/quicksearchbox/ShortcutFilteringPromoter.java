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

import java.util.ArrayList;

/**
 * A promoter that adds shortcuts which meet some criteria, as defined by the {@link #accept}
 * method.
 */
public abstract class ShortcutFilteringPromoter<C extends SuggestionCursor>
        extends PromoterWrapper<C> {

    private static final String TAG = "QSB.ShortcutFilteringPromoter";
    private static final boolean DBG = false;

    public ShortcutFilteringPromoter(Promoter<C> nextPromoter) {
        super(nextPromoter);
    }

    @Override
    public void pickPromoted(SuggestionCursor shortcuts,
            ArrayList<C> suggestions, int maxPromoted,
            ListSuggestionCursor promoted) {
        int shortcutCount = shortcuts == null ? 0 : shortcuts.getCount();
        int promotedShortcutCount = Math.min(shortcutCount, maxPromoted);
        if (DBG) {
            Log.d(TAG, "pickPromoted(shortcutCount = " + shortcutCount +
                    ", maxPromoted = " + maxPromoted + ") this = " + this);
        }

        for (int i = 0; i < promotedShortcutCount; ++i) {
            shortcuts.moveTo(i);
            if (accept(shortcuts)) {
                if (DBG) Log.d(TAG, "Accepting suggestion " + shortcuts.getSuggestionText1());
                promoted.add(new SuggestionPosition(shortcuts, i));
            } else {
                if (DBG) Log.d(TAG, "Not accepting suggestion " + shortcuts.getSuggestionText1());
            }

        }
        super.pickPromoted(shortcuts, suggestions, maxPromoted, promoted);
    }

    /**
     * Determine if this shortcut should be added to the promoted suggestion list.
     * @param s The suggestion in question
     * @return true to include it in the results
     */
    protected abstract boolean accept(Suggestion s);

}
