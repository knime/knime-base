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
import java.util.Optional;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.defaultnodesettings.FileChooserHelper;
import org.knime.filehandling.core.port.FileSystemPortObject;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;

/**
 * An abstract NodeModel for file reading nodes taking care of reading data from {@link java.nio.file.Path Paths}.
 *
 * @author Julian Bunzel, KNIME GmbH, Berlin, Germany
 * @since 4.1
 */
public abstract class AbstractSimpleFileReaderNodeModel extends NodeModel {

    /**
     * Creates a new model with the given number of input and output data ports.
     *
     * @param nrInDataPorts number of input data ports
     * @param nrOutDataPorts number of output data ports
     */
    public AbstractSimpleFileReaderNodeModel(final int nrInDataPorts, final int nrOutDataPorts) {
        super(nrInDataPorts, nrOutDataPorts);
    }

    /**
     * Creates a new model with the given PortTypes.
     *
     * @param inPorts input port types
     * @param outPorts output port types
     */
    public AbstractSimpleFileReaderNodeModel(final PortType[] inPorts, final PortType[] outPorts) {
        super(inPorts, outPorts);
    }

    @Override
    public BufferedDataTable[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        Optional<FSConnection> fs = FileSystemPortObject.getFileSystemConnection(inData, 0);
        final FileHandlingUtil util = getFileHandlingUtil(fs);
        return new BufferedDataTable[]{util.createDataTable(exec)};
    }

    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        Optional<FSConnection> fs = FileSystemPortObjectSpec.getFileSystemConnection(inSpecs, 0);

        return new StreamableOperator() {

            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                final FileHandlingUtil util = getFileHandlingUtil(fs);
                final RowOutput output = (RowOutput)outputs[0];
                util.pushRowsToOutput(output, exec);
                output.close();
            }
        };
    }

    /**
     * Returns a new instance of {@link FileHandlingUtil}.
     *
     * @param fs
     * @return FileHandlingUtil
     * @throws IOException
     */
    public FileHandlingUtil getFileHandlingUtil(final Optional<FSConnection> fs) throws IOException {
        return new FileHandlingUtil(getReader(), getFileChooserHelper(fs));
    }

    /**
     * Returns a new instance of {@link FileChooserHelper}.
     *
     * @param fs
     * @return FileChooserHelper
     * @throws IOException
     */
    public abstract FileChooserHelper getFileChooserHelper(final Optional<FSConnection> fs) throws IOException;

    /**
     * Returns a specific implementation of {@link FilesToDataTableReader}.
     *
     * @return FilesToDataTableReader Specific implementation of {@code FilesToDataTableReader}
     */
    public abstract FilesToDataTableReader getReader();
}
