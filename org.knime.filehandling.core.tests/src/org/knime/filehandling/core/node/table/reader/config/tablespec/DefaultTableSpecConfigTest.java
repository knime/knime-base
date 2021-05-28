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
package org.knime.filehandling.core.node.table.reader.config.tablespec;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.knime.filehandling.core.node.table.reader.TRFTestingUtils.a;
import static org.knime.filehandling.core.node.table.reader.TRFTestingUtils.checkTransformation;
import static org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigTestingUtils.COL1;
import static org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigTestingUtils.COL2;
import static org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigTestingUtils.COL3;
import static org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigTestingUtils.PATH1;
import static org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigTestingUtils.PATH2;
import static org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigTestingUtils.RAW_SPEC;
import static org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigTestingUtils.ROOT_PATH;
import static org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigTestingUtils.SPEC1;
import static org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigTestingUtils.SPEC2;
import static org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigTestingUtils.getProductionPaths;
import static org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigTestingUtils.stub;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.filehandling.core.node.table.reader.SourceGroup;
import org.knime.filehandling.core.node.table.reader.TRFTestingUtils;
import org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigTestingUtils.TableSpecConfigBuilder;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.filehandling.core.node.table.reader.selector.ColumnTransformation;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformation;
import org.knime.filehandling.core.node.table.reader.selector.UnknownColumnsTransformation;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Contains test for the {@link DefaultTableSpecConfig}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 *
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"javadoc"})
public class DefaultTableSpecConfigTest {

    private Map<String, TypedReaderTableSpec<String>> m_individualSpecs;

    @Mock
    private SourceGroup<String> m_sourceGroup;

    @Mock
    private ConfigID m_configID;

    @Mock
    private ColumnTransformation<String> m_trans1;

    @Mock
    private ColumnTransformation<String> m_trans2;

    @Mock
    private ColumnTransformation<String> m_trans3;

