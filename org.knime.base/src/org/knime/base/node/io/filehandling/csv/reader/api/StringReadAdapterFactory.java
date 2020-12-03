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
package org.knime.base.node.io.filehandling.csv.reader.api;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.data.DataType;
import org.knime.core.data.blob.BinaryObjectDataCell;
import org.knime.core.data.convert.map.DoubleCellValueProducer;
import org.knime.core.data.convert.map.IntCellValueProducer;
import org.knime.core.data.convert.map.LongCellValueProducer;
import org.knime.core.data.convert.map.MappingException;
import org.knime.core.data.convert.map.MappingFramework;
import org.knime.core.data.convert.map.PrimitiveCellValueProducer;
import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.data.convert.map.SimpleCellValueProducerFactory;
import org.knime.core.data.convert.map.SupplierCellValueProducerFactory;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.filehandling.core.node.table.reader.ReadAdapter;
import org.knime.filehandling.core.node.table.reader.ReadAdapter.ReadAdapterParams;
import org.knime.filehandling.core.node.table.reader.ReadAdapterFactory;

/**
 * Factory for StringReadAdapter objects.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @since 4.2
 */
public enum StringReadAdapterFactory implements ReadAdapterFactory<Class<?>, String> {
        /**
         * The singleton instance.
         */
        INSTANCE;

    private static final ProducerRegistry<Class<?>, StringReadAdapter> PRODUCER_REGISTRY = initializeProducerRegistry();

    private static final Map<Class<?>, DataType> DEFAULT_TYPES = createDefaultTypeMap();

    private static Map<Class<?>, DataType> createDefaultTypeMap() {
        final Map<Class<?>, DataType> defaultTypes = new HashMap<>();
        defaultTypes.put(Boolean.class, BooleanCell.TYPE);
        defaultTypes.put(Byte.class, IntCell.TYPE);
        defaultTypes.put(Short.class, IntCell.TYPE);
        defaultTypes.put(Integer.class, IntCell.TYPE);
        defaultTypes.put(Long.class, LongCell.TYPE);
        defaultTypes.put(Float.class, DoubleCell.TYPE);
        defaultTypes.put(Double.class, DoubleCell.TYPE);
        defaultTypes.put(String.class, StringCell.TYPE);
        defaultTypes.put(InputStream.class, BinaryObjectDataCell.TYPE);
        return Collections.unmodifiableMap(defaultTypes);
    }

    private static ProducerRegistry<Class<?>, StringReadAdapter> initializeProducerRegistry() {
        final ProducerRegistry<Class<?>, StringReadAdapter> registry =
            MappingFramework.forSourceType(StringReadAdapter.class);
        registry.register(
            new SupplierCellValueProducerFactory<>(Integer.class, Integer.class, StringToIntCellValueProducer::new));
        registry.register(
            new SupplierCellValueProducerFactory<>(Double.class, Double.class, StringToDoubleCellValueProducer::new));
        registry.register(
            new SupplierCellValueProducerFactory<>(Long.class, Long.class, StringToLongCellValueProducer::new));
        registry.register(new SimpleCellValueProducerFactory<>(String.class, String.class,
            StringReadAdapterFactory::readStringFromSource));
        registry.register(new SimpleCellValueProducerFactory<>(LocalDate.class, LocalDate.class,
            StringReadAdapterFactory::readLocalDateFromSource));
        registry.register(new SimpleCellValueProducerFactory<>(LocalTime.class, LocalTime.class,
            StringReadAdapterFactory::readLocalTimeFromSource));
        registry.register(new SimpleCellValueProducerFactory<>(InputStream.class, InputStream.class,
            StringReadAdapterFactory::readByteFieldsFromSource));
        return registry;
    }

    private static String readStringFromSource(final StringReadAdapter source,
        final ReadAdapterParams<StringReadAdapter, CSVTableReaderConfig> params) {
        return source.get(params);
    }

    private static LocalDate readLocalDateFromSource(final StringReadAdapter source,
        final ReadAdapterParams<StringReadAdapter, CSVTableReaderConfig> params) {
        final String localDate = source.get(params);
        return LocalDate.parse(localDate);
    }

