/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Jun 30, 2020 (bjoern): created
 */
package org.knime.filehandling.core.connections.base;

import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.knime.filehandling.core.connections.FSPath;

/**
 * Base implementation of a "paged" path iterator. Paging is important when a directory contains too many entries to
 * fetch them all at once and the fetching needs to be split into multiple requests (this is for example the case with
 * some of the object stores).
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @param <T> The path type.
 * @noreference non-public API
 * @noextend non-public API
 */
public abstract class PagedPathIterator<T extends FSPath> implements Iterator<T> {

    /**
     * The path to list.
     */
    protected final T m_path;

    private final Filter<? super Path> m_filter;

    private T m_next;

    private Iterator<T> m_currPage;

    /**
     * Creates new instance.
     *
     * @param path The path to create an iterator for.
     * @param filter Filter to apply on the returned paths.
     */
    protected PagedPathIterator(final T path, final Filter<? super Path> filter) {
        m_path = path;
        m_filter = filter;
    }

    /**
     * Sets the first page that shall get returned by this path iterator. This method should be called to initialize
     * this iterator, i.e. before {@link #hasNext()} or {@link #next()} get called. This method skips to the next path
     * that matches the filter provider to the constructor. In consequence, it may completely consume the given iterator
     * as well as subsequent "pages" returned by {@link #loadNextPage()}.
     *
     * @param firstPage An iterator that contains the first page of paths.
     * @throws IOException When something went wrong while fetching a page.
     */
    protected void setFirstPage(final Iterator<T> firstPage) throws IOException {
        m_currPage = firstPage;
        m_next = getNextPath();
    }

    private T getNextPath() throws IOException {
        T next = getNextPathFromCurrPage();
        while (next == null && hasNextPage()) {
            m_currPage = loadNextPage();
            next = getNextPathFromCurrPage();
        }

        return next;
    }

    private T getNextPathFromCurrPage() throws IOException {
        if (m_currPage == null) {
            return null;
        }

        while (m_currPage.hasNext()) {
            final T next = m_currPage.next();
            if (m_filter.accept(next)) {
                return next;
            }
        }

        // we have exhausted the current page -> delete it
        m_currPage = null;

        return null;
    }

    @Override
    public boolean hasNext() {
        return m_next != null;
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        final T toReturn = m_next;

        try {
            m_next = getNextPath();
        } catch (IOException ex) {
            throw new DirectoryIteratorException(ex);
        }

        return toReturn;
    }

    /**
     * To be implemented by subclasses in order to determine whether the next page of paths can be loaded.
     *
     * @return true, if {@link #loadNextPage()} can be called to retrieve the next page, false otherwise.
     */
    protected abstract boolean hasNextPage();

    /**
     * To be implemented by subclasses in order to fetch the next page of paths to return. Implementations can expect
     * that this method is only called after {@link #hasNextPage()} has returned true, hence it must never return null.
     *
     * @return the next page of paths to return (never null).
     * @throws IOException When something went wrong while fetching the next page of paths.
     */
    protected abstract Iterator<T> loadNextPage() throws IOException;

}
