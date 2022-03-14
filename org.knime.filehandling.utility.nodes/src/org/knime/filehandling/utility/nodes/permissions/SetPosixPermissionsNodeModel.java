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
 *   Nov 10, 2021 (Alexander Bondaletov): created
 */
package org.knime.filehandling.utility.nodes.permissions;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
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
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.location.FSPathProvider;
import org.knime.filehandling.core.connections.location.MultiFSPathProviderFactory;
import org.knime.filehandling.core.connections.meta.FSDescriptorRegistry;
import org.knime.filehandling.core.connections.meta.FSType;
import org.knime.filehandling.core.data.location.FSLocationValue;
import org.knime.filehandling.core.data.location.FSLocationValueMetaData;
import org.knime.filehandling.core.defaultnodesettings.ExceptionUtil;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;
import org.knime.filehandling.core.util.FSLocationColumnUtils;
import org.knime.filehandling.core.util.FSMissingMetadataException;

/**
 * The node allows to assing POSIX permission for files and folders.
 *
 * @author Alexander Bondaletov
 */
public class SetPosixPermissionsNodeModel extends NodeModel {

    private static final String STATUS_COLUMN_NAME = "Status";

    private final int m_inputFsConnectionIdx;

    private final int m_inputTableIdx;

    private final SetPosixPermissionsNodeSettings m_settings;

    /**
     * @param config Ports configuration
     */
    protected SetPosixPermissionsNodeModel(final PortsConfiguration config) {
        super(config.getInputPorts(), config.getOutputPorts());
        final Map<String, int[]> inputPortLocation = config.getInputPortLocation();
        m_inputFsConnectionIdx =
            Optional.ofNullable(inputPortLocation.get(SetPosixPermissionsNodeFactory.CONNECTION_INPUT_PORT_GRP_NAME))//
                .map(a -> a[0])//
                .orElse(-1);
        m_inputTableIdx = inputPortLocation.get(SetPosixPermissionsNodeFactory.DATA_TABLE_INPUT_PORT_GRP_NAME)[0];
        m_settings = new SetPosixPermissionsNodeSettings();
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec inputTableSpec = ((DataTableSpec)inSpecs[m_inputTableIdx]);

        if (StringUtils.isBlank(m_settings.getColumn())) {
            autoGuess(inSpecs);
            setWarningMessage(
                String.format("Auto-guessed column containing file/folder paths '%s'", m_settings.getColumn()));
        }
        try {
            validateSettings(inSpecs);

            final int pathColIdx = inputTableSpec.findColumnIndex(m_settings.getColumn());
            try (final var fac = new SetPosixPermissionsFactory(createStatusColumn(inputTableSpec), pathColIdx,
                getFSConnection(inSpecs), false)) {
                return new PortObjectSpec[]{createColumnRearranger(inputTableSpec, fac).createSpec()};
            }
        } catch (FSMissingMetadataException ex) { // NOSONAR AP-17965 ignore missing metadata
            return new PortObjectSpec[]{null};
        }
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        final PortObjectSpec[] inSpecs = Arrays.stream(inObjects)//
            .map(PortObject::getSpec)//
            .toArray(PortObjectSpec[]::new);

        checkPosixCapabilities(inSpecs);

        final DataTableSpec inputTableSpec = (DataTableSpec)inSpecs[m_inputTableIdx];
        final int pathColIdx = inputTableSpec.findColumnIndex(m_settings.getColumn());

        final var fsConnection = getFSConnection(inSpecs);
        final var statusColumn = createStatusColumn(inputTableSpec);
        try (final var fac = new SetPosixPermissionsFactory(statusColumn, pathColIdx, fsConnection, true)) {
            return new PortObject[]{exec.createColumnRearrangeTable((BufferedDataTable)inObjects[m_inputTableIdx],
                createColumnRearranger(inputTableSpec, fac), exec)};
        }
    }

    private void validateSettings(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        m_settings.validate();

        final DataTableSpec inSpec = (DataTableSpec)inSpecs[m_inputTableIdx];
        final String pathColName = m_settings.getColumn();
        final int colIndex = inSpec.findColumnIndex(pathColName);

        // check column existence
        CheckUtils.checkSetting(colIndex >= 0, "The selected column '%s' is not part of the input", pathColName);

        // validate the selected column
        final DataColumnSpec pathColSpec = inSpec.getColumnSpec(colIndex);
        final Optional<String> warningMsg = FSLocationColumnUtils.validateFSLocationColumn(pathColSpec,
            m_inputFsConnectionIdx >= 0 ? (FileSystemPortObjectSpec)inSpecs[m_inputFsConnectionIdx] : null);
        warningMsg.ifPresent(this::setWarningMessage);

        checkPosixCapabilities(inSpecs);
    }

