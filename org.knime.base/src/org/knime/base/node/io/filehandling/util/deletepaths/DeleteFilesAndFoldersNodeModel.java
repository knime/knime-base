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
 *
 * History
 *   Aug 3, 2020 (Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.util.deletepaths;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.BufferedDataTableRowOutput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.data.location.FSLocationValueMetaData;
import org.knime.filehandling.core.data.location.cell.FSLocationCellFactory;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.ReadPathAccessor;
import org.knime.filehandling.core.defaultnodesettings.status.NodeModelStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;

/**
 * Node model of the "Delete Files/Folders" node.
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 */
final class DeleteFilesAndFoldersNodeModel extends NodeModel {

    private static final int LOCATION_CELL_IDX = 0;

    private static final int SUCCESS_CELL_IDX = 1;

    private final DeleteFilesAndFoldersNodeConfig m_config;

    private final NodeModelStatusConsumer m_statusConsumer;

    /**
     *
     * @param portsConfig this nodes ports configuration
     */
    DeleteFilesAndFoldersNodeModel(final PortsConfiguration portsConfig) {
        super(portsConfig.getInputPorts(), portsConfig.getOutputPorts());
        m_statusConsumer = new NodeModelStatusConsumer(EnumSet.of(MessageType.ERROR, MessageType.INFO));
        m_config = new DeleteFilesAndFoldersNodeConfig(portsConfig);
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        final BufferedDataTableRowOutput rowOutput =
            new BufferedDataTableRowOutput(exec.createDataContainer(createOutputSpec()));
        writeOutput(rowOutput, exec);
        return new PortObject[]{rowOutput.getDataTable()};
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        m_config.getFileChooserSettings().configureInModel(inSpecs, m_statusConsumer);
        m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);
        return new PortObjectSpec[]{createOutputSpec()};
    }

    private DataTableSpec createOutputSpec() {
        final DataColumnSpecCreator colCreator = new DataColumnSpecCreator("Path", FSLocationCellFactory.TYPE);
        final FSLocation location = m_config.getFileChooserSettings().getLocation();
        final FSLocationValueMetaData metaData = new FSLocationValueMetaData(location.getFileSystemCategory(),
            location.getFileSystemSpecifier().orElse(null));
        colCreator.addMetaData(metaData, true);

        if (!m_config.isAbortedIfFails()) {
            return new DataTableSpec(colCreator.createSpec(),
                new DataColumnSpecCreator("Deleted successfully", BooleanCellFactory.TYPE).createSpec());
        } else {
            return new DataTableSpec(colCreator.createSpec());
        }
    }

    private void writeOutput(final RowOutput rowOutput, final ExecutionContext exec)
        throws IOException, InvalidSettingsException, InterruptedException, CanceledExecutionException {
        final FileStoreFactory fsFac = FileStoreFactory.createFileStoreFactory(exec);
        try (final ReadPathAccessor accessor = m_config.getFileChooserSettings().createReadPathAccessor();) {
            final List<FSPath> fsPaths = accessor.getFSPaths(m_statusConsumer);
            m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);
            long rec = 0;
            final int numEntries = fsPaths.size();
            final FSLocationCellFactory locationFactory =
                new FSLocationCellFactory(fsFac, m_config.getFileChooserSettings().getLocation());

            final DataCell[] row = new DataCell[m_config.isAbortedIfFails() ? 1 : 2];

            for (final FSPath p : fsPaths) {
                boolean successfullyDeleted;
                try {
                    walkFileTree(p);
                    successfullyDeleted = true;
                } catch (IOException e) {
                    if (m_config.isAbortedIfFails()) {
                        throw new IOException(String.format(
                            "The file/directory '%s' couldn't be deleted and the execution was aborted due to user "
                                + "settings.",
                            p), e);
                    } else {
                        successfullyDeleted = false;
                    }
                }

                row[LOCATION_CELL_IDX] = locationFactory.createCell(p.toFSLocation());
                if (!m_config.isAbortedIfFails()) {
                    row[SUCCESS_CELL_IDX] = BooleanCellFactory.create(successfullyDeleted);
                }
                rowOutput.push(new DefaultRow(RowKey.createRowKey(rec), row));

                ++rec;
                final long curEntry = rec;
                exec.checkCanceled();
                exec.setProgress(rec / (double)numEntries,
                    () -> String.format("Processing entry %d out of %d", curEntry, numEntries));
            }
        } finally {
            rowOutput.close();
            fsFac.close();
        }
    }

    private static void walkFileTree(final Path p) throws IOException {
        Files.walkFileTree(p, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_config.saveSettingsForModel(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config.validateSettingsForModel(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config.loadSettingsForModel(settings);
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // not used
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // not used
    }

    @Override
    protected void reset() {
        // not used
    }
}
