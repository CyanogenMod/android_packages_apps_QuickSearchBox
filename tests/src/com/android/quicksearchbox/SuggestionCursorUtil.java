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

import junit.framework.Assert;

/**
 * Test utilities for {@link ShortcutCursor}.
 */
public class SuggestionCursorUtil extends Assert {

    public static void assertNoSuggestions(SuggestionCursor suggestions) {
        assertNoSuggestions("", suggestions);
    }

    public static void assertNoSuggestions(String message, SuggestionCursor suggestions) {
        assertNotNull(suggestions);
        assertEquals(message, 0, suggestions.getCount());
    }

    public static void assertSameSuggestion(String message, int position,
            SuggestionCursor expected, SuggestionCursor observed) {
        message +=  " at position " + position;
        expected.moveTo(position);
        observed.moveTo(position);
        assertEquals(message + ", source", expected.getSuggestionSource(),
                observed.getSuggestionSource());
        assertEquals(message + ", shortcutId", expected.getShortcutId(),
                observed.getShortcutId());
        assertEquals(message + ", spinnerWhileRefreshing", expected.isSpinnerWhileRefreshing(),
                observed.isSpinnerWhileRefreshing());
        assertEquals(message + ", format", expected.getSuggestionFormat(),
                observed.getSuggestionFormat());
        assertEquals(message + ", icon1", expected.getSuggestionIcon1(),
                observed.getSuggestionIcon1());
        assertEquals(message + ", icon2", expected.getSuggestionIcon2(),
                observed.getSuggestionIcon2());
        assertEquals(message + ", text1", expected.getSuggestionText1(),
                observed.getSuggestionText1());
        assertEquals(message + ", text2", expected.getSuggestionText2(),
                observed.getSuggestionText2());
        assertEquals(message + ", text2Url", expected.getSuggestionText2Url(),
                observed.getSuggestionText2Url());
        assertEquals(message + ", action", expected.getSuggestionIntentAction(),
                observed.getSuggestionIntentAction());
        assertEquals(message + ", data", expected.getSuggestionIntentDataString(),
                observed.getSuggestionIntentDataString());
        assertEquals(message + ", extraData", expected.getSuggestionIntentExtraData(),
                observed.getSuggestionIntentExtraData());
        assertEquals(message + ", query", expected.getSuggestionQuery(),
                observed.getSuggestionQuery());
        assertEquals(message + ", displayQuery", expected.getSuggestionDisplayQuery(),
                observed.getSuggestionDisplayQuery());
        assertEquals(message + ", logType", expected.getSuggestionLogType(),
                observed.getSuggestionLogType());
    }

    public static void assertSameSuggestions(SuggestionCursor expected, SuggestionCursor observed) {
        assertSameSuggestions("", expected, observed);
    }

    public static void assertSameSuggestions(
            String message, SuggestionCursor expected, SuggestionCursor observed) {
        assertNotNull(expected);
        assertNotNull(message, observed);
        assertEquals(message + ", count", expected.getCount(), observed.getCount());
        assertEquals(message + ", userQuery", expected.getUserQuery(), observed.getUserQuery());
        int count = expected.getCount();
        for (int i = 0; i < count; i++) {
            assertSameSuggestion(message, i, expected, observed);
        }
    }

    public static ListSuggestionCursor slice(SuggestionCursor cursor, int start, int length) {
        ListSuggestionCursor out = new ListSuggestionCursor(cursor.getUserQuery());
        for (int i = start; i < start + length; i++) {
            out.add(new SuggestionPosition(cursor, i));
        }
        return out;
    }
}
