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
 *   Oct 23, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.knime.filehandling.core.node.table.reader.TRFTestingUtils.checkTransformation;
import static org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec.createWithName;
import static org.knime.filehandling.core.node.table.reader.util.MultiTableUtils.getNameAfterInit;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.data.DataType;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfig;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.filehandling.core.node.table.reader.selector.ColumnTransformation;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformation;
import org.knime.filehandling.core.node.table.reader.selector.UnknownColumnsTransformation;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Contains unit tests for the {@link DefaultTableTransformationFactory}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("javadoc") // keeping the link is more useful
@RunWith(MockitoJUnitRunner.class)
public class DefaultTableTransformationFactoryTest {

    private static final String SIEGFRIEDA = "siegfrieda";

    private static final String ELSA = "elsa";

    private static final String FRIEDA = "frieda";

    private static final String BERTA = "berta";

    private static TypedReaderColumnSpec<String> createColSpec(final String name, final String type) {
        return createWithName(name, type, true);
    }

    @SafeVarargs
    private static TypedReaderTableSpec<String> createTableSpec(final TypedReaderColumnSpec<String>... cols) {
        return new TypedReaderTableSpec<>(Arrays.asList(cols));
    }

    private static final TypedReaderColumnSpec<String> HANS = createColSpec("hans", ELSA);

    private static final TypedReaderColumnSpec<String> RUDIGER = createColSpec("rudiger", FRIEDA);

    private static final TypedReaderColumnSpec<String> ULF = createColSpec("ulf", BERTA);

    private static final TypedReaderTableSpec<String> UNION = createTableSpec(HANS, RUDIGER, ULF);

    private static final TypedReaderTableSpec<String> INTERSECTION = createTableSpec(RUDIGER);

    private static final RawSpec<String> RAW_SPEC = new RawSpec<>(UNION, INTERSECTION);

    @Mock
    private ProductionPathProvider<String> m_prodPathProvider;

    @Mock
    private MultiTableReadConfig<?, String> m_config;

    @Mock
    private TableSpecConfig<String> m_tableSpecConfig;

    @SuppressWarnings("rawtypes") // unfortunately that's necessary to satisfy the compiler
    @Mock
    private TableTransformation m_configuredTransformationModel;//NOSONAR

    private DefaultTableTransformationFactory<String> m_testInstance;

    @Before
    public void init() {
        m_testInstance = new DefaultTableTransformationFactory<>(m_prodPathProvider);
    }

    @SuppressWarnings("unchecked") // unfortunately that's necessary to satisfy the compiler
    private void setupConfig(final boolean hasTableSpecConfig) {
        when(m_config.hasTableSpecConfig()).thenReturn(hasTableSpecConfig);
        if (hasTableSpecConfig) {
            when(m_config.getTableSpecConfig()).thenReturn(m_tableSpecConfig);
            when(m_tableSpecConfig.getTableTransformation()).thenReturn(m_configuredTransformationModel);
        }
    }

    @Test
    public void testDontEnforceTypes() {
        TypedReaderColumnSpec<String> a = createColSpec("A", "X");
        TypedReaderColumnSpec<String> aWithDifferentType = createColSpec("A", "Y");
        final TableTransformation<String> configuredTransformation = new TableTransformationMocker()//
            .addTransformation(a, true)//
            .withEnforceTypes(false)//
            .mock();
        ProductionPath alternativeProdPath = mockProductionPath(IntCell.TYPE);
        when(m_prodPathProvider.getDefaultProductionPath("Y")).thenReturn(alternativeProdPath);

        final RawSpec<String> newRawSpec = new RawSpecBuilder<String>().addColumn(aWithDifferentType, true).build();
        final TableTransformation<String> result =
            m_testInstance.createFromExisting(newRawSpec, m_config, configuredTransformation);
        assertEquals(alternativeProdPath, result.getTransformation(aWithDifferentType).getProductionPath());
    }

