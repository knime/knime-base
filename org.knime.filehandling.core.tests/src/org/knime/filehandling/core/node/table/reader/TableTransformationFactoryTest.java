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

import static java.util.Arrays.asList;
import static org.knime.filehandling.core.node.table.reader.TRFTestingUtils.checkTransformation;
import static org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec.createWithName;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.data.DataType;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.TableSpecConfig;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.filehandling.core.node.table.reader.selector.ColumnTransformation;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformation;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Contains unit tests for the {@link TableTransformationFactory}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("javadoc") // keeping the link is more useful
@RunWith(MockitoJUnitRunner.class)
public class TableTransformationFactoryTest {

    private static final String SIEGFRIEDA = "siegfrieda";

    private static final String ELSA = "elsa";

    private static final String FRIEDA = "frieda";

    private static final String BERTA = "berta";

    private static TypedReaderColumnSpec<String> createColSpec(final String name, final String type) {
        return createWithName(name, type, true);
    }

    @SafeVarargs
    private static TypedReaderTableSpec<String> createTableSpec(final TypedReaderColumnSpec<String>... cols) {
        return new TypedReaderTableSpec<>(asList(cols));
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
    private MultiTableReadConfig<?> m_config;

    @Mock
    private TableSpecConfig m_tableSpecConfig;

    @SuppressWarnings("rawtypes") // unfortunately that's necessary to satisfy the compiler
    @Mock
    private TableTransformation m_configuredTransformationModel;//NOSONAR

    private TableTransformationFactory<Path, String> m_testInstance;

    @Before
    public void init() {
        m_testInstance = new TableTransformationFactory<>(m_prodPathProvider);
    }

    @SuppressWarnings("unchecked") // unfortunately that's necessary to satisfy the compiler
    private void setupConfig(final boolean hasTableSpecConfig) {
        when(m_config.hasTableSpecConfig()).thenReturn(hasTableSpecConfig);
        if (hasTableSpecConfig) {
            when(m_config.getTableSpecConfig()).thenReturn(m_tableSpecConfig);
            when(m_tableSpecConfig.getTransformationModel()).thenReturn(m_configuredTransformationModel);
        }
    }

    @Test
    public void testDontEnforceTypes() {
        TypedReaderTableSpec<String> hansByHimself = createTableSpec(HANS);
        TypedReaderColumnSpec<String> hansWithDifferentType = createColSpec("hans", SIEGFRIEDA);
        TypedReaderTableSpec<String> newHansByHimself = createTableSpec(hansWithDifferentType);
        RawSpec<String> newRawSpec = new RawSpec<>(newHansByHimself, newHansByHimself);

        ProductionPath oldHansPath = mockProductionPath(DoubleCell.TYPE);
        ColumnTransformation<String> hansTrans = mockTransformation(HANS, "hans", oldHansPath, 0, true);
        when(m_configuredTransformationModel.getTransformation(HANS)).thenReturn(hansTrans);
        when(m_configuredTransformationModel.getColumnFilterMode()).thenReturn(ColumnFilterMode.UNION);
        when(m_configuredTransformationModel.getRawSpec()).thenReturn(new RawSpec<>(hansByHimself, hansByHimself));
        final ProductionPath siegfriedaProdPath = mockProductionPath(IntCell.TYPE);
        when(m_prodPathProvider.getDefaultProductionPath(SIEGFRIEDA)).thenReturn(siegfriedaProdPath);

        when(m_config.hasTableSpecConfig()).thenReturn(true);
        when(m_config.getTableSpecConfig()).thenReturn(m_tableSpecConfig);
        when(m_tableSpecConfig.getTransformationModel()).thenReturn(m_configuredTransformationModel);

        TableTransformation<String> result = m_testInstance.create(newRawSpec, m_config);

        checkTransformation(result.getTransformation(hansWithDifferentType), hansWithDifferentType, "hans",
            siegfriedaProdPath, 0, true);
    }

    @Test
    public void testEnforceTypes() {
        testEnforceTypes(true);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testEnforceTypesFailsBecauseNoAlternativePathIsAvailable() {
        testEnforceTypes(false);
    }

    private void testEnforceTypes(final boolean alternativePathAvailable) {
        final TypedReaderTableSpec<String> configured = createTableSpec(HANS);
        TypedReaderColumnSpec<String> hansWithDifferentType = createColSpec("hans", SIEGFRIEDA);
        final TypedReaderTableSpec<String> newSpec = createTableSpec(hansWithDifferentType);
        final RawSpec<String> newRawSpec = new RawSpec<>(newSpec, newSpec);
        when(m_configuredTransformationModel.enforceTypes()).thenReturn(true);
        final ProductionPath elsaProdPath = mockProductionPath(LongCell.TYPE);

        ColumnTransformation<String> hansTrans = mockTransformation(HANS, "hans", elsaProdPath, 0, true);
        when(m_configuredTransformationModel.getTransformation(HANS)).thenReturn(hansTrans);
        when(m_configuredTransformationModel.getColumnFilterMode()).thenReturn(ColumnFilterMode.UNION);
        when(m_configuredTransformationModel.getRawSpec()).thenReturn(new RawSpec<>(configured, configured));

        final ProductionPath siegfriedaProdPath = mockProductionPath(IntCell.TYPE);
        List<ProductionPath> availablePaths = new ArrayList<>(asList(siegfriedaProdPath));
        if (alternativePathAvailable) {
            availablePaths.add(elsaProdPath);
        }
        when(m_prodPathProvider.getAvailableProductionPaths(SIEGFRIEDA))
            .thenReturn(availablePaths);

        when(m_config.hasTableSpecConfig()).thenReturn(true);
        when(m_config.getTableSpecConfig()).thenReturn(m_tableSpecConfig);
        when(m_tableSpecConfig.getTransformationModel()).thenReturn(m_configuredTransformationModel);

        TableTransformation<String> result = m_testInstance.create(newRawSpec, m_config);
        checkTransformation(result.getTransformation(hansWithDifferentType), hansWithDifferentType, "hans",
            elsaProdPath, 0, true);
    }

    @Test
    public void testCreateFromConfiguredTransformationModel() {
        for (int i = 0; i < 3; i++) {
            for (ColumnFilterMode colFilterMode : ColumnFilterMode.values()) {
                testCreateFromConfigured(colFilterMode, true, i);
                testCreateFromConfigured(colFilterMode, false, i);
            }
        }
    }

    private static ProductionPath mockProductionPath(final DataType destinationType) {
        ProductionPath prodPath = TRFTestingUtils.mockProductionPath();
        when(prodPath.getConverterFactory().getDestinationType()).thenReturn(destinationType);
        return prodPath;
    }

    private void testCreateFromConfigured(final ColumnFilterMode colFilterMode, final boolean keepUnknown,
        final int unknownPos) {
        setupConfig(true);

        ProductionPath hansProdPath = mockProductionPath(IntCell.TYPE);
        ProductionPath newHansProdPath = mockProductionPath(IntCell.TYPE);
        ProductionPath rudigerProdPath = mockProductionPath(IntCell.TYPE);
        ProductionPath ulfProdPath = mockProductionPath(IntCell.TYPE);

        TypedReaderColumnSpec<String> configuredHans = createColSpec("hans", SIEGFRIEDA);

        TypedReaderTableSpec<String> oldUnion = createTableSpec(configuredHans, RUDIGER);
        final RawSpec<String> oldRawSpec = new RawSpec<>(oldUnion, oldUnion);
        ColumnTransformation<String> hansTrans = mockTransformation(configuredHans, "franz", hansProdPath, 1, true);
        ColumnTransformation<String> rudigerTrans = mockTransformation(RUDIGER, "sepp", rudigerProdPath, 0, true);
        when(rudigerTrans.compareTo(hansTrans)).thenReturn(-1);
        when(m_configuredTransformationModel.stream()).thenReturn(Stream.of(hansTrans, rudigerTrans));
        when(m_configuredTransformationModel.keepUnknownColumns()).thenReturn(keepUnknown);
        when(m_configuredTransformationModel.getColumnFilterMode()).thenReturn(colFilterMode);
        when(m_configuredTransformationModel.getPositionForUnknownColumns()).thenReturn(unknownPos);
        when(m_configuredTransformationModel.getRawSpec()).thenReturn(oldRawSpec);
        when(m_configuredTransformationModel.getTransformation(configuredHans)).thenReturn(hansTrans);
        when(m_configuredTransformationModel.getTransformation(RUDIGER)).thenReturn(rudigerTrans);

        when(m_prodPathProvider.getDefaultProductionPath(ELSA)).thenReturn(newHansProdPath);
        when(m_prodPathProvider.getAvailableProductionPaths(ELSA)).thenReturn(asList(newHansProdPath));
        when(m_prodPathProvider.getDefaultProductionPath(BERTA)).thenReturn(ulfProdPath);
        when(m_prodPathProvider.getAvailableProductionPaths(BERTA)).thenReturn(asList(ulfProdPath));

        final TableTransformation<String> transformationModel = m_testInstance.create(RAW_SPEC, m_config);

        final boolean union = colFilterMode == ColumnFilterMode.UNION;

        final int actualUnknownPos;
        if (union) {
            actualUnknownPos = unknownPos;
        } else {
            // when in intersection mode, HANS is considered to be new
            // hence there is only one known column (RUDIGER)
            actualUnknownPos = unknownPos == 0 ? 0 : 1;
        }

        int hansPos;
        if (union) {
            hansPos = unknownPos <= 1 ? 2 : 1;
        } else {
            // RUDIGER is the only known column so the position for new columns is at most 1
            hansPos = actualUnknownPos;
        }

        String hansName;
        if (union) {
            // we keep the stored transformation
            hansName = "franz";
        } else {
            // hans is considered to be new because it isn't in the intersection
            hansName = "hans";
        }

        checkTransformation(transformationModel.getTransformation(HANS), HANS, hansName, newHansProdPath, hansPos,
            union || keepUnknown);

        int seppPos;
        if (union) {
            // hans is not considered to be new
            seppPos = unknownPos == 0 ? 1 : 0;
        } else {
            // hans is considered to be new
            seppPos = unknownPos == 0 ? 2 : 0;
        }

        checkTransformation(transformationModel.getTransformation(RUDIGER), RUDIGER, "sepp", rudigerProdPath, seppPos,
            true);
        checkTransformation(transformationModel.getTransformation(ULF), ULF, "ulf", ulfProdPath,
            union ? actualUnknownPos : actualUnknownPos + 1, keepUnknown);
    }

    private static ColumnTransformation<String> mockTransformation(final TypedReaderColumnSpec<String> externalSpec,
        final String name, final ProductionPath prodPath, final int position, final boolean keep) {
        @SuppressWarnings("unchecked")
        final ColumnTransformation<String> trans = Mockito.mock(ColumnTransformation.class);
        when(trans.getExternalSpec()).thenReturn(externalSpec);
        when(trans.getName()).thenReturn(name);
        when(trans.getOriginalName()).thenReturn(externalSpec.getName().get());
        when(trans.getProductionPath()).thenReturn(prodPath);
        when(trans.getPosition()).thenReturn(position);
        when(trans.keep()).thenReturn(keep);
        return trans;
    }

    @Test
    public void testCreateDefaultTransformationModelNoSpecMergeMode() {
        setupConfig(false);
        ProductionPath hansProdPath = TRFTestingUtils.mockProductionPath();
        ProductionPath rudigerProdPath = TRFTestingUtils.mockProductionPath();
        ProductionPath ulfProdPath = TRFTestingUtils.mockProductionPath();
        when(m_prodPathProvider.getDefaultProductionPath(ELSA)).thenReturn(hansProdPath);
        when(m_prodPathProvider.getDefaultProductionPath(FRIEDA)).thenReturn(rudigerProdPath);
        when(m_prodPathProvider.getDefaultProductionPath(BERTA)).thenReturn(ulfProdPath);

        final TableTransformation<String> transformationModel = m_testInstance.create(RAW_SPEC, m_config);

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
        ProductionPath hansProdPath = TRFTestingUtils.mockProductionPath();
        ProductionPath rudigerProdPath = TRFTestingUtils.mockProductionPath();
        ProductionPath ulfProdPath = TRFTestingUtils.mockProductionPath();
        when(m_prodPathProvider.getDefaultProductionPath(ELSA)).thenReturn(hansProdPath);
        when(m_prodPathProvider.getDefaultProductionPath(FRIEDA)).thenReturn(rudigerProdPath);
        when(m_prodPathProvider.getDefaultProductionPath(BERTA)).thenReturn(ulfProdPath);

        final TableTransformation<String> transformationModel = m_testInstance.create(RAW_SPEC, m_config);

        checkTransformation(transformationModel.getTransformation(HANS), HANS, "hans", hansProdPath, 0, true);
        checkTransformation(transformationModel.getTransformation(RUDIGER), RUDIGER, "rudiger", rudigerProdPath, 1,
            true);
        checkTransformation(transformationModel.getTransformation(ULF), ULF, "ulf", ulfProdPath, 2, true);
    }

}
