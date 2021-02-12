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
 *   Sep 25, 2019 (julian): created
 */
package org.knime.base.node.io.filehandling;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.streamable.BufferedDataTableRowOutput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.filehandling.core.defaultnodesettings.FileChooserHelper;

/**
 * Utility class providing a method to create a {@link BufferedDataTable BufferedDataTables} or to push
 * {@link DefaultRow DefaultRow} to a specified {@link RowOutput}. Rows are created by the specific implementation of
 * {@link FilesToDataTableReader}.
 *
 * @author Julian Bunzel, KNIME GmbH, Berlin, Germany
 * @since 4.1
 */
public final class FileHandlingUtil {

    private final FilesToDataTableReader m_reader;

    private final FileChooserHelper m_helper;

    /**
     * Creates a new instance of {@link FileHandlingUtil}.
     *
     * @param reader Specific implementation of {@link FilesToDataTableReader} used to handle the data
     * @param helper {@link FileChooserHelper} to read files/folders
     */
    public FileHandlingUtil(final FilesToDataTableReader reader, final FileChooserHelper helper) {
        m_reader = reader;
        m_helper = helper;
    }

    /**
     * Creates and returns {@link BufferedDataTable} by reading data from {@link java.nio.file.Path Paths}
     *
     * @param exec ExecutionContext
     * @return Returns a new {@link BufferedDataTable}
     * @throws Exception Thrown if new data table could not be created
     *
     */
    public final BufferedDataTable createDataTable(final ExecutionContext exec) throws Exception {
        exec.setMessage("Create data table specification");
        final ExecutionMonitor subExec = exec.createSubProgress(0.1);
        BufferedDataTableRowOutput output =
            new BufferedDataTableRowOutput(exec.createDataContainer(
                m_reader.createDataTableSpec(m_helper.getPaths(), subExec)));
        pushRowsToOutput(output, exec);
        output.close();
        return output.getDataTable();
    }

    /**
     * Returns the {@link DataTableSpec}.
     *
     * @return A DataTableSpec
     * @throws InvalidSettingsException Thrown if spec could not be created due to invalid settings
     */
    public final DataTableSpec createDataTableSpec() throws InvalidSettingsException {
        try {
            return m_reader.createDataTableSpec(m_helper.getPaths());
        } catch (final IOException e) {
            throw new InvalidSettingsException("Could not get paths based on specified file/directory.", e);
        }
    }

    /**
     * Method used to push rows to the specified RowOutput.
     *
     * @param output The RowOutput
     * @param exec ExecutionContext
     * @throws Exception Thrown if could rows could not be pushed to RowOutput
     */
    public final void pushRowsToOutput(final RowOutput output, final ExecutionContext exec) throws Exception {
        final List<Path> pathList = m_helper.getPaths();
        if (pathList.isEmpty()) {
            throw new InvalidSettingsException("No files selected");
        }
        final int noOfPaths = pathList.size();
        final double progressPerPath = 1.0 / noOfPaths;
        for (int i = 0; i < noOfPaths; i++) {
            exec.checkCanceled();
            exec.setMessage("Processing file " + (i + 1) + " of " + noOfPaths);
            m_reader.pushRowsToOutput(pathList.get(i), output, exec.createSubExecutionContext(progressPerPath));
        }
    }
}