    @Test
    public void testEnforceTypesKeepsTypeForNewlyEmptyColumns() {
        TypedReaderColumnSpec<String> a = createColSpec("A", "X");
        TableTransformationMocker mocker = new TableTransformationMocker()//
            .addTransformation(a, true)//
            .withEnforceTypes(true);
        final TableTransformation<String> configuredTransformation = mocker//
            .mock();
        final TypedReaderColumnSpec<String> emptyA = TypedReaderColumnSpec.createWithName("A", "Y", false);
        final RawSpec<String> newRawSpec = new RawSpecBuilder<String>().addColumn(emptyA, true).build();

        final TableTransformation<String> result =
            m_testInstance.createFromExisting(newRawSpec, m_config, configuredTransformation);

        assertEquals(mocker.getTransformation(0).getProductionPath(),
            result.getTransformation(emptyA).getProductionPath());
    }

    @Test
    public void testEnforceTypes() {
        testEnforceTypes(true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEnforceTypesFailsBecauseNoAlternativePathIsAvailable() {
        testEnforceTypes(false);
    }

    private void testEnforceTypes(final boolean alternativePathAvailable) {
        final TypedReaderColumnSpec<String> a = createColSpec("A", "X");
        final TableTransformation<String> configuredTransformation = new TableTransformationMocker()//
            .withEnforceTypes(true)//
            .addTransformation(a, true)//
            .mock();
        final TypedReaderColumnSpec<String> aWithDifferentType = createColSpec("A", "Y");
        final RawSpec<String> newRawSpec = new RawSpecBuilder<String>().addColumn(aWithDifferentType, true).build();

        final ProductionPath intPath = mockProductionPath(IntCell.TYPE);
        List<ProductionPath> availablePaths = new ArrayList<>(singletonList(intPath));
        ProductionPath alternateStringPath = null;
        if (alternativePathAvailable) {
            alternateStringPath = mockProductionPath(StringCell.TYPE);
            availablePaths.add(alternateStringPath);
        }
        when(m_prodPathProvider.getAvailableProductionPaths("Y")).thenReturn(availablePaths);

        TableTransformation<String> result =
            m_testInstance.createFromExisting(newRawSpec, m_config, configuredTransformation);

        assertEquals(alternateStringPath, result.getTransformation(aWithDifferentType).getProductionPath());
    }

    @Test
    public void testColumnMovesOutOfIntersection() {
        final TypedReaderColumnSpec<String> a = createColSpec("A", "X");
        final TypedReaderColumnSpec<String> b = createColSpec("B", "Y");
        final TypedReaderColumnSpec<String> c = createColSpec("C", "Z");
        final TypedReaderColumnSpec<String> d = createColSpec("D", "X");

        final TableTransformation<String> configuredTransformation = new TableTransformationMocker()//
            .withColumnFilterMode(ColumnFilterMode.INTERSECTION)//
            .withKeepUnknowns(true)//
            .withUnknownPos(1)//
            .addTransformation(a, false)//
            .addTransformation(b, true)//
            .addTransformation(c, true)//
            .addTransformation(d, false)//
            .mock();

        final RawSpec<String> newRawSpec = new RawSpecBuilder<String>()//
            .addColumn(a, false)//
            .addColumn(b, true)//
            .addColumn(c, false)// c moves out of the intersection
            .addColumn(d, false)//
            .build();

        TableTransformation<String> result =
            m_testInstance.createFromExisting(newRawSpec, m_config, configuredTransformation);

        assertEquals(0, result.getTransformation(a).getPosition());
        // c and d are not part of the output i.e. their position doesn't really matter
        // we thus insert them at the unknown column position
        assertEquals(1, result.getTransformation(c).getPosition());
        assertEquals(2, result.getTransformation(d).getPosition());
        assertEquals(3, result.getTransformation(b).getPosition());
    }

    @Test
    public void testSkipNewlyEmptyColumn() {
        TypedReaderColumnSpec<String> a = createColSpec("A", "X");
        TypedReaderColumnSpec<String> b = createColSpec("B", "Y");
        TableTransformation<String> configuredTransformation = new TableTransformationMocker()//
            .withSkipEmptyColumns(true)//
            .withUnknownPos(2)//
            .addTransformation(a, true)//
            .addTransformation(b, true)//
            .mock();
        TypedReaderColumnSpec<String> emptyA = TypedReaderColumnSpec.createWithName("A", "X", false);
        when(m_config.skipEmptyColumns()).thenReturn(true);
        RawSpec<String> newRawSpec = new RawSpecBuilder<String>().addColumn(emptyA, true).addColumn(b, true).build();
        TableTransformation<String> result =
            m_testInstance.createFromExisting(newRawSpec, m_config, configuredTransformation);
        assertEquals(0, result.getTransformation(b).getPosition());
        assertEquals(1, result.getTransformation(emptyA).getPosition());
    }

    @Test
    public void testSkipEmptyIsSwitchedOffByFlowVariable() {
        final TypedReaderColumnSpec<String> a = createColSpec("a", "X");
        final TypedReaderColumnSpec<String> b = TypedReaderColumnSpec.createWithName("b", "X", false);
        final TypedReaderColumnSpec<String> c = createColSpec("c", "X");

        final TableTransformation<String> configuredTransformation = new TableTransformationMocker()//
            .withSkipEmptyColumns(true)//
            .withKeepUnknowns(true)//
            .withUnknownPos(3)//
            .addTransformation(a, true)//
            .addTransformation(b, true)//
            .addTransformation(c, true)//
            .mock();

        // simulate skipEmptyColumns being switched off via flow variable
        when(m_config.skipEmptyColumns()).thenReturn(false);

        final TableTransformation<String> result = m_testInstance
            .createFromExisting(configuredTransformation.getRawSpec(), m_config, configuredTransformation);

        final ColumnTransformation<String> bTrans = result.getTransformation(b);
        // b used to be filtered and should thus be treated as unknown
        // the unknown position is 2 instead of the stored 3 because there are only two "known" columns
        assertEquals(2, bTrans.getPosition());
    }

    private static class TableTransformationMocker {
        private final List<TypedReaderColumnSpec<String>> m_columns = new ArrayList<>();

        private final List<ColumnTransformation<String>> m_transformations = new ArrayList<>();

        private final RawSpecBuilder<String> m_rawSpecBuilder = new RawSpecBuilder<>();

        private ColumnFilterMode m_columnFilterMode = ColumnFilterMode.UNION;

        private int m_unknownColPos = 0;

        private boolean m_enforceTypes = false;

        private boolean m_keepUnknowns;

        private boolean m_skipEmptyColumns = false;

        TableTransformationMocker withColumnFilterMode(final ColumnFilterMode columnFilterMode) {
            m_columnFilterMode = columnFilterMode;
            return this;
        }

        TableTransformationMocker addTransformation(final TypedReaderColumnSpec<String> column, final String name,
            final DataType destinationType, final int position, final boolean keep, final boolean isInIntersection) {
            m_rawSpecBuilder.addColumn(column, isInIntersection);
            m_columns.add(column);
            final ProductionPath path = mockProductionPath(destinationType);
            m_transformations.add(mockTransformation(column, name, path, position, keep));
            return this;
        }

        private static ColumnTransformation<String> mockTransformation(final TypedReaderColumnSpec<String> externalSpec,
            final String name, final ProductionPath prodPath, final int position, final boolean keep) {
            @SuppressWarnings("unchecked")
            final ColumnTransformation<String> trans = Mockito.mock(ColumnTransformation.class);
            when(trans.getExternalSpec()).thenReturn(externalSpec);
            when(trans.getName()).thenReturn(name);
            when(trans.getOriginalName()).thenReturn(externalSpec.getName().get());//NOSONAR
            when(trans.getProductionPath()).thenReturn(prodPath);
            when(trans.getPosition()).thenReturn(position);
            when(trans.keep()).thenReturn(keep);
            return trans;
        }

        TableTransformationMocker addTransformation(final TypedReaderColumnSpec<String> column,
            final boolean isInIntersection) {
            return addTransformation(column, getNameAfterInit(column), StringCell.TYPE, m_columns.size(), true,
                isInIntersection);
        }

        TableTransformationMocker withUnknownPos(final int pos) {
            m_unknownColPos = pos;
            return this;
        }

        TableTransformationMocker withSkipEmptyColumns(final boolean skipEmptyColumns) {
            m_skipEmptyColumns = skipEmptyColumns;
            return this;
        }

        TableTransformationMocker withKeepUnknowns(final boolean keepUnknowns) {
            m_keepUnknowns = keepUnknowns;
            return this;
        }

        TableTransformationMocker withEnforceTypes(final boolean enforceTypes) {
            m_enforceTypes = enforceTypes;
            return this;
        }

        ColumnTransformation<String> getTransformation(final int i) {
            return m_transformations.get(i);
        }

        TableTransformation<String> mock() {
            @SuppressWarnings("unchecked")
            final TableTransformation<String> mock = Mockito.mock(TableTransformation.class);
            when(mock.getColumnFilterMode()).thenReturn(m_columnFilterMode);
            final UnknownColumnsTransformation unknownColsTrans = Mockito.mock(UnknownColumnsTransformation.class);
            when(unknownColsTrans.getPosition()).thenReturn(m_unknownColPos);
            when(unknownColsTrans.keep()).thenReturn(m_keepUnknowns);
            when(mock.getTransformationForUnknownColumns()).thenReturn(unknownColsTrans);
            when(mock.getRawSpec()).thenReturn(m_rawSpecBuilder.build());
            when(mock.enforceTypes()).thenReturn(m_enforceTypes);
            when(mock.skipEmptyColumns()).thenReturn(m_skipEmptyColumns);
            Iterator<TypedReaderColumnSpec<String>> colIterator = m_columns.iterator();
            Iterator<ColumnTransformation<String>> transIterator = m_transformations.iterator();
            while (colIterator.hasNext()) {
                TypedReaderColumnSpec<String> col = colIterator.next();
                ColumnTransformation<String> trans = transIterator.next();
                when(trans.compareTo(any())).thenAnswer(invocation -> {
                    final ColumnTransformation<?> other = invocation.getArgument(0);
                    return Integer.compare(trans.getPosition(), other.getPosition());
                });
                when(mock.getTransformation(col)).thenReturn(trans);
            }
            return mock;
        }
    }

    private static ProductionPath mockProductionPath(final DataType destinationType) {
        ProductionPath prodPath = TRFTestingUtils.mockProductionPath();
        when(prodPath.getConverterFactory().getDestinationType()).thenReturn(destinationType);
        return prodPath;
    }

    @Test
    public void testCreateFromConfiguredTransformationModel() {
        final CreateFromConfiguredTester tester = new CreateFromConfiguredTester();
        for (int i = 0; i < 3; i++) {
            for (ColumnFilterMode colFilterMode : ColumnFilterMode.values()) {
                tester.test(colFilterMode, true, i);
                tester.test(colFilterMode, false, i);
            }
        }
    }

    private class CreateFromConfiguredTester {

        ProductionPath m_newHansProdPath = mockProductionPath(IntCell.TYPE);

        ProductionPath m_ulfProdPath = mockProductionPath(IntCell.TYPE);

        private TableTransformationMocker m_transformationMocker;

        private ColumnFilterMode m_colFilterMode;

        private boolean m_keepUnknown;

        private int m_unknownPos;

        private boolean m_isUnion;

        void test(final ColumnFilterMode colFilterMode, final boolean keepUnknown, final int unknownPos) {
            assignTestParameters(colFilterMode, keepUnknown, unknownPos);
            setup();
            final TableTransformation<String> transformationModel =
                m_testInstance.createFromExisting(RAW_SPEC, m_config, m_transformationMocker.mock());
            evaluate(transformationModel);
        }

        private void assignTestParameters(final ColumnFilterMode colFilterMode, final boolean keepUnknown,
            final int unknownPos) {
            m_colFilterMode = colFilterMode;
            m_keepUnknown = keepUnknown;
            m_unknownPos = unknownPos;
            m_isUnion = colFilterMode == ColumnFilterMode.UNION;
        }

        private void setup() {
            setupConfig(true);
            setupProductionPathProvider();
            setupConfiguredTableTransformation();
        }

        private void setupConfiguredTableTransformation() {
            TypedReaderColumnSpec<String> configuredHans = createColSpec("hans", SIEGFRIEDA);
            m_transformationMocker = new TableTransformationMocker()//
                .withKeepUnknowns(m_keepUnknown)//
                .withColumnFilterMode(m_colFilterMode)//
                .withUnknownPos(m_unknownPos)//
                .addTransformation(configuredHans, "franz", IntCell.TYPE, 1, true, true)//
                .addTransformation(RUDIGER, "sepp", IntCell.TYPE, 0, true, true);
        }

        private void setupProductionPathProvider() {
            when(m_prodPathProvider.getDefaultProductionPath(ELSA)).thenReturn(m_newHansProdPath);
            when(m_prodPathProvider.getAvailableProductionPaths(ELSA)).thenReturn(singletonList(m_newHansProdPath));
            when(m_prodPathProvider.getDefaultProductionPath(BERTA)).thenReturn(m_ulfProdPath);
            when(m_prodPathProvider.getAvailableProductionPaths(BERTA)).thenReturn(singletonList(m_ulfProdPath));
        }

        private void evaluate(final TableTransformation<String> transformationModel) {
            final int actualUnknownPos = findActualUnknownPos();
            int hansPos = findHansPos(actualUnknownPos);
            String hansName = findHansName();
            checkTransformation(transformationModel.getTransformation(HANS), HANS, hansName, m_newHansProdPath, hansPos,
                m_isUnion || m_keepUnknown);
            int seppPos = findSeppPos();
            checkTransformation(transformationModel.getTransformation(RUDIGER), RUDIGER, "sepp",
                m_transformationMocker.getTransformation(1).getProductionPath(), seppPos, true);
            checkTransformation(transformationModel.getTransformation(ULF), ULF, "ulf", m_ulfProdPath,
                m_isUnion ? actualUnknownPos : (actualUnknownPos + 1), m_keepUnknown);
        }

        private int findSeppPos() {
            int seppPos;
            if (m_isUnion) {
                // hans is not considered to be new
                seppPos = m_unknownPos == 0 ? 1 : 0;
            } else {
                // hans is considered to be new
                seppPos = m_unknownPos == 0 ? 2 : 0;
            }
            return seppPos;
        }

        private String findHansName() {
            if (m_isUnion) {
                // we keep the stored transformation
                return "franz";
            } else {
                // hans is considered to be new because it isn't in the intersection
                return "hans";
            }
        }

        private int findHansPos(final int actualUnknownPos) {
            int hansPos;
            if (m_isUnion) {
                hansPos = m_unknownPos <= 1 ? 2 : 1;
            } else {
                // RUDIGER is the only known column so the position for new columns is at most 1
                hansPos = actualUnknownPos;
            }
            return hansPos;
        }

        private int findActualUnknownPos() {
            final int actualUnknownPos;
            if (m_isUnion) {
                actualUnknownPos = m_unknownPos;
            } else {
                // when in intersection mode, HANS is considered to be new
                // hence there is only one known column (RUDIGER)
                actualUnknownPos = m_unknownPos == 0 ? 0 : 1;
            }
            return actualUnknownPos;
        }

    }

    @Test
    public void testCreateDefaultTransformationModelNoSpecMergeMode() {
        setupConfig(false);
        testCreateDefaultTransformation();
    }

    private void testCreateDefaultTransformation() {
        ProductionPath hansProdPath = TRFTestingUtils.mockProductionPath();
        ProductionPath rudigerProdPath = TRFTestingUtils.mockProductionPath();
        ProductionPath ulfProdPath = TRFTestingUtils.mockProductionPath();
        when(m_prodPathProvider.getDefaultProductionPath(ELSA)).thenReturn(hansProdPath);
        when(m_prodPathProvider.getDefaultProductionPath(FRIEDA)).thenReturn(rudigerProdPath);
        when(m_prodPathProvider.getDefaultProductionPath(BERTA)).thenReturn(ulfProdPath);

        final TableTransformation<String> transformationModel = m_testInstance.createNew(RAW_SPEC, m_config);

        checkTransformation(transformationModel.getTransformation(HANS), HANS, "hans", hansProdPath, 0, true);
        checkTransformation(transformationModel.getTransformation(RUDIGER), RUDIGER, "rudiger", rudigerProdPath, 1,
            true);
        checkTransformation(transformationModel.getTransformation(ULF), ULF, "ulf", ulfProdPath, 2, true);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testCreateDefaultIntersectionSpecMergeMode() {
        setupConfig(false);
        when(m_config.getSpecMergeMode()).thenReturn(SpecMergeMode.INTERSECTION);
        testCreateDefaultTransformation();
    }

}
