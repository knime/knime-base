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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
import org.knime.filehandling.core.node.ImmutableTableTransformation;
import org.knime.filehandling.core.node.table.reader.ImmutableColumnTransformation;
import org.knime.filehandling.core.node.table.reader.SourceGroup;
import org.knime.filehandling.core.node.table.reader.SpecMergeMode;
import org.knime.filehandling.core.node.table.reader.TRFTestingUtils;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.filehandling.core.node.table.reader.selector.ColumnTransformation;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
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
@SuppressWarnings({"javadoc", "deprecation"})
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

    private Map<String, TypedReaderTableSpec<String>> m_individualSpecs;

    private static final String PATH1 = "first";

    private static final String PATH2 = "second";

    private static final String[] ITEMS = {PATH1, PATH2};

    private static final List<TypedReaderTableSpec<String>> INDIVIDUAL_SPECS = asList(SPEC1, SPEC2);

    @Mock
    private SourceGroup<String> m_sourceGroup;

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
        m_individualSpecs = new LinkedHashMap<>();
        m_individualSpecs.put(PATH1, SPEC1);
        m_individualSpecs.put(PATH2, SPEC2);
    }

    /**
     * Tests {@link DefaultTableSpecConfig#createFromTransformationModel(String, TableTransformation, Map)}.
     */
    @Test
    public void testCreateFromTransformationModel() {
        @SuppressWarnings("unchecked")
        final TableTransformation<String> transformationModel = mock(TableTransformation.class);

        when(transformationModel.getRawSpec()).thenReturn(RAW_SPEC);

        when(transformationModel.getColumnFilterMode()).thenReturn(ColumnFilterMode.UNION);
        when(transformationModel.keepUnknownColumns()).thenReturn(true);
        when(transformationModel.getPositionForUnknownColumns()).thenReturn(3);

        when(transformationModel.stream()).thenReturn(Stream.of(m_trans1, m_trans2, m_trans3));

        final ProductionPath p1 = TRFTestingUtils.mockProductionPath();
        final ProductionPath p2 = TRFTestingUtils.mockProductionPath();
        final ProductionPath p3 = TRFTestingUtils.mockProductionPath();

        new TransformationStubber<>(m_trans1)//
            .withName("A")//
            .withKeep(true).withPosition(2)//
            .withProductionPath(p1)//
            .withExternalSpec(COL1);
        new TransformationStubber<>(m_trans2)//
            .withName("B")//
            .withKeep(false)//
            .withPosition(0)//
            .withProductionPath(p2)//
            .withExternalSpec(COL2);
        new TransformationStubber<>(m_trans3)//
            .withName("G")//
            .withKeep(true)//
            .withPosition(1)//
            .withProductionPath(p3)//
            .withExternalSpec(COL3);

        final TableSpecConfig<String> config =
            DefaultTableSpecConfig.createFromTransformationModel(ROOT_PATH, m_individualSpecs, transformationModel);

        final TableSpecConfig<String> expected = new ConfigBuilder()//
            .withNames("A", "B", "G")//
            .withPositions(2, 0, 1)//
            .withProductionPaths(a(p1, p2, p3))//
            .withKeep(true, false, true)//
            .build();
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

    private void stubSourceGroup(final String id, final String... items) {
        when(m_sourceGroup.getID()).thenReturn(id);
        when(m_sourceGroup.size()).thenReturn(items.length);
        when(m_sourceGroup.stream()).thenReturn(Arrays.stream(items));
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

        final DataTableSpec outputSpec =
            new DataTableSpec("default", a("a", "b", "c"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));

        final ProductionPath[] prodPaths =
            getProductionPaths(a("X", "Y", "Z"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));
        final TableSpecConfig<String> cfg = new ConfigBuilder().withNames("a", "b", "c").build();

        // tests save / load
        final NodeSettings s = new NodeSettings("origin");
        cfg.save(s);
        DefaultTableSpecConfig.validate(s, REGISTRY);
        final TableSpecConfig<String> load =
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
        stubSourceGroup(ROOT_PATH, m_individualSpecs.keySet().toArray(new String[0]));
        assertTrue(load.isConfiguredWith(m_sourceGroup));
        stubSourceGroup(ROOT_PATH, PATH1);
        assertFalse(load.isConfiguredWith(m_sourceGroup));
        stubSourceGroup(ROOT_PATH, PATH1, PATH2, PATH1);
        assertFalse(load.isConfiguredWith(m_sourceGroup));

        // test reader specs
        for (final Entry<String, TypedReaderTableSpec<String>> entry : m_individualSpecs.entrySet()) {
            assertEquals(entry.getValue(), load.getSpec(entry.getKey()));
        }
    }

    @Test
    public void testGetTransformationModel() {
        final ProductionPath[] expectedProdPaths =
            getProductionPaths(a("X", "Y", "Z"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));

        final TableSpecConfig<String> tsc = new ConfigBuilder()//
            .withNames("A", "B", "G")//
            .withPositions(1, 2, 0)//
            .withKeep(true, false, true)//
            .build();

        final TableTransformation<String> tm = tsc.getTransformationModel();
        assertEquals(RAW_SPEC, tm.getRawSpec());

        checkTransformation(tm.getTransformation(COL1), COL1, "A", expectedProdPaths[0], 1, true);
        checkTransformation(tm.getTransformation(COL2), COL2, "B", expectedProdPaths[1], 2, false);
        checkTransformation(tm.getTransformation(COL3), COL3, "G", expectedProdPaths[2], 0, true);
    }

    private static ProductionPath[] getProductionPaths(final String[] externalTypes, final DataType[] knimeTypes) {
        return IntStream.range(0, externalTypes.length)
            .mapToObj(i -> REGISTRY.getAvailableProductionPaths(externalTypes[i]).stream()
                .filter(p -> p.getConverterFactory().getDestinationType().equals(knimeTypes[i])).findFirst().get())
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

        final DefaultTableSpecConfig<String> config = new ConfigBuilder()//
            .withPositions(1, 0, 2)//
            .withKeep(true, false, true)//
            .withNames("A", "B", "G")//
            .build();

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
                SerializeUtil.loadProductionPath(productionPathSettings, REGISTRY, "production_path_" + i).get());//NOSONAR
        }

        assertArrayEquals(ORIGINAL_NAMES, settings.getStringArray("original_names_Internals"));
        assertArrayEquals(positionalMapping, settings.getIntArray("positional_mapping_Internals"));
        assertArrayEquals(keep, settings.getBooleanArray("keep_Internals"));
    }

    @Test
    public void testLoadPre43() throws InvalidSettingsException {
        final NodeSettings settings = create42Settings();

        final TableSpecConfig<String> loaded = DefaultTableSpecConfig.load(settings, REGISTRY,
            MOST_GENERIC_EXTERNAL_TYPE, SpecMergeMode.FAIL_ON_DIFFERING_SPECS);

        final ProductionPath[] prodPaths =
            getProductionPaths(a(MOST_GENERIC_EXTERNAL_TYPE, "Y", MOST_GENERIC_EXTERNAL_TYPE),
                a(StringCell.TYPE, IntCell.TYPE, StringCell.TYPE));

        TypedReaderColumnSpec<String> col3WithMostGenericType = TypedReaderColumnSpec.createWithName("C", "X", true);

        final TableSpecConfig<String> expected = new ConfigBuilder()//
            .withKeep(false, true, false)//
            .withProductionPaths(prodPaths)//
            .withSpecs(SPEC1, new TypedReaderTableSpec<>(asList(COL2, col3WithMostGenericType)))//
            .withCols(COL1, COL2, col3WithMostGenericType)//
            .withRawSpec(
                new RawSpec<>(new TypedReaderTableSpec<>(asList(COL1, COL2, col3WithMostGenericType)), INTERSECTION))//
            .build();
        assertEquals(expected, loaded);
    }

    private NodeSettings create42Settings() {
        // only intersection is stored i.e. the prodPaths (and therefore types) of columns A and C are not stored
        final ProductionPath[] productionPaths = getProductionPaths(a("Y"), a(IntCell.TYPE));
        final DataTableSpec tableSpec = new DataTableSpec(a("B"), a(IntCell.TYPE));
        return createSettings(tableSpec, productionPaths, m_individualSpecs, null, null, null);
    }

    private LinkedHashMap<String, ReaderTableSpec<?>> getIndividualSpecsWithoutTypes() {
        return m_individualSpecs.entrySet().stream()
            .collect(toMap(Entry::getKey, e -> ReaderTableSpec.createReaderTableSpec(e.getValue().stream()//
                .map(s -> (ReaderColumnSpec)s)//
                .map(MultiTableUtils::getNameAfterInit)//
                .collect(toList())), (x, y) -> x, LinkedHashMap::new));
    }

    private static void saveProductionPaths(final NodeSettingsWO settings, final ProductionPath[] productionPaths) {
        int i = 0;
        for (final ProductionPath pP : productionPaths) {
            SerializeUtil.storeProductionPath(pP, settings, "production_path_" + i);
            i++;
        }
        settings.addInt("num_production_paths_Internals", i);
    }

    private static void saveIndividualSpecs(final Map<String, TypedReaderTableSpec<String>> individualSpecs,
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
        final ProductionPath[] productionPaths =
            getProductionPaths(a("X", "Y", "Z"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));
        String[] originalNames = a("A", "B", "C");
        final DataTableSpec tableSpec =
            new DataTableSpec(originalNames, a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));
        int[] positions = new int[]{2, 1, 0};
        boolean[] keep = new boolean[]{true, false, true};

        final NodeSettings settings =
            createSettings(tableSpec, productionPaths, m_individualSpecs, originalNames, positions, keep);

        final TableSpecConfig<String> loaded = DefaultTableSpecConfig.load(settings, REGISTRY,
            MOST_GENERIC_EXTERNAL_TYPE, SpecMergeMode.FAIL_ON_DIFFERING_SPECS);

        getIndividualSpecsWithoutTypes();
        final TableSpecConfig<String> expected =
            new ConfigBuilder().withPositions(2, 1, 0).withKeep(true, false, true).build();
        assertEquals(expected, loaded);
    }

    /**
     * Tests {@link DefaultTableSpecConfig#isConfiguredWith(List)},
     * {@link DefaultTableSpecConfig#isConfiguredWith(String)} and
     * {@link DefaultTableSpecConfig#isConfiguredWith(String, List)}.
     */
    @Test
    public void testIsConfiguredWith() {
        final TableSpecConfig<String> tsc = new ConfigBuilder().build();
        assertTrue(tsc.isConfiguredWith(ROOT_PATH));
        assertFalse(tsc.isConfiguredWith("foobar"));

        stubSourceGroup(ROOT_PATH, PATH1, PATH2);
        assertTrue(tsc.isConfiguredWith(m_sourceGroup));
        stubSourceGroup(ROOT_PATH, PATH1);
        assertFalse(tsc.isConfiguredWith(m_sourceGroup));
        stubSourceGroup(ROOT_PATH, PATH1, PATH2, "foo");
        assertFalse(tsc.isConfiguredWith(m_sourceGroup));
        stubSourceGroup(ROOT_PATH, PATH1, PATH2, PATH1);
        assertFalse(tsc.isConfiguredWith(m_sourceGroup));

        stubSourceGroup("foobar", PATH1, PATH2);
        assertFalse(tsc.isConfiguredWith(m_sourceGroup));
    }

    @Test
    public void testGetDataTableSpec() {
        final TableSpecConfig<String> tsc =
            new ConfigBuilder().withPositions(2, 0, 1).withKeep(true, false, true).build();
        final DataTableSpec expected = new DataTableSpec("default", a("C", "A"), a(DoubleCell.TYPE, StringCell.TYPE));
        assertEquals(expected, tsc.getDataTableSpec());
    }

    @Test
    public void testGetPaths() {
        final TableSpecConfig<String> tsc = new ConfigBuilder().build();
        assertEquals(asList("first", "second"), tsc.getItems());
    }

    @Test
    public void testGetSpec() {
        final TableSpecConfig<String> tsc = new ConfigBuilder().build();
        assertEquals(m_individualSpecs.get(PATH1), tsc.getSpec("first"));
        assertEquals(m_individualSpecs.get(PATH2), tsc.getSpec("second"));
    }

    @Test
    public void testGetProductionPaths() {
        final TableSpecConfig<String> tsc =
            new ConfigBuilder().withKeep(true, false, true).withPositions(2, 0, 1).build();
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
        final Map<String, TypedReaderTableSpec<String>> individualSpecs, final String[] originalNames,
        final int[] positions, final boolean[] keep) {
        final NodeSettings settings = new NodeSettings("test");
        settings.addString("root_path_Internals", ROOT_PATH);
        tableSpec.save(settings.addNodeSettings("datatable_spec_Internals"));
        settings.addStringArray("file_paths_Internals", individualSpecs.keySet().stream().toArray(String[]::new));
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
    @SuppressWarnings("unlikely-arg-type")
    @Test
    public void testEqualsHashCode() {
        final DefaultTableSpecConfig<String> tsc = new ConfigBuilder().build();

        assertTrue(tsc.equals(tsc));//NOSONAR
        assertFalse(tsc.equals(null));//NOSONAR
        assertEquals(tsc.hashCode(), tsc.hashCode());

        assertFalse(tsc.equals("SomeString"));//NOSONAR

        final DefaultTableSpecConfig<String> equal = new ConfigBuilder().build();

        assertTrue(tsc.equals(equal));
        assertEquals(tsc.hashCode(), equal.hashCode());

        final DefaultTableSpecConfig<String> differentRoot = new ConfigBuilder().withRoot("different_root").build();
        assertFalse(tsc.equals(differentRoot));

        final DefaultTableSpecConfig<String> differentNames = new ConfigBuilder().withNames("K", "L", "C").build();
        assertFalse(tsc.equals(differentNames));

        DataType[] types = a(LongCell.TYPE, IntCell.TYPE, DoubleCell.TYPE);
        final DefaultTableSpecConfig<?> differentTypes =
            new ConfigBuilder().withProductionPaths(getProductionPaths(a("X", "Y", "Z"), types)).build();
        assertFalse(tsc.equals(differentTypes));

        Map<String, TypedReaderTableSpec<String>> differentPathMap = new LinkedHashMap<>();
        differentPathMap.put("foo", SPEC1);
        differentPathMap.put(PATH2, SPEC2);

        final DefaultTableSpecConfig<String> differentPaths = new ConfigBuilder().withItems("foo", PATH2).build();
        assertFalse(tsc.equals(differentPaths));

        final TypedReaderTableSpec<String> diffIndividualSpec =
            TRFTestingUtils.createTypedTableSpec(asList("A", "B", "C"), asList("X", "Y", "Z"));
        Map<String, TypedReaderTableSpec<String>> differentIndividualSpecMap = new LinkedHashMap<>();
        differentIndividualSpecMap.put(PATH1, diffIndividualSpec);
        differentIndividualSpecMap.put(PATH2, SPEC2);
        final DefaultTableSpecConfig<?> differentIndividualSpec =
            new ConfigBuilder().withSpecs(diffIndividualSpec, SPEC2).build();

        assertFalse(tsc.equals(differentIndividualSpec));

        final DefaultTableSpecConfig<?> differentPositions = new ConfigBuilder().withPositions(1, 0, 2).build();
        assertFalse(tsc.equals(differentPositions));

        final DefaultTableSpecConfig<?> differentKeep = new ConfigBuilder().withKeep(true, false, true).build();
        assertFalse(tsc.equals(differentKeep));
    }

    private static class ConfigBuilder {

        private String m_root = ROOT_PATH;

        private String[] m_items = ITEMS.clone();

        private List<TypedReaderTableSpec<String>> m_individualSpecs = new ArrayList<>(INDIVIDUAL_SPECS);

        private ColumnFilterMode m_columnFilterMode = ColumnFilterMode.UNION;

        private boolean m_keepUnknown = true;

        private boolean m_enforceTypes = false;

        private int m_unknownColPosition = 3;

        private List<TypedReaderColumnSpec<String>> m_cols = asList(COL1, COL2, COL3);

        private RawSpec<String> m_rawSpec = RAW_SPEC;

        ProductionPath[] m_prodPaths =
            getProductionPaths(a("X", "Y", "Z"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));

        String[] m_names = a("A", "B", "C");

        Integer[] m_positions = a(0, 1, 2);

        Boolean[] m_keeps = a(true, true, true);

        DefaultTableSpecConfig<String> build() {
            final List<ImmutableColumnTransformation<String>> colTrans =
                createColTrans(m_cols, asList(m_prodPaths), asList(m_names), asList(m_positions), asList(m_keeps));
            final ImmutableTableTransformation<String> tableTrans = new ImmutableTableTransformation<>(colTrans,
                m_rawSpec, m_columnFilterMode, m_unknownColPosition, m_keepUnknown, m_enforceTypes);
            return new DefaultTableSpecConfig<>(m_root, m_items, m_individualSpecs, tableTrans);
        }

        private static List<ImmutableColumnTransformation<String>> createColTrans(
            final List<TypedReaderColumnSpec<String>> specs, final List<ProductionPath> prodPaths,
            final List<String> names, final List<Integer> positions, final List<Boolean> keep) {
            return IntStream.range(0, specs.size())//
                .mapToObj(i -> new ImmutableColumnTransformation<String>(specs.get(i), prodPaths.get(i), keep.get(i),
                    positions.get(i), names.get(i)))//
                .collect(toList());
        }

        ConfigBuilder withRoot(final String rootItem) {
            m_root = rootItem;
            return this;
        }

        ConfigBuilder withItems(final String... items) {
            m_items = items.clone();
            return this;
        }

        ConfigBuilder withRawSpec(final RawSpec<String> rawSpec) {
            m_rawSpec = rawSpec;
            return this;
        }

        @SafeVarargs
        final ConfigBuilder withSpecs(final TypedReaderTableSpec<String>... specs) {
            m_individualSpecs = asList(specs);
            return this;
        }

        @SafeVarargs
        final ConfigBuilder withCols(final TypedReaderColumnSpec<String>... cols) {
            m_cols = asList(cols);
            return this;
        }

        ConfigBuilder withNames(final String... names) {
            m_names = names.clone();
            return this;
        }

        ConfigBuilder withProductionPaths(final ProductionPath[] productionPaths) {
            m_prodPaths = productionPaths.clone();
            return this;
        }

        ConfigBuilder withPositions(final Integer... positions) {
            m_positions = positions.clone();
            return this;
        }

        ConfigBuilder withKeep(final Boolean... keep) {
            m_keeps = keep.clone();
            return this;
        }

    }

}
