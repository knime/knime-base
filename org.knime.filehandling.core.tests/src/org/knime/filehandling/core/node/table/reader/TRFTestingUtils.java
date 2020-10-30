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
 *   May 28, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.convert.map.CellValueProducerFactory;
import org.knime.core.data.convert.map.MappingFramework;
import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.convert.map.SimpleCellValueProducerFactory;
import org.knime.core.data.convert.map.Source;
import org.knime.core.data.def.StringCell;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableSpecConfig;
import org.knime.filehandling.core.node.table.reader.selector.Transformation;
import org.knime.filehandling.core.node.table.reader.spec.ReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.ReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;

/**
 * Contains utility methods for testing classes relying on a {@link DefaultTableSpecConfig}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 *
 */
public final class TRFTestingUtils implements Source<String> {

    private static ProducerRegistry<String, TRFTestingUtils> m_registry;

    private TRFTestingUtils() {
        // static utility class
    }

    public static synchronized ProducerRegistry<String, TRFTestingUtils> getProducerRegistry() {
        if (m_registry == null) {
            m_registry = MappingFramework.forSourceType(TRFTestingUtils.class);
            m_registry.register(new SimpleCellValueProducerFactory<>("foo", String.class, null));
        }
        return m_registry;
    }

    public static Path mockPath(final String path) {
        final Path p = mock(Path.class);
        when(p.toString()).thenReturn(path);
        return p;
    }

    public static ReaderTableSpec<ReaderColumnSpec> createSpec(final String... cols) {
        return ReaderTableSpec.createReaderTableSpec(Arrays.asList(cols));
    }

    public static DataTableSpec createDataTableSpec(final String... cols) {
        return new DataTableSpec(Arrays.stream(cols)//
            .map(s -> new DataColumnSpecCreator(s, StringCell.TYPE).createSpec())//
            .toArray(DataColumnSpec[]::new));
    }

    public static ProductionPath mockProductionPath() {
        final CellValueProducerFactory<?, ?, ?, ?> cellValueProducerFactory = mock(CellValueProducerFactory.class);
        final JavaToDataCellConverterFactory<?> dataCellConverterFactory = mock(JavaToDataCellConverterFactory.class);
        return new ProductionPath(cellValueProducerFactory, dataCellConverterFactory);
    }

    public static TypedReaderTableSpec<String> createTypedTableSpec(final Collection<String> names,
        final Collection<String> types) {
        return TypedReaderTableSpec.create(names, types, types.stream().map(t -> true).collect(toList()));
    }

    public static <T> T[] a(final T...values) {
        return values;
    }

    /**
     * Convenience method for checking a {@link Transformation}.
     *
     * @param <T> type used to identify external data types
     * @param transformation to check
     * @param col1 expected return value of {@link Transformation#getExternalSpec()}
     * @param name expected return value of {@link Transformation#getName()}
     * @param prodPath expected return value of {@link Transformation#getProductionPath()}
     * @param position expected return value of {@link Transformation#getPosition()}
     * @param keep expected return value of {@link Transformation#keep()}
     */
    public static <T> void checkTransformation(final Transformation<T> transformation, final TypedReaderColumnSpec<T> col1, final String name, final ProductionPath prodPath, final int position, final boolean keep) {
        assertEquals(col1, transformation.getExternalSpec());
        assertEquals(name, transformation.getName());
        assertEquals(prodPath, transformation.getProductionPath());
        assertEquals(position, transformation.getPosition());
        assertEquals(keep, transformation.keep());
    }
}
