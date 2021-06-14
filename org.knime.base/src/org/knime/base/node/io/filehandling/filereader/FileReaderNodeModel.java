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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.io.filehandling.filereader;

import java.io.File;
import java.util.EnumSet;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.BufferedDataTableRowOutput;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.util.DuplicateKeyException;
import org.knime.core.util.tokenizer.SettingsStatus;
import org.knime.filehandling.core.defaultnodesettings.status.NodeModelStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;

/**
 * @author Peter Ohl, University of Konstanz
 */
final class FileReaderNodeModel extends NodeModel {
    /**
     * The id this objects uses to store its file history in the <code>StringHistory</code> object. Don't reuse this id
     * unless you want to share the history list.
     */
    public static final String FILEREADER_HISTORY_ID = "ASCIIfile";

    /** The node logger fot this class. */
    private static final NodeLogger LOGGER = NodeLogger.getLogger(FileReaderNodeModel.class);

    /*
     * The settings structure used to create a DataTable from during execute.
     */
    private FileReaderNodeSettings m_frSettings;

    private final NodeModelStatusConsumer m_statusConsumer;

    /**
     * Creates a new model that creates and holds a Filetable.
     *
     * @param portsConfiguration the ports configuration
     * @param fileReaderNodeSettings
     */
    FileReaderNodeModel(final PortsConfiguration portsConfiguration,
        final FileReaderNodeSettings fileReaderNodeSettings) {
        super(portsConfiguration.getInputPorts(), portsConfiguration.getOutputPorts());
        m_frSettings = fileReaderNodeSettings;
        m_statusConsumer = new NodeModelStatusConsumer(EnumSet.of(MessageType.ERROR, MessageType.WARNING));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // m_frSettings = null;
    }

    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {

        return new StreamableOperator() {

            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                assert inputs.length == 0;

                LOGGER.info("Preparing to read from '" + m_frSettings.getDataFileLocation().getPath() + "'.");

                // check again the settings - especially file existence (under Linux
                // files could be deleted/renamed since last config-call...
                SettingsStatus status = m_frSettings.getStatusOfSettings(true, null);
                if (status.getNumOfErrors() > 0) {
                    throw new InvalidSettingsException(status.getAllErrorMessages(10));
                }

                DataTableSpec tSpec = m_frSettings.createDataTableSpec();

                FileTable fTable = new FileTable(tSpec, m_frSettings, m_frSettings.getSkippedColumns(), exec);

                RowOutput rowOutput = (RowOutput)outputs[0]; // data output port

                int row = 0;
                FileRowIterator it = fTable.iterator();
                try {
                    if (it.getZipEntryName() != null) {
                        // seems we are reading a ZIP archive.
                        LOGGER.info("Reading entry '" + it.getZipEntryName() + "' from the specified ZIP archive.");
                    }

                    while (it.hasNext()) {
                        row++;
                        DataRow next = it.next();
                        final int finalRow = row;
                        exec.setMessage(() -> "Reading row #" + finalRow + " (\"" + next.getKey() + "\")");
                        exec.checkCanceled();
                        rowOutput.push(next);
                    }
                    rowOutput.close();

                    if (it.zippedSourceHasMoreEntries()) {
                        // after reading til the end of the file this returns a valid
                        // result
                        setWarningMessage(
                            "Source is a ZIP archive with multiple " + "entries. Only reading first entry!");
                    }
                } catch (DuplicateKeyException dke) {
                    String msg = dke.getMessage();
                    if (msg == null) {
                        msg = "Duplicate row IDs";
                    }
                    msg += ". Consider making IDs unique in the advanced settings.";
                    DuplicateKeyException newDKE = new DuplicateKeyException(msg);
                    newDKE.initCause(dke);
                    throw newDKE;
                }
                // user settings allow for truncating the table
                if (it.iteratorEndedEarly()) {
                    setWarningMessage("Data was truncated due to user settings.");
                }
                // closes all sources.
                fTable.dispose();
            }
        };
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        DataTableSpec spec = m_frSettings.createDataTableSpec(false);
        BufferedDataTableRowOutput output = new BufferedDataTableRowOutput(exec.createDataContainer(spec));
        createStreamableOperator(null, null).runFinal(new PortInput[0], new PortOutput[]{output}, exec);
        return new PortObject[]{output.getDataTable()};
    }

    /**
     * @return the current settings for the file reader. Could be <code>null</code>.
     */
    FileReaderSettings getFileReaderSettings() {
        return m_frSettings;
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        if (m_frSettings == null) {
            throw new InvalidSettingsException("No Settings available.");
        }

        // see if settings are good enough for execution
        m_frSettings.getDataFileLocation().configureInModel(inSpecs, m_statusConsumer);
        m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);
        SettingsStatus status = m_frSettings.getStatusOfSettings(true, null);
        if (status.getNumOfErrors() == 0) {
            return new DataTableSpec[]{m_frSettings.createDataTableSpec()};
        }

        throw new InvalidSettingsException(status.getAllErrorMessages(0));
    }

    /*
     * validates the settings object, or reads its settings from it. Depending
     * on the specified value of the 'validateOnly' parameter.
     */
    private void readSettingsFromConfiguration(final NodeSettingsRO settings, final boolean validateOnly)
        throws InvalidSettingsException {
        if (settings == null) {
            throw new NullPointerException("Can't read filereader node settings" + " from null config object");
        }
        m_frSettings.getDataFileLocation().validateSettings(settings);
        m_frSettings.getDataFileLocation().loadSettingsFrom(settings);
        // will puke and die if config is not readable.
        FileReaderNodeSettings newSettings = new FileReaderNodeSettings(settings, m_frSettings.getDataFileLocation());

        // check consistency of settings.
        SettingsStatus status = newSettings.getStatusOfSettings();
        if (status.getNumOfErrors() > 0) {
            throw new InvalidSettingsException(status.getAllErrorMessages(0));
        }

        if (!validateOnly) {
            // everything looks good - take over the new settings.
            m_frSettings = newSettings;
        }
    }

    /**
     * Reads in all user settings of the model. If they are incomplete, inconsistent, or in any way invalid it will
     * throw an exception.
     *
     * @param settings the object to read the user settings from. Must not be <code>null</code> and must be validated
     *            with the validate method below.
     * @throws InvalidSettingsException if the settings are incorrect - which should not happen as they are supposed to
     *             be validated before.
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettingsRO)
     * @see NodeModel#validateSettings(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        readSettingsFromConfiguration(settings, /* validateOnly = */false);
    }

    /**
     * Writes the current user settings into a configuration object.
     *
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {

        if (settings == null) {
            throw new NullPointerException("Can't write filereader node " + "settings into null config object.");
        }
        FileReaderNodeSettings s = m_frSettings;

        if (s == null) {
            s = new FileReaderNodeSettings(m_frSettings.getDataFileLocation()); // NOSONAR cannot be null
        }
        s.saveToConfiguration(settings);

    }

    /**
     * Checks all user settings in the specified spec object. If they are incomplete, inconsistent, or in any way
     * invalid it will throw an exception.
     *
     * @param settings the object to read the user settings from. Must not be <code>null</code>.
     * @throws InvalidSettingsException if the settings in the specified object are incomplete, inconsistent, or in any
     *             way invalid.
     * @see NodeModel#validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        readSettingsFromConfiguration(settings, /* validateOnly = */true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) {
        // no internals to save.
        return;
    }

}
