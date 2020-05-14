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
 *   Feb 12, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import java.util.Optional;

import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.filehandling.core.defaultnodesettings.SettingsModelFileChooser2;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.ReaderConfigUtils;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.rowkey.DefaultRowKeyGeneratorContextFactory;
import org.knime.filehandling.core.node.table.reader.rowkey.RowKeyGeneratorContextFactory;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy;
import org.knime.filehandling.core.node.table.reader.type.mapping.DefaultTypeMappingFactory;
import org.knime.filehandling.core.node.table.reader.type.mapping.TypeMappingFactory;
import org.knime.filehandling.core.node.table.reader.util.MultiTableReadFactory;
import org.knime.filehandling.core.port.FileSystemPortObject;

/**
 * An abstract implementation of a node factory for table reader nodes based on the table reader framework.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <C> the type of {@link ReaderSpecificConfig}
 * @param <T> the type used to identify external data types
 * @param <V> the type used as value by the reader
 */
public abstract class AbstractTableReaderNodeFactory<C extends ReaderSpecificConfig<C>, T, V>
    extends ConfigurableNodeFactory<TableReaderNodeModel<C>> {

    /**
     * Creates a {@link SettingsModelFileChooser2} configured for this reader node.
     *
     * @return a new file chooser model configured for this reader
     */
    protected abstract SettingsModelFileChooser2 createFileChooserModel();

    /**
     * Creates the {@link ReaderSpecificConfig} that holds all the configurations specific to this reader node.
     *
     * @return the ReaderSpecificConfig
     */
    protected abstract C createReaderSpecificConfig();

    /**
     * Returns the {@link ReadAdapterFactory} used by this reader node.
     *
     * @return the ReadAdapterFactory
     */
    protected abstract ReadAdapterFactory<T, V> getReadAdapterFactory();

    /**
     * Creates the {@link TableReader} for this reader node.
     *
     * @return a new table reader
     */
    protected abstract TableReader<C, T, V> createReader();

    /**
     * Extracts a string representation from <b>value</b> that is used as row key.
     *
     * @param value to extract the row key from
     * @return a string representation of <b>value</b> that can be used as row key
     */
    protected abstract String extractRowKey(V value);

    /**
     * Returns the {@link TypeHierarchy} of the external types.
     *
     * @return the type hierarchy of the external types
     */
    protected abstract TypeHierarchy<T, T> getTypeHierarchy();

    @Override
    public final TableReaderNodeModel<C> createNodeModel(final NodeCreationConfiguration creationConfig) {
        final MultiTableReadConfig<C> config = createConfig();
        final SettingsModelFileChooser2 fileChooserModel = createFileChooserModel();
        final ReadAdapterFactory<T, V> readAdapterFactory = getReadAdapterFactory();
        final TypeMappingFactory<T, V> typeMappingFactory = new DefaultTypeMappingFactory<>(readAdapterFactory);
        final RowKeyGeneratorContextFactory<V> rowKeyGenFactory =
            new DefaultRowKeyGeneratorContextFactory<>(this::extractRowKey);
        final MultiTableReadFactory<T, V> multiTableReadFactory =
            new DefaultMultiTableReadFactory<>(typeMappingFactory, getTypeHierarchy(), rowKeyGenFactory);
        final MultiTableReader<C, T, V> reader = new MultiTableReader<>(createReader(), multiTableReadFactory);
        return new TableReaderNodeModel<>(config, fileChooserModel, reader, getProducerRegistry(),
            creationConfig.getPortConfig().orElseThrow(IllegalStateException::new));
    }

    @Override
    protected final Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        PortsConfigurationBuilder builder = new PortsConfigurationBuilder();
        // Don't forget to update TableReaderNodeModel.FS_CONNECTION_PORT if the index changes
        builder.addOptionalInputPortGroup("File System Connection", FileSystemPortObject.TYPE);
        builder.addFixedOutputPortGroup("Data Table", BufferedDataTable.TYPE);
        return Optional.of(builder);
    }

    /**
     * Creates a {@link MultiTableReadConfig} for use in a reader node model.
     *
     * @return {@link MultiTableReadConfig} for a node model
     */
    protected MultiTableReadConfig<C> createConfig() {
        return ReaderConfigUtils.createMultiTableReadConfig(createReaderSpecificConfig(),
            getReadAdapterFactory().getProducerRegistry());
    }

    @Override
    protected final int getNrNodeViews() {
        return 0;
    }

    @Override
    public final NodeView<TableReaderNodeModel<C>> createNodeView(final int viewIndex,
        final TableReaderNodeModel<C> nodeModel) {
        return null;
    }

    /**
     * Returns the {@link ProducerRegistry} used by the {@link ReadAdapterFactory}.
     *
     * @return the {@link ProducerRegistry}
     */
    public ProducerRegistry<T, ? extends ReadAdapter<T, V>> getProducerRegistry() {
        return getReadAdapterFactory().getProducerRegistry();
    }
}
