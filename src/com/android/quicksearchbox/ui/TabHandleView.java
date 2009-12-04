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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;

/**
 * Suggestion tab handle view.
 */
public class TabHandleView extends RelativeLayout {

    public TabHandleView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TabHandleView(Context context) {
        super(context);
    }

    public void setIcon(Drawable icon) {
        ImageView imageView = (ImageView) findViewById(R.id.tab_icon);
        imageView.setImageDrawable(icon);
    }

}
