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
 *   Jan 7, 2025 (david): created
 */
package org.knime.time.node.format.durationperiodformatmanager;

import java.util.Arrays;
import java.util.List;

import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.property.ValueFormatHandler;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.time.util.DateTimeUtils;

/**
 * The node model for the DurationPeriodFormatManager node.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public class DurationPeriodFormatManagerNodeModel extends WebUINodeModel<DurationPeriodFormatManagerNodeSettings> {

    DurationPeriodFormatManagerNodeModel(final WebUINodeConfiguration config) {
        super(config, DurationPeriodFormatManagerNodeSettings.class);
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs,
        final DurationPeriodFormatManagerNodeSettings modelSettings) throws InvalidSettingsException {

        return new DataTableSpec[]{createSpecWithAttachedFormatter(inSpecs[0], modelSettings)};
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final DurationPeriodFormatManagerNodeSettings modelSettings) throws Exception {

        return new BufferedDataTable[]{ //
            exec.createSpecReplacerTable( //
                inData[0], //
                createSpecWithAttachedFormatter(inData[0].getDataTableSpec(), modelSettings) //
            ) //
        };
    }

    private static DataTableSpec createSpecWithAttachedFormatter(final DataTableSpec spec,
        final DurationPeriodFormatManagerNodeSettings settings) {

        var valueFormatHandler = new ValueFormatHandler( //
            new DurationPeriodDataValueFormatter( //
                settings.m_format, //
                settings.m_alignment //
            ) //
        );
        var columnNames = getInputColumnNames(spec, settings);
        var newTableSpecBuilder = new DataTableSpecCreator(spec);

        for (var columnName : columnNames) {
            var colSpec = spec.getColumnSpec(columnName);

            var newColSpecBuilder = new DataColumnSpecCreator(colSpec);
            newColSpecBuilder.setValueFormatHandler(valueFormatHandler);

            newTableSpecBuilder.replaceColumn(spec.findColumnIndex(columnName), newColSpecBuilder.createSpec());
        }

        return newTableSpecBuilder.createSpec();
    }

    /**
     * Get the column names that are to be processed during node execution.
     *
     * @param inputSpec
     * @param settings
     * @return
     */
    private static List<String> getInputColumnNames(final DataTableSpec inputSpec,
        final DurationPeriodFormatManagerNodeSettings settings) {

        var compatibleColumns =
            ColumnSelectionUtil.getCompatibleColumns(inputSpec, DateTimeUtils.INTERVAL_COLUMN_TYPES);
        return Arrays.asList(settings.m_columnFilter.filter(compatibleColumns));
    }
}
