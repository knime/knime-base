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
 *   Nov 13, 2020 (Tobias): created
 */
package org.knime.filehandling.core.node.table.reader;

import java.util.Optional;

import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.config.StorableMultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.paths.PathSettings;
import org.knime.filehandling.core.node.table.reader.paths.SourceSettings;
import org.knime.filehandling.core.node.table.reader.preview.dialog.AbstractTableReaderNodeDialog;
import org.knime.filehandling.core.node.table.reader.rowkey.DefaultRowKeyGeneratorContextFactory;
import org.knime.filehandling.core.node.table.reader.rowkey.GenericRowKeyGeneratorContextFactory;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy;
import org.knime.filehandling.core.port.FileSystemPortObject;

/**
 * An abstract implementation of a node factory for table reader nodes based on the table reader framework.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 * @param <I> the item type to read from
 * @param <C> the type of {@link ReaderSpecificConfig}
 * @param <T> the type used to identify external data types
 * @param <V> the type used as value by the reader
 * @noreference non-public API
 * @noextend non-public API
 */
public abstract class GenericAbstractTableReaderNodeFactory<I, C extends ReaderSpecificConfig<C>, T, V>
    extends ConfigurableNodeFactory<TableReaderNodeModel<I, C, T>> {

    /** The file system ports group id. */
    protected static final String FS_CONNECT_GRP_ID = "File System Connection";

    /**
     * Creates a {@link PathSettings} object configured for this reader node.
     *
     * @param nodeCreationConfig the {@link NodeCreationConfiguration}
     *
     * @return a new path settings object configured for this reader
     */
    protected abstract SourceSettings<I> createPathSettings(final NodeCreationConfiguration nodeCreationConfig);

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
    protected abstract GenericTableReader<I, C, T, V> createReader();

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
    public TableReaderNodeModel<I, C, T> createNodeModel(final NodeCreationConfiguration creationConfig) {
        final StorableMultiTableReadConfig<C, T> config = createConfig(creationConfig);
        final SourceSettings<I> pathSettings = createPathSettings(creationConfig);
        final MultiTableReader<I, C, T> reader = createMultiTableReader();
        final Optional<? extends PortsConfiguration> portConfig = creationConfig.getPortConfig();
        if (portConfig.isPresent()) {
            return new TableReaderNodeModel<>(config, pathSettings, reader, portConfig.get());
        } else {
            return new TableReaderNodeModel<>(config, pathSettings, reader);
        }
    }

    /**
     * Creates a new @link MultiTableReader and returns it.
     *
     * @return a new multi table reader
     */
    protected final MultiTableReader<I, C, T> createMultiTableReader() {
        return new MultiTableReader<>(createMultiTableReadFactory(createReader()));
    }

    @Override
    protected NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        final MultiTableReadFactory<I, C, T> readFactory = createMultiTableReadFactory(createReader());
        final ProductionPathProvider<T> productionPathProvider = createProductionPathProvider();
        return createNodeDialogPane(creationConfig, readFactory, productionPathProvider);
    }

    /**
     * Creates and returns a new {@link DefaultProductionPathProvider} using the {@link ReadAdapterFactory} returned by
     * {@link #getReadAdapterFactory()}.
     *
     * @return the default production path provider
     */
    protected ProductionPathProvider<T> createProductionPathProvider() {
        final ReadAdapterFactory<T, V> readAdapterFactory = getReadAdapterFactory();
        return new DefaultProductionPathProvider<>(readAdapterFactory.getProducerRegistry(),
            readAdapterFactory::getDefaultType);
    }

    /**
     * Creates the node dialog.
     *
     * @param creationConfig {@link NodeCreationConfiguration}
     * @param readFactory the {@link MultiTableReadFactory} needed to create an {@link AbstractTableReaderNodeDialog}
     * @param defaultProductionPathFn provides the default {@link ProductionPath} for all external types
     * @return the node dialog
     */
    protected abstract AbstractTableReaderNodeDialog<I, C, T> createNodeDialogPane(
        final NodeCreationConfiguration creationConfig, final MultiTableReadFactory<I, C, T> readFactory,
        final ProductionPathProvider<T> defaultProductionPathFn);

    /**
     * Creates a new {@link MultiTableReadFactory} with the given {@link GenericTableReader} and returns it.
     *
     * @param reader the table reader used to create the {@link MultiTableReadFactory}
     *
     * @return a new multi table reader
     */
    protected MultiTableReadFactory<I, C, T>
        createMultiTableReadFactory(final GenericTableReader<I, C, T, V> reader) {
        final ReadAdapterFactory<T, V> readAdapterFactory = getReadAdapterFactory();
        final ProductionPathProvider<T> productionPathProvider = createProductionPathProvider();
        final GenericRowKeyGeneratorContextFactory<I, V> rowKeyGenFactory =
            new DefaultRowKeyGeneratorContextFactory<>(this::extractRowKey, "File");
        return new DefaultMultiTableReadFactory<>(getTypeHierarchy(), rowKeyGenFactory, reader, productionPathProvider,
            readAdapterFactory::createReadAdapter);
    }

    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        PortsConfigurationBuilder builder = new PortsConfigurationBuilder();
        // Don't forget to update TableReaderNodeModel.FS_CONNECTION_PORT if the index changes
        builder.addOptionalInputPortGroup(FS_CONNECT_GRP_ID, FileSystemPortObject.TYPE);
        builder.addFixedOutputPortGroup("Data Table", BufferedDataTable.TYPE);
        return Optional.of(builder);
    }

    /**
     * Creates a {@link MultiTableReadConfig} for use in a reader node model.</br>
     *
     * @param nodeCreationConfig the {@link NodeCreationConfiguration}
     * @return {@link MultiTableReadConfig} for a node model
     */
    protected abstract StorableMultiTableReadConfig<C, T>
        createConfig(final NodeCreationConfiguration nodeCreationConfig);

    @Override
    protected final int getNrNodeViews() {
        return 0;
    }

    @Override
    public final NodeView<TableReaderNodeModel<I, C, T>> createNodeView(final int viewIndex,
        final TableReaderNodeModel<I, C, T> nodeModel) {
        return null;
    }

    /**
     * Returns the {@link ProducerRegistry} used by the {@link ReadAdapterFactory}.
     *
     * @return the {@link ProducerRegistry}
     */
    protected ProducerRegistry<T, ? extends ReadAdapter<T, V>> getProducerRegistry() {
        return getReadAdapterFactory().getProducerRegistry();
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

}