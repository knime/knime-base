/*
 * ------------------------------------------------------------------------
< *  Copyright by KNIME AG, Zurich, Switzerland
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
package org.knime.filehandling.utility.nodes.deletepaths.table;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.data.location.FSLocationValue;
import org.knime.filehandling.core.data.location.FSLocationValueMetaData;
import org.knime.filehandling.core.port.FileSystemPortObject;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;
import org.knime.filehandling.core.util.FSLocationColumnUtils;
import org.knime.filehandling.core.util.FSMissingMetadataException;
import org.knime.filehandling.utility.nodes.deletepaths.AbstractDeleteFilesAndFoldersNodeConfig;
import org.knime.filehandling.utility.nodes.deletepaths.AbstractDeleteFilesAndFoldersNodeModel;
import org.knime.filehandling.utility.nodes.deletepaths.DeleteFilesFolderIterator;

/**
 * Node model of the "Delete Files/Folders (Table based)" node.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 */
final class DeleteFilesAndFoldersTableNodeModel
    extends AbstractDeleteFilesAndFoldersNodeModel<DeleteFilesAndFoldersTableNodeConfig> {

    private final int m_dataTablePortIndex;

    private final int m_inputFSConnectionIndex;

    /**
     * Constructor.
     *
     * @param portsConfig this nodes ports configuration
     */
    DeleteFilesAndFoldersTableNodeModel(final PortsConfiguration portsConfig) {
        super(portsConfig, new DeleteFilesAndFoldersTableNodeConfig());
        final Map<String, int[]> inputPortLocation = portsConfig.getInputPortLocation();

        m_inputFSConnectionIndex = Optional
            .ofNullable(inputPortLocation.get(AbstractDeleteFilesAndFoldersNodeConfig.CONNECTION_INPUT_PORT_GRP_NAME))//
            .map(a -> a[0])//
            .orElse(-1);

        m_dataTablePortIndex =
            inputPortLocation.get(DeleteFilesAndFoldersTableNodeFactory.TABLE_INPUT_PORT_GRP_NAME)[0];
    }

    @Override
    protected PortObjectSpec[] doConfigure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final SettingsModelString pathCol = getConfig().getColumnSelection();
        if (pathCol.getStringValue() == null) {
            autoGuess(inSpecs);
            setWarningMessage(String.format("Auto-guessed column containing file/folder paths '%s'", pathCol.getStringValue()));
        }
        try {
            validateSettings(inSpecs, pathCol);
            getStatusConsumer().setWarningsIfRequired(this::setWarningMessage);
            return new PortObjectSpec[]{createOutputSpec(inSpecs)};

        } catch (FSMissingMetadataException ex) {
            // AP-17965: ignore missing meta data
            setWarningMessage(ex.getMessage());
            return new PortObjectSpec[]{null};
        }
    }

    /**
     * Validates the settings.
     *
     * @param inSpecs the {@link PortObjectSpec}
     * @param pathColModel the Path column {@link SettingsModelString}
     * @throws InvalidSettingsException
     */
    private void validateSettings(final PortObjectSpec[] inSpecs, final SettingsModelString pathCol)
        throws InvalidSettingsException {
        final DataTableSpec inSpec = (DataTableSpec)inSpecs[m_dataTablePortIndex];
        final String pathColName = pathCol.getStringValue();
        final int colIndex = inSpec.findColumnIndex(pathColName);

        // check column existence
        CheckUtils.checkSetting(colIndex >= 0, "The selected column '%s' is not part of the input", pathColName);

        // validate the selected column
        final DataColumnSpec pathColSpec = inSpec.getColumnSpec(colIndex);
        final Optional<String> warningMsg = FSLocationColumnUtils.validateFSLocationColumn(pathColSpec,
            m_inputFSConnectionIndex >= 0 ? (FileSystemPortObjectSpec)inSpecs[m_inputFSConnectionIndex] : null);
        warningMsg.ifPresent(this::setWarningMessage);
    }

    /**
     * Auto guesses the column with the {@link FSLocationValue}.
     *
     * @param inSpecs the {@link PortObjectSpec}
     * @throws InvalidSettingsException
     */
    private void autoGuess(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec inputTableSpec = (DataTableSpec)inSpecs[m_dataTablePortIndex];
        getConfig().getColumnSelection().setStringValue(inputTableSpec.stream()//
            .filter(dcs -> dcs.getType().isCompatible(FSLocationValue.class))//
            .map(DataColumnSpec::getName)//
            .findFirst()//
            .orElseThrow(() -> new InvalidSettingsException("No applicable column available"))//
        );
    }

    @SuppressWarnings("resource")
    @Override
    protected DeleteFilesFolderIterator getDeleteFilesFolderIterator(final PortObject[] inData)
        throws IOException, InvalidSettingsException {
        final BufferedDataTable table = (BufferedDataTable)inData[m_dataTablePortIndex];
        return new DeleteTableIterator(table,
            table.getSpec().findColumnIndex(getConfig().getColumnSelection().getStringValue()),
            getFSConnection(inData).orElse(null));
    }

    /**
     * Returns an {@link Optional} of a {@link FSConnection}.
     *
     * @param inData the {@link PortObject}
     * @return an {@link Optional} of a {@link FSConnection}
     */
    private Optional<FSConnection> getFSConnection(final PortObject[] inData) {
        return m_inputFSConnectionIndex < 0 //
            ? Optional.empty() //
            : ((FileSystemPortObject)inData[m_inputFSConnectionIndex]).getFileSystemConnection();
    }

    @Override
    protected FSLocationValueMetaData getFSLocationValueMetaData(final PortObjectSpec[] inSpecs) {
        FSLocationValueMetaData fsLocationMetaData;
        if (m_inputFSConnectionIndex >= 0) {
            final FileSystemPortObjectSpec fsPortObject = (FileSystemPortObjectSpec)inSpecs[m_inputFSConnectionIndex];
            final FSLocationSpec fsLocationSpec = fsPortObject.getFSLocationSpec();
            fsLocationMetaData = new FSLocationValueMetaData(fsLocationSpec.getFileSystemCategory(),
                fsLocationSpec.getFileSystemSpecifier().orElse(null));
        } else {
            final DataColumnSpec colSpec = ((DataTableSpec)inSpecs[m_dataTablePortIndex])
                .getColumnSpec(getConfig().getColumnSelection().getStringValue());
            fsLocationMetaData =
                colSpec.getMetaDataOfType(FSLocationValueMetaData.class).orElseThrow(() -> new IllegalStateException(
                    String.format("Path column '%s' without meta data encountered.", colSpec.getName())));
        }

        return fsLocationMetaData;
    }
}
