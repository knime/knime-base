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

import java.nio.file.Path;
import java.util.Optional;

import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.config.StorableMultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.paths.PathSettings;
import org.knime.filehandling.core.node.table.reader.preview.dialog.AbstractTableReaderNodeDialog;
import org.knime.filehandling.core.node.table.reader.rowkey.DefaultRowKeyGeneratorContextFactory;
import org.knime.filehandling.core.node.table.reader.rowkey.GenericRowKeyGeneratorContextFactory;

/**
 * An abstract implementation of a node factory for table reader nodes based on the table reader framework.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <C> the type of {@link ReaderSpecificConfig}
 * @param <T> the type used to identify external data types
 * @param <V> the type used as value by the reader
 * @noreference non-public API
 * @noextend non-public API
 */
public abstract class AbstractTableReaderNodeFactory<C extends ReaderSpecificConfig<C>, T, V>
    extends GenericAbstractTableReaderNodeFactory<Path, C, T, V> {


    @Override
    public final TableReaderNodeModel<C> createNodeModel(final NodeCreationConfiguration creationConfig) {
        final StorableMultiTableReadConfig<C> config = createConfig();
        final PathSettings pathSettings = createPathSettings(creationConfig);
        final MultiTableReader<C> reader = createMultiTableReader();
        final Optional<? extends PortsConfiguration> portConfig = creationConfig.getPortConfig();
        if (portConfig.isPresent()) {
            return new TableReaderNodeModel<>(config, pathSettings, reader, portConfig.get());
        } else {
            return new TableReaderNodeModel<>(config, pathSettings, reader);
        }
    }

    /**
     * Creates the {@link TableReader} for this reader node.
     *
     * @return a new table reader
     */
    @Override
    protected abstract TableReader<C, T, V> createReader();

    /**
     * Creates a new @link MultiTableReader and returns it.
     *
     * @return a new multi table reader
     */
    private MultiTableReader<C> createMultiTableReader() {
        return new MultiTableReader<>(createMultiTableReadFactory(createReader()));
    }

    /**
     * Creates a new @link MultiTableReader with the given {@link TableReader} and returns it.
     *
     * @param reader the table reader used to create the multi table reader
     *
     * @return a new multi table reader
     */
    @Override
    protected final MultiTableReadFactory<C, T> createMultiTableReadFactory(final GenericTableReader<Path, C, T, V> reader) {
        final ReadAdapterFactory<T, V> readAdapterFactory = getReadAdapterFactory();
        DefaultProductionPathProvider<T> productionPathProvider = createProductionPathProvider();
        final GenericRowKeyGeneratorContextFactory<Path, V> rowKeyGenFactory =
            new DefaultRowKeyGeneratorContextFactory<>(this::extractRowKey, "File");
        return new DefaultMultiTableReadFactory<>(getTypeHierarchy(), rowKeyGenFactory, (TableReader<C, T, V>)reader,
                productionPathProvider, readAdapterFactory::createReadAdapter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected AbstractTableReaderNodeDialog<C, T> createNodeDialogPane(final NodeCreationConfiguration creationConfig,
        final GenericMultiTableReadFactory<Path, C, T> readFactory,
        final ProductionPathProvider<T> defaultProductionPathFn) {
        return createNodeDialogPane(creationConfig, (MultiTableReadFactory<C, T>) readFactory, defaultProductionPathFn);
    }

    /**
     * Creates the node dialog.
     *
     * @param creationConfig {@link NodeCreationConfiguration}
     * @param readFactory the {@link MultiTableReadFactory} needed to create an {@link AbstractTableReaderNodeDialog}
     * @param defaultProductionPathFn provides the default {@link ProductionPath} for all external types
     * @return the node dialog
     */
    protected abstract AbstractTableReaderNodeDialog<C, T> createNodeDialogPane(
        final NodeCreationConfiguration creationConfig, final MultiTableReadFactory<C, T> readFactory,
        final ProductionPathProvider<T> defaultProductionPathFn);
}
