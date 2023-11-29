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
 *   Mar 5, 2020 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.utility.nodes.transfer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEException;
import org.knime.core.node.KNIMEException.KNIMERuntimeException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.connections.DefaultFSLocationSpec;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.data.location.FSLocationValueMetaData;
import org.knime.filehandling.core.data.location.cell.SimpleFSLocationCellFactory;
import org.knime.filehandling.core.defaultnodesettings.status.NodeModelStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;
import org.knime.filehandling.utility.nodes.transfer.iterators.TransferEntry;
import org.knime.filehandling.utility.nodes.transfer.iterators.TransferIterator;

/**
 * Abstract node model of the Transfer Files/Folders node.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 *
 * @param <T> an instance of {@link AbstractTransferFilesNodeConfig}
 */
public abstract class AbstractTransferFilesNodeModel<T extends AbstractTransferFilesNodeConfig> extends NodeModel {

    private final T m_config;

    private final NodeModelStatusConsumer m_statusConsumer =
        new NodeModelStatusConsumer(EnumSet.of(MessageType.ERROR, MessageType.WARNING));

    /**
     * Constructor.
     *
     * @param portsConfig the {@link PortsConfiguration}
     * @param config the {@link AbstractTransferFilesNodeConfig}
     */
    protected AbstractTransferFilesNodeModel(final PortsConfiguration portsConfig, final T config) {
        super(portsConfig.getInputPorts(), portsConfig.getOutputPorts());
        m_config = config;
    }

    /**
     * Returns the configuration.
     *
     * @return the configuration
     */
    protected final T getConfig() {
        return m_config;
    }

    /**
     * Returns the status message consumer.
     *
     * @return the status message consumer
     */
    protected final NodeModelStatusConsumer getStatusConsumer() {
        return m_statusConsumer;
    }

