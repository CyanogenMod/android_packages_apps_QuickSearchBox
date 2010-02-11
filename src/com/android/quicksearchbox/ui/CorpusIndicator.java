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

import com.android.quicksearchbox.R;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageButton;

/**
 * Utilities for setting up the corpus indicator.
 */
public class CorpusIndicator {

    public static final int ICON_VIEW_ID = R.id.search_source_selector_icon;

    private final View mView;

    private final ImageButton mIconView;

    public CorpusIndicator(View view) {
        mView = view;
        mIconView = (ImageButton) view.findViewById(ICON_VIEW_ID);
    }

    /**
     * Sets the icon displayed in the search source selector.
     */
    public void setSourceIcon(Drawable icon) {
        mIconView.setImageDrawable(icon);
    }

    public void setVisibility(int visibility) {
        mView.setVisibility(visibility);
    }

    public void setOnKeyListener(View.OnKeyListener listener) {
        mIconView.setOnKeyListener(listener);
    }

    public void setOnClickListener(View.OnClickListener listener) {
        mIconView.setOnClickListener(listener);
    }

}
