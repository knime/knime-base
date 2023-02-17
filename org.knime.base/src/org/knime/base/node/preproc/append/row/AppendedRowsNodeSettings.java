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
 *   21 Dec 2022 (ivan): created
 */
package org.knime.base.node.preproc.append.row;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.impl.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.impl.Schema;
import org.knime.core.webui.node.dialog.persistence.field.FieldNodeSettingsPersistor;
import org.knime.core.webui.node.dialog.persistence.field.Persist;

/**
 * Currently only used for the node dialogue, backwards compatible loading is ensured
 * by the node model. If this is ever used for the node model, backwards compatible loading will
 * need to be implemented.
 *
 * @author Jonas Klotz, KNIME GbmH, Berlin, Germany
 * @author Ivan Prigarin, KNIME GbmH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GbmH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
final class AppendedRowsNodeSettings implements DefaultNodeSettings {

    @Persist(configKey = AppendedRowsNodeModel.CFG_HILITING)
    @Schema(title = "Enable hiliting",
            description = "Enable hiliting between both inputs and the concatenated output table.")
    boolean m_enableHiliting = false; //NOSONAR being explicit is desired here

    @Persist(configKey = AppendedRowsNodeModel.CFG_SUFFIX)
    @Schema(title = "Suffix",
            description = "The suffix to be appended to RowIDs.")
    String m_suffix = "_dup";

    @Persist(customPersistor = RowIdResolutionPersistor.class)
    @Schema(title = "If there are duplicate RowIDs",
            description = "Select how to resolve duplicate RowIDs:"
            + "<ul>"
            // Skip option description
            + "<li><b>Skip</b>: Duplicate row identifiers (RowID) occurring in the "
            + "second table are not appended to the output table. This option is "
            + "relatively memory intensive as it needs to cache the row IDs in "
            + "order to find duplicates. Furthermore a full data duplication is needed.</li>"
            // Append suffix option description
            + "<li><b>Append suffix</b>: The output table will contain all rows, but "
            + "duplicate RowIDs are labeled with a suffix. Similar to "
            + "the \"Skip Rows\" option this method is also memory intensive.</li>"
            // Fail option description
            + "<li><b>Fail</b>: The node will fail during execution if duplicate "
            + "RowIDs are encountered. This option is efficient while checking uniqueness.</li>"
            + "</ul>")
    RowIdResolution m_rowIdResolution = RowIdResolution.APPEND;

    @Persist(customPersistor = ColumnSetOperationPersistor.class)
    @Schema(title = "How to combine input columns",
            description = "Choose the output column selection process:"
                    + "<ul>"
                    // Intersection option description
                    + "<li><b>Intersection</b>: Use only the columns that appear "
                    + "in every input table. Any other column is ignored and won't appear "
                    + "in the output table.</li>"
                    // Union option description
                    + "<li><b>Union</b>: Use all columns from all input "
                    + "tables. Fill rows with missing values if they miss cells for some columns.</li>"
                    + "</ul>")
    ColumnSetOperation m_columnSetOperation = ColumnSetOperation.UNION;

    enum RowIdResolution {
        @Schema(title="Skip")
        SKIP,

        @Schema(title="Append suffix")
        APPEND,

        @Schema(title="Fail")
        FAIL;
    }

    enum ColumnSetOperation {
        @Schema(title="Intersection")
        INTERSECTION,

        @Schema(title="Union")
        UNION;
    }

    private static final class RowIdResolutionPersistor implements FieldNodeSettingsPersistor<RowIdResolution> {

        @Override
        public RowIdResolution load(final NodeSettingsRO settings) throws InvalidSettingsException {
            // NB: The order is important
            // If both are true (for whatever reason) the node model will use DuplicatePolicy.Fail
            if (settings.getBoolean(AppendedRowsNodeModel.CFG_FAIL_ON_DUPLICATES)) {
                return RowIdResolution.FAIL;
            } else if (settings.getBoolean(AppendedRowsNodeModel.CFG_APPEND_SUFFIX)) {
                return RowIdResolution.APPEND;
            } else {
                return RowIdResolution.SKIP;
            }
        }

        @Override
        public void save(final RowIdResolution obj, final NodeSettingsWO settings) {
            settings.addBoolean(AppendedRowsNodeModel.CFG_APPEND_SUFFIX, obj == RowIdResolution.APPEND);
            settings.addBoolean(AppendedRowsNodeModel.CFG_FAIL_ON_DUPLICATES, obj == RowIdResolution.FAIL);
        }

        @Override
        public String[] getConfigKeys() {
            return new String[]{AppendedRowsNodeModel.CFG_APPEND_SUFFIX, AppendedRowsNodeModel.CFG_FAIL_ON_DUPLICATES};
        }
    }

    private static final class ColumnSetOperationPersistor implements FieldNodeSettingsPersistor<ColumnSetOperation> {

        @Override
        public ColumnSetOperation load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.getBoolean(AppendedRowsNodeModel.CFG_INTERSECT_COLUMNS)) {
                return ColumnSetOperation.INTERSECTION;
            } else {
                return ColumnSetOperation.UNION;
            }
        }

        @Override
        public void save(final ColumnSetOperation obj, final NodeSettingsWO settings) {
            settings.addBoolean(AppendedRowsNodeModel.CFG_INTERSECT_COLUMNS, obj == ColumnSetOperation.INTERSECTION);
        }

        @Override
        public String[] getConfigKeys() {
            return new String[]{AppendedRowsNodeModel.CFG_INTERSECT_COLUMNS};
        }
    }
}
