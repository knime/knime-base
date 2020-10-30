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
package org.knime.filehandling.core.node.table.reader.config;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.knime.filehandling.core.node.table.reader.TRFTestingUtils.a;
import static org.knime.filehandling.core.node.table.reader.TRFTestingUtils.checkTransformation;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.convert.map.MappingFramework;
import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.convert.map.SimpleCellValueProducerFactory;
import org.knime.core.data.convert.map.Source;
import org.knime.core.data.convert.util.SerializeUtil;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.filehandling.core.node.table.reader.SpecMergeMode;
import org.knime.filehandling.core.node.table.reader.TRFTestingUtils;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.selector.ColumnTransformation;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformation;
import org.knime.filehandling.core.node.table.reader.spec.ReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.ReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.util.MultiTableUtils;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 *
 * Contains test for the {@link DefaultTableSpecConfig}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultTableSpecConfigTest {

    private static class DummySource implements Source<String> {
    }

    private static final TypedReaderColumnSpec<String> COL1 = TypedReaderColumnSpec.createWithName("A", "X", true);

    private static final TypedReaderColumnSpec<String> COL2 = TypedReaderColumnSpec.createWithName("B", "Y", true);

    private static final TypedReaderColumnSpec<String> COL3 = TypedReaderColumnSpec.createWithName("C", "Z", true);

    private static final TypedReaderTableSpec<String> SPEC1 = new TypedReaderTableSpec<>(asList(COL1, COL2));

    private static final TypedReaderTableSpec<String> SPEC2 = new TypedReaderTableSpec<>(asList(COL2, COL3));

    private static final TypedReaderTableSpec<String> UNION = new TypedReaderTableSpec<>(asList(COL1, COL2, COL3));

    private static final TypedReaderTableSpec<String> INTERSECTION = new TypedReaderTableSpec<>(asList(COL2));

    private static final RawSpec<String> RAW_SPEC = new RawSpec<>(UNION, INTERSECTION);

    private static final String[] ORIGINAL_NAMES = {"A", "B", "C"};

    private static final String MOST_GENERIC_EXTERNAL_TYPE = "X";

    private static final String ROOT_PATH = "root";

    private static final ProducerRegistry<String, DummySource> REGISTRY = createTestingRegistry();

    private Map<Path, ReaderTableSpec<?>> m_individualSpecs;

    private Path m_path1;

    private Path m_path2;

    @Mock
    private ColumnTransformation<String> m_trans1;

    @Mock
    private ColumnTransformation<String> m_trans2;

    @Mock
    private ColumnTransformation<String> m_trans3;

    private static ProducerRegistry<String, DummySource> createTestingRegistry() {
        final ProducerRegistry<String, DummySource> registry = MappingFramework.forSourceType(DummySource.class);
        registry.register(new SimpleCellValueProducerFactory<>("X", String.class, null));
        registry.register(new SimpleCellValueProducerFactory<>("X", Long.class, null));
        registry.register(new SimpleCellValueProducerFactory<>("Y", Integer.class, null));
        registry.register(new SimpleCellValueProducerFactory<>("Z", Double.class, null));
        return registry;
    }

    /**
     * Initializes the needed members before each test.
     */
    @Before
    public void init() {
        m_path1 = TRFTestingUtils.mockPath("first");
        m_path2 = TRFTestingUtils.mockPath("second");
        m_individualSpecs = new LinkedHashMap<>();
        m_individualSpecs.put(m_path1, SPEC1);
        m_individualSpecs.put(m_path2, SPEC2);
    }

    /**
     * Tests {@link DefaultTableSpecConfig#createFromTransformationModel(String, Map, TableTransformation)}.
     */
    @Test
    public void testCreateFromTransformationModel() {
        @SuppressWarnings("unchecked")
        final TableTransformation<String> transformationModel = mock(TableTransformation.class);

        RawSpec<String> rawSpec = new RawSpec<>(new TypedReaderTableSpec<>(asList(COL1, COL2, COL3)),
            new TypedReaderTableSpec<>(asList(COL1, COL3)));

        when(transformationModel.getRawSpec()).thenReturn(rawSpec);

        when(transformationModel.getTransformation(COL1)).thenReturn(m_trans1);
        when(transformationModel.getTransformation(COL2)).thenReturn(m_trans2);
        when(transformationModel.getTransformation(COL3)).thenReturn(m_trans3);

        when(transformationModel.getColumnFilterMode()).thenReturn(ColumnFilterMode.UNION);
        when(transformationModel.keepUnknownColumns()).thenReturn(true);
        when(transformationModel.getPositionForUnknownColumns()).thenReturn(3);


        final ProductionPath p1 = TRFTestingUtils.mockProductionPath();
        when(p1.getConverterFactory().getDestinationType()).thenReturn(StringCell.TYPE);
        final ProductionPath p2 = TRFTestingUtils.mockProductionPath();
        when(p2.getConverterFactory().getDestinationType()).thenReturn(IntCell.TYPE);
        final ProductionPath p3 = TRFTestingUtils.mockProductionPath();
        when(p3.getConverterFactory().getDestinationType()).thenReturn(DoubleCell.TYPE);

        new TransformationStubber<>(m_trans1).withName("A").withKeep(true).withPosition(2).withProductionPath(p1);
        new TransformationStubber<>(m_trans2).withName("B").withKeep(false).withPosition(0).withProductionPath(p2);
        new TransformationStubber<>(m_trans3).withName("G").withKeep(true).withPosition(1).withProductionPath(p3);

        final TableSpecConfig config =
            DefaultTableSpecConfig.createFromTransformationModel(ROOT_PATH, m_individualSpecs, transformationModel);

        final String[] expectedOriginalNames = {"A", "B", "C"};
        final int[] expectedPositionalMapping = {2, 0, 1};
        final boolean[] expectedKeep = {true, false, true};
        final DataTableSpec expectedOutputSpec = new DataTableSpec("default", new String[]{"A", "B", "G"},
            new DataType[]{StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE});
        final TableSpecConfig expected = new DefaultTableSpecConfig(ROOT_PATH, expectedOutputSpec, m_individualSpecs,
            new ProductionPath[]{p1, p2, p3}, expectedOriginalNames, expectedPositionalMapping, expectedKeep,
            expectedKeep.length, ColumnFilterMode.UNION, true);
        assertEquals(expected, config);
    }

    private static final class TransformationStubber<T> {
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

        TransformationStubber<T> withExternalSpec(final TypedReaderColumnSpec<T> spec) {
            when(m_mockTransformation.getExternalSpec()).thenReturn(spec);
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

        ColumnTransformation<T> getTransformation() {
            return m_mockTransformation;
        }
    }

    /**
     * Tests that a {@link DefaultTableSpecConfig} can be correctly loaded after it has been saved to
     * {@link NodeSettings}.
     *
     * @throws InvalidSettingsException - does not happen
     */
    @Test
    public void testSaveLoad() throws InvalidSettingsException {
        // create a TableSpecConfig

        final DataTableSpec outputSpec = new DataTableSpec("default", a("a", "b", "c"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));

        final ProductionPath[] prodPaths =
            getProductionPaths(a("X", "Y", "Z"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));
        final String[] originalNames = {"A", "B", "C"};
        final int[] positionalMapping = {0, 1, 2};
        final boolean[] keep = {true, true, true};

        LinkedHashMap<Path, ReaderTableSpec<?>> individualSpecsWithoutTypes = getIndividualSpecsWithoutTypes();
        final TableSpecConfig cfg = new DefaultTableSpecConfig(ROOT_PATH, outputSpec, individualSpecsWithoutTypes,
            prodPaths, originalNames, positionalMapping, keep, originalNames.length, ColumnFilterMode.UNION, true);

        // tests save / load
        final NodeSettings s = new NodeSettings("origin");
        cfg.save(s);
        DefaultTableSpecConfig.validate(s, REGISTRY);
        final TableSpecConfig load =
            DefaultTableSpecConfig.load(s, REGISTRY, MOST_GENERIC_EXTERNAL_TYPE, SpecMergeMode.FAIL_ON_DIFFERING_SPECS);

        assertEquals(cfg, load);

        // test root path
        assertTrue(load.isConfiguredWith(ROOT_PATH));
        assertFalse(load.isConfiguredWith("foo"));

        // test specs
        DataTableSpec loadedDataTableSpec = load.getDataTableSpec();
        assertEquals(outputSpec, loadedDataTableSpec);
        assertNotEquals(new DataTableSpec(new DataColumnSpecCreator("Blub", IntCell.TYPE).createSpec()),
            loadedDataTableSpec);

        // test production paths
        assertArrayEquals(prodPaths, load.getProductionPaths());
        prodPaths[0] = prodPaths[1];
        assertNotEquals(prodPaths, load.getProductionPaths());

        // tests paths
        assertTrue(load.isConfiguredWith(new ArrayList<>(m_individualSpecs.keySet())));
        assertFalse(load.isConfiguredWith(Arrays.asList(m_path1)));
        assertFalse(load.isConfiguredWith(Arrays.asList(m_path1, m_path2, m_path1)));

        // test reader specs
        for (final Entry<Path, ReaderTableSpec<?>> entry : individualSpecsWithoutTypes.entrySet()) {
            assertEquals(entry.getValue(), load.getSpec(entry.getKey().toString()));
        }
    }

    @Test
    public void testGetTransformationModel() {
        final DataTableSpec knimeSpec =
            new DataTableSpec(a("A", "B", "G"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));
        final TableSpecConfig tsc = new DefaultTableSpecConfig(ROOT_PATH, knimeSpec, m_individualSpecs,
            getProductionPaths(a("X", "Y", "Z"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE)), a("A", "B", "C"),
            new int[]{1, 2, 0}, new boolean[]{true, false, true}, 3, ColumnFilterMode.UNION, true);

        final TableTransformation<String> tm = tsc.getTransformationModel();
        assertEquals(RAW_SPEC, tm.getRawSpec());

        final ProductionPath[] expectedProdPaths =
            getProductionPaths(a("X", "Y", "Z"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));

        checkTransformation(tm.getTransformation(COL1), COL1, "A", expectedProdPaths[0], 1, true);
        checkTransformation(tm.getTransformation(COL2), COL2, "B", expectedProdPaths[1], 2, false);
        checkTransformation(tm.getTransformation(COL3), COL3, "G", expectedProdPaths[2], 0, true);
    }


    private static ProductionPath[] getProductionPaths(final String[] externalTypes, final DataType[] knimeTypes) {
        return IntStream.range(0, externalTypes.length)
            .mapToObj(i -> REGISTRY.getAvailableProductionPaths(externalTypes[i]).stream()
                .filter(p -> p.getConverterFactory().getDestinationType() == knimeTypes[i]).findFirst().get())
            .toArray(ProductionPath[]::new);
    }

    /**
     * Tests {@link DefaultTableSpecConfig#save(NodeSettingsWO)}
     *
     * @throws InvalidSettingsException shouldn't be thrown
     */
    @Test
    public void testSave() throws InvalidSettingsException {
        final DataTableSpec expectedOutputSpec = new DataTableSpec("default", new String[]{"A", "B", "G"},
            new DataType[]{StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE});
        final ProductionPath[] productionPaths =
            getProductionPaths(a("X", "Y", "Z"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));
        final int[] positionalMapping = {1, 0, 2};
        final boolean[] keep = {true, false, true};
        final DefaultTableSpecConfig config =
            new DefaultTableSpecConfig(ROOT_PATH, expectedOutputSpec, m_individualSpecs, productionPaths,
                ORIGINAL_NAMES, positionalMapping, keep, keep.length, ColumnFilterMode.UNION, true);

        final NodeSettings settings = new NodeSettings("test");
        config.save(settings);

        assertEquals(ROOT_PATH, settings.getString("root_path_Internals"));
        assertEquals(expectedOutputSpec, DataTableSpec.load(settings.getNodeSettings("datatable_spec_Internals")));
        assertArrayEquals(new String[]{"first", "second"}, settings.getStringArray("file_paths_Internals"));

        final NodeSettings individualSpecs = settings.getNodeSettings("individual_specs_Internals");
        assertArrayEquals(new String[]{"A", "B"}, individualSpecs.getStringArray("individual_spec_0"));
        assertArrayEquals(new String[]{"B", "C"}, individualSpecs.getStringArray("individual_spec_1"));

        final NodeSettings productionPathSettings = settings.getNodeSettings("production_paths_Internals");
        for (int i = 0; i < productionPaths.length; i++) {
            assertEquals(productionPaths[i],
                SerializeUtil.loadProductionPath(productionPathSettings, REGISTRY, "production_path_" + i).get());
        }

        assertArrayEquals(ORIGINAL_NAMES, settings.getStringArray("original_names_Internals"));
        assertArrayEquals(positionalMapping, settings.getIntArray("positional_mapping_Internals"));
        assertArrayEquals(keep, settings.getBooleanArray("keep_Internals"));
    }

    @Test
    public void testLoadPre43() throws InvalidSettingsException {
        final NodeSettings settings = create42Settings();

        final TableSpecConfig loaded = DefaultTableSpecConfig.load(settings, REGISTRY, MOST_GENERIC_EXTERNAL_TYPE,
            SpecMergeMode.FAIL_ON_DIFFERING_SPECS);

        final DataTableSpec fullKnimeSpec =
            new DataTableSpec("default", a("A", "B", "C"), a(StringCell.TYPE, IntCell.TYPE, StringCell.TYPE));
        final ProductionPath[] prodPaths =
            getProductionPaths(a(MOST_GENERIC_EXTERNAL_TYPE, "Y", MOST_GENERIC_EXTERNAL_TYPE),
                a(StringCell.TYPE, IntCell.TYPE, StringCell.TYPE));
        final String[] originalNames = {"A", "B", "C"};
        final int[] positions = {0, 1, 2};
        final boolean[] keep = {false, true, false};
        // TODO extract method
        final Map<Path, ReaderTableSpec<?>> individualSpecs = getIndividualSpecsWithoutTypes();
        final TableSpecConfig expected = new DefaultTableSpecConfig(ROOT_PATH, fullKnimeSpec, individualSpecs,
            prodPaths, originalNames, positions, keep, keep.length, ColumnFilterMode.UNION, true);
        assertEquals(expected, loaded);
    }

    private LinkedHashMap<Path, ReaderTableSpec<?>> getIndividualSpecsWithoutTypes() {
        return m_individualSpecs.entrySet().stream()
            .collect(toMap(Entry::getKey, e -> ReaderTableSpec.createReaderTableSpec(e.getValue().stream()//
                .map(s -> (ReaderColumnSpec)s)//
                .map(MultiTableUtils::getNameAfterInit)//
                .collect(toList())), (x, y) -> x, LinkedHashMap::new));
    }

    private NodeSettings create42Settings() {
        final ProductionPath[] productionPaths = getProductionPaths(a("Y"), a(IntCell.TYPE));
        final DataTableSpec tableSpec = new DataTableSpec(a("B"), a(IntCell.TYPE));
        return createSettings(tableSpec, productionPaths, m_individualSpecs, null, null, null);
    }

    private static void saveProductionPaths(final NodeSettingsWO settings, final ProductionPath[] productionPaths) {
        int i = 0;
        for (final ProductionPath pP : productionPaths) {
            SerializeUtil.storeProductionPath(pP, settings, "production_path_" + i);
            i++;
        }
        settings.addInt("num_production_paths_Internals", i);
    }

    private static void saveIndividualSpecs(final Map<Path, ReaderTableSpec<?>> individualSpecs,
        final NodeSettingsWO settings) {
        int i = 0;
        for (final ReaderTableSpec<? extends ReaderColumnSpec> readerTableSpec : individualSpecs.values()) {
            settings.addStringArray("individual_spec_" + i//
                , readerTableSpec.stream()//
                    .map(MultiTableUtils::getNameAfterInit)//
                    .toArray(String[]::new)//
            );
            i++;
        }
    }

    /**
     * Tests loading from settings.
     *
     * @throws InvalidSettingsException never thrown
     */
    @Test
    public void testLoad() throws InvalidSettingsException {
        final ProductionPath[] productionPaths = getProductionPaths(a("X", "Y", "Z"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));
        String[] originalNames = a("A", "B", "C");
        final DataTableSpec tableSpec =
            new DataTableSpec(originalNames, a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));
        int[] positions = new int[]{2, 1, 0};
        boolean[] keep = new boolean[]{true, false, true};

        final NodeSettings settings =
            createSettings(tableSpec, productionPaths, m_individualSpecs, originalNames, positions, keep);

        final TableSpecConfig loaded = DefaultTableSpecConfig.load(settings, REGISTRY, MOST_GENERIC_EXTERNAL_TYPE,
            SpecMergeMode.FAIL_ON_DIFFERING_SPECS);

        final Map<Path, ReaderTableSpec<?>> individualSpecs = getIndividualSpecsWithoutTypes();
        final TableSpecConfig expected = new DefaultTableSpecConfig(ROOT_PATH, tableSpec, individualSpecs,
            productionPaths, originalNames, positions, keep, keep.length, ColumnFilterMode.UNION, true);
        assertEquals(expected, loaded);
    }

    /**
     * Tests {@link DefaultTableSpecConfig#isConfiguredWith(List)},
     * {@link DefaultTableSpecConfig#isConfiguredWith(String)} and
     * {@link DefaultTableSpecConfig#isConfiguredWith(String, List)}.
     */
    @Test
    public void testIsConfiguredWith() {
        final DataTableSpec knimeSpec =
            new DataTableSpec(a("A", "B", "C"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));
        final TableSpecConfig tsc = new DefaultTableSpecConfig(ROOT_PATH, knimeSpec, m_individualSpecs,
            getProductionPaths(a("X", "Y", "Z"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE)), a("A", "B", "C"),
            new int[]{0, 1, 2}, new boolean[]{true, true, true}, 3, ColumnFilterMode.UNION, true);
        assertTrue(tsc.isConfiguredWith(ROOT_PATH));
        assertFalse(tsc.isConfiguredWith("foobar"));

        assertTrue(tsc.isConfiguredWith(asList(m_path1, m_path2)));
        assertFalse(tsc.isConfiguredWith(asList(m_path1)));
        assertFalse(tsc.isConfiguredWith(asList(m_path1, m_path2, mock(Path.class))));
        assertFalse(tsc.isConfiguredWith(asList(m_path1, m_path2, m_path1)));

        assertTrue(tsc.isConfiguredWith(ROOT_PATH, asList(m_path1, m_path2)));
        assertFalse(tsc.isConfiguredWith("foobar", asList(m_path1, m_path2)));
        assertFalse(tsc.isConfiguredWith(ROOT_PATH, asList(m_path1)));
        assertFalse(tsc.isConfiguredWith(ROOT_PATH, asList(m_path1, m_path2, mock(Path.class))));
        assertFalse(tsc.isConfiguredWith(ROOT_PATH, asList(m_path1, m_path2, m_path1)));
    }

    @Test
    public void testGetDataTableSpec() {
        final DataTableSpec knimeSpec =
            new DataTableSpec(a("A", "B", "C"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));
        final TableSpecConfig tsc = new DefaultTableSpecConfig(ROOT_PATH, knimeSpec, m_individualSpecs,
            getProductionPaths(a("X", "Y", "Z"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE)), a("A", "B", "C"),
            new int[]{2, 0, 1}, new boolean[]{true, false, true}, 3, ColumnFilterMode.UNION, true);

        final DataTableSpec expected = new DataTableSpec("default", a("C", "A"), a(DoubleCell.TYPE, StringCell.TYPE));
        assertEquals(expected, tsc.getDataTableSpec());
    }

    @Test
    public void testGetPaths() {
        final DataTableSpec knimeSpec =
            new DataTableSpec(a("A", "B", "C"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));
        final TableSpecConfig tsc = new DefaultTableSpecConfig(ROOT_PATH, knimeSpec, m_individualSpecs,
            getProductionPaths(a("X", "Y", "Z"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE)), a("A", "B", "C"),
            new int[]{0, 1, 2}, new boolean[]{true, true, true}, 3, ColumnFilterMode.UNION, true);
        assertEquals(asList("first", "second"), tsc.getPaths());
    }

    @Test
    public void testGetSpec() {
        final DataTableSpec knimeSpec =
            new DataTableSpec(a("A", "B", "C"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));
        final TableSpecConfig tsc = new DefaultTableSpecConfig(ROOT_PATH, knimeSpec, m_individualSpecs,
            getProductionPaths(a("X", "Y", "Z"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE)), a("A", "B", "C"),
            new int[]{0, 1, 2}, new boolean[]{true, true, true}, 3, ColumnFilterMode.UNION, true);

        assertEquals(m_individualSpecs.get(m_path1), tsc.getSpec("first"));
        assertEquals(m_individualSpecs.get(m_path2), tsc.getSpec("second"));
    }

    @Test
    public void testGetProductionPaths() {
        final DataTableSpec knimeSpec =
            new DataTableSpec(a("A", "B", "C"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));
        final TableSpecConfig tsc = new DefaultTableSpecConfig(ROOT_PATH, knimeSpec, m_individualSpecs,
            getProductionPaths(a("X", "Y", "Z"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE)), a("A", "B", "C"),
            new int[]{2, 0, 1}, new boolean[]{true, false, true}, 3, ColumnFilterMode.UNION, true);

        final ProductionPath[] expected = getProductionPaths(a("Z", "X"), a(DoubleCell.TYPE, StringCell.TYPE));

        assertArrayEquals(expected, tsc.getProductionPaths());
    }

    /**
     * Tests if {@link DefaultTableSpecConfig#validate(NodeSettingsRO, ProducerRegistry)} succeeds for valid settings.
     *
     * @throws InvalidSettingsException not thrown
     */
    @Test
    public void testValidateSucceeds() throws InvalidSettingsException {
        final ProductionPath[] productionPaths =
            getProductionPaths(a("X", "Y", "Z"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));
        String[] originalNames = a("A", "B", "C");
        final DataTableSpec tableSpec =
            new DataTableSpec(originalNames, a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));
        int[] positions = new int[]{2, 1, 0};
        boolean[] keep = new boolean[]{true, false, true};

        final NodeSettings settings =
            createSettings(tableSpec, productionPaths, m_individualSpecs, originalNames, positions, keep);

        DefaultTableSpecConfig.validate(settings, REGISTRY);
    }

    private static NodeSettings createSettings(final DataTableSpec tableSpec, final ProductionPath[] productionPaths,
        final Map<Path, ReaderTableSpec<?>> individualSpecs, final String[] originalNames, final int[] positions,
        final boolean[] keep) {
        final NodeSettings settings = new NodeSettings("test");
        settings.addString("root_path_Internals", ROOT_PATH);
        tableSpec.save(settings.addNodeSettings("datatable_spec_Internals"));
        settings.addStringArray("file_paths_Internals",
            individualSpecs.keySet().stream().map(Path::toString).toArray(String[]::new));
        saveIndividualSpecs(individualSpecs, settings.addNodeSettings("individual_specs_Internals"));
        saveProductionPaths(settings.addNodeSettings("production_paths_Internals"), productionPaths);
        if (originalNames != null) {
            settings.addStringArray("original_names_Internals", originalNames);
        }
        if (positions != null) {
            settings.addIntArray("positional_mapping_Internals", positions);
        }
        if (keep != null) {
            settings.addBooleanArray("keep_Internals", keep);
        }
        return settings;
    }

    /**
     * Tests that old settings (written with 4.2) can still be validated.
     *
     * @throws InvalidSettingsException never thrown
     */
    @Test
    public void testValidateOldSettings() throws InvalidSettingsException {
        final NodeSettings settings = create42Settings();
        DefaultTableSpecConfig.validate(settings, REGISTRY);
    }

    /**
     * Tests the equals and hashCode implementations.
     */
    @Test
    public void testEqualsHashCode() {
        final ProductionPath[] productionPaths =
            getProductionPaths(a("X", "Y", "Z"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));
        String[] originalNames = a("A", "B", "C");
        final DataTableSpec tableSpec =
            new DataTableSpec(originalNames, a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));
        int[] positions = new int[]{2, 1, 0};
        boolean[] keep = new boolean[]{true, false, true};

        final DefaultTableSpecConfig tsc = new DefaultTableSpecConfig(ROOT_PATH, tableSpec, m_individualSpecs,
            productionPaths, originalNames, positions, keep, 3, ColumnFilterMode.UNION, true);

        assertTrue(tsc.equals(tsc));
        assertFalse(tsc.equals(null));
        assertEquals(tsc.hashCode(), tsc.hashCode());

        assertFalse(tsc.equals("SomeString"));

        final DefaultTableSpecConfig equal = new DefaultTableSpecConfig(ROOT_PATH, tableSpec, m_individualSpecs,
            productionPaths, originalNames, positions, keep, 3, ColumnFilterMode.UNION, true);

        assertTrue(tsc.equals(equal));
        assertEquals(tsc.hashCode(), equal.hashCode());

        final DefaultTableSpecConfig differentRoot = new DefaultTableSpecConfig("different_root", tableSpec,
            m_individualSpecs, productionPaths, originalNames, positions, keep, 3, ColumnFilterMode.UNION, true);
        assertFalse(tsc.equals(differentRoot));

        final DefaultTableSpecConfig differentNames = new DefaultTableSpecConfig(ROOT_PATH,
            new DataTableSpec(a("K", "L", "C"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE)), m_individualSpecs,
            productionPaths, originalNames, positions, keep, 3, ColumnFilterMode.UNION, true);

        assertFalse(tsc.equals(differentNames));

        DataType[] types = a(LongCell.TYPE, IntCell.TYPE, DoubleCell.TYPE);
        final DefaultTableSpecConfig differentTypes = new DefaultTableSpecConfig(ROOT_PATH,
            new DataTableSpec(a("A", "B", "C"), types), m_individualSpecs, getProductionPaths(a("X", "Y", "Z"), types),
            originalNames, positions, keep, 3, ColumnFilterMode.UNION, true);
        assertFalse(tsc.equals(differentTypes));

        Map<Path, ReaderTableSpec<?>> differentPathMap = new LinkedHashMap<>();
        differentPathMap.put(mock(Path.class), SPEC1);
        differentPathMap.put(m_path2, SPEC2);

        final DefaultTableSpecConfig differentPaths = new DefaultTableSpecConfig(ROOT_PATH, tableSpec, differentPathMap,
            productionPaths, originalNames, positions, keep, 3, ColumnFilterMode.UNION, true);
        assertFalse(tsc.equals(differentPaths));

        final TypedReaderTableSpec<String> diffIndividualSpec =
            TRFTestingUtils.createTypedTableSpec(asList("A", "B", "C"), asList("X", "Y", "Z"));
        Map<Path, ReaderTableSpec<?>> differentIndividualSpecMap = new LinkedHashMap<>();
        differentIndividualSpecMap.put(m_path1, diffIndividualSpec);
        differentIndividualSpecMap.put(m_path2, SPEC2);
        final DefaultTableSpecConfig differentIndividualSpec =
            new DefaultTableSpecConfig(ROOT_PATH, tableSpec, differentIndividualSpecMap, productionPaths, originalNames,
                positions, keep, 3, ColumnFilterMode.UNION, true);

        assertFalse(tsc.equals(differentIndividualSpec));

        final DefaultTableSpecConfig differentPositions =
            new DefaultTableSpecConfig(ROOT_PATH, tableSpec, m_individualSpecs, productionPaths, originalNames,
                new int[]{0, 1, 2}, keep, 3, ColumnFilterMode.UNION, true);
        assertFalse(tsc.equals(differentPositions));

        final DefaultTableSpecConfig differentKeep =
            new DefaultTableSpecConfig(ROOT_PATH, tableSpec, m_individualSpecs, productionPaths, originalNames,
                positions, new boolean[]{true, true, true}, 3, ColumnFilterMode.UNION, true);
        assertFalse(tsc.equals(differentKeep));
    }

}
