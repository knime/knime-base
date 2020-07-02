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
package org.knime.filehandling.core.node.table.reader.type.mapping;

import java.util.function.Supplier;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.node.table.reader.ReadAdapter;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.spec.ReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.ReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.util.MultiTableUtils;

/**
 * Default implementation of {@link TypeMapping} that is based on KNIME's type mapping framework.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class DefaultTypeMapping<C extends ReaderSpecificConfig<C>, V> implements TypeMapping<V> {

    private final Supplier<ReadAdapter<?, V>> m_readAdapterSupplier;

    private final ProductionPath[] m_productionPaths;

    private final C m_readerSpecificConfig;

    DefaultTypeMapping(final Supplier<ReadAdapter<?, V>> readAdapterSupplier, final ProductionPath[] productionPaths, final C config) {
        m_productionPaths = productionPaths;
        m_readAdapterSupplier = readAdapterSupplier;
        m_readerSpecificConfig = config;
    }

    @Override
    public TypeMapper<V> createTypeMapper(final FileStoreFactory fsFactory) {
        return new DefaultTypeMapper<>(m_readAdapterSupplier.get(), m_productionPaths, fsFactory, m_readerSpecificConfig);
    }

    @Override
    public DataTableSpec map(final ReaderTableSpec<?> spec) {
        CheckUtils.checkArgument(spec.size() == m_productionPaths.length,
            "The provided spec %s has not the expected number of columns (%s).", spec, m_productionPaths.length);
        final DataColumnSpec[] columns = new DataColumnSpec[m_productionPaths.length];
        for (int i = 0; i < m_productionPaths.length; i++) {
            final ProductionPath productionPath = m_productionPaths[i];
            final ReaderColumnSpec column = spec.getColumnSpec(i);
            columns[i] = new DataColumnSpecCreator(MultiTableUtils.getNameAfterInit(column),
                productionPath.getConverterFactory().getDestinationType()).createSpec();
        }
        return new DataTableSpec(columns);
    }

    @Override
    public ProductionPath[] getProductionPaths() {
        return m_productionPaths;
    }
}