    private TableSpecConfigBuilder builder() {
        return new TableSpecConfigBuilder(m_configID);
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
     * Tests {@link DefaultTableSpecConfig#createFromTransformationModel(String, String, TableTransformation, Map)}.
     */
    @Test
    public void testCreateFromTransformationModel() {
        @SuppressWarnings("unchecked")
        final TableTransformation<String> transformationModel = mock(TableTransformation.class);

        final UnknownColumnsTransformation unknownColsTrans = mock(UnknownColumnsTransformation.class);
        when(unknownColsTrans.keep()).thenReturn(true);
        when(unknownColsTrans.getPosition()).thenReturn(3);

        when(transformationModel.getRawSpec()).thenReturn(RAW_SPEC);

        when(transformationModel.getColumnFilterMode()).thenReturn(ColumnFilterMode.UNION);
        when(transformationModel.getTransformationForUnknownColumns()).thenReturn(unknownColsTrans);

        when(transformationModel.stream()).thenReturn(Stream.of(m_trans1, m_trans2, m_trans3));

        final ProductionPath p1 = TRFTestingUtils.mockProductionPath();
        final ProductionPath p2 = TRFTestingUtils.mockProductionPath();
        final ProductionPath p3 = TRFTestingUtils.mockProductionPath();

        stub(m_trans1)//
            .withName("A")//
            .withKeep(true).withPosition(2)//
            .withProductionPath(p1)//
            .withExternalSpec(COL1);
        stub(m_trans2)//
            .withName("B")//
            .withKeep(false)//
            .withPosition(0)//
            .withProductionPath(p2)//
            .withExternalSpec(COL2);
        stub(m_trans3)//
            .withName("G")//
            .withKeep(true)//
            .withPosition(1)//
            .withProductionPath(p3)//
            .withExternalSpec(COL3);

        final TableSpecConfig<String> config = DefaultTableSpecConfig.createFromTransformationModel(ROOT_PATH,
            m_configID, m_individualSpecs, transformationModel);

        final TableSpecConfig<String> expected = builder()//
            .withNames("A", "B", "G")//
            .withPositions(2, 0, 1)//
            .withProductionPaths(a(p1, p2, p3))//
            .withKeep(true, false, true)//
            .build();
        assertEquals(expected, config);
    }

    private void stubSourceGroup(final String id, final String... items) {
        when(m_sourceGroup.getID()).thenReturn(id);
        when(m_sourceGroup.size()).thenReturn(items.length);
        when(m_sourceGroup.stream()).thenReturn(Arrays.stream(items));
    }

    @Test
    public void testGetTransformationModel() {
        final ProductionPath[] expectedProdPaths =
            getProductionPaths(a("X", "Y", "Z"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));

        final TableSpecConfig<String> tsc = builder()//
            .withNames("A", "B", "G")//
            .withPositions(1, 2, 0)//
            .withKeep(true, false, true)//
            .build();

        final TableTransformation<String> tm = tsc.getTableTransformation();
        assertEquals(RAW_SPEC, tm.getRawSpec());

        checkTransformation(tm.getTransformation(COL1), COL1, "A", expectedProdPaths[0], 1, true);
        checkTransformation(tm.getTransformation(COL2), COL2, "B", expectedProdPaths[1], 2, false);
        checkTransformation(tm.getTransformation(COL3), COL3, "G", expectedProdPaths[2], 0, true);
    }

    @Test
    public void testIsConfiguredWithconfigIDAndSourceGroupSucceeds() {
        final TableSpecConfig<String> tsc = builder().build();
        when(m_configID.isCompatible(m_configID)).thenReturn(true);
        stubSourceGroup(ROOT_PATH, PATH1, PATH2);
        assertTrue(tsc.isConfiguredWith(m_configID, m_sourceGroup));
    }

    @Test
    public void testIsConfiguredWithconfigIDAndSourceGroupDifferentConfig() {
        final TableSpecConfig<String> tsc = builder().build();
        stubSourceGroup(ROOT_PATH, PATH1, PATH2);
        ConfigID otherID = mock(ConfigID.class);
        assertFalse(tsc.isConfiguredWith(otherID, m_sourceGroup));
    }

    @Test
    public void testIsConfiguredWithconfigIDAndSourceGroupDifferentItems() {
        final TableSpecConfig<String> tsc = builder().build();
        stubSourceGroup(ROOT_PATH, PATH1, "foo");
        assertFalse(tsc.isConfiguredWith(m_configID, m_sourceGroup));
    }

    public void testIsConfiguredWithConfigIDAndSourceGroupDifferentSourceGroupID() {
        final TableSpecConfig<String> tsc = builder().build();
        stubSourceGroup("other", PATH1, PATH2);
        assertFalse(tsc.isConfiguredWith(m_configID, m_sourceGroup));
    }

    @Test
    public void testIsConfiguredWithconfigIDAndSourceGroupIDSucceeds() {
        final TableSpecConfig<String> tsc = builder().build();
        when(m_configID.isCompatible(m_configID)).thenReturn(true);
        assertTrue(tsc.isConfiguredWith(m_configID, ROOT_PATH));
    }

    @Test
    public void testIsConfiguredWithconfigIDAndSourceGroupIDDifferentConfig() {
        final TableSpecConfig<String> tsc = builder().build();
        final ConfigID otherconfigID = mock(ConfigID.class);
        assertFalse(tsc.isConfiguredWith(otherconfigID, ROOT_PATH));
    }

    @Test
    public void testIsConfiguredWithconfigIDAndSourceGroupIDDifferentSourceGroupID() {
        final TableSpecConfig<String> tsc = builder().build();
        assertFalse(tsc.isConfiguredWith(m_configID, "foo"));
    }

    @Test
    public void testGetDataTableSpec() {
        final TableSpecConfig<String> tsc = builder().withPositions(2, 0, 1).withKeep(true, false, true).build();
        final DataTableSpec expected = new DataTableSpec("default", a("C", "A"), a(DoubleCell.TYPE, StringCell.TYPE));
        assertEquals(expected, tsc.getDataTableSpec());
    }

    @Test
    public void testGetPaths() {
        final TableSpecConfig<String> tsc = builder().build();
        assertEquals(asList("first", "second"), tsc.getItems());
    }

    @Test
    public void testGetSpec() {
        final TableSpecConfig<String> tsc = builder().build();
        assertEquals(m_individualSpecs.get(PATH1), tsc.getSpec("first"));
        assertEquals(m_individualSpecs.get(PATH2), tsc.getSpec("second"));
    }

    /**
     * Tests the equals and hashCode implementations.
     */
    @SuppressWarnings("unlikely-arg-type")
    @Test
    public void testEqualsHashCode() {
        final DefaultTableSpecConfig<String> tsc = builder().build();

        assertTrue(tsc.equals(tsc));//NOSONAR
        assertFalse(tsc.equals(null));//NOSONAR
        assertEquals(tsc.hashCode(), tsc.hashCode());

        assertFalse(tsc.equals("SomeString"));//NOSONAR

        final DefaultTableSpecConfig<String> equal = builder().build();

        assertTrue(tsc.equals(equal));
        assertEquals(tsc.hashCode(), equal.hashCode());

        final DefaultTableSpecConfig<String> differentRoot = builder().withRoot("different_root").build();
        assertFalse(tsc.equals(differentRoot));

        final DefaultTableSpecConfig<String> differentConfig = new TableSpecConfigBuilder(mock(ConfigID.class)).build();
        assertFalse(tsc.equals(differentConfig));

        final DefaultTableSpecConfig<String> differentNames = builder().withNames("K", "L", "C").build();
        assertFalse(tsc.equals(differentNames));

        DataType[] types = a(LongCell.TYPE, IntCell.TYPE, DoubleCell.TYPE);
        final DefaultTableSpecConfig<?> differentTypes =
            builder().withProductionPaths(getProductionPaths(a("X", "Y", "Z"), types)).build();
        assertFalse(tsc.equals(differentTypes));

        final DefaultTableSpecConfig<String> differentPaths = builder().withItems("foo", PATH2).build();
        assertFalse(tsc.equals(differentPaths));

        final TypedReaderTableSpec<String> diffIndividualSpec =
            TRFTestingUtils.createTypedTableSpec(asList("A", "B", "C"), asList("X", "Y", "Z"));
        final DefaultTableSpecConfig<?> differentIndividualSpec =
            builder().withSpecs(diffIndividualSpec, SPEC2).build();

        assertFalse(tsc.equals(differentIndividualSpec));

        final DefaultTableSpecConfig<?> differentPositions = builder().withPositions(1, 0, 2).build();
        assertFalse(tsc.equals(differentPositions));

        final DefaultTableSpecConfig<?> differentKeep = builder().withKeep(true, false, true).build();
        assertFalse(tsc.equals(differentKeep));
    }

}