    private static LocalTime readLocalTimeFromSource(final StringReadAdapter source,
        final ReadAdapterParams<StringReadAdapter, CSVTableReaderConfig> params) {
        final String localTime = source.get(params);
        return LocalTime.parse(localTime);
    }

    private static InputStream readByteFieldsFromSource(final StringReadAdapter source,
        final ReadAdapterParams<StringReadAdapter, CSVTableReaderConfig> params) {
        final String bytes = source.get(params);
        return new ByteArrayInputStream(bytes.getBytes());
    }

    private abstract static class AbstractReadAdapterToPrimitiveCellValueProducer<S extends ReadAdapter<?, ?>, T>
        implements PrimitiveCellValueProducer<S, T, ReadAdapterParams<S, CSVTableReaderConfig>> {

        @Override
        public final boolean producesMissingCellValue(final S source,
            final ReadAdapterParams<S, CSVTableReaderConfig> params) throws MappingException {
            return source.get(params) == null;
        }
    }

    private static class StringToIntCellValueProducer
        extends AbstractReadAdapterToPrimitiveCellValueProducer<StringReadAdapter, Integer>
        implements IntCellValueProducer<StringReadAdapter, ReadAdapterParams<StringReadAdapter, CSVTableReaderConfig>> {

        private IntegerParser m_parser;

        @Override
        public int produceIntCellValue(final StringReadAdapter source,
            final ReadAdapterParams<StringReadAdapter, CSVTableReaderConfig> params) throws MappingException {
            init(params);
            return m_parser.parseInt(source.get(params));
        }

        private void init(final ReadAdapterParams<StringReadAdapter, CSVTableReaderConfig> params) {
            if (m_parser == null) {
                m_parser = new IntegerParser(params.getConfig());
            }
        }

    }

    private static class StringToDoubleCellValueProducer
        extends AbstractReadAdapterToPrimitiveCellValueProducer<StringReadAdapter, Double> implements
        DoubleCellValueProducer<StringReadAdapter, ReadAdapterParams<StringReadAdapter, CSVTableReaderConfig>> {

        private DoubleParser m_parser;

        @Override
        public double produceDoubleCellValue(final StringReadAdapter source,
            final ReadAdapterParams<StringReadAdapter, CSVTableReaderConfig> params) throws MappingException {
            init(params);
            return m_parser.parse(source.get(params));
        }

        private void init(final ReadAdapterParams<StringReadAdapter, CSVTableReaderConfig> params) {
            if (m_parser == null) {
                m_parser = new DoubleParser(params.getConfig());
            }
        }

    }

    private static class StringToLongCellValueProducer
        extends AbstractReadAdapterToPrimitiveCellValueProducer<StringReadAdapter, Long> implements
        LongCellValueProducer<StringReadAdapter, ReadAdapterParams<StringReadAdapter, CSVTableReaderConfig>> {

        private IntegerParser m_parser;

        @Override
        public long produceLongCellValue(final StringReadAdapter source,
            final ReadAdapterParams<StringReadAdapter, CSVTableReaderConfig> params) throws MappingException {
            init(params);
            return m_parser.parseLong(source.get(params));
        }

        private void init(final ReadAdapterParams<StringReadAdapter, CSVTableReaderConfig> params) {
            if (m_parser == null) {
                m_parser = new IntegerParser(params.getConfig());
            }
        }

    }

    @Override
    public ReadAdapter<Class<?>, String> createReadAdapter() {
        return new StringReadAdapter();
    }

    @Override
    public ProducerRegistry<Class<?>, StringReadAdapter> getProducerRegistry() {
        return PRODUCER_REGISTRY;
    }

    /**
     * Returns the {@link Map} providing the default {@link DataType DataTypes}.
     *
     * @return {@link Map} containing the default {@link DataType DataTypes}
     */
    @SuppressWarnings("static-method")
    public Map<Class<?>, DataType> getDefaultTypeMap() {
        return DEFAULT_TYPES;
    }

    /**
     * @since 4.3
     */
    @Override
    public DataType getDefaultType(final Class<?> type) {
        return DEFAULT_TYPES.get(type);
    }

}
