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
 *   Mar 9, 2021 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.transfer;

import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.defaultnodesettings.EnumConfig;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;

/**
 * Abstract factory for the Transfer Files/Folder node.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 * @param <T> an instance of {@link AbstractTransferFilesNodeModel}
 */
public abstract class AbstractTransferFilesNodeFactory<T extends AbstractTransferFilesNodeModel<? extends AbstractTransferFilesNodeConfig>>
    extends ConfigurableNodeFactory<T> {

    /** The name of the optional source connection input port group. */
    protected static final String CONNECTION_SOURCE_PORT_GRP_NAME = "Source File System Connection";

    /** The name of the optional destination connection input port group. */
    protected static final String CONNECTION_DESTINATION_PORT_GRP_NAME = "Destination File System Connection";

    /**
     * Returns the {@link SettingsModelWriterFileChooser} to specify the destination.
     *
     * @param portsConfiguration the {@link PortsConfiguration}
     * @return the {@link SettingsModelWriterFileChooser} to specify the destination
     */
    protected static final SettingsModelWriterFileChooser
        getDestinationFileWriter(final PortsConfiguration portsConfiguration) {
        return new SettingsModelWriterFileChooser("destination_location", portsConfiguration,
            AbstractTransferFilesNodeFactory.CONNECTION_DESTINATION_PORT_GRP_NAME, EnumConfig.create(FilterMode.FOLDER),
            EnumConfig.create(FileOverwritePolicy.IGNORE),
            FSCategory.getStandardNonTrivialFSCategories());
    }

}
