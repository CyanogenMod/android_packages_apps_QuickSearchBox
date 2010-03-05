
package com.android.quicksearchbox;

import android.database.DataSetObserver;

import java.util.Collection;

/**
 * Search source set.
 */
public interface Sources {

    /**
     * Gets all sources.
     */
    Collection<Source> getSources();

    /**
     * Gets a source by name.
     *
     * @return A source, or {@code null} if no source with the given name exists.
     */
    Source getSource(String name);

    /**
     * Gets the web search source.
     */
    Source getWebSearchSource();

    /**
     * After calling, clients must call {@link #close()} when done with this object.
     */
    void load();

    /**
     * Releases all resources used by this object. It is possible to call
     * {@link #load()} again after calling this method.
     */
    void close();

    /**
     * Register an observer that is called when changes happen to this data set.
     *
     * @param observer gets notified when the data set changes.
     */
    void registerDataSetObserver(DataSetObserver observer);

    /**
     * Unregister an observer that has previously been registered with
     * {@link #registerDataSetObserver(DataSetObserver)}
     *
     * @param observer the observer to unregister.
     */
    void unregisterDataSetObserver(DataSetObserver observer);

}
