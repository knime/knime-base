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
 *   01.03.2021 (lars.schweikardt): created
 */
package org.knime.filehandling.utility.nodes.deletepaths.filechooser;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.knime.core.node.InvalidSettingsException;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.ReadPathAccessor;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.utility.nodes.deletepaths.DeleteFilesFolderIterator;

/**
 * A {@link DeleteFilesFolderIterator} based on the {@link SettingsModelReaderFileChooser}.
 *
 * @author Lars Schweikardt, KNIME GmbH, Berlin, Germany
 */
final class DeleteFileChooserIterator implements DeleteFilesFolderIterator {

    private final ReadPathAccessor m_accessor;

    private final Iterator<FSPath> m_iterator;

    private final long m_size;

    DeleteFileChooserIterator(final SettingsModelReaderFileChooser settingsModelFileChooser,
        final Consumer<StatusMessage> statusMessageConsumer) throws IOException, InvalidSettingsException {
        m_accessor = settingsModelFileChooser.createReadPathAccessor();
        try {
            final List<FSPath> paths = m_accessor.getFSPaths(statusMessageConsumer);
            m_size = paths.size();
            m_iterator = paths.iterator();
        } catch (IOException | InvalidSettingsException e) {
            m_accessor.close();
            throw e;
        }
    }

    @Override
    public boolean hasNext() {
        return m_iterator.hasNext();
    }

    @Override
    public FSPath next() {
        return m_iterator.next();
    }

    @Override
    public void close() throws IOException {
        m_accessor.close();
    }

    @Override
    public long size() {
        return m_size;
    }
}
