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
 *   Mar 27, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.node.ExecutionMonitor;
import org.knime.filehandling.core.node.table.reader.config.GenericMultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.rowkey.GenericRowKeyGeneratorContextFactory;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformation;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy;
import org.knime.filehandling.core.node.table.reader.util.StagedMultiTableRead;

/**
 * Default implementation of {@code MultiTableReadFactory}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> the type representing external data types
 * @param <V> the type representing values
 */
final class DefaultMultiTableReadFactory<C extends ReaderSpecificConfig<C>, T, V>
    extends GenericDefaultMultiTableReadFactory<Path, C, T, V> implements MultiTableReadFactory<C, T> {

    /**
     * Constructor.
     *
     * @param typeHierarchy the {@link TypeHierarchy}
     * @param rowKeyGeneratorFactory the {@link GenericRowKeyGeneratorContextFactory}
     * @param reader for the particular reader node
     * @param productionPathProvider provides {@link ProductionPath ProductionPaths} for external data types
     * @param readAdpaterSupplier creates new {@link ReadAdapter} instances
     */
    DefaultMultiTableReadFactory(final TypeHierarchy<T, T> typeHierarchy,
        final GenericRowKeyGeneratorContextFactory<Path, V> rowKeyGeneratorFactory, final TableReader<C, T, V> reader,
        final ProductionPathProvider<T> productionPathProvider, final Supplier<ReadAdapter<T, V>> readAdpaterSupplier) {
        super(typeHierarchy, rowKeyGeneratorFactory, reader, productionPathProvider, readAdpaterSupplier);
    }

    @Override
    public StagedMultiTableRead<T> create(final String rootPath, final List<Path> paths,
        final GenericMultiTableReadConfig<Path, C> config, final ExecutionMonitor exec) throws IOException {
        return (StagedMultiTableRead<T>)super.create(rootPath, paths, config, exec);
    }

    @Override
    public StagedMultiTableRead<T> createFromConfig(final String rootPath, final List<Path> paths,
        final GenericMultiTableReadConfig<Path, C> config) {
        return (StagedMultiTableRead<T>)super.createFromConfig(rootPath, paths, config);
    }

    @Override
    protected TableReader<C, T, V> getReader() {
        return (TableReader<C, T, V>)super.getReader();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DefaultStagedMultiTableRead<C, T, V> createStagedMultiTableReader(final String rootPath,
        final Map<Path, TypedReaderTableSpec<T>> individualSpecs, final GenericMultiTableReadConfig<Path, C> config,
        final TableTransformation<T> defaultTransformation) {
        return new DefaultStagedMultiTableRead<>(getReader(), rootPath, individualSpecs, getRowKeyGeneratorFactory(),
                getReadAdapterSupplier(), defaultTransformation, config.getTableReadConfig());
    }
}