    @Override
    protected final PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return doConfigure(inSpecs);
    }

    /**
     * Configures the concrete node model instance.
     *
     * @param inSpecs the input specs
     * @return the output objects specs or null
     * @throws InvalidSettingsException - If something goes wrong during configuration
     */
    protected abstract PortObjectSpec[] doConfigure(PortObjectSpec[] inSpecs) throws InvalidSettingsException;

    /**
     * Creates the output spec of the output of the node.
     *
     * @param inSpecs The input data table specs
     * @return the output {@link DataTableSpec}
     */
    protected final DataTableSpec createOutputSpec(final PortObjectSpec[] inSpecs) {
        final ArrayList<DataColumnSpec> columnSpecs = new ArrayList<>();
        columnSpecs.add(createMetaColumnSpec(getSrcLocationSpecs(inSpecs), "Source Path"));
        columnSpecs.add(createMetaColumnSpec(getDestLocationSpecs(inSpecs), "Destination Path"));
        columnSpecs.add(new DataColumnSpecCreator("Directory", BooleanCell.TYPE).createSpec());
        columnSpecs.add(new DataColumnSpecCreator("Status", StringCell.TYPE).createSpec());

        final boolean deleteSourceFiles = m_config.getDeleteSourceFilesModel().getBooleanValue();
        if (deleteSourceFiles && !m_config.getFailOnDeletionModel().getBooleanValue()) {
            columnSpecs.add(new DataColumnSpecCreator("Source deleted", BooleanCell.TYPE).createSpec());
        }
        if (!m_config.failIfSourceDoesNotExist()) {
            final String specName;
            if (deleteSourceFiles) {
                specName = "Source existed";
            } else {
                specName = "Source exists";
            }
            columnSpecs.add(new DataColumnSpecCreator(specName, BooleanCell.TYPE).createSpec());
        }
        return new DataTableSpec(columnSpecs.toArray(new DataColumnSpec[0]));
    }

    /**
     * Returns the source location's {@link FSLocationSpec}.
     *
     * @param inSpecs the input specs
     * @return the source location's specs
     */
    protected abstract Set<DefaultFSLocationSpec> getSrcLocationSpecs(final PortObjectSpec[] inSpecs);

    /**
     * Returns the target location's {@link FSLocationSpec}.
     *
     * @param inSpecs the input specs
     * @return the target location's specs
     */
    protected Set<DefaultFSLocationSpec> getDestLocationSpecs(final PortObjectSpec[] inSpecs) {
        return getLocationSpecs(getConfig().getDestinationFileChooserModel().getLocation());
    }

    /**
     * Convenience method that converts an {@link FSLocation} to an {@link Set} of {@link DefaultFSLocationSpec}s.
     *
     * @param location the location to convert into a {@link DefaultFSLocationSpec}
     * @return the converted {@link FSLocation}
     */
    protected static final Set<DefaultFSLocationSpec> getLocationSpecs(final FSLocation location) {
        return Collections.singleton(new DefaultFSLocationSpec(location.getFileSystemCategory(),
            location.getFileSystemSpecifier().orElseGet(() -> null)));
    }

    /**
     * Creates a {@link DataColumnSpec} for the source or destination path which includes the meta data.
     *
     * @param sourceLocationSpecs the location spec specifying the file system
     * @param columnName the name of the column
     * @return the {@link DataColumnSpec}
     */
    private static DataColumnSpec createMetaColumnSpec(final Set<DefaultFSLocationSpec> locationSpecs,
        final String columnName) {
        final FSLocationValueMetaData metaData = new FSLocationValueMetaData(locationSpecs);
        final DataColumnSpecCreator fsLocationSpec =
            new DataColumnSpecCreator(columnName, SimpleFSLocationCellFactory.TYPE);
        fsLocationSpec.addMetaData(metaData, true);
        return fsLocationSpec.createSpec();
    }

    @Override
    protected final PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        // Create container for output table
        final DataTableSpec outputSpec = createOutputSpec(Arrays.stream(inObjects)//
            .map(PortObject::getSpec)//
            .toArray(PortObjectSpec[]::new));
        final BufferedDataContainer container = exec.createDataContainer(outputSpec);
        final PathCopier2 pathCopier = new PathCopier2(m_config.getTransferPolicy(),
            m_config.getVerboseOutputModel().getBooleanValue(), m_config.getDeleteSourceFilesModel().getBooleanValue(),
            m_config.getFailOnDeletionModel().getBooleanValue(), m_config.failIfSourceDoesNotExist());
        try (final TransferIterator iter = getTransferIterator(inObjects)) {
            final long numOfFiles = iter.size();
            long rowIdx = 0;
            final double maxProg = 1d / numOfFiles;
            while (iter.hasNext()) {
                exec.checkCanceled();
                final ExecutionContext subExec = exec.createSubExecutionContext(maxProg);
                rowIdx = transfer(subExec, container, rowIdx, pathCopier, iter.next());
            }
            container.close();
            return new PortObject[]{container.getTable()};
        } catch (final Exception e) {
            container.close();
            throw (e);
        }
    }

    /**
     * Returns an instance of {@link TransferIterator} that specifies the files/folders to transfer.
     *
     * @param inObjects the input objects
     * @return an instance of {@link TransferIterator}
     * @throws IOException - If anything went wrong while creating the iterator
     * @throws InvalidSettingsException - If the settings don't allow for the creation of the iterator
     */
    protected abstract TransferIterator getTransferIterator(final PortObject[] inObjects)
        throws IOException, InvalidSettingsException;

    private long transfer(final ExecutionContext exec, final DataContainer container, long rowIdx,
        final PathCopier2 pathCopier, final TransferEntry entry)
        throws IOException, CanceledExecutionException, InvalidSettingsException, KNIMERuntimeException {
        try {
            entry.validate();
            final DataCell[][] rows = pathCopier.transfer(exec, entry);
            for (final DataCell[] row : rows) {
                container.addRowToTable(new DefaultRow(RowKey.createRowKey(rowIdx), row));
                rowIdx++;
            }
        } catch (final IOException ioe) {
            throw wrapTransferException(ioe, rowIdx).orElseThrow(() -> ioe).toUnchecked();
        }
        return rowIdx;
    }

    /**
     * By default, this method does not wrap he provided exception. The method can be overridden to add additional
     * information (like row indices etc...) to the exception message. See AP-21591.
     *
     * @param ioe The raised IOException
     * @param rowIdx The row index where the exception was thrown.
     * @return {@link Optional#empty()} if no additional information is added to the exception,
     *         {@link Optional#of(KNIMEException)} otherwise.
     */
    protected Optional<KNIMEException> wrapTransferException(final IOException ioe, final long rowIdx) {
        return Optional.empty();
    }

    @Override
    protected final void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) {
        // nothing to do
    }

    @Override
    protected final void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) {
        // nothing to do
    }

    @Override
    protected final void saveSettingsTo(final NodeSettingsWO settings) {
        m_config.saveSettingsForModel(settings);
    }

    @Override
    protected final void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config.validateSettingsForModel(settings);
    }

    @Override
    protected final void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config.loadSettingsInModel(settings);
    }

    @Override
    protected final void reset() {
        // nothing to do
    }

}
