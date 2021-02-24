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
 *   Jun 10, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.filechooser.reader;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

import org.knime.core.node.NodeModel;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.EnumConfig;
import org.knime.filehandling.core.defaultnodesettings.filechooser.AbstractSettingsModelFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.defaultnodesettings.status.NodeModelStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.core.node.table.reader.paths.PathSettings;

/**
 * File chooser settings model for reader nodes.
 *
 * <b>Intended usage:</b> If you only need the path string, it is sufficient to call {@link #getLocation()} and then
 * {@link FSLocation#getPath()}. This call does not cause any I/O as opposed to calls on the {@link ReadPathAccessor}
 * returned by {@link #createReadPathAccessor()}.</br>
 * However, if you need access to the actual {@link FSPath} objects, you will have to use the
 * {@link #createReadPathAccessor()}.</br>
 * When used in the {@link NodeModel}, it is paramount to call the {@link #configureInModel(PortObjectSpec[], Consumer)}
 * method in the {@code configure} method of the {@link NodeModel}. This serves two purposes: It updates the model with
 * the incoming file system and validates that the file system is indeed the correct one. The
 * {@link #configureInModel(PortObjectSpec[], Consumer)} accepts a {@link Consumer} of {@link StatusMessage} in order to
 * report warning messages. In most cases you can make use of {@link NodeModelStatusConsumer}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class SettingsModelReaderFileChooser
    extends AbstractSettingsModelFileChooser<SettingsModelReaderFileChooser> implements PathSettings {

    /**
     * Constructor.
     *
     * @param configName under which to store the settings
     * @param portsConfig {@link PortsConfiguration} of the corresponding KNIME node
     * @param fileSystemPortIdentifier identifier of the file system port group in <b>portsConfig</b>
     * @param filterModeConfig the {@link EnumConfig} specifying the default and supported {@link FilterMode
     *            FilterModes}
     * @param convenienceFS the {@link Set} of {@link FSCategory convenience file systems} that should be available if
     *            no file system port is present
     * @param fileExtensions the supported file extensions
     */
    public SettingsModelReaderFileChooser(final String configName, final PortsConfiguration portsConfig,
        final String fileSystemPortIdentifier, final EnumConfig<FilterMode> filterModeConfig,
        final Set<FSCategory> convenienceFS, final String... fileExtensions) {
        super(configName, portsConfig, fileSystemPortIdentifier, filterModeConfig, convenienceFS, fileExtensions);
    }

    /**
     * Constructor.
     *
     * @param configName under which to store the settings
     * @param portsConfig {@link PortsConfiguration} of the corresponding KNIME node
     * @param fileSystemPortIdentifier identifier of the file system port group in <b>portsConfig</b>
     * @param filterModeConfig the {@link EnumConfig} specifying the default and supported {@link FilterMode
     *            FilterModes}
     * @param fileExtensions the supported file extensions
     */
    public SettingsModelReaderFileChooser(final String configName, final PortsConfiguration portsConfig,
        final String fileSystemPortIdentifier, final EnumConfig<FilterMode> filterModeConfig,
        final String... fileExtensions) {
        super(configName, portsConfig, fileSystemPortIdentifier, filterModeConfig, EnumSet.allOf(FSCategory.class),
            fileExtensions);
    }

    private SettingsModelReaderFileChooser(final SettingsModelReaderFileChooser toCopy) {
        super(toCopy);
    }

    /**
     * {@inheritDoc} </br>
     * If you only need the path string, use {@link #getLocation()} instead.
     *
     * @see #getLocation()
     */
    @Override
    public ReadPathAccessor createReadPathAccessor() {
        return super.createPathAccessor();
    }

    @Override
    public SettingsModelReaderFileChooser createClone() {
        return new SettingsModelReaderFileChooser(this);
    }

    @Override
    public String getPath() {
        return getLocation().getPath();
    }

    @Override
    protected String getModelTypeID() {
        return "SMID_ReaderFileChooser";
    }

}
