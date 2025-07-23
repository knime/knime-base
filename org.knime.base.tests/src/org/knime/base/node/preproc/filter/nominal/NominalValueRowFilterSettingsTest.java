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
 *   Jan 29, 2024 (hornm): created
 */
package org.knime.base.node.preproc.filter.nominal;

import java.io.FileInputStream;
import java.io.IOException;

import org.knime.base.node.preproc.filter.nominal.NominalValueRowCommonSettings.NominalValueRowFilterNodeSettings;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.testing.node.dialog.DefaultNodeSettingsSnapshotTest;
import org.knime.testing.node.dialog.SnapshotTestConfiguration;

/**
 * {@link DefaultNodeSettingsSnapshotTest} for the {@link NominalValueRowFilterNodeSettings}.
 *
 * @author Jakob Sanowski, KNIME GmbH, Konstanz, Germany
 */
final class NominalValueRowFilterSettingsTest extends DefaultNodeSettingsSnapshotTest {

    protected NominalValueRowFilterSettingsTest() {
        super(getConfig());
    }

    private static SnapshotTestConfiguration getConfig() {
        final var dataTable = new DataTableSpecCreator();
        final var column = new DataColumnSpecCreator("Test", StringCell.TYPE);
        final var cells = new DataCell[20];
        for (int i = 0; i < cells.length; i++) {
            cells[i] = new StringCell(Integer.toString(i));
        }
        column.setDomain(new DataColumnDomainCreator(cells).createDomain());
        final var columnWithoutDomain = new DataColumnSpecCreator("ColumnWithoutDomain", StringCell.TYPE);
        dataTable.addColumns(columnWithoutDomain.createSpec(), column.createSpec());

        return SnapshotTestConfiguration.builder() //
            .addInputTableSpec(dataTable.createSpec()) //
            .testJsonFormsForModel(NominalValueRowFilterNodeSettings.class) //
            .testJsonFormsWithInstance(SettingsType.MODEL, () -> createSettings()) //
            .testJsonFormsWithInstance(SettingsType.MODEL, () -> readSettings()) //
            .testNodeSettingsStructure(() -> createSettings()) //
            .testNodeSettingsStructure(() -> readSettings()) //
            .build();
    }

    private static NominalValueRowFilterNodeSettings createSettings() {
        return new NominalValueRowFilterNodeSettings();
    }

    private static NominalValueRowFilterNodeSettings readSettings() {
        try {
            var path = getSnapshotPath(NominalValueRowFilterNodeSettings.class).getParent().resolve("node_settings")
                .resolve("NominalValueRowFilterNodeSettings.xml");
            try (var fis = new FileInputStream(path.toFile())) {
                var nodeSettings = NodeSettings.loadFromXML(fis);
                return NodeParametersUtil.loadSettings(nodeSettings.getNodeSettings(SettingsType.MODEL.getConfigKey()),
                    NominalValueRowFilterNodeSettings.class);
            }
        } catch (IOException | InvalidSettingsException e) {
            throw new RuntimeException(e);
        }
    }
}