    private void checkPosixCapabilities(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final Set<FSType> fsTypesToCheck = new HashSet<>();
        if (m_inputFsConnectionIdx != -1) {
            fsTypesToCheck.add(((FileSystemPortObjectSpec)inSpecs[m_inputFsConnectionIdx]).getFSType());
        } else {
            final DataTableSpec inSpec = (DataTableSpec)inSpecs[m_inputTableIdx];
            final DataColumnSpec pathColSpec = inSpec.getColumnSpec(m_settings.getColumn());

            FSLocationValueMetaData metaData =
                    pathColSpec.getMetaDataOfType(FSLocationValueMetaData.class).orElseThrow(() -> new IllegalStateException(
                        String.format("Path column '%s' without meta data encountered.", m_settings.getColumn())));
            metaData.getFSLocationSpecs().stream() //
                .map(FSLocationSpec::getFSType) //
                .forEach(fsTypesToCheck::add);
        }

        var fsWithoutPosix = fsTypesToCheck.stream() //
            .filter(t -> !FSDescriptorRegistry.getFSDescriptor(t) //
                .orElseThrow(() -> new IllegalStateException("File system not found: " + t.getName())) //
                .getCapabilities() //
                .canSetPosixAttributes()) //
            .findFirst();

        if (!fsWithoutPosix.isEmpty() && m_settings.failIfSetPermissionsFails()) {
            if (m_inputFsConnectionIdx != -1) {
                throw new IllegalStateException("The connected file system does not support setting POSIX permissions");
            } else {
                throw new IllegalStateException(String.format("The file system %s does not support setting POSIX permissions",
                    fsWithoutPosix.orElseThrow().getName()));
            }
        }
    }

    private void autoGuess(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec inputTableSpec = (DataTableSpec)inSpecs[m_inputTableIdx];
        m_settings.getSelectedColumnModel().setStringValue(inputTableSpec.stream()//
            .filter(dcs -> dcs.getType().isCompatible(FSLocationValue.class))//
            .map(DataColumnSpec::getName)//
            .findFirst()//
            .orElseThrow(() -> new InvalidSettingsException("No Path column available"))//
        );
    }

    private static DataColumnSpec createStatusColumn(final DataTableSpec inputTableSpec) {
        final var uniqueNameGen = new UniqueNameGenerator(inputTableSpec);
        return uniqueNameGen.newColumn(STATUS_COLUMN_NAME, StringCell.TYPE);
    }

    private static ColumnRearranger createColumnRearranger(final DataTableSpec inputTableSpec,
        final SetPosixPermissionsFactory fac) {
        final var colRearranger = new ColumnRearranger(inputTableSpec);
        colRearranger.append(fac);
        return colRearranger;
    }

    private FSConnection getFSConnection(final PortObjectSpec[] inSpecs) {
        return m_inputFsConnectionIdx < 0 //
            ? null //
            : ((FileSystemPortObjectSpec)inSpecs[0]).getFileSystemConnection().orElse(null);
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettingsFrom(settings);
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no internals
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        //no internals
    }

    @Override
    protected void reset() {
        // nothing to do
    }

    private final class SetPosixPermissionsFactory extends SingleCellFactory implements Closeable {

        private final int m_colIdx;

        private final MultiFSPathProviderFactory m_multiFSPathProviderCellFactory;

        @SuppressWarnings("resource")
        private SetPosixPermissionsFactory(final DataColumnSpec colSpecs, //
            final int pathColIdx, //
            final FSConnection fsConnection, //
            final boolean initFactory) {

            super(colSpecs);

            m_colIdx = pathColIdx;
            m_multiFSPathProviderCellFactory = initFactory ? new MultiFSPathProviderFactory(fsConnection) : null;
        }

        @Override
        public DataCell getCell(final DataRow row) {
            final DataCell c = row.getCell(m_colIdx);
            if (c.isMissing()) {
                return DataType.getMissingCell();
            } else {
                return createCell((FSLocationValue)c);
            }
        }

        private DataCell createCell(final FSLocationValue cell) {
            try (final FSPathProvider pathProvder = m_multiFSPathProviderCellFactory
                    .getOrCreateFSPathProviderFactory(cell.getFSLocation()).create(cell.getFSLocation())) {

                return createCell(pathProvder.getPath());

            } catch (final NoSuchFileException e) { //NOSONAR
                throw new IllegalArgumentException(String.format("The file/folder '%s' does not exist", e.getMessage()),
                    e);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }

        private DataCell createCell(final FSPath path) {
            var status = "Success";
            try {
                Files.setPosixFilePermissions(path, m_settings.getPermissions());
            } catch (NoSuchFileException e ) {
                if (m_settings.failIfFileDoesExist()) {
                    throw new IllegalArgumentException(
                        String.format("The file/folder '%s' does not exist", path.toString()), e);
                } else {
                    status = "File/folder does not exist";
                }
            } catch (IOException e) {
                if (m_settings.failIfSetPermissionsFails()) {
                    final var formattedException = ExceptionUtil.wrapIOException(e);
                    throw new IllegalArgumentException(String.format("Failed to set permissions on %s: %s", //
                        path.toString(), //
                        formattedException.getMessage()), formattedException);
                } else {
                    status = "Failed: " + e.getMessage();
                }
            } catch (UnsupportedOperationException e) {
                if(m_settings.failIfSetPermissionsFails()) {
                    throw e;
                } else {
                    status = "Failed: The file system does not support setting POSIX permissions";
                }
            }
            return new StringCell(status);
        }

        @Override
        public void close() {
            if (m_multiFSPathProviderCellFactory != null) {
                m_multiFSPathProviderCellFactory.close();
            }
        }
    }
}
