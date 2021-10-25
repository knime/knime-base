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
 */
package org.knime.filehandling.core.fs.knime.local.workflowaware;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

/**
 * Iterates over the contents of a folder (workflow group) in a {@link LocalWorkflowAwareFileSystem}.
 *
 * @author Sascha Wolke, KNIME GmbH
 */
final class LocalWorkflowAwarePathIterator implements Iterator<LocalWorkflowAwarePath> {

    private final LocalWorkflowAwarePath[] m_paths;

    private int m_currIdx = 0;

    /**
     * Creates an iterator over all the files and folder in the given paths location.
     *
     * @param folder Folder (workflow group) whose contents should be listed.
     * @param filter
     * @throws IOException on I/O errors
     */
    LocalWorkflowAwarePathIterator(final LocalWorkflowAwarePath folder,
        final Filter<? super Path> filter) throws IOException {

        try (@SuppressWarnings("resource")
        final Stream<Path> stream = Files.list(folder.getFileSystem().toLocalPathWithAccessibilityCheck(folder))) {
            m_paths =  stream//
                .map(p -> (LocalWorkflowAwarePath)folder.resolve(p.getFileName().toString())) //
                .filter(p -> {
                    try {
                        return filter.accept(p);
                    } catch (final IOException ex) { // wrap exception
                        throw new UncheckedIOException(ex);
                    }
                }).toArray(LocalWorkflowAwarePath[]::new);
        } catch (final UncheckedIOException ex) { // unwrap exception
            if (ex.getCause() != null) {
                throw ex.getCause();
            } else {
                throw ex;
            }
        }
    }

    @Override
    public boolean hasNext() {
        return m_currIdx < m_paths.length;
    }

    @Override
    public LocalWorkflowAwarePath next() {
        if (m_currIdx >= m_paths.length) {
            throw new NoSuchElementException();
        }

        final LocalWorkflowAwarePath next = m_paths[m_currIdx];
        m_currIdx++;
        return next;
    }
}
