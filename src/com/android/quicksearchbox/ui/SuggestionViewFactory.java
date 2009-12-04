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

import android.graphics.drawable.Drawable;
import android.view.ViewGroup;

/**
 * Creates suggestion views.
 */
public interface SuggestionViewFactory {

    /**
     * Creates a suggestion view.
     *
     * @param parentViewType Used to create LayoutParams of the right type.
     */
    SuggestionView createSuggestionView(ViewGroup parentViewType);

    /**
     * Creates a suggestion list view.
     *
     * @param parentViewType Used to create LayoutParams of the right type.
     */
    SuggestionListView createSuggestionListView(ViewGroup parentViewType);

    /**
     * Creates a tab handle view.
     *
     * @param parentViewType Used to create LayoutParams of the right type.
     */
    TabHandleView createSuggestionTabView(ViewGroup parentViewType);

    /**
     * Gets the icon to use for the promoted tab.
     */
    Drawable getPromotedIcon();

}
