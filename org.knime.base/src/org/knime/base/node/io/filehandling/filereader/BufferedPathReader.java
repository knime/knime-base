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
 *   Jun 2, 2021 (Mark Ortmann): created
 */
package org.knime.base.node.io.filehandling.filereader;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Consumer;

import org.knime.base.node.util.BufferedFileReader;
import org.knime.core.node.InvalidSettingsException;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.ReadPathAccessor;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;

/**
 * Wrapper to a {@link BufferedPathReader} that operates on a {@link SettingsModelReaderFileChooser}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class BufferedPathReader implements Closeable {

    private final SettingsModelReaderFileChooser m_fileChooser;

    private ReadPathAccessor m_pathAccessor = null;

    private FSPath m_path;

    private long m_fileSize;

    BufferedPathReader(final SettingsModelReaderFileChooser fileChooser) {
        m_fileChooser = fileChooser;

    }

    BufferedFileReader createBufferedFileReader(final String charsetName,
        final Consumer<StatusMessage> statusMessageConsumer) throws IOException, InvalidSettingsException {
        openAccessor(statusMessageConsumer);
        return BufferedFileReader.createReader(m_path.getFileName().toString(), () -> Files.newInputStream(m_path),
            charsetName, m_fileSize);
    }

    private synchronized void openAccessor(final Consumer<StatusMessage> statusMessageConsumer)
        throws IOException, InvalidSettingsException {
        if (m_pathAccessor == null) {
            m_pathAccessor = m_fileChooser.createReadPathAccessor();
            m_path = m_pathAccessor.getFSPaths(statusMessageConsumer).get(0);
            m_fileSize = Files.size(m_path);
        }
    }

    @Override
    public synchronized void close() throws IOException {
        if (m_pathAccessor != null) {
            m_pathAccessor.close();
            m_pathAccessor = null;
        }
    }

}
