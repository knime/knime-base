/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.filehandling.core.node.portobject.reader;

import java.io.InputStream;
import java.nio.file.Path;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.port.PortObject;
import org.knime.filehandling.core.connections.FSFiles;

/**
 * Abstract node model for port object reader nodes that read directly from a file.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 * @param <C> the config used by the node
 * @noreference non-public API
 * @noextend non-public API
 */
public abstract class PortObjectFromFileReaderNodeModel<C extends PortObjectReaderNodeConfig>
    extends PortObjectFromPathReaderNodeModel<C> {

    /**
     * Constructor.
     *
     * @param creationConfig the node creation configuration
     * @param config the reader configuration
     */
    protected PortObjectFromFileReaderNodeModel(final NodeCreationConfiguration creationConfig, final C config) {
        super(creationConfig, config);
    }

    @Override
    protected final PortObject[] readFromPath(final Path inputPath, final ExecutionContext exec) throws Exception {
        try (final InputStream inputStream = FSFiles.newInputStream(inputPath)) {
            return read(inputStream, exec);
        }
    }

    /**
     * Reads the object in.
     *
     * @param inputStream the input stream of the object
     * @param exec the execution context
     * @return the read in port object(s)
     * @throws Exception if any exception occurs
     */
    protected abstract PortObject[] read(final InputStream inputStream, final ExecutionContext exec) throws Exception;

}
