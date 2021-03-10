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
 *   May 6, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.mine.transformation.pca.reverse;

import java.io.File;

import org.knime.base.data.statistics.TransformationMatrix;
import org.knime.base.node.mine.transformation.port.TransformationPortObject;
import org.knime.base.node.mine.transformation.port.TransformationPortObjectSpec;
import org.knime.base.node.mine.transformation.settings.TransformationComputeSettings;
import org.knime.base.node.mine.transformation.settings.TransformationReverseSettings;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortObjectInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.StreamableFunction;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.util.CheckUtils;

/**
 * The PCA reverse node model.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 *
 */
final class PCA2ReverseNodeModel extends NodeModel {

    /** Index of model data port. */
    private static final int MODEL_IN_PORT = 0;

    /** Index of input data port. */
    static final int DATA_IN_PORT = 1;

    /** The compute settings. */
    private final TransformationComputeSettings m_computeSettings = new TransformationComputeSettings();

    /** The reverse settings. */
    private final TransformationReverseSettings m_reverseSettings = new TransformationReverseSettings();

    /**
     * Constructor.
     */
    PCA2ReverseNodeModel() {
        super(new PortType[]{TransformationPortObject.TYPE, BufferedDataTable.TYPE},
            new PortType[]{BufferedDataTable.TYPE});
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        final DataTableSpec inSpec = (DataTableSpec)inData[DATA_IN_PORT].getSpec();
        final TransformationPortObject model = (TransformationPortObject)inData[MODEL_IN_PORT];

        final ColumnRearranger cr = createColumnRearranger(inSpec, model.getSpec(), model.getTransformationMatrix());
        final BufferedDataTable result =
            exec.createColumnRearrangeTable((BufferedDataTable)inData[DATA_IN_PORT], cr, exec);
        final PortObject[] out = {result};
        return out;
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec inSpec = (DataTableSpec)inSpecs[DATA_IN_PORT];
        final String[] columnNames = m_computeSettings.getUsedColsModel().applyTo(inSpec).getIncludes();
        CheckUtils.checkSetting(columnNames != null && columnNames.length > 0, "Please selected at least one column");
        if (inSpecs[MODEL_IN_PORT] != null && columnNames != null) {
            final TransformationPortObjectSpec transSpec = (TransformationPortObjectSpec)inSpecs[MODEL_IN_PORT];
            CheckUtils.checkSetting(columnNames.length <= transSpec.getInputColumnNames().length,
                "Unable to reverse transformation: The number of selected columns is higher than the number of "
                    + "columns used to calculate the transformation model");
        }
        return new PortObjectSpec[]{
            createColumnRearranger(inSpec, (TransformationPortObjectSpec)inSpecs[MODEL_IN_PORT], null).createSpec()};
    }

    private ColumnRearranger createColumnRearranger(final DataTableSpec tableSpec,
        final TransformationPortObjectSpec transSpec, final TransformationMatrix transMtx) {
        final ColumnRearranger cr = new ColumnRearranger(tableSpec);
        final String[] colNames = m_computeSettings.getUsedColsModel().applyTo(tableSpec).getIncludes();

        if (m_reverseSettings.getRemoveUsedColsModel().getBooleanValue()) {
            cr.remove(colNames);
        }

        cr.append(new AbstractCellFactory(true, createAddTableSpec(tableSpec, transSpec.getInputColumnNames())) {
            final int[] m_colIdx = tableSpec.columnsToIndices(colNames);

            final int m_dim = transSpec.getInputColumnNames().length;

            final boolean m_failOnMissings = m_computeSettings.getFailOnMissingsModel().getBooleanValue();

            @Override
            public DataCell[] getCells(final DataRow row) {
                return transMtx.reverseProjection(row, m_colIdx, m_dim, m_failOnMissings);
            }
        });

        return cr;
    }

    private static DataColumnSpec[] createAddTableSpec(final DataTableSpec inSpecs, final String[] colNames) {
        final DataColumnSpec[] specs = new DataColumnSpec[colNames.length];
        for (int i = 0; i < colNames.length; i++) {
            final String colName = DataTableSpec.getUniqueColumnName(inSpecs, colNames[i]);
            final DataColumnSpecCreator specCreator =
                new DataColumnSpecCreator(colName, DataType.getType(DoubleCell.class));
            specs[i] = specCreator.createSpec();
        }
        return specs;
    }

    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {
            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                final DataTableSpec inSpec = (DataTableSpec)inSpecs[DATA_IN_PORT];

                final TransformationPortObject inModel =
                    (TransformationPortObject)((PortObjectInput)inputs[MODEL_IN_PORT]).getPortObject();

                final ColumnRearranger cr =
                    createColumnRearranger(inSpec, inModel.getSpec(), inModel.getTransformationMatrix());

                final StreamableFunction func = cr.createStreamableFunction(DATA_IN_PORT, 0);
                func.runFinal(inputs, outputs, exec);
            }
        };
    }

    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[]{InputPortRole.NONDISTRIBUTED_NONSTREAMABLE, InputPortRole.DISTRIBUTED_STREAMABLE};
    }

    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED};
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) {
        // nothing to do
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) {
        // nothing to do
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_computeSettings.loadValidatedSettingsFrom(settings);
        m_reverseSettings.loadValidatedSettingsFrom(settings);
    }

    @Override
    protected void reset() {
        // nothing to do
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_computeSettings.saveSettingsTo(settings);
        m_reverseSettings.saveSettingsTo(settings);

    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_computeSettings.validateSettings(settings);
        m_reverseSettings.validateSettings(settings);

    }
}