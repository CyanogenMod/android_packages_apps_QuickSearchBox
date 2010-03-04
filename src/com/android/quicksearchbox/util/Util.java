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

package com.android.quicksearchbox.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * General utilities.
 */
public class Util {

    public static <A> Set<A> setOfFirstN(List<A> list, int n) {
        int end = Math.min(list.size(), n);
        HashSet<A> set = new HashSet<A>(end);
        for (int i = 0; i < end; i++) {
            set.add(list.get(i));
        }
        return set;
    }

}
