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

import com.android.quicksearchbox.ui.CorporaAdapter;
import com.android.quicksearchbox.ui.CorpusViewFactory;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.GridView;

/**
 * Corpus selection dialog.
 */
public class CorpusSelectionDialog extends Dialog {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.SelectSearchSourceDialog";

    private static final int NUM_COLUMNS = 4;

    private GridView mCorpusGrid;

    private OnCorpusSelectedListener mListener;

    private Corpus mCorpus;

    private CorporaAdapter mAdapter;

    public CorpusSelectionDialog(Context context) {
        super(context, R.style.Theme_SelectSearchSource);
    }

    /**
     * Shows the corpus selection dialog.
     *
     * @param corpus The currently selected corpus.
     */
    public void show(Corpus corpus) {
        mCorpus = corpus;
        show();
    }

    public void setOnCorpusSelectedListener(OnCorpusSelectedListener listener) {
        mListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.corpus_selection_dialog);
        mCorpusGrid = (GridView) findViewById(R.id.corpus_grid);
        mCorpusGrid.setNumColumns(NUM_COLUMNS);
        mCorpusGrid.setOnItemClickListener(new CorpusClickListener());
        // TODO: for some reason, putting this in the XML layout instead makes
        // the list items unclickable.
        mCorpusGrid.setFocusable(true);

        Window window = getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        // Put window on top of input method
        lp.flags |= WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
        window.setAttributes(lp);
        if (DBG) Log.d(TAG, "Window params: " + lp);
    }

    @Override
    protected void onStart() {
        super.onStart();
        CorporaAdapter adapter =
                CorporaAdapter.createGridAdapter(getViewFactory(), getCorpusRanker());
        setAdapter(adapter);
        mCorpusGrid.setSelection(adapter.getCorpusPosition(mCorpus));
    }

    @Override
    protected void onStop() {
        setAdapter(null);
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        SearchSettings.addSearchSettingsMenuItem(getContext(), menu);
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // Cancel dialog on any touch down event which is not handled by the corpus grid
            cancel();
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = super.onKeyDown(keyCode, event);
        if (handled) {
            return handled;
        }
        // Dismiss dialog on up move when nothing, or an item on the top row, is selected.
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            int selectedRow = mCorpusGrid.getSelectedItemPosition() / NUM_COLUMNS;
            if (selectedRow <= 0) {
                cancel();
                return true;
            }
        }
        // Dismiss dialog when typing on hard keyboard (soft keyboard is behind the dialog,
        // so that can't be typed on)
        if (event.isPrintingKey()) {
            cancel();
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        SearchActivity searchActivity = getSearchActivity();
        if (searchActivity.startedIntoCorpusSelectionDialog()) {
            searchActivity.onBackPressed();
        }
        cancel();
    }

    private SearchActivity getSearchActivity() {
        return (SearchActivity) getOwnerActivity();
    }

    private void setAdapter(CorporaAdapter adapter) {
        if (adapter == mAdapter) return;
        if (mAdapter != null) mAdapter.close();
        mAdapter = adapter;
        mCorpusGrid.setAdapter(mAdapter);
    }

    private QsbApplication getQsbApplication() {
        return (QsbApplication) getContext().getApplicationContext();
    }

    private CorpusRanker getCorpusRanker() {
        return getQsbApplication().getCorpusRanker();
    }

    private CorpusViewFactory getViewFactory() {
        return getQsbApplication().getCorpusViewFactory();
    }

    protected void selectCorpus(Corpus corpus) {
        dismiss();
        if (mListener != null) {
            mListener.onCorpusSelected(corpus);
        }
    }

    private class CorpusClickListener implements AdapterView.OnItemClickListener {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Corpus corpus = (Corpus) parent.getItemAtPosition(position);
            selectCorpus(corpus);
        }
    }

    public interface OnCorpusSelectedListener {
        void onCorpusSelected(Corpus corpus);
    }
}
