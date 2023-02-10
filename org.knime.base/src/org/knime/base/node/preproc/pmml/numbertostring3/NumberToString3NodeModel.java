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
package org.knime.base.node.preproc.pmml.numbertostring3;

import java.util.Arrays;

import org.knime.base.node.preproc.pmml.PMMLStringConversionTranslator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;
import org.knime.core.node.port.pmml.preproc.DerivedFieldMapper;

/**
 * The NodeModel for the Number to String Node that converts numbers
 * to StringValues.
 *
 * @author cebron, University of Konstanz
 * @since 4.0
 */
public class NumberToString3NodeModel extends AbstractNumberToStringNodeModel<SettingsModelColumnFilter2>{

    /** if there should be an optional pmml input port. */
    private boolean m_pmmlInEnabled;

    /**
     * @return a SettingsModelColumnFilter2 for the included columns filtered for numerical values
     */
    @SuppressWarnings("unchecked")
    static SettingsModelColumnFilter2 createInclModel() {
        return new SettingsModelColumnFilter2(CFG_INCLUDED_COLUMNS, DoubleValue.class);
    }

    /**
     * Constructor with one data inport, one data outport and an optional
     * PMML inport and outport.
     * @param pmmlInEnabled true if there should be an optional input port
     * @since 3.0
     */
    public NumberToString3NodeModel(final boolean pmmlInEnabled) {
        super(pmmlInEnabled, createInclModel());
        m_pmmlInEnabled = pmmlInEnabled;
    }


    /**
     * Constructor with one data inport, one data outport and an optional
     * PMML inport and outport.
     */
    public NumberToString3NodeModel() {
        this(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String[] getInclCols(final DataTableSpec inSpec) {
        return getInclCols().applyTo(inSpec).getIncludes();
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
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec dts = (DataTableSpec)inSpecs[0];
        // find indices to work on
        int[] indices = findColumnIndices(dts);
        ConverterFactory converterFac =
                new ConverterFactory(indices, dts);
        ColumnRearranger colre = new ColumnRearranger(dts);
        colre.replace(converterFac, indices);
        DataTableSpec outDataSpec = colre.createSpec();

        // create the PMML spec based on the optional incoming PMML spec
        PMMLPortObjectSpec pmmlSpec = m_pmmlInEnabled ? (PMMLPortObjectSpec)inSpecs[1] : null;
        PMMLPortObjectSpecCreator pmmlSpecCreator
                = new PMMLPortObjectSpecCreator(pmmlSpec, dts);

        return new PortObjectSpec[]{outDataSpec, pmmlSpecCreator.createSpec()};
    }
/**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable inData = (BufferedDataTable)inObjects[0];
        DataTableSpec inSpec = inData.getDataTableSpec();
        // find indices to work on.
        String[] inclCols = getInclCols(inSpec);
        BufferedDataTable resultTable = null;
        if (inclCols.length == 0) {
            // nothing to convert, let's return the input table.
            resultTable = inData;
            setWarningMessage("No columns selected,"
                    + " returning input DataTable.");
        } else {
            int[] indices = findColumnIndices(inData.getSpec());
            ConverterFactory converterFac
                    = new ConverterFactory(indices, inSpec);
            ColumnRearranger colre = new ColumnRearranger(inSpec);
            colre.replace(converterFac, indices);

            resultTable = exec.createColumnRearrangeTable(inData, colre, exec);
        }

        // the optional PMML in port (can be null)
        PMMLPortObject inPMMLPort = m_pmmlInEnabled ? (PMMLPortObject)inObjects[1] : null;
        PMMLStringConversionTranslator trans
                = new PMMLStringConversionTranslator(
                        Arrays.asList(getInclCols(inSpec)), StringCell.TYPE,
                        new DerivedFieldMapper(inPMMLPort));

        PMMLPortObjectSpecCreator creator = new PMMLPortObjectSpecCreator(
                inPMMLPort, inSpec);
        PMMLPortObject outPMMLPort = new PMMLPortObject(
               creator.createSpec(), inPMMLPort, inSpec);
        outPMMLPort.addGlobalTransformations(trans.exportToTransDict());

        return new PortObject[]{resultTable, outPMMLPort};
    }
}
