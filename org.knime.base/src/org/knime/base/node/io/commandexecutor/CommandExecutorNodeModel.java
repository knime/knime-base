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
 *   Mar 6, 2026 (janniksemperowitsch): created
 */
package org.knime.base.node.io.commandexecutor;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 *
 * @author janniksemperowitsch
 */
public class CommandExecutorNodeModel extends NodeModel{

    /**
     *
     */
    protected CommandExecutorNodeModel() {
        super(0, 2);
        // TODO Auto-generated constructor stub
    }

    @SuppressWarnings("unused")
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        //Table 1 for Stdout
        DataTableSpec outSpec = new DataTableSpecCreator()
                .addColumns(new DataColumnSpecCreator("Output", StringCell.TYPE).createSpec())
                .createSpec();
        //Table 2 for Stderr //--------------------------------------------------------------------------------Maybe get an if for merge
        DataTableSpec errSpec = new DataTableSpecCreator()
                .addColumns(new DataColumnSpecCreator("Error", StringCell.TYPE).createSpec())
                .createSpec();

        //--------------------------------------------------------------------------------------------------------Container for something... Leon
        BufferedDataContainer outContainer = exec.createDataContainer(outSpec);
        BufferedDataContainer errContainer = exec.createDataContainer(errSpec);

        //fill containers //---------------------------------------------------------------------------------true is tester for node to avoid errors
        if (false) { //
            for (int i = 0; i < 10; i++) {
                outContainer.addRowToTable(new DefaultRow("Row_" + i, new StringCell("out" + i)));
                errContainer.addRowToTable(new DefaultRow("Row_" + i, new StringCell("err" + i)));
            }
        } else {
            //String[] command = {"ls", "-la"}; //-------------------------------------------------------------should be he value from the Widget command
            String[] command = {"python3", "error.py"};
            boolean merge = false; //------------------------------------------------------------------------should be the value from the Widget merge
            StringBuilder outputBuffer = new StringBuilder();
            StringBuilder errorBuffer = new StringBuilder();
            CommandExecutorBashHandler.commandHandler(command, merge, outputBuffer, errorBuffer);

            String[] outputLines = outputBuffer.toString().split("\\R");
            String[] errorLines = errorBuffer.toString().split("\\R");

            for (int i = 0; i < outputLines.length; i++)  {
                outContainer.addRowToTable(new DefaultRow("Row_" + i, new StringCell(outputLines[i])));

            }
            for (int i = 0; i < errorLines.length; i++)  {
                errContainer.addRowToTable(new DefaultRow("Row_" + i, new StringCell(errorLines[i])));
            }

        }
        outContainer.close();
        errContainer.close();

        return new BufferedDataTable[]{outContainer.getTable(), errContainer.getTable()};
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        DataTableSpec outSpec = new DataTableSpecCreator()
                .addColumns(new DataColumnSpecCreator("Output", StringCell.TYPE).createSpec())
                .createSpec();
        DataTableSpec errSpec = new DataTableSpecCreator()
                .addColumns(new DataColumnSpecCreator("Error", StringCell.TYPE).createSpec())
                .createSpec();

        return new DataTableSpec[]{outSpec, errSpec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // TODO Auto-generated method stub

    }

}
