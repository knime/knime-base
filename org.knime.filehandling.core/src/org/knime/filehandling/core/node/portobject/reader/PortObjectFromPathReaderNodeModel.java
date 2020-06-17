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

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.context.url.URLConfiguration;
import org.knime.core.node.port.PortObject;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.ReadPathAccessor;
import org.knime.filehandling.core.node.portobject.PortObjectIONodeModel;

/**
 * Abstract node model for port object reader nodes that read from a {@link Path}.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @param <C> the config used by the node
 */
public abstract class PortObjectFromPathReaderNodeModel<C extends PortObjectReaderNodeConfig>
    extends PortObjectIONodeModel<C> {

    /**
     * Constructor.
     *
     * @param creationConfig the node creation configuration
     * @param config the reader configuration
     */
    protected PortObjectFromPathReaderNodeModel(final NodeCreationConfiguration creationConfig, final C config) {
        super(creationConfig.getPortConfig().orElseThrow(IllegalStateException::new), config);
        // Check if a URL is already configured and set it if so.
        // This is, e.g., the case when a file has been dropped into AP and the node has automatically been created.
        final Optional<? extends URLConfiguration> urlConfig = creationConfig.getURLConfig();
        if (urlConfig.isPresent()) {
            // we get an URL so the file system needs to be custom url
            getConfig().getFileChooserModel().setLocation(new FSLocation(
                FSCategory.CUSTOM_URL, "1000", urlConfig.get().getUrl().toString()));
        }
    }

    @Override
    protected final PortObject[] execute(final PortObject[] data, final ExecutionContext exec) throws Exception {
        try (final ReadPathAccessor accessor = getConfig().getFileChooserModel().createReadPathAccessor()) {
            final List<FSPath> paths = accessor.getFSPaths(getNodeModelStatusConsumer());
            assert paths.size() == 1;
            getNodeModelStatusConsumer().setWarningsIfRequired(this::setWarningMessage);
            return readFromPath(paths.get(0), exec);
        } catch (NoSuchFileException e) {
            throw new IOException(String.format("The file '%s' does not exist.", e.getFile()));
        }
    }

    /**
     * Reads the object from a path.
     *
     * @param inputPath the input path of the object
     * @param exec the execution context
     * @return the read in port object(s)
     * @throws Exception if any exception occurs
     */
    protected abstract PortObject[] readFromPath(final Path inputPath, final ExecutionContext exec) throws Exception;

}
