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

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;

/**
 * Provides content to a {@link TabView}.
 */
public interface TabAdapter {

    /**
     * Gets the number of tabs.
     */
    int getTabCount();

    /**
     * Gets a unique string identifying a tab.
     *
     * @param position Tab position.
     */
    String getTag(int position);

    /**
     * Gets the position of a tab given its tag.
     */
    int getTabPosition(String tag);

    /**
     * Gets the view used for the tab handle.
     */
    View getTabHandleView(int position, ViewGroup parent);

    /**
     * Gets the view used for the tab content.
     */
    View getTabContentView(int position, ViewGroup parent);

    /**
     * Closes the tab adapter, releasing any resources that it may be using.
     */
    void close();

    /**
     * Register an observer that is called when changes happen to the data used by this adapter.
     *
     * @param observer the object that gets notified when the data set changes.
     */
    void registerDataSetObserver(DataSetObserver observer);

    /**
     * Unregister an observer that has previously been registered with this
     * adapter via {@link #registerDataSetObserver}.
     *
     * @param observer the object to unregister.
     */
    void unregisterDataSetObserver(DataSetObserver observer);

}
