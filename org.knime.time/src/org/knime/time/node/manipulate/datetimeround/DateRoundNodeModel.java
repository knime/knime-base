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
 *   Dec 3, 2024 (Tobias Kampmann): created
 */
package org.knime.time.node.manipulate.datetimeround;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUISimpleStreamableFunctionNodeModel;
import org.knime.time.node.manipulate.datetimeround.DateTimeRoundModelUtils.SelectedColumns;
import org.knime.time.util.ReplaceOrAppend;

/**
 *
 * @author Tobias Kampmann
 */
@SuppressWarnings("restriction")
public class DateRoundNodeModel extends WebUISimpleStreamableFunctionNodeModel<DateRoundNodeSettings> {

    /**
     * @param configuration
     */
    protected DateRoundNodeModel(final WebUINodeConfiguration configuration) {
        super(configuration, DateRoundNodeSettings.class);
    }

    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec spec,
        final DateRoundNodeSettings modelSettings) throws InvalidSettingsException {

        ColumnRearranger rearranger = new ColumnRearranger(spec);

        SelectedColumns selection = DateTimeRoundModelUtils.getSelectedColumns(spec,
            DateRoundNodeSettings.DATE_COLUMN_TYPES, modelSettings.m_columnFilter);

        if(selection.areColumnsMissing()) {
            setWarningMessage("Some columns are missing in the input table. Please check the configuration.");
        }

        for (String selectedColumn : selection.selectedColumns()) {

            SingleCellFactory factory = createCellFactory(spec, selectedColumn, modelSettings);

            if (modelSettings.m_replaceOrAppend == ReplaceOrAppend.REPLACE) {
                rearranger.replace(factory, selectedColumn);
            } else {
                rearranger.append(factory);
            }
        }
        return rearranger;
    }

    SingleCellFactory createCellFactory(final DataTableSpec spec, final String selectedColumn,
        final DateRoundNodeSettings settings) {
        var indexOfTargetColumn = spec.findColumnIndex(selectedColumn);

        DataColumnSpec newColSpec = DateTimeRoundModelUtils.createColumnSpec(spec, selectedColumn,
            settings.m_replaceOrAppend, settings.m_outputColumnSuffix);

        return new DateTimeRoundModelUtils.RoundCellFactory( //
            newColSpec, //
            indexOfTargetColumn, //
            DateRoundingUtil.createDateRounder(settings), //
            createMessageBuilder(), //
            this::setWarning); //

    }

}
