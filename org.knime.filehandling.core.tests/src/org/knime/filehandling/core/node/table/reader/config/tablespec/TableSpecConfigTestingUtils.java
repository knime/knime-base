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
 *   Dec 15, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.config.tablespec;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.knime.filehandling.core.node.table.reader.TRFTestingUtils.a;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import org.knime.core.data.DataType;
import org.knime.core.data.convert.datacell.OriginAwareJavaToDataCellConverterRegistry;
import org.knime.core.data.convert.map.MappingFramework;
import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.convert.map.SimpleCellValueProducerFactory;
import org.knime.core.data.convert.map.Source;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.filehandling.core.node.table.reader.ImmutableColumnTransformation;
import org.knime.filehandling.core.node.table.reader.ImmutableTableTransformation;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.filehandling.core.node.table.reader.selector.ColumnTransformation;
import org.knime.filehandling.core.node.table.reader.selector.ImmutableUnknownColumnsTransformation;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;

/**
 * Utility class for testing DefaultTableSpecConfig and related classes.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class TableSpecConfigTestingUtils {

    static final TypedReaderColumnSpec<String> COL1 = TypedReaderColumnSpec.createWithName("A", "X", true);

    static final TypedReaderColumnSpec<String> COL2 = TypedReaderColumnSpec.createWithName("B", "Y", true);

    static final TypedReaderColumnSpec<String> COL3 = TypedReaderColumnSpec.createWithName("C", "Z", true);

    static final TypedReaderTableSpec<String> SPEC1 = new TypedReaderTableSpec<>(asList(COL1, COL2));

    static final TypedReaderTableSpec<String> SPEC2 = new TypedReaderTableSpec<>(asList(COL2, COL3));

    static final TypedReaderTableSpec<String> UNION = new TypedReaderTableSpec<>(asList(COL1, COL2, COL3));

    static final TypedReaderTableSpec<String> INTERSECTION =
        new TypedReaderTableSpec<>(Collections.singletonList(COL2));

    static final RawSpec<String> RAW_SPEC = new RawSpec<>(UNION, INTERSECTION);

    static final String[] ORIGINAL_NAMES = {"A", "B", "C"};

    static final String ROOT_PATH = "root";

    static final String PATH1 = "first";

    static final String PATH2 = "second";

    static final String[] ITEMS = {PATH1, PATH2};

    static final List<TypedReaderTableSpec<String>> INDIVIDUAL_SPECS = asList(SPEC1, SPEC2);

    static class DummySource implements Source<String> {
    }

    static final ProducerRegistry<String, DummySource> REGISTRY = createTestingRegistry();

    private static ProducerRegistry<String, DummySource> createTestingRegistry() {
        final ProducerRegistry<String, DummySource> registry = MappingFramework.forSourceType(DummySource.class);
        registry.register(new SimpleCellValueProducerFactory<>("X", String.class, null));
        registry.register(new SimpleCellValueProducerFactory<>("X", Long.class, null));
        registry.register(new SimpleCellValueProducerFactory<>("Y", Integer.class, null));
        registry.register(new SimpleCellValueProducerFactory<>("Z", Double.class, null));
        return registry;
    }

    static ProductionPath[] getProductionPaths(final String[] externalTypes, final DataType[] knimeTypes) {
        return IntStream.range(0, externalTypes.length)
            .mapToObj(i -> getProductionPath(externalTypes[i], knimeTypes[i]))//
            .toArray(ProductionPath[]::new);
    }

    static ProductionPath getProductionPath(final String externalType, final DataType knimeType) {
        return REGISTRY.getFactoriesForSourceType(externalType).stream()//
            .flatMap(p -> OriginAwareJavaToDataCellConverterRegistry.INSTANCE
                .getConverterFactoriesBySourceType(p.getDestinationType()).stream()
                .filter(f -> f.getDestinationType().equals(knimeType))//
                .map(f -> new ProductionPath(p, f)))//
            .findFirst().orElseThrow();
    }

    static class TableSpecConfigBuilder {

        private String m_root = "root";

        private String[] m_items = ITEMS.clone();

        private List<TypedReaderTableSpec<String>> m_individualSpecs = new ArrayList<>(INDIVIDUAL_SPECS);

        private ColumnFilterMode m_columnFilterMode = ColumnFilterMode.UNION;

        private boolean m_keepUnknown = true;

        private boolean m_enforceTypes = false;

        private boolean m_skipEmptyColumns = false;

        private int m_unknownColPosition = 3;

        private List<TypedReaderColumnSpec<String>> m_cols = asList(COL1, COL2, COL3);

        private RawSpec<String> m_rawSpec = RAW_SPEC;

        private ConfigID m_configID;

        ProductionPath[] m_prodPaths =
            getProductionPaths(a("X", "Y", "Z"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));

        String[] m_names = a("A", "B", "C");

        Integer[] m_positions = a(0, 1, 2);

        Boolean[] m_keeps = a(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE);

        TableSpecConfigBuilder(final ConfigID configID) {
            m_configID = configID;
        }

        DefaultTableSpecConfig<String> build() {
            final List<ImmutableColumnTransformation<String>> colTrans =
                createColTrans(m_cols, asList(m_prodPaths), asList(m_names), asList(m_positions), asList(m_keeps));
            final ImmutableUnknownColumnsTransformation unknownColsTrans =
                new ImmutableUnknownColumnsTransformation(m_unknownColPosition, m_keepUnknown, false, null);
            final ImmutableTableTransformation<String> tableTrans = new ImmutableTableTransformation<>(colTrans,
                m_rawSpec, m_columnFilterMode, unknownColsTrans, m_enforceTypes, m_skipEmptyColumns);
            return new DefaultTableSpecConfig<>(m_root, m_configID, m_items, m_individualSpecs, tableTrans);
        }

        private static List<ImmutableColumnTransformation<String>> createColTrans(
            final List<TypedReaderColumnSpec<String>> specs, final List<ProductionPath> prodPaths,
            final List<String> names, final List<Integer> positions, final List<Boolean> keep) {
            return IntStream.range(0, specs.size())//
                .mapToObj(i -> new ImmutableColumnTransformation<String>(specs.get(i), prodPaths.get(i), keep.get(i),
                    positions.get(i), names.get(i)))//
                .collect(toList());
        }

        TableSpecConfigBuilder withRoot(final String rootItem) {
            m_root = rootItem;
            return this;
        }

        TableSpecConfigBuilder withItems(final String... items) {
            m_items = items.clone();
            return this;
        }

        TableSpecConfigBuilder withRawSpec(final RawSpec<String> rawSpec) {
            m_rawSpec = rawSpec;
            return this;
        }

        @SafeVarargs
        final TableSpecConfigBuilder withSpecs(final TypedReaderTableSpec<String>... specs) {
            m_individualSpecs = asList(specs);
            return this;
        }

        @SafeVarargs
        final TableSpecConfigBuilder withCols(final TypedReaderColumnSpec<String>... cols) {
            m_cols = asList(cols);
            return this;
        }

        TableSpecConfigBuilder withNames(final String... names) {
            m_names = names.clone();
            return this;
        }

        TableSpecConfigBuilder withProductionPaths(final ProductionPath[] productionPaths) {
            m_prodPaths = productionPaths.clone();
            return this;
        }

        TableSpecConfigBuilder withPositions(final Integer... positions) {
            m_positions = positions.clone();
            return this;
        }

        TableSpecConfigBuilder withKeep(final Boolean... keep) {
            m_keeps = keep.clone();
            return this;
        }

        TableSpecConfigBuilder withColumnFilterMode(final ColumnFilterMode columnFilterMode) {
            m_columnFilterMode = columnFilterMode;
            return this;
        }

        TableSpecConfigBuilder withKeepUnknown(final boolean keepUnknown) {
            m_keepUnknown = keepUnknown;
            return this;
        }

        TableSpecConfigBuilder withUnknownPosition(final int unknownPosition) {
            m_unknownColPosition = unknownPosition;
            return this;
        }

        TableSpecConfigBuilder withEnforceTypes(final boolean enforceTypes) {
            m_enforceTypes = enforceTypes;
            return this;
        }

        TableSpecConfigBuilder withSkipEmptyColumns(final boolean skipEmptyColumns) {
            m_skipEmptyColumns = skipEmptyColumns;
            return this;
        }

    }

    static final class TransformationStubber<T> {

        private final ColumnTransformation<T> m_mockTransformation;

        TransformationStubber(final ColumnTransformation<T> mockTransformation) {
            m_mockTransformation = mockTransformation;
        }

        TransformationStubber<T> withName(final String name) {
            when(m_mockTransformation.getName()).thenReturn(name);
            return this;
        }

        TransformationStubber<T> withProductionPath(final ProductionPath prodPath) {
            when(m_mockTransformation.getProductionPath()).thenReturn(prodPath);
            return this;
        }

        TransformationStubber<T> withPosition(final int position) {
            when(m_mockTransformation.getPosition()).thenReturn(position);
            return this;
        }

        TransformationStubber<T> withKeep(final boolean keep) {
            when(m_mockTransformation.keep()).thenReturn(keep);
            return this;
        }

        TransformationStubber<T> withExternalSpec(final TypedReaderColumnSpec<T> externalSpec) {
            when(m_mockTransformation.getExternalSpec()).thenReturn(externalSpec);
            return this;
        }
    }

    static <T> TransformationStubber<T> stub(final ColumnTransformation<T> mockTransformation) {
        return new TransformationStubber<>(mockTransformation);
    }

    private TableSpecConfigTestingUtils() {
        // static utitlity class
    }

}
