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

import android.graphics.Rect;
import android.view.View;

/**
 * General utilities.
 */
public class Util {

    // NOTE: The version of this method in android.app.SearchSourceSelector
    // uses getCompatibilityInfo(), since it may be running in compatibility
    // mode. QuickSearchBox doesn't run in compatibility mode, and
    // getCompatibilityInfo() is hidden, so we don't use it here.
    public static Rect getOnScreenRect(View v) {
        final int[] pos = new int[2];
        v.getLocationOnScreen(pos);
        final Rect rect = new Rect();
        rect.left = pos[0];
        rect.top = pos[1];
        rect.right = pos[0] + v.getWidth();
        rect.bottom = pos[1] + v.getHeight();
        return rect;
    }

}
