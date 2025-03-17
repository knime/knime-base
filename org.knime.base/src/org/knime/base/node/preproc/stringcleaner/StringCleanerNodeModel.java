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
 *   21 Oct 2022 (jasper): created
 */
package org.knime.base.node.preproc.stringcleaner;

import static org.knime.core.webui.node.dialog.defaultdialog.util.column.ColumnSelectionUtil.getStringColumns;

import java.util.LinkedList;

import org.knime.base.node.preproc.stringcleaner.StringCleanerNodeSettings.OutputOption;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingCell;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 * Implementation of the String Cleaner node.
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 * @since 5.2
 */
@SuppressWarnings("restriction") // New Node UI is not yet API
public class StringCleanerNodeModel extends WebUINodeModel<StringCleanerNodeSettings> {

    /**
     * Instantiate a new String Cleaner Node
     *
     * @param configuration node description
     * @param modelSettingsClass a reference to {@link StringCleanerNodeSettings}
     */
    StringCleanerNodeModel(final WebUINodeConfiguration configuration,
        final Class<StringCleanerNodeSettings> modelSettingsClass) {
        super(configuration, modelSettingsClass);
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs, final StringCleanerNodeSettings modelSettings)
        throws InvalidSettingsException {
        final var outputSpec = createColumnRearranger(modelSettings, inSpecs[0]).createSpec();
        return new DataTableSpec[]{outputSpec};
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final StringCleanerNodeSettings modelSettings) throws Exception {
        final var rearranger = createColumnRearranger(modelSettings, inData[0].getDataTableSpec());
        return new BufferedDataTable[]{exec.createColumnRearrangeTable(inData[0], rearranger, exec)};
    }

    private static ColumnRearranger createColumnRearranger(final StringCleanerNodeSettings settings,
        final DataTableSpec spec) throws InvalidSettingsException {
        final var selectedColumns =
            spec.columnsToIndices(settings.m_columnsToClean.filter(getStringColumns(spec)));
        final var insertedColumns = new LinkedList<DataColumnSpec>();

        // Create new column specs
        for (var colIx : selectedColumns) {
            final var oldSpec = spec.getColumnSpec(colIx);
            final var newSpec = new DataColumnSpecCreator(oldSpec);
            newSpec.setType(StringCell.TYPE);
            newSpec.setDomain(null);
            if (settings.m_output == OutputOption.APPEND) {
                newSpec.setName(DataTableSpec.getUniqueColumnName(spec, oldSpec.getName() + settings.m_outputSuffix));
            }
            insertedColumns.add(newSpec.createSpec());
        }

        // Create String cleaner (throws ISE if validateSettings() hasn't been called before)
        final var stringCleaner = StringCleaner.fromSettings(settings);

        // Cell factory that produces the cleaned strings
        final var fac = new AbstractCellFactory(insertedColumns.toArray(DataColumnSpec[]::new)) {
            @Override
            public DataCell[] getCells(final DataRow row) {
                final var cells = new LinkedList<DataCell>();
                for (var colIx : selectedColumns) {
                    final var oldCell = row.getCell(colIx);
                    final DataCell replacement;
                    if (oldCell.isMissing()) {
                        replacement = oldCell;
                    } else if (oldCell instanceof StringValue sv) {
                        replacement = new StringCell(stringCleaner.clean(sv.getStringValue()));
                    } else {
                        replacement = new MissingCell("Input cell was not a string cell.");
                    }
                    cells.add(replacement);
                }
                CheckUtils.checkState(cells.size() == selectedColumns.length, "Too few or many cells were produced");
                return cells.toArray(DataCell[]::new);
            }
        };

        final var cr = new ColumnRearranger(spec);
        if (settings.m_output == OutputOption.APPEND) {
            cr.append(fac);
        } else if (settings.m_output == OutputOption.REPLACE) {
            cr.replace(fac, selectedColumns);
        }
        return cr;
    }

    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs, final StringCleanerNodeSettings settings) throws InvalidSettingsException {
        final var rearranger = createColumnRearranger(settings, (DataTableSpec)inSpecs[0]);
        return rearranger.createStreamableFunction(0, 0);
    }

    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[]{InputPortRole.DISTRIBUTED_STREAMABLE};
    }

    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED};
    }

    @Override
    protected void validateSettings(final StringCleanerNodeSettings settings) throws InvalidSettingsException {
        StringCleaner.validateSettings(settings);
    }

}
