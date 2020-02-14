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
package org.knime.base.node.io.filehandling.table.csv;

import org.knime.core.data.convert.map.CellValueProducer;
import org.knime.core.data.convert.map.CellValueProducerFactory;
import org.knime.core.data.convert.map.DoubleCellValueProducer;
import org.knime.core.data.convert.map.IntCellValueProducer;
import org.knime.core.data.convert.map.LongCellValueProducer;
import org.knime.core.data.convert.map.MappingException;
import org.knime.core.data.convert.map.MappingFramework;
import org.knime.core.data.convert.map.PrimitiveCellValueProducer;
import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.data.convert.map.SimpleCellValueProducerFactory;
import org.knime.filehandling.core.node.table.reader.ReadAdapter;
import org.knime.filehandling.core.node.table.reader.ReadAdapter.ReadAdapterParams;
import org.knime.filehandling.core.node.table.reader.read.ReadAdapterFactory;

/**
 * Factory for StringReadAdapter objects.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class StringReadAdapterFactory implements ReadAdapterFactory<Class<?>, String> {

    static final ProducerRegistry<Class<?>, StringReadAdapter> PRODUCER_REGISTRY = initializeProducerRegistry();

    private static ProducerRegistry<Class<?>, StringReadAdapter> initializeProducerRegistry() {
        final ProducerRegistry<Class<?>, StringReadAdapter> registry =
            MappingFramework.forSourceType(StringReadAdapter.class);
        registry.register(createProducerFactory(Integer.class, new StringToIntCellValueProducer()));
        registry.register(createProducerFactory(Double.class, new StringToDoubleCellValueProducer()));
        registry.register(createProducerFactory(Long.class, new StringToLongCellValueProducer()));
        registry.register(new SimpleCellValueProducerFactory<>(String.class, String.class,
            StringReadAdapterFactory::readStringFromSource));
        return registry;
    }

    private static <T> CellValueProducerFactory<StringReadAdapter, Class<?>, ?, ?>
        createProducerFactory(final Class<T> javaType, final CellValueProducer<StringReadAdapter, ?, ?> producer) {
        return new SimpleCellValueProducerFactory<>(javaType, javaType, producer);
    }

    private static String readStringFromSource(final StringReadAdapter source,
        final ReadAdapterParams<StringReadAdapter> params) {
        return source.get(params);
    }

    private abstract static class AbstractReadAdapterToPrimitiveCellValueProducer<S extends ReadAdapter<?, ?>, T>
        implements PrimitiveCellValueProducer<S, T, ReadAdapterParams<S>> {

        @Override
        public final boolean producesMissingCellValue(final S source, final ReadAdapterParams<S> params)
            throws MappingException {
            return source.get(params) == null;
        }
    }

    private static class StringToIntCellValueProducer
        extends AbstractReadAdapterToPrimitiveCellValueProducer<StringReadAdapter, Integer>
        implements IntCellValueProducer<StringReadAdapter, ReadAdapterParams<StringReadAdapter>> {

        @Override
        public int produceIntCellValue(final StringReadAdapter source,
            final ReadAdapterParams<StringReadAdapter> params) throws MappingException {
            return Integer.parseInt(source.get(params));
        }

    }

    private static class StringToDoubleCellValueProducer
        extends AbstractReadAdapterToPrimitiveCellValueProducer<StringReadAdapter, Double>
        implements DoubleCellValueProducer<StringReadAdapter, ReadAdapterParams<StringReadAdapter>> {

        @Override
        public double produceDoubleCellValue(final StringReadAdapter source,
            final ReadAdapterParams<StringReadAdapter> params) throws MappingException {
            return Double.parseDouble(source.get(params));
        }

    }

    private static class StringToLongCellValueProducer
        extends AbstractReadAdapterToPrimitiveCellValueProducer<StringReadAdapter, Long>
        implements LongCellValueProducer<StringReadAdapter, ReadAdapterParams<StringReadAdapter>> {

        @Override
        public long produceLongCellValue(final StringReadAdapter source,
            final ReadAdapterParams<StringReadAdapter> params) throws MappingException {
            return Long.parseLong(source.get(params));
        }

    }

    @Override
    public StringReadAdapter createReadAdapter() {
        return new StringReadAdapter();
    }

    @Override
    public ProducerRegistry<Class<?>, StringReadAdapter> getProducerRegistry() {
        return PRODUCER_REGISTRY;
    }

}
