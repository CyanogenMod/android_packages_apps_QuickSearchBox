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

import android.database.Cursor;

/**
 * The result of getting suggestions from a single source.
 *
 * This class is similar to a cursor, in that it is moved to a suggestion, and then
 * the suggestion info can be read out.
 *
 */
public class SourceResult extends CursorBackedSuggestionCursor {

    /** The suggestion source. */
    private final Source mSource;

    /**
     * Creates a result for a failed or canceled query.
     */
    public SourceResult(Source source, String userQuery) {
        this(source, userQuery, null);
    }

    /**
     * Creates a new source result.
     *
     * @param source The suggestion source. Must be non-null.
     * @param cursor The cursor containing the suggestions. May be null.
     */
    public SourceResult(Source source, String userQuery, Cursor cursor) {
        super(userQuery, cursor);
        if (source == null) {
            throw new NullPointerException("source is null");
        }
        mSource = source;
    }

    protected Source getSource() {
        return mSource;
    }

    @Override
    public String toString() {
        return "SourceResult{source=" + mSource + ",query=" + getUserQuery() + "}";
    }
}
