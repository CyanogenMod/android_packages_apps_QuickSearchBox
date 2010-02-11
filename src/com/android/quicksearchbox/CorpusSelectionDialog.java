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
import com.android.quicksearchbox.ui.SuggestionViewFactory;

import android.app.Dialog;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.GridView;


/**
 * Corpus selection dialog.
 */
public class CorpusSelectionDialog extends Dialog {

    private static final boolean DBG = true;
    private static final String TAG = "QSB.SelectSearchSourceDialog";

    private GridView mCorpusGrid;

    private Corpus mCorpus;

    private String mQuery;

    private Bundle mAppData;

    public CorpusSelectionDialog(Context context) {
        super(context, R.style.Theme_SelectSearchSource);
        setContentView(R.layout.corpus_selection_dialog);
        mCorpusGrid = (GridView) findViewById(R.id.corpus_grid);
        mCorpusGrid.setOnItemClickListener(new CorpusClickListener());
        // TODO: for some reason, putting this in the XML layout instead makes
        // the list items unclickable.
        mCorpusGrid.setFocusable(true);
        setCanceledOnTouchOutside(true);
        positionWindow();
    }

    public void setCorpus(Corpus corpus) {
        mCorpus = corpus;
    }

    public void setQuery(String query) {
        mQuery = query;
    }

    public void setAppData(Bundle appData) {
        mAppData = appData;
    }

    private void positionWindow() {
        Resources resources = getContext().getResources();
        int x = resources.getDimensionPixelSize(R.dimen.corpus_selection_dialog_x);
        int y = resources.getDimensionPixelSize(R.dimen.corpus_selection_dialog_y);
        positionArrowAt(x, y);
    }

    private void positionArrowAt(int x, int y) {
        Window window = getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.x = x;
        lp.y = y;
        lp.gravity = Gravity.TOP | Gravity.LEFT;
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        // Use screen coordinates
        lp.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        lp.flags |=  WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        // Put window on top of input method
        lp.flags |= WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
        window.setAttributes(lp);
        if (DBG) Log.d(TAG, "Window params: " + lp);
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateCorpora();
    }

    private void updateCorpora() {
        mCorpusGrid.setAdapter(
                new CorporaAdapter(getViewFactory(), getCorpora(), getCorpusRanker()));
    }

    private QsbApplication getQsbApplication() {
        return (QsbApplication) getContext().getApplicationContext();
    }

    private Corpora getCorpora() {
        return getQsbApplication().getCorpora();
    }

    private CorpusRanker getCorpusRanker() {
        return getQsbApplication().getCorpusRanker();
    }

    private SuggestionViewFactory getViewFactory() {
        return getQsbApplication().getSuggestionViewFactory();
    }

    protected void selectCorpus(Corpus corpus) {
        dismiss();
        // If a new source was selected, start QSB with that source.
        // If the old source was selected, just finish.
        if (!isCurrentCorpus(corpus)) {
            switchCorpus(corpus);
        }
    }

    private boolean isCurrentCorpus(Corpus corpus) {
        if (corpus == null) return mCorpus == null;
        return corpus.equals(mCorpus);
    }

    private void switchCorpus(Corpus corpus) {
        if (DBG) Log.d(TAG, "switchSource(" + corpus + ")");

        Intent searchIntent = new Intent(SearchManager.INTENT_ACTION_GLOBAL_SEARCH);
        searchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        searchIntent.setData(SearchActivity.getCorpusUri(corpus));
        searchIntent.putExtra(SearchManager.QUERY, mQuery);
        searchIntent.putExtra(SearchManager.APP_DATA, mAppData);

        try {
            if (DBG) Log.d(TAG, "startActivity(" + searchIntent.toUri(0) + ")");
            getOwnerActivity().startActivity(searchIntent);
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "Couldn't start QSB: " + ex);
        }
    }

    private class CorpusClickListener implements AdapterView.OnItemClickListener {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Corpus corpus = (Corpus) parent.getItemAtPosition(position);
            selectCorpus(corpus);
        }
    }
}
