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
 *   Nov 4, 2022 (Adrian): created
 */
package org.knime.base.node.preproc.table.cropper;

import org.knime.core.webui.node.dialog.impl.ChoicesProvider;
import org.knime.core.webui.node.dialog.impl.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.impl.Schema;
import org.knime.core.webui.node.dialog.persistence.field.Persist;

/**
 * Settings of the Range Filter node.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstany, Germany
 */
@SuppressWarnings("restriction")
final class TableCropperSettings implements DefaultNodeSettings {

    /**
     * Constructor for auto-configure.
     *
     * @param context of the creation
     */
    TableCropperSettings(final SettingsCreationContext context) {
        var spec = context.getDataTableSpecs()[0];
        if (spec != null) {
            m_startColumnName = spec.getColumnSpec(0).getName();
            m_endColumnName = spec.getColumnSpec(spec.getNumColumns() - 1).getName();
            m_endColumnNumber = spec.getNumColumns();
        }
    }

    /**
     * Constructor for deserialization.
     */
    TableCropperSettings() {
    }

    @Schema(title = "Column range mode",
        description = "Specify the range of columns included in the output by defining the start and end column. "
            + "The options for specifiying the start and end columns are:" //
            + "<ul>" //
            + "<li><b>By name</b>: Specify the name of the start and end columns to include.</li>"//
            + "<li><b>By number</b>: Specify the start and end column by their number in the table. "//
            + "The first column has number 1.</li>"//
            + "</ul>")
    ColumnRangeMode m_columnRangeMode = ColumnRangeMode.BY_NAME;

    @Schema(title = "Start column", description = "Select the first column to include.", choices = AllColumns.class)
    String m_startColumnName;

    @Schema(title = "End column (inclusive)", description = "Select the last column to include.",
        choices = AllColumns.class)
    String m_endColumnName;

    @Schema(title = "Start column number",
        description = "Number of the first column to include (the first column of the table has number 1).", min = 1)
    int m_startColumnNumber = 1;

    @Schema(title = "End column number (inclusive)",
        description = "Number of the last column to include (the first column of the table has number 1).", min = 1)
    int m_endColumnNumber = 1;

    @Schema(title = "Start row number",
        description = "Select the first row to include (the first row of the table has number 1).", min = 1)
    long m_startRowNumber = 1;

    @Schema(title = "End row number (inclusive)",
        description = "Select the last row to include (the first row of the table has number 1).", min = 1)
    long m_endRowNumber = 1;

    @Schema( //
        title = "Update domains of all columns", //
        description = "Advanced setting to enable recomputation of the domains of all columns in the output table " //
            + "such that the domains' bounds exactly match the bounds of the data in the output table." //
    )
    @Persist(optional = true)
    boolean m_updateDomains;

    private static final class AllColumns implements ChoicesProvider {

        @Override
        public String[] choices(final SettingsCreationContext context) {
            var spec = context.getDataTableSpecs()[0];
            return spec != null ? spec.getColumnNames() : new String[0];
        }

    }

    enum ColumnRangeMode {
            @Schema(title = "By name") //
            BY_NAME, //
            @Schema(title = "By number") //
            BY_NUMBER;
    }

}
