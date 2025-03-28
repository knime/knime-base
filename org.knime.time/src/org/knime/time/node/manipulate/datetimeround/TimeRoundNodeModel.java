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

import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.message.MessageBuilder;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUISimpleStreamableFunctionNodeModel;
import org.knime.time.node.manipulate.datetimeround.DateTimeRoundModelUtils.RoundCellFactory;
import org.knime.time.util.DateTimeUtils;
import org.knime.time.util.ReplaceOrAppend.InputColumn;

/**
 *
 * @author Tobias Kampmann
 */
@SuppressWarnings("restriction")
public class TimeRoundNodeModel extends WebUISimpleStreamableFunctionNodeModel<TimeRoundNodeSettings> {

    /**
     * @param configuration
     */
    protected TimeRoundNodeModel(final WebUINodeConfiguration configuration) {
        super(configuration, TimeRoundNodeSettings.class);
    }

    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec spec,
        final TimeRoundNodeSettings modelSettings) throws InvalidSettingsException {

        String[] selectedColumns = DateTimeRoundModelUtils.getSelectedColumns(spec, DateTimeUtils.TIME_COLUMN_TYPES,
            modelSettings.m_columnFilter);

        final var messageBuilder = createMessageBuilder();
        return modelSettings.m_replaceOrAppend.createRearranger(selectedColumns, spec, (inputColumn,
            newColumnName) -> createCellFactory(inputColumn, newColumnName, modelSettings, messageBuilder),
            modelSettings.m_outputColumnSuffix, () -> {
                final var issueCount = messageBuilder.getIssueCount();
                if (issueCount > 0) {
                    messageBuilder.withSummary("Problems occurred in " + issueCount + " rows.").build()
                        .ifPresent(this::setWarning);
                }
            });
    }

    private static SingleCellFactory createCellFactory(final InputColumn inputColumn, final String newColumnName,
        final TimeRoundNodeSettings settings, final MessageBuilder messageBuilder) {

        // type of column doesn't change
        var newColSpec = new DataColumnSpecCreator(newColumnName, inputColumn.spec().getType()).createSpec();

        return new RoundCellFactory( //
            newColSpec, //
            inputColumn.index(), //
            TimeRoundingUtil.createRoundingOperator(settings), //
            messageBuilder); //
    }
}
