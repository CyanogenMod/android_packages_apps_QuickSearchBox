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

import android.text.Spannable;
import android.text.style.StyleSpan;

/**
 * Suggestion formatter interface. This is used to bold (or otherwise highlight) portions of a
 * suggestion which were not a part of the query.
 */
public abstract class SuggestionFormatter {

    private int mQueryTextStyle;
    private int mQueryStyleFlags;

    private int mSuggestedTextStyle;
    private int mSuggestedStyleFlags;


    protected SuggestionFormatter() {
    }

    /**
     * Formats a suggestion for display in the UI.
     *
     * @param query the query as entered by the user
     * @param suggestion the suggestion
     * @return Formatted suggestion text.
     */
    public abstract CharSequence formatSuggestion(CharSequence query, CharSequence suggestion);

    /**
     * Sets the text format to use for portions of a suggestions which form a part of the original
     * used query.
     *
     * @param style Style for query text. Values are constants defined in
     *      {@link android.graphics.Typeface}
     * @param flags Flags  to determine how the span will behave when text is inserted at the start
     *      or end of the span's range.
     */
    public void setQueryTextFormat(int style, int flags) {
        mQueryTextStyle = style;
        mQueryStyleFlags = flags;
    }

    /**
     * Sets the text format to use for portions of a suggestion which are new.
     *
     * @param style Style for suggested text. Values are constants defined in
     *      {@link android.graphics.Typeface}
     * @param flags Flags to determine how the span will behave when text is inserted at the start
     *      or end of the span's range.
     */
    public void setSuggestedTextFormat(int style, int flags) {
        mSuggestedTextStyle = style;
        mSuggestedStyleFlags = flags;
    }

    protected void applyQueryTextStyle(Spannable text, int start, int end) {
        if (start == end) return;
        StyleSpan style = new StyleSpan(mQueryTextStyle);
        text.setSpan(style, start, end, mQueryStyleFlags);
    }

    protected void applySuggestedTextStyle(Spannable text, int start, int end) {
        if (start == end) return;
        StyleSpan style = new StyleSpan(mSuggestedTextStyle);
        text.setSpan(style, start, end, mSuggestedStyleFlags);
    }

}
