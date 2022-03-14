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
 *   Jan 28, 2021 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.compress.table;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.data.location.FSLocationValue;
import org.knime.filehandling.core.port.FileSystemPortObject;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;
import org.knime.filehandling.core.util.FSLocationColumnUtils;
import org.knime.filehandling.core.util.FSMissingMetadataException;
import org.knime.filehandling.utility.nodes.compress.AbstractCompressNodeConfig;
import org.knime.filehandling.utility.nodes.compress.AbstractCompressNodeModel;
import org.knime.filehandling.utility.nodes.compress.iterator.CompressIterator;

/**
 * NodeModel of the "Compress Files/Folder (Table)" node.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class CompressTableNodeModel extends AbstractCompressNodeModel<CompressTableNodeConfig> {

    private final int m_inputTableIdx;

    private final int m_inputConnectionIdx;

    /**
     * Constructor.
     *
     * @param portsConfig the ports configuration
     * @param inputTableIdx the index of the input table
     */
    CompressTableNodeModel(final PortsConfiguration portsConfig, final int inputTableIdx) {
        super(portsConfig, new CompressTableNodeConfig(portsConfig));
        final Map<String, int[]> inputPortLocation = portsConfig.getInputPortLocation();
        m_inputTableIdx = inputTableIdx;
        m_inputConnectionIdx =
            Optional.ofNullable(inputPortLocation.get(AbstractCompressNodeConfig.CONNECTION_INPUT_FILE_PORT_GRP_NAME))//
                .map(a -> a[0])//
                .orElseGet(() -> -1);
    }

    @Override
    protected void doConfigure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        autoGuess(inSpecs);
        try {
            validateSettings(inSpecs);
        } catch (FSMissingMetadataException ex) {
            // AP-17965: ignore missing meta data
            setWarningMessage(ex.getMessage());
        }
    }

    private void autoGuess(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final SettingsModelString pathColModel = getConfig().getPathColModel();
        if (pathColModel.getStringValue() == null) {
            final DataTableSpec inputTableSpec = (DataTableSpec)inSpecs[m_inputTableIdx];
            getConfig().getPathColModel().setStringValue(inputTableSpec.stream()//
                .filter(dcs -> dcs.getType().isCompatible(FSLocationValue.class))//
                .map(DataColumnSpec::getName)//
                .findFirst()//
                .orElseThrow(() -> new InvalidSettingsException("No applicable column available"))//
            );
            setWarningMessage(
                String.format("Auto-guessed column containing file/folder paths '%s'", pathColModel.getStringValue()));
        }
    }

    private void validateSettings(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec inSpec = (DataTableSpec)inSpecs[m_inputTableIdx];
        validatePathColumn(inSpecs, inSpec);
        validateEntryNameColumn(inSpec);
    }

    private void validatePathColumn(final PortObjectSpec[] inSpecs, final DataTableSpec inSpec)
        throws InvalidSettingsException {
        final String pathCol = getConfig().getPathColModel().getStringValue();
        final int pathColIndex = inSpec.findColumnIndex(pathCol);

        // check column existence
        CheckUtils.checkSetting(pathColIndex >= 0, "The selected column '%s' is not part of the input", pathCol);

        // validate the selected column
        final DataColumnSpec pathColSpec = inSpec.getColumnSpec(pathColIndex);
        final Optional<String> warningMsg = FSLocationColumnUtils.validateFSLocationColumn(pathColSpec,
            m_inputConnectionIdx >= 0 ? (FileSystemPortObjectSpec)inSpecs[m_inputConnectionIdx] : null);
        warningMsg.ifPresent(this::setWarningMessage);
    }

    private void validateEntryNameColumn(final DataTableSpec inSpec) throws InvalidSettingsException {
        final TableTruncationSettings config = getConfig().getTruncationSettings();
        if (config.entryNameDefinedByTableColumn()) {
            final String entryNameCol = config.getEntryNameColModel().getStringValue();
            CheckUtils.checkSettingNotNull(entryNameCol, "Please select the column containing the archive entry names");
            final int entryNameColIndex = inSpec.findColumnIndex(entryNameCol);

            // check column existence
            CheckUtils.checkSetting(entryNameColIndex >= 0, "The selected column '%s' is not part of the input",
                entryNameCol);

            final DataType entryNameColType = inSpec.getColumnSpec(entryNameColIndex).getType();
            CheckUtils.checkSetting(
                entryNameColType.isCompatible(StringValue.class)
                    || entryNameColType.isCompatible(FSLocationValue.class),
                "The selected column '%s' has the wrong type.", entryNameCol);
        }
    }

    @SuppressWarnings("resource")
    @Override
    protected CompressIterator getFilesToCompress(final PortObject[] inData, final boolean includeEmptyFolders)
        throws IOException, InvalidSettingsException {
        final BufferedDataTable table = (BufferedDataTable)inData[m_inputTableIdx];
        final CompressTableNodeConfig config = getConfig();
        final TableTruncationSettings truncationSettings = config.getTruncationSettings();
        final int pathColIdx = table.getSpec().findColumnIndex(config.getPathColModel().getStringValue());
        final FSConnection fsConnection = getFSConnection(inData);
        if (truncationSettings.entryNameDefinedByTableColumn()) {
            return new MappedCompressTableIterator(table, pathColIdx, fsConnection, includeEmptyFolders,
                table.getSpec().findColumnIndex(truncationSettings.getEntryNameColModel().getStringValue()));
        } else {
            return new TruncatedCompressTableIterator(truncationSettings, table, pathColIdx, fsConnection,
                includeEmptyFolders);
        }
    }

    private FSConnection getFSConnection(final PortObject[] inData) {
        return m_inputConnectionIdx < 0 //
            ? null //
            : ((FileSystemPortObject)inData[0]).getFileSystemConnection().orElse(null);
    }

}
