package org.knime.filehandling.core.example.node.writer.table;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.core.defaultnodesettings.status.NodeModelStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;

final class TableWriterSkeletonNodeModel extends NodeModel {

    private final TableWriterSkeletonSettings m_settings;

    private final int m_tableIdx;

    private final NodeModelStatusConsumer m_statusConsumer;

    public TableWriterSkeletonNodeModel(final PortsConfiguration portsCfg, final TableWriterSkeletonSettings settings,
        final int tableIdx) {
        super(portsCfg.getInputPorts(), portsCfg.getOutputPorts());
        m_settings = settings;
        m_tableIdx = tableIdx;
        m_statusConsumer = new NodeModelStatusConsumer(EnumSet.of(MessageType.ERROR, MessageType.WARNING));
    }

    // Make sure to overwrite the PortObjectSpec[] version!
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        // configure the writer file choose here to ensure that flow variables are
        // correctly set / valid
        m_settings.getWriterFileChooser().configureInModel(inSpecs, m_statusConsumer);
        // inform the user about potential problems
        m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);
        // no output
        return new PortObjectSpec[]{};
    }

    // Make sure to overwrite the PortObjectSpec[] version!
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        // open the writer path accessor using try-with-resources
        try (final var accessor = m_settings.getWriterFileChooser().createWritePathAccessor()) {
            // get the path to write the table to
            final FSPath outputPath = accessor.getOutputPath(m_statusConsumer);
            // inform the user about potential problems
            m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);

            createParentDirectories(outputPath);
            checkOverwrite(outputPath);

            // open the output stream using FSFiles and the open options specified by the
            // file overwrite policy (FAIL, OVERWRITE, APPEND)
            try (final var os = FSFiles.newOutputStream(outputPath,
                m_settings.getWriterFileChooser().getFileOverwritePolicy().getOpenOptions())) {
                final BufferedDataTable table = (BufferedDataTable)inObjects[m_tableIdx];
                // TODO: write the table to os
            }
        }

        return new PortObject[]{};
    }

    private void createParentDirectories(final FSPath outpath) throws IOException {
        final FSPath parentPath = (FSPath)outpath.getParent();
        if ((parentPath != null) && !FSFiles.exists(parentPath)) {
            if (m_settings.getWriterFileChooser().isCreateMissingFolders()) {
                FSFiles.createDirectories(parentPath);
            } else {
                throw new IOException(String.format(
                    "The directory '%s' does not exist and must not be created due to user settings.", parentPath));
            }
        }
    }

    private void checkOverwrite(final FSPath outpath) throws IOException {
        final var fileOverwritePolicy = m_settings.getWriterFileChooser().getFileOverwritePolicy();
        if ((fileOverwritePolicy == FileOverwritePolicy.FAIL) && FSFiles.exists(outpath)) {
            throw new IOException(
                "Output file '" + outpath.toString() + "' exists and must not be overwritten due to user settings.");
        }
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveInModel(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.validateInModel(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadInModel(settings);
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
    protected void reset() {
        // nothing to do
    }
}
