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
package org.knime.base.node.mine.transformation.pca.compute;

import org.apache.commons.math3.linear.RealMatrix;
import org.knime.base.data.statistics.TransformationMatrix;
import org.knime.base.data.statistics.calculation.PCA;
import org.knime.base.node.mine.transformation.pca.AbstractPCA2NodeModel;
import org.knime.base.node.mine.transformation.port.TransformationPortObject;
import org.knime.base.node.mine.transformation.port.TransformationPortObjectSpec;
import org.knime.base.node.mine.transformation.port.TransformationPortObjectSpec.TransformationType;
import org.knime.base.node.mine.transformation.util.TransformationUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

/**
 * The PCA compute node model
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class PCA2ComputeNodeModel extends AbstractPCA2NodeModel {

    /**
     * Constructor.
     */
    PCA2ComputeNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE, BufferedDataTable.TYPE, TransformationPortObject.TYPE});

    }

    @Override
    protected PortObject[] doExecute(final BufferedDataTable inTable, final ExecutionContext exec)
        throws InvalidSettingsException, CanceledExecutionException {
        final PCA pca = new PCA();
        TransformationMatrix transMtx = pca.calcTransformationMatrix(exec.createSubExecutionContext(0.9), inTable,
            getColumnNames(), m_computeSettings.getFailOnMissingsModel().getBooleanValue());
        // the PCA's covariance matrix cannot be null so it's save to call get without further checks
        return new PortObject[]{
            createCovarianceMatrix(exec.createSubExecutionContext(0.05), pca.getCovMatrix().get()), TransformationUtils
                .createEigenDecompositionTable(exec.createSubExecutionContext(0.05), transMtx, getColumnNames()),
            new TransformationPortObject(TransformationType.PCA, transMtx, getColumnNames())};
    }

    private DataTableSpec createCovarianceMatrixSpec() {
        final DataType[] types = new DataType[getColumnNames().length];
        for (int i = 0; i < types.length; i++) {
            types[i] = DoubleCell.TYPE;
        }
        return new DataTableSpec("covariance matrix", getColumnNames(), types);
    }

    private BufferedDataTable createCovarianceMatrix(final ExecutionContext exec, final RealMatrix covMtx) {
        final String[] colNames = getColumnNames();

        final DataTableSpec covTableSpec = createCovarianceMatrixSpec();
        final int nCol = covTableSpec.getNumColumns();

        BufferedDataContainer bufTbl = exec.createDataContainer(covTableSpec);
        final DataCell[] cells = new DataCell[nCol];

        for (int i = 0; i < nCol; i++) {
            for (int j = 0; j < nCol; j++) {
                cells[j] = new DoubleCell(covMtx.getEntry(i, j));
            }
            bufTbl.addRowToTable(new DefaultRow(colNames[i], cells));
        }

        bufTbl.close();
        return bufTbl.getTable();
    }

    @Override
    protected PortObjectSpec[] doConfigure(final DataTableSpec inSpec) throws InvalidSettingsException {
        return new PortObjectSpec[]{createCovarianceMatrixSpec(),
            TransformationUtils.createDecompositionTableSpec(getColumnNames()),
            new TransformationPortObjectSpec(TransformationType.PCA, getColumnNames(), getColumnNames().length)};
    }

}
