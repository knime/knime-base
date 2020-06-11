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
 *   May 26, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.filechooser.reader;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.knime.core.node.InvalidSettingsException;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;

/**
 * Allows to access {@link FSPath} objects in reader nodes.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public interface ReadPathAccessor extends Closeable {

    /**
     * Retrieves the {@link FSPath paths} corresponding to the settings provided in the constructor.</br>
     * Reader nodes should make use of this method.
     *
     * @param statusMessageConsumer for communicating non-fatal errors and warnings
     * @return the list of paths corresponding to the settings
     * @throws IOException if an I/O problem occurs while listing the files
     * @throws InvalidSettingsException if the settings are invalid e.g. the root path is invalid
     */
    List<FSPath> getFSPaths(final Consumer<StatusMessage> statusMessageConsumer)
        throws IOException, InvalidSettingsException;

    /**
     * Retrieves the {@link Path paths} corresponding to the settings provided in the constructor.</br>
     * Reader nodes should make use of this method.
     *
     * @param statusMessageConsumer for communicating non-fatal errors and warnings
     * @return the list of paths corresponding to the settings
     * @throws IOException if an I/O problem occurs while listing the files
     * @throws InvalidSettingsException if the settings are invalid e.g. the root path is invalid
     */
    default List<Path> getPaths(final Consumer<StatusMessage> statusMessageConsumer)
        throws IOException, InvalidSettingsException {
        return getFSPaths(statusMessageConsumer).stream()//
            .map(Path.class::cast)//
            .collect(Collectors.toList());
    }

    /**
     * Returns the root {@link FSPath} from which {@link #getFSPaths(Consumer)} starts its search.
     *
     * @param statusMessageConsumer consumer for status messages
     * @return the root {@link FSPath}
     * @throws IOException if an I/O problem occurs while accessing the root path
     * @throws InvalidSettingsException if the settings are invalid
     */
    FSPath getRootPath(final Consumer<StatusMessage> statusMessageConsumer)
        throws IOException, InvalidSettingsException;

    /**
     * Returns the {@link FileFilterStatistic} of the last {@link #getFSPaths(Consumer)}/{@link #getPaths(Consumer)}
     * call.
     *
     * @return the {@link FileFilterStatistic} of the last {@link #getFSPaths(Consumer)}/{@link #getPaths(Consumer)}
     *         call
     * @throws IllegalStateException if {@link #getFSPaths(Consumer)} or {@link #getPaths(Consumer)} hasn't been called
     *             yet
     */
    FileFilterStatistic getFileFilterStatistic();

}
