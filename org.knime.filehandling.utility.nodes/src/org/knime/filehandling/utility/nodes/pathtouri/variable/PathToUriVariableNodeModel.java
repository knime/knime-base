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
 *   Jan 10, 2023 (Zkriya Rakhimberdiyev): created
 */
package org.knime.filehandling.utility.nodes.pathtouri.variable;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.NoSuchFileException;
import java.util.EnumSet;
import java.util.Map.Entry;

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
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableTypeRegistry;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.location.FSPathProvider;
import org.knime.filehandling.core.connections.location.MultiFSPathProviderFactory;
import org.knime.filehandling.core.connections.uriexport.URIExporter;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.status.NodeModelStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;

/**
 * The node model allowing to convert path flow variables to URIs.
 *
 * @author Zkriya Rakhimberdiyev
 */
final class PathToUriVariableNodeModel extends NodeModel {

    private final PathToUriVariableNodeConfig m_config;

    private final URIExporterModelHelper m_modelHelper;

    private final NodeModelStatusConsumer m_statusConsumer =
            new NodeModelStatusConsumer(EnumSet.of(MessageType.WARNING));

    PathToUriVariableNodeModel(final PortsConfiguration portsConfig) {
        super(portsConfig.getInputPorts(), portsConfig.getOutputPorts());
        m_config = new PathToUriVariableNodeConfig(portsConfig,
            () -> getAvailableFlowVariables(FSLocationVariableType.INSTANCE));
        m_modelHelper = m_config.getExporterModelHelper();
    }

    @Override
    @SuppressWarnings("resource") // the FSPathProviderFactorys are closed by the MultiFSPathProviderCellFactory
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final var nameGenerator = new UniqueNameGenerator(
            getAvailableFlowVariables(VariableTypeRegistry.getInstance().getAllTypes()).keySet());

        final var fsConnection =
                FileSystemPortObjectSpec.getFileSystemConnection(inSpecs,
                    m_config.getFileSystemConnectionPortIndex()).orElse(null);

        m_modelHelper.setPortObjectSpecs(inSpecs);
        m_modelHelper.validate(m_statusConsumer, false);
        m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);

        for (final Entry<String, FlowVariable> entry : m_config.getFilteredFlowVariables().entrySet()) {
            final var fsLocation = entry.getValue().getValue(FSLocationVariableType.INSTANCE);

            try (final var multiPathProviderFactory = new MultiFSPathProviderFactory(fsConnection);
                    final FSPathProvider pathProvider =
                            multiPathProviderFactory.getOrCreateFSPathProviderFactory(fsLocation).create(fsLocation)) {

                final var fsPath = pathProvider.getPath();
                if (m_config.getFailIfPathNotExistsModel().getBooleanValue() && !FSFiles.exists(fsPath)) {
                    throw new UncheckedIOException(String.format("The path '%s' does not exist.", fsPath),
                        new NoSuchFileException(fsPath.toString()));
                }

                final var value = convertPathToURI(pathProvider.getFSConnection(), fsPath);
                final var name = nameGenerator.newName(entry.getKey() + m_config.getVariableSuffixModel().getStringValue());

                pushFlowVariableString(name, value);
            } catch (URISyntaxException ex) {
                throw new UrlConversionException(
                    String.format("Failed convert %s to a URL: %s", fsLocation.toString(), ex.getMessage()), ex);
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }
        return new PortObjectSpec[]{FlowVariablePortObjectSpec.INSTANCE};
    }

    @SuppressWarnings("serial")
    static class UrlConversionException extends RuntimeException {
        UrlConversionException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    private String convertPathToURI(final FSConnection fsConnection, final FSPath path)
        throws URISyntaxException, InvalidSettingsException {

        final URIExporter exporter = m_modelHelper.createExporter(fsConnection);
        return exporter.toUri(path).toString();
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) {
        // do nothing since configure already pushed the new variables
        return new PortObject[]{FlowVariablePortObject.INSTANCE};
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Nothing to do here
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Nothing to do here
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
        m_config.loadValidatedSettingsForModel(settings);
    }

    @Override
    protected void reset() {
        // Nothing to do here
    }
}
