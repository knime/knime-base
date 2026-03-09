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
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.inactive.InactiveBranchPortObject;
import org.knime.core.node.port.inactive.InactiveBranchPortObjectSpec;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 *
 * @author janniksemperowitsch
 */
/**
 * Data model for the Command Executor node, managing execution logic and state.
 * * <p>Extends {@link WebUINodeModel} to handle command-specific settings defined in
 * {@link CommandExecutorNodeSettings} and provide integration with the Web UI.</p>
 */
public class CommandExecutorNodeModel extends WebUINodeModel<CommandExecutorNodeSettings>{

    /** The node logger for this class. */
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(CommandExecutorNodeModel.class);

    /**
     *
     */
    protected CommandExecutorNodeModel() {
        super(new PortType[0],
            new PortType[]{BufferedDataTable.TYPE, BufferedDataTable.TYPE},
            CommandExecutorNodeSettings.class);
    }

    @SuppressWarnings("unused")
    @Override//Portobject -> Rowagg
    protected PortObject[] execute (final PortObject[] outPortObjects,
    final ExecutionContext exec,
    final CommandExecutorNodeSettings modelSettings) throws Exception {/*
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec,
            final CommandExecutorNodeSettings modelSettings) throws Exception {*/

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


        String[] command = Stream.concat(
            Stream.of(modelSettings.m_command),
            Arrays.stream(modelSettings.m_newArgumentSettings).map(arg -> arg.m_argumentToAppend)
        ).toArray(String[]::new);

        // System.out.println("+++\\nCommand to execute: " + Arrays.toString(command) + "\n+++"); //-------------------------------Debug help

        boolean merge = modelSettings.m_mergeErrorStream;
        boolean cut = modelSettings.m_singleOutputCell; //true -> all in one cell end may be cut off
        StringBuilder outputBuffer = new StringBuilder();
        StringBuilder errorBuffer = new StringBuilder();

        CommandExecutorBashHandler.commandHandler(command, merge, cut, outputBuffer, errorBuffer, LOGGER);
        String[] outputLines;
        String[] errorLines;
        if (cut) {
            outputLines = new String[] { outputBuffer.toString() };
            errorLines = new String[] { errorBuffer.toString() };
        } else {
            final Pattern LINE_BREAK_PATTERN = Pattern.compile("\\R");
            outputLines = LINE_BREAK_PATTERN.split(outputBuffer.toString());
            errorLines = LINE_BREAK_PATTERN.split(errorBuffer.toString());
            //outputLines = outputBuffer.toString().split("\\R");
            //errorLines = errorBuffer.toString().split("\\R");
        }
        String line;
        for (int i = 0; i < outputLines.length; i++)  {
            line = outputLines[i];
            if (!line.strip().isEmpty()) {
                outContainer.addRowToTable(new DefaultRow("Row_" + i, new StringCell(line)));
            }

        }
        for (int i = 0; i < errorLines.length; i++)  {
            line = errorLines[i];
            if (!line.strip().isEmpty()) {
                errContainer.addRowToTable(new DefaultRow("Row_" + i, new StringCell(line)));
            }
        }

        outContainer.close();
        errContainer.close();
        if (merge) {
            return new PortObject[]{outContainer.getTable(), InactiveBranchPortObject.INSTANCE};
        }
        return new PortObject[]{outContainer.getTable(), errContainer.getTable()};
    }

    @Override //Portobject
    protected PortObjectSpec[] configure(final PortObjectSpec[] outSpecs,
        final CommandExecutorNodeSettings modelSettings) throws InvalidSettingsException {
        DataTableSpec outSpec = new DataTableSpecCreator()
                .addColumns(new DataColumnSpecCreator("Output", StringCell.TYPE).createSpec())
                .createSpec();
        DataTableSpec errSpec = new DataTableSpecCreator()
                .addColumns(new DataColumnSpecCreator("Error", StringCell.TYPE).createSpec())
                .createSpec();
        if(modelSettings.m_mergeErrorStream) {
            return new PortObjectSpec[] {outSpec, InactiveBranchPortObjectSpec.INSTANCE};
        }
        return new PortObjectSpec[] {outSpec, errSpec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {

    }

}
