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
package com.android.quicksearchbox.util;

import com.android.quicksearchbox.QsbApplication;

import android.content.SharedPreferences;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Compatibility utilities
 */
public class Compat {

    private static final boolean DBG = true;
    private static final String TAG = "QSB.Compat";

    /**
     * Commits any changes made in a shared preference editor. An asynchronous version is
     * is used on platform versions where this is available.
     */
    public static void applyPrefs(SharedPreferences.Editor editor) {
        if (QsbApplication.isHoneycombOrLater()) {
            try {
                if (DBG) Log.d(TAG, "Using apply()");
                Method apply = SharedPreferences.Editor.class.getMethod("apply");
                apply.invoke(editor);
                return;
            } catch (NoSuchMethodException e) {
                Log.wtf(TAG, "No apply() method", e);
                // fall through to commit()
            } catch (InvocationTargetException e) {
                Log.wtf(TAG, "apply() failed", e);
                // fall through to commit()
            } catch (IllegalAccessException e) {
                Log.wtf(TAG, "apply() is not accessible", e);
                // fall through to commit()
            }
        }
        if (DBG) Log.d(TAG, "Using commit()");
        editor.commit();
    }

}
