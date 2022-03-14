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
 *   Mar 2, 2021 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.transfer.table;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.DefaultFSLocationSpec;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.data.location.FSLocationValue;
import org.knime.filehandling.core.port.FileSystemPortObject;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;
import org.knime.filehandling.core.util.FSLocationColumnUtils;
import org.knime.filehandling.core.util.FSMissingMetadataException;
import org.knime.filehandling.utility.nodes.transfer.AbstractTransferFilesNodeModel;
import org.knime.filehandling.utility.nodes.transfer.iterators.TransferIterator;

/**
 * The Transfer Files/Folder (Table) node model.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class TransferFilesTableNodeModel extends AbstractTransferFilesNodeModel<TransferFilesTableNodeConfig> {

    private final int m_inputTableIdx;

    private final int m_srcConnectionIdx;

    private final int m_destConnectionIdx;

    /**
     * Constructor.
     *
     * @param portsConfig the {@link PortsConfiguration}
     * @param config the {@link TransferFilesTableNodeConfig}
     * @param inputTableIdx the table input port index
     * @param srcConnectionIdx the source connection index, -1 if the port does not exist
     * @param destConnectionIdx the destination connection index, -1 if the port does not exist
     */
    TransferFilesTableNodeModel(final PortsConfiguration portsConfig, final TransferFilesTableNodeConfig config,
        final int inputTableIdx, final int srcConnectionIdx, final int destConnectionIdx) {
        super(portsConfig, config);
        m_inputTableIdx = inputTableIdx;
        m_srcConnectionIdx = srcConnectionIdx;
        m_destConnectionIdx = destConnectionIdx;
    }

    @Override
    protected PortObjectSpec[] doConfigure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        try {
            doConfigureInput(inSpecs);
            doConfigureOutput(inSpecs);

            getStatusConsumer().setWarningsIfRequired(this::setWarningMessage);
            return new PortObjectSpec[]{createOutputSpec(inSpecs)};

        } catch (FSMissingMetadataException ex) {// NOSONAR AP-17965 ignore missing metadata
            return new PortObjectSpec[]{null};
        }
    }

    private void doConfigureInput(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final SettingsModelString srcPathColModel = getConfig().getSrcPathColModel();
        if (srcPathColModel.getStringValue() == null) {
            autoGuess(inSpecs, srcPathColModel);
            setWarningMessage(String.format("Auto-guessed column containing input file/folder paths '%s'",
                srcPathColModel.getStringValue()));
        }
        validateSettings(inSpecs, srcPathColModel, m_srcConnectionIdx);
    }

    private void doConfigureOutput(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final SettingsModelString destPathColModel = getConfig().getDestPathColModel();
        if (getConfig().destDefinedByTableColumn()) {
            // No need for auto-guessing since this is false by default
            validateSettings(inSpecs, destPathColModel, m_destConnectionIdx);
        } else {
            getConfig().getDestinationFileChooserModel().configureInModel(inSpecs, getStatusConsumer());
        }
    }

    private void autoGuess(final PortObjectSpec[] inSpecs, final SettingsModelString pathColModel)
        throws InvalidSettingsException {
        final DataTableSpec inputTableSpec = (DataTableSpec)inSpecs[m_inputTableIdx];
        pathColModel.setStringValue(inputTableSpec.stream()//
            .filter(dcs -> dcs.getType().isCompatible(FSLocationValue.class))//
            .map(DataColumnSpec::getName)//
            .findFirst()//
            .orElseThrow(() -> new InvalidSettingsException("No applicable column available"))//
        );
    }

    private void validateSettings(final PortObjectSpec[] inSpecs, final SettingsModelString pathColModel,
        final int connectionIdx) throws InvalidSettingsException {
        final DataTableSpec inSpec = (DataTableSpec)inSpecs[m_inputTableIdx];
        final String pathColName = pathColModel.getStringValue();
        final int colIndex = inSpec.findColumnIndex(pathColName);

        // check column existence
        CheckUtils.checkSetting(colIndex >= 0, "The selected column '%s' is not part of the input", pathColName);

        // validate the selected column
        final DataColumnSpec pathColSpec = inSpec.getColumnSpec(colIndex);
        final Optional<String> warningMsg = FSLocationColumnUtils.validateFSLocationColumn(pathColSpec,
            connectionIdx >= 0 ? (FileSystemPortObjectSpec)inSpecs[connectionIdx] : null);
        warningMsg.ifPresent(this::setWarningMessage);
    }

    @Override
    protected Set<DefaultFSLocationSpec> getSrcLocationSpecs(final PortObjectSpec[] inSpecs) {
        return getLocationSpec(inSpecs, m_srcConnectionIdx, getConfig().getSrcPathColModel().getStringValue());
    }

    @Override
    protected Set<DefaultFSLocationSpec> getDestLocationSpecs(final PortObjectSpec[] inSpecs) {
        if (getConfig().destDefinedByTableColumn()) {
            return getLocationSpec(inSpecs, m_destConnectionIdx, getConfig().getDestPathColModel().getStringValue());
        } else {
            return super.getDestLocationSpecs(inSpecs);
        }
    }

    private Set<DefaultFSLocationSpec> getLocationSpec(final PortObjectSpec[] inSpecs, final int connectionIdx,
        final String colName) {
        if (connectionIdx >= 0) {
            final FSLocationSpec locationSpec = ((FileSystemPortObjectSpec)inSpecs[connectionIdx]).getFSLocationSpec();
            return Collections.singleton(new DefaultFSLocationSpec(locationSpec.getFSCategory(),
                locationSpec.getFileSystemSpecifier().orElseGet(() -> null)));
        } else {
            return getLocationSpecs(inSpecs, colName);
        }
    }

    private Set<DefaultFSLocationSpec> getLocationSpecs(final PortObjectSpec[] inSpecs, final String pathColName) {
        final DataTableSpec spec = (DataTableSpec)inSpecs[m_inputTableIdx];
        final DataColumnSpec fsLocationColSpec = spec.getColumnSpec(pathColName);
        return FSLocationColumnUtils.validateAndGetMetaData(fsLocationColSpec).getFSLocationSpecs();
    }

    @SuppressWarnings("resource")
    @Override
    protected TransferIterator getTransferIterator(final PortObject[] inObjects)
        throws IOException, InvalidSettingsException {
        final BufferedDataTable t = (BufferedDataTable)inObjects[m_inputTableIdx];
        final DataTableSpec spec = t.getSpec();
        final TransferFilesTableNodeConfig cfg = getConfig();
        final int srcColIdx = spec.findColumnIndex(cfg.getSrcPathColModel().getStringValue());
        final FSConnection srcConnection = getFSConnection(inObjects, m_srcConnectionIdx);
        if (cfg.destDefinedByTableColumn()) {
            final int destColIdx = spec.findColumnIndex(cfg.getDestPathColModel().getStringValue());
            return new TransferTableIterator(t, srcColIdx, destColIdx, srcConnection,
                getFSConnection(inObjects, m_destConnectionIdx));
        } else {
            return new TransferFileChooserIterator(cfg.getTruncationSettings(), t, srcColIdx, srcConnection,
                cfg.getDestinationFileChooserModel(), getStatusConsumer());
        }
    }

    private static FSConnection getFSConnection(final PortObject[] inObjects, final int connectionIdx) {
        return connectionIdx < 0 //
            ? null //
            : ((FileSystemPortObject)inObjects[connectionIdx]).getFileSystemConnection().orElse(null);
    }

}
