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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.filehandling.core.node.table.reader.spec.ReaderTableSpec;

/**
 *
 * Contains test for the {@link TableSpecConfig}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 *
 */
public class TableSpecConfigTest {

    /**
     * Tests that a {@link TableSpecConfig} can be correctly be loaded after it has been saved to {@link NodeSettings}.
     *
     * @throws InvalidSettingsException - does not happen
     */
    @Test
    public void testSaveLoad() throws InvalidSettingsException {
        // create a TableSpecConfig
        final String rootPath = "root";
        final DataTableSpec outputSpec = TableSpecConfigUtils.createDataTableSpec("a", "b");
        final ProducerRegistry<String, TableSpecConfigUtils> registry = TableSpecConfigUtils.getProducerRegistry();
        final ProductionPath[] prodPaths = registry.getAvailableProductionPaths().toArray(new ProductionPath[0]);
        final Map<Path, ReaderTableSpec<?>> individualSpecs = new HashMap<>();
        final Path p1 = TableSpecConfigUtils.mockPath("first");
        final Path p2 = TableSpecConfigUtils.mockPath("second");
        individualSpecs.put(p1, TableSpecConfigUtils.createSpec("A", "B"));
        individualSpecs.put(p2, TableSpecConfigUtils.createSpec("C", "E"));
        final TableSpecConfig cfg = new TableSpecConfig(rootPath, outputSpec, individualSpecs, prodPaths);

        // tests save / load
        final NodeSettings s = new NodeSettings("origin");
        cfg.save(s);
        TableSpecConfig.validate(s, registry);
        final TableSpecConfig load = TableSpecConfig.load(s, registry);

        // test root path
        assertTrue(load.isConfiguredWith(rootPath));
        assertFalse(load.isConfiguredWith("foo"));

        // test specs
        assertEquals(outputSpec, load.getDataTableSpec());
        assertNotEquals(new DataTableSpec(new DataColumnSpecCreator("Blub", IntCell.TYPE).createSpec()),
            load.getDataTableSpec());

        // test production paths
        assertArrayEquals(prodPaths, load.getProductionPaths());
        prodPaths[0] = prodPaths[1];
        assertNotEquals(prodPaths, load.getProductionPaths());

        // tests paths
        assertTrue(load.isConfiguredWith(new ArrayList<>(individualSpecs.keySet())));
        assertFalse(load.isConfiguredWith(Arrays.asList(p1)));
        assertFalse(load.isConfiguredWith(Arrays.asList(p1, p2, p1)));

        // test reader specs
        for (final Entry<Path, ReaderTableSpec<?>> entry : individualSpecs.entrySet()) {
            assertEquals(entry.getValue(), load.getSpec(entry.getKey().toString()));
        }
        assertEquals(cfg, load);
    }

    /**
     * Tests that a {@link TableSpecConfig} can be initialized when production paths are empty.
     *
     * @throws InvalidSettingsException - does not happen
     */
    @Test
    public void testEmptyProdPathCreation() throws InvalidSettingsException {
        // create a TableSpecConfig
        final String rootPath = "root";
        final DataTableSpec outputSpec = TableSpecConfigUtils.createDataTableSpec("a", "b");
        final ProducerRegistry<String, TableSpecConfigUtils> registry = TableSpecConfigUtils.getProducerRegistry();
        final ProductionPath[] prodPaths = new ProductionPath[0];
        final Map<Path, ReaderTableSpec<?>> individualSpecs = new HashMap<>();
        final Path p1 = TableSpecConfigUtils.mockPath("first");
        final Path p2 = TableSpecConfigUtils.mockPath("second");
        individualSpecs.put(p1, TableSpecConfigUtils.createSpec("A", "B"));
        individualSpecs.put(p2, TableSpecConfigUtils.createSpec("C", "E"));
        final TableSpecConfig cfg = new TableSpecConfig(rootPath, outputSpec, individualSpecs, prodPaths);

        // tests save / load
        final NodeSettings s = new NodeSettings("origin");
        cfg.save(s);
        TableSpecConfig.validate(s, registry);
        final TableSpecConfig load = TableSpecConfig.load(s, registry);
        assertEquals(cfg, load);
    }
}
