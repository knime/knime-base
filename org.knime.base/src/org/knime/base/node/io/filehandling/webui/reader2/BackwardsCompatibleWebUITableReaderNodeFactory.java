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
 *   Jan 27, 2026 (Paul Bärnreuther): created
 */
package org.knime.base.node.io.filehandling.webui.reader2;

import java.util.function.Supplier;

import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.node.table.reader.BackwardsCompatibleCommonTableReaderNodeModel;
import org.knime.filehandling.core.node.table.reader.CommonTableReaderNodeModel;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.config.StorableMultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.paths.Source;
import org.knime.filehandling.core.node.table.reader.paths.SourceSettings;
import org.knime.filehandling.core.util.SettingsUtils;
import org.knime.node.parameters.NodeParameters;

/**
 * Use this factory instead of {@link WebUITableReaderNodeFactory} when migrating an existing table reader node to a new
 * node parameter based implementation.
 *
 * The node will detect old configurations (per default by checking for the existence of a key in the settings that only
 * the old version used: "settings") and use the legacy source settings to read the data in that case.
 *
 * @param <P> the type of node parameters
 * @param <S> the type of {@link Source}
 * @param <C> the type of {@link ReaderSpecificConfig}
 * @param <T> the type used to identify external data types
 * @param <V> the type used as value by the reader
 * @param <M> the type of {@link MultiTableReadConfig}
 *
 * @author Paul Bärnreuther
 * @since 5.11
 */
public abstract class BackwardsCompatibleWebUITableReaderNodeFactory<P extends NodeParameters, S extends Source<FSPath>, //
        C extends ReaderSpecificConfig<C>, T, V, //
        M extends StorableMultiTableReadConfig<C, T>>
    extends WebUITableReaderNodeFactory<P, S, C, T, V, M> {

    /**
     * @param nodeParametersClass the class of the node parameters
     */
    protected BackwardsCompatibleWebUITableReaderNodeFactory(final Class<P> nodeParametersClass) {
        super(nodeParametersClass);
    }

    @Override
    public CommonTableReaderNodeModel<FSPath, S, C, T, M>
        createNodeModel(final NodeCreationConfiguration creationConfig) {
        final Supplier<M> configCreator = () -> createConfig(creationConfig);
        final var pathSettings = createPathSettings(creationConfig);
        final var reader = createMultiTableReader();
        final var serializer = createSerializer();
        final var portConfig = creationConfig.getPortConfig();
        final var legacySourceSettings = createLegacySourceSettings(creationConfig);
        return portConfig.isPresent()
            ? new BackwardsCompatibleCommonTableReaderNodeModel<>(configCreator, pathSettings, reader, serializer,
                portConfig.get(), legacySourceSettings, this::isLegacyConfiguration)
            : new BackwardsCompatibleCommonTableReaderNodeModel<>(configCreator, pathSettings, reader, serializer,
                legacySourceSettings, this::isLegacyConfiguration);
    }

    /**
     * Initializes the legacy source settings (usually an instance of {@link SettingsModelReaderFileChooser}) that were
     * used by the node before its migration.
     *
     * @param creationConfig the node creation configuration
     * @return the empty legacy source settings
     */
    protected abstract SourceSettings<FSPath>
        createLegacySourceSettings(final NodeCreationConfiguration creationConfig);

    /**
     * A method that returns whether the given settings represent a legacy configuration of the node.
     *
     * @param settings the node settings to check
     * @return true if the settings represent a legacy configuration, false otherwise
     */
    protected boolean isLegacyConfiguration(final NodeSettingsRO settings) {
        return settings.containsKey(SettingsUtils.CFG_SETTINGS_TAB);
    }

}
