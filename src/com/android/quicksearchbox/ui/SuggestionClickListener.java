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

package com.android.quicksearchbox.ui;

/**
 * Listener interface for clicks on suggestions.
 */
public interface SuggestionClickListener {

    /**
     * Called when a suggestion is clicked.
     *
     * @param adapter Adapter that contains the clicked suggestion.
     * @param position Position of the clicked suggestion.
     */
    void onSuggestionClicked(SuggestionsAdapter adapter, int position);

    /**
     * Called when a quick contact badge in a suggestion is clicked.
     *
     * @param adapter Adapter that contains the clicked suggestion.
     * @param position Position of the suggestion.
     */
    void onSuggestionQuickContactClicked(SuggestionsAdapter adapter, int position);

    /**
     * Called when the user requests to remove a suggestion from the search history.
     *
     * @param adapter Adapter that contains the clicked suggestion.
     * @param position Position of the clicked suggestion.
     */
    void onSuggestionRemoveFromHistoryClicked(SuggestionsAdapter adapter, int position);

    /**
     * Called when the "query refine" button of a suggestion is clicked.
     *
     * @param adapter Adapter that contains the clicked suggestion.
     * @param position Position of the suggestion.
     */
    void onSuggestionQueryRefineClicked(SuggestionsAdapter adapter, int position);
}
