/*
 * ------------------------------------------------------------------------
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
 * --------------------------------------------------------------------
 *
 * History
 *   03.07.2007 (cebron): created
 */
package org.knime.base.node.preproc.colconvert.numbertostring2;

import java.util.Arrays;
import java.util.stream.Stream;

import org.knime.base.node.preproc.pmml.numbertostring3.AbstractNumberToStringNodeModel;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.StreamableOperator;

/**
 * The NodeModel for the Number to String Node that converts numbers to StringValues.
 *
 * @author Johannes Schweig
 * @since 4.0
 */
public class NumberToString2NodeModel extends AbstractNumberToStringNodeModel<SettingsModelColumnFilter2> {

    /**
     * @return a SettingsModelColumnFilter2 for the included columns filtered for numerical values
     */
    @SuppressWarnings("unchecked")
    static SettingsModelColumnFilter2 createInclModel() {
        return new SettingsModelColumnFilter2(CFG_INCLUDED_COLUMNS, DoubleValue.class);
    }

    /**
     * Constructor
     */
    public NumberToString2NodeModel() {
        super(createInclModel());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String[] getStoredInclCols(final DataTableSpec inSpec) {
        String[] inclCols = getInclCols().applyTo(inSpec).getIncludes();
        String[] remInclCols = getInclCols().applyTo(inSpec).getRemovedFromIncludes();
        return Stream.concat(Arrays.stream(inclCols), Arrays.stream(remInclCols)).toArray(String[]::new);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isKeepAllSelected() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        // find indices to work on
        int[] indices = findColumnIndices(inSpecs[0]);
        ConverterFactory converterFac =
                new ConverterFactory(indices, inSpecs[0]);
        ColumnRearranger colre = new ColumnRearranger(inSpecs[0]);
        colre.replace(converterFac, indices);
        DataTableSpec newspec = colre.createSpec();
        return new DataTableSpec[]{newspec};
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        StringBuilder warnings = new StringBuilder();
        // find indices to work on.
        DataTableSpec inSpec = inData[0].getDataTableSpec();
        String[] inclCols = getStoredInclCols(inSpec);
        if (inclCols.length == 0) {
            // nothing to convert, let's return the input table.
            setWarningMessage("No columns selected,"
                    + " returning input DataTable.");
            return new BufferedDataTable[]{inData[0]};
        }

        ConverterFactory converterFactory = createConverterFactory(inSpec);
        ColumnRearranger colre = createColumnRearranger(converterFactory, inSpec);

        BufferedDataTable resultTable =
                exec.createColumnRearrangeTable(inData[0], colre, exec);
        String errorMessage = converterFactory.getErrorMessage();

        if (errorMessage.length() > 0) {
            warnings.append("Problems occurred, see Console messages.\n");
        }
        if (warnings.length() > 0) {
            getLogger().warn(errorMessage);
            setWarningMessage(warnings.toString());
        }
        return new BufferedDataTable[]{resultTable};
    }

    private ConverterFactory createConverterFactory(final DataTableSpec inSpec) throws InvalidSettingsException {
        return new ConverterFactory(findColumnIndices(inSpec), inSpec);
    }

    private ColumnRearranger createColumnRearranger(final ConverterFactory converterFactory, final DataTableSpec inSpec) throws InvalidSettingsException {
        ColumnRearranger colre = new ColumnRearranger(inSpec);
        colre.replace(converterFactory, findColumnIndices(inSpec));
        return colre;
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
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec inSpec = (DataTableSpec)inSpecs[0];
        final ColumnRearranger colRearranger = createColumnRearranger(createConverterFactory(inSpec), inSpec);
        return colRearranger.createStreamableFunction();
    }
}
