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
 *   Dec 14, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.config;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.knime.filehandling.core.node.table.reader.TRFTestingUtils.a;
import static org.knime.filehandling.core.node.table.reader.config.DefaultTableSpecConfigTestingUtils.ITEMS;
import static org.knime.filehandling.core.node.table.reader.config.DefaultTableSpecConfigTestingUtils.PATH1;
import static org.knime.filehandling.core.node.table.reader.config.DefaultTableSpecConfigTestingUtils.PATH2;
import static org.knime.filehandling.core.node.table.reader.config.DefaultTableSpecConfigTestingUtils.REGISTRY;
import static org.knime.filehandling.core.node.table.reader.config.DefaultTableSpecConfigTestingUtils.ROOT_PATH;
import static org.knime.filehandling.core.node.table.reader.config.DefaultTableSpecConfigTestingUtils.getProductionPaths;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.convert.util.SerializeUtil;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.filehandling.core.node.table.reader.SourceGroup;
import org.knime.filehandling.core.node.table.reader.SpecMergeMode;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableSpecConfigSerializer.EmptyConfigID;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableSpecConfigSerializer.ExternalConfig;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableSpecConfigTestingUtils.TableSpecConfigBuilder;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.spec.ReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.ReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.util.MultiTableUtils;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Contains unit tests for {@link DefaultTableSpecConfigSerializer}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultTableSpecConfigSerializerTest {

    private static final TypedReaderColumnSpec<String> COL1 = TypedReaderColumnSpec.createWithName("A", "X", true);

    private static final TypedReaderColumnSpec<String> COL2 = TypedReaderColumnSpec.createWithName("B", "Y", true);

    private static final TypedReaderColumnSpec<String> COL3 = TypedReaderColumnSpec.createWithName("C", "Z", true);

    private static final TypedReaderTableSpec<String> SPEC1 = new TypedReaderTableSpec<>(asList(COL1, COL2));

    private static final TypedReaderTableSpec<String> SPEC2 = new TypedReaderTableSpec<>(asList(COL2, COL3));

    private static final TypedReaderTableSpec<String> INTERSECTION = new TypedReaderTableSpec<>(asList(COL2));

    private static final String MOST_GENERIC_EXTERNAL_TYPE = "X";

    @Mock
    private ProductionPathLoader m_prodPathLoader;

    @Mock
    private ConfigIDLoader m_configIDLoader;

    @Mock
    private ConfigID m_configID;

    @Mock
    private SourceGroup<String> m_sourceGroup;

    private Map<String, TypedReaderTableSpec<String>> m_individualSpecs;

    private DefaultTableSpecConfigSerializer<String> m_testInstance;

    /**
     * Run before each test.<br>
     * Initializes objects needed in the tests.
     *
     * @throws InvalidSettingsException never thrown
     */
    @Before
    public void init() throws InvalidSettingsException {
        m_individualSpecs = new LinkedHashMap<>();
        m_individualSpecs.put(PATH1, SPEC1);
        m_individualSpecs.put(PATH2, SPEC2);
        m_testInstance = new DefaultTableSpecConfigSerializer<>(REGISTRY, MOST_GENERIC_EXTERNAL_TYPE, m_configIDLoader);
    }

    private TableSpecConfigBuilder builder() {
        return new TableSpecConfigBuilder(m_configID);
    }

    /**
     * Tests that a {@link DefaultTableSpecConfig} can be correctly loaded after it has been saved to
     * {@link NodeSettings}.
     *
     * @throws InvalidSettingsException - does not happen
     */
    @Test
    public void testSaveLoad() throws InvalidSettingsException {
        when(m_configIDLoader.createFromSettings(any())).thenReturn(m_configID);

        final DataTableSpec outputSpec =
            new DataTableSpec("default", a("a", "b", "c"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));

        final ProductionPath[] prodPaths =
            getProductionPaths(a("X", "Y", "Z"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));
        final TableSpecConfig<String> cfg = builder().withNames("a", "b", "c").build();

        // tests save / load
        final NodeSettings s = new NodeSettings("origin");

        cfg.save(s);
        m_testInstance.validate(s);
        @SuppressWarnings("deprecation")
        final DefaultTableSpecConfig<String> load =
            m_testInstance.load(s, new ExternalConfig(SpecMergeMode.FAIL_ON_DIFFERING_SPECS, false));

        assertEquals(cfg, load);

        // test root path
        assertEquals(ROOT_PATH, load.getSourceGroupID());

        // test specs
        DataTableSpec loadedDataTableSpec = load.getDataTableSpec();
        assertEquals(outputSpec, loadedDataTableSpec);
        assertNotEquals(new DataTableSpec(new DataColumnSpecCreator("Blub", IntCell.TYPE).createSpec()),
            loadedDataTableSpec);

        // tests paths
        assertEquals(asList(ITEMS), load.getItems());

        // test reader specs
        for (final Entry<String, TypedReaderTableSpec<String>> entry : m_individualSpecs.entrySet()) {
            assertEquals(entry.getValue(), load.getSpec(entry.getKey()));
        }
    }

    /**
     * Tests loading settings.
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

        when(m_configIDLoader.createFromSettings(any())).thenReturn(m_configID);

        final NodeSettings settings =
            createSettings(tableSpec, productionPaths, m_individualSpecs, originalNames, positions, keep, true);

        @SuppressWarnings("deprecation")
        final TableSpecConfig<String> loaded =
            m_testInstance.load(settings, new ExternalConfig(SpecMergeMode.FAIL_ON_DIFFERING_SPECS, false));

        final TableSpecConfig<String> expected = builder()//
            .withPositions(2, 1, 0)//
            .withKeep(true, false, true)//
            .build();
        assertEquals(expected, loaded);
    }

    /**
     * Tests loading from settings stored with 4.3.0.
     *
     * @throws InvalidSettingsException never thrown
     */
    @Test
    public void testLoadPre431() throws InvalidSettingsException {
        final ProductionPath[] productionPaths =
            getProductionPaths(a("X", "Y", "Z"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));
        String[] originalNames = a("A", "B", "C");
        final DataTableSpec tableSpec =
            new DataTableSpec(originalNames, a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));
        int[] positions = new int[]{2, 1, 0};
        boolean[] keep = new boolean[]{true, false, true};

        final NodeSettings settings =
            createSettings(tableSpec, productionPaths, m_individualSpecs, originalNames, positions, keep, false);

        @SuppressWarnings("deprecation")
        final TableSpecConfig<String> loaded =
            m_testInstance.load(settings, new ExternalConfig(SpecMergeMode.FAIL_ON_DIFFERING_SPECS, false));

        final TableSpecConfig<String> expected = new TableSpecConfigBuilder(EmptyConfigID.INSTANCE)//
            .withPositions(2, 1, 0)//
            .withKeep(true, false, true)//
            .build();
        assertEquals(expected, loaded);
    }

    /**
     * Tests that configs saved prior to 4.3 can still be loaded.
     *
     * @throws InvalidSettingsException never thrown
     */
    @Test
    public void testLoadPre43() throws InvalidSettingsException {
        final NodeSettings settings = create42Settings();

        @SuppressWarnings("deprecation")
        final TableSpecConfig<String> loaded =
            m_testInstance.load(settings, new ExternalConfig(SpecMergeMode.FAIL_ON_DIFFERING_SPECS, false));

        final ProductionPath[] prodPaths =
            getProductionPaths(a(MOST_GENERIC_EXTERNAL_TYPE, "Y", MOST_GENERIC_EXTERNAL_TYPE),
                a(StringCell.TYPE, IntCell.TYPE, StringCell.TYPE));

        TypedReaderColumnSpec<String> col3WithMostGenericType = TypedReaderColumnSpec.createWithName("C", "X", true);

        final TableSpecConfig<String> expected = new TableSpecConfigBuilder(EmptyConfigID.INSTANCE)//
            .withKeep(false, true, false)//
            .withProductionPaths(prodPaths)//
            .withSpecs(SPEC1, new TypedReaderTableSpec<>(asList(COL2, col3WithMostGenericType)))//
            .withCols(COL1, COL2, col3WithMostGenericType)//
            .withRawSpec(
                new RawSpec<>(new TypedReaderTableSpec<>(asList(COL1, COL2, col3WithMostGenericType)), INTERSECTION))//
            .build();
        assertEquals(expected, loaded);
    }

    /**
     * Tests if {@link DefaultTableSpecConfigSerializer#validate(NodeSettingsRO)} succeeds for valid settings.
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
            createSettings(tableSpec, productionPaths, m_individualSpecs, originalNames, positions, keep, false);

        m_testInstance.validate(settings);
    }

    /**
     * Tests that old settings (written with 4.2) can still be validated.
     *
     * @throws InvalidSettingsException never thrown
     */
    @Test
    public void testValidateOldSettings() throws InvalidSettingsException {
        final NodeSettings settings = create42Settings();
        m_testInstance.validate(settings);
    }

    private NodeSettings create42Settings() {
        // only intersection is stored i.e. the prodPaths (and therefore types) of columns A and C are not stored
        final ProductionPath[] productionPaths = getProductionPaths(a("Y"), a(IntCell.TYPE));
        final DataTableSpec tableSpec = new DataTableSpec(a("B"), a(IntCell.TYPE));
        return createSettings(tableSpec, productionPaths, m_individualSpecs, null, null, null, false);
    }

    private static NodeSettings createSettings(final DataTableSpec tableSpec, final ProductionPath[] productionPaths,
        final Map<String, TypedReaderTableSpec<String>> individualSpecs, final String[] originalNames,
        final int[] positions, final boolean[] keep, final boolean addconfigIDSubsettings) {
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
        if (addconfigIDSubsettings) {
            settings.addNodeSettings("config_id");
        }
        return settings;
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
}
