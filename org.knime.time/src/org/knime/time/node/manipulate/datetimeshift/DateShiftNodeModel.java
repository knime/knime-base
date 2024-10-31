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
 *   Nov 11, 2024 (tobias): created
 */
package org.knime.time.node.manipulate.datetimeshift;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;
import org.knime.time.node.manipulate.datetimeshift.DateTimeShiftUtils.GenericShiftType;

/**
 * The node model of the node which shifts date columns.
 *
 * @author Tobias Kampmann, TNG
 */
@SuppressWarnings("restriction")
final class DateShiftNodeModel extends WebUINodeModel<DateShiftNodeSettings> {

    /**
     * @param configuration
     */
    protected DateShiftNodeModel(final WebUINodeConfiguration configuration) {
        super(configuration, DateShiftNodeSettings.class);
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs, final DateShiftNodeSettings settings)
        throws InvalidSettingsException {

        switch (settings.m_shiftMode) {
            case PERIOD:
                DateTimeShiftSettingsUtils.validatePeriodColumn(settings.m_periodColumn, inSpecs[0]);
                break;
            case NUMERICAL:
                DateTimeShiftSettingsUtils.validateNumericalColumn(settings.m_numericalColumn, inSpecs[0]);
                break;
            default:
                break;
        }

        DataTableSpec in = inSpecs[0];
        ColumnRearranger r = createColumnRearranger(in, settings);
        DataTableSpec out = r.createSpec();
        return new DataTableSpec[]{out};
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final DateShiftNodeSettings modelSettings) throws Exception {//NOSONAR

        BufferedDataTable in = inData[0];
        ColumnRearranger r = createColumnRearranger(in.getDataTableSpec(), modelSettings);
        BufferedDataTable out = exec.createColumnRearrangeTable(in, r, exec);
        return new BufferedDataTable[]{out};
    }

    private ColumnRearranger createColumnRearranger(final DataTableSpec spec, final DateShiftNodeSettings settings) {

        var shiftNodeSettings = new ShiftContext.Builder() //
            .temporalColumn(settings.m_periodColumn) //
            .temporalValue(settings.m_shiftPeriodValue) //
            .numericalColumn(settings.m_numericalColumn) //
            .granularity(settings.m_granularity.getGranularity()) //
            .replaceOrAppend(settings.m_replaceOrAppend) //
            .outputColumnSuffix(settings.m_outputColumnSuffix) //
            .selectedColumnNames(settings.m_columnFilter, DateShiftNodeSettings.DATE_COLUMN_TYPES, spec) //
            .messageBuilder(createMessageBuilder()) //
            .messageConsumer(this::setWarning) //
            .build();

        return switch (settings.m_shiftMode) {
            case PERIOD -> DateTimeShiftUtils.createColumnRearranger(spec, shiftNodeSettings,
                GenericShiftType.TEMPORAL_COLUMN);
            case NUMERICAL -> DateTimeShiftUtils.createColumnRearranger(spec, shiftNodeSettings,
                GenericShiftType.NUMERICAL_COLUMN);
            case SHIFT -> DateTimeShiftUtils.createColumnRearranger(spec, shiftNodeSettings,
                GenericShiftType.TEMPORAL_VALUE);
            default -> new ColumnRearranger(spec); // do nothing
        };
    }
}
