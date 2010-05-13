
package com.android.quicksearchbox;

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
     * Updates the list of sources.
     */
    void update();

}
