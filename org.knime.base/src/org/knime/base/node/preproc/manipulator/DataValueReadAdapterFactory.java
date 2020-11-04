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
 *   Feb 5, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.manipulator;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.convert.java.DataCellToJavaConverter;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;
import org.knime.core.data.convert.java.DataCellToJavaConverterRegistry;
import org.knime.core.data.convert.map.CellValueProducer;
import org.knime.core.data.convert.map.CellValueProducerFactory;
import org.knime.core.data.convert.map.MappingException;
import org.knime.core.data.convert.map.MappingFramework;
import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.data.def.StringCell;
import org.knime.filehandling.core.node.table.reader.ReadAdapter;
import org.knime.filehandling.core.node.table.reader.ReadAdapter.ReadAdapterParams;
import org.knime.filehandling.core.node.table.reader.ReadAdapterFactory;

/**
 * Factory for DataValueReadAdapter objects.
 *
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("unchecked")
public enum DataValueReadAdapterFactory implements ReadAdapterFactory<DataType, DataValue> {
        /**
         * The singleton instance.
         */
        INSTANCE;

    private static final ProducerRegistry<DataType, DataValueReadAdapter> PRODUCER_REGISTRY;

    static {
        PRODUCER_REGISTRY = MappingFramework.forSourceType(DataValueReadAdapter.class);
        PRODUCER_REGISTRY.unregisterAllProducers();
        final Set<DataType> dataTypes = DataCellToJavaConverterRegistry.getInstance().getAllConvertibleDataTypes();
        Set<String> registered = new HashSet<>();
        for (final DataType sourceType : dataTypes) {
            Collection<DataCellToJavaConverterFactory<?, ?>> factoriesForSourceType =
                    DataCellToJavaConverterRegistry.getInstance().getFactoriesForSourceType(sourceType);
            for (DataCellToJavaConverterFactory<?, ?> factory : factoriesForSourceType) {
                final Class<?> destinationType = factory.getDestinationType();
                final String identifier = sourceType.getName() + "->" + destinationType.getName();
                if (registered.add(identifier)) {
                    @SuppressWarnings("rawtypes")
                    final CellValueProducerFactoryImplementation<?> converter =
                    new CellValueProducerFactoryImplementation(destinationType, identifier, sourceType,
                        factory.create());
                    PRODUCER_REGISTRY.register(converter);
                }
            }
        }
        //register the most common type
        PRODUCER_REGISTRY.register(new FallBackProducerFactory());
    }

    private static final class FallBackProducerFactory implements
        CellValueProducerFactory<DataValueReadAdapter, DataType, String, ReadAdapterParams<DataValueReadAdapter, TableManipulatorConfig>> {
        @Override
        public Class<?> getDestinationType() {
            return String.class;
        }

        @Override
        public DataType getSourceType() {
            return DataTypeTypeHierarchy.TOP;
        }

        @Override
        public String getIdentifier() {
            return getSourceType().getName() + "->" + getDestinationType().getName();
        }

        @Override
        public CellValueProducer<DataValueReadAdapter, String, ReadAdapterParams<DataValueReadAdapter, TableManipulatorConfig>>
            create() {
            return new CellValueProducer<DataValueReadAdapter, String, ReadAdapter.ReadAdapterParams<DataValueReadAdapter,TableManipulatorConfig>>() {

                @Override
                public String produceCellValue(final DataValueReadAdapter source,
                    final ReadAdapterParams<DataValueReadAdapter, TableManipulatorConfig> params)
                    throws MappingException {
                    final DataValue value = source.get(params);
                    return value == null ? null : value.toString();
                }
            };
        }
    }

    private static final class CellValueProducerFactoryImplementation<D> implements
        CellValueProducerFactory<DataValueReadAdapter, DataType, D, ReadAdapterParams<DataValueReadAdapter, TableManipulatorConfig>> {
        private final Class<?> m_destinationType;
        private final String m_identifier;
        private final DataType m_sourceType;
        private DataCellToJavaConverter<DataValue, D> m_converter;

        private CellValueProducerFactoryImplementation(final Class<?> destinationType, final String identifier,
            final DataType sourceType, final DataCellToJavaConverter<DataValue, D> converter) {
            m_destinationType = destinationType;
            m_identifier = identifier;
            m_sourceType = sourceType;
            m_converter = converter;
        }

        @Override
        public Class<?> getDestinationType() {
            return m_destinationType;
        }

        @Override
        public DataType getSourceType() {
            return m_sourceType;
        }

        @Override
        public String getIdentifier() {
            return m_identifier;
        }

        @Override
        public CellValueProducer<DataValueReadAdapter, D, ReadAdapterParams<DataValueReadAdapter, TableManipulatorConfig>> create() {
            return new CellValueProducer<DataValueReadAdapter, D, ReadAdapter.ReadAdapterParams<DataValueReadAdapter,TableManipulatorConfig>>() {

                @Override
                public D produceCellValue(final DataValueReadAdapter source,
                    final ReadAdapterParams<DataValueReadAdapter, TableManipulatorConfig> params)
                    throws MappingException {
                    final DataValue value = source.get(params);
                    try {
                        return value == null ? null : m_converter.convert(value);
                    } catch (Exception e) {
                        throw new MappingException(e);
                    }
                }
            };
        }
    }

    @Override
    public DataType getDefaultType(final DataType type) {
        if (DataTypeTypeHierarchy.TOP.equals(type)) {
            return StringCell.TYPE;
        }
        return type;
    }

    @Override
    public Map<DataType, DataType> getDefaultTypeMap() {
        throw new UnsupportedOperationException("Type map not supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReadAdapter<DataType, DataValue> createReadAdapter() {
        return new DataValueReadAdapter();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProducerRegistry<DataType, ? extends ReadAdapter<DataType, DataValue>> getProducerRegistry() {
        return PRODUCER_REGISTRY;
    }
}
